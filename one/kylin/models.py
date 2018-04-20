import itertools
import logging
import sqlparse
import json
import datetime
from past.builtins import basestring

import pandas as pd
import numpy

from sqlalchemy import (
    Column, Integer, String, ForeignKey, Text, Boolean,
    DateTime, JSON, TIMESTAMP
)
import sqlalchemy as sa
from sqlalchemy import asc, and_, desc, select, or_, MetaData, Table
from sqlalchemy.sql.expression import TextAsFrom
from sqlalchemy.orm import backref, relationship
from sqlalchemy.sql import table, literal_column, text, column, join
from sqlalchemy.engine.url import make_url
from sqlalchemy import create_engine
from sqlalchemy.pool import NullPool

from flask import escape, g, Markup
from flask_appbuilder import Model
from flask_babel import lazy_gettext as _

from superset import db, utils, import_util, sm
from superset.connectors.base.models import BaseDatasource, BaseColumn, BaseMetric
from superset.utils import DTTM_ALIAS, QueryStatus
from superset.models.helpers import QueryResult
from superset.models.core import Database
from superset.jinja_context import get_template_processor
from superset.models.helpers import set_perm

from superset import app, db, db_engine_specs, utils, sm
import six

import kylinpy


class KylinColumn(Model, BaseColumn):

    __tablename__ = 'kylin_columns'

    datasource_id = Column(Integer, ForeignKey('kylin_datasources.id'))
    datasource = relationship(
        'KylinDatasource',
        backref=backref('columns', cascade='all, delete-orphan'),
        foreign_keys=[datasource_id])

    def __repr__(self):
        return self.column_name

    @property
    def expression(self):
        pass

    @property
    def sqla_col(self):
        name = self.column_name
        if not self.expression:
            col = column(self.column_name).label(name)
        else:
            col = literal_column(self.expression).label(name)
        return col


class KylinMetric(Model, BaseMetric):

    __tablename__ = 'kylin_metrics'

    datasource_id = Column(Integer, ForeignKey('kylin_datasources.id'))
    datasource = relationship(
        'KylinDatasource',
        backref=backref('metrics', cascade='all, delete-orphan'),
        foreign_keys=[datasource_id])
    expression = Column(Text)

    @property
    def sqla_col(self):
        name = self.metric_name
        return literal_column(self.expression).label(name)

    @property
    def perm(self):
        pass


class KylinProject(Model):

    __tablename__ = 'kylin_projects'
    type = "kylin"

    id = Column(Integer, primary_key=True)
    project_name = Column(String(255), nullable=False)
    description = Column(String(255), nullable=True)
    uuid = Column(String(40), nullable=False)
    active = Column(Boolean, nullable=False, default=True)
    last_modified = Column(String(20), nullable=False)
    chunk = Column(Text, nullable=False)
    cache_timeout = 0

    def __repr__(self):
        return self.project_name

    @property
    def backend(self):
        url = make_url(self.sqlalchemy_uri_decrypted)
        return url.get_backend_name()

    @property
    def data(self):
        return {
            'name': self.project_name,
            'backend': 'kylin',
        }

    @property
    def sqlalchemy_uri_decrypted(self):
        # conn = sqla.engine.url.make_url(self.sqlalchemy_uri)
        # if self.custom_password_store:
        #     conn.password = self.custom_password_store(conn)
        # else:
        #     conn.password = self.password
        # todo
        return str(self.kylin_client)

    def get_extra(self):
        extra = {}
        if self.extra:
            try:
                extra = json.loads(self.extra)
            except Exception as e:
                logging.error(e)
        return extra

    extra = {}

    def get_sqla_engine(self, schema=None, nullpool=False, user_name=None):
        extra = self.get_extra()
        uri = make_url(self.sqlalchemy_uri_decrypted)
        params = extra.get('engine_params', {})
        if nullpool:
            params['poolclass'] = NullPool
        uri = self.db_engine_spec.adjust_database_uri(uri, schema)
        # if self.impersonate_user:
        #     uri.username = user_name if user_name else g.user.username
        return create_engine(uri, **params)

    @property
    def db_engine_spec(self):
        return db_engine_specs.engines.get(
            self.backend, db_engine_specs.BaseEngineSpec)

    @property
    def kylin_client(self):
        _client = kylinpy.Kylinpy(
            host=app.config.get('KAP_HOST'),
            port=app.config.get('KAP_PORT'),
            username=app.config.get('KAP_ADMIN'),
            password=app.config.get('KAP_PASSWORD'),
            project=self.project_name,
            version='v2'
        )
        return _client

    @classmethod
    def create_kylin_project(cls, project):
        session = db.session
        session.add(KylinProject(
            project_name=project['name'],
            description=project['description'],
            uuid=project['uuid'],
            active=True,
            last_modified=project['last_modified'],
            chunk=json.dumps(project)
        ))
        session.commit()

    def refresh_kylin_project(self, project):
        self.last_modified = project['last_modified']
        self.chunk = json.dumps(project)
        db.session.commit()

    def inaction(self):
        self.active = False
        db.session.commit()

    def fetch_cubes(self):
        cubes = [c for c in self.kylin_client.cubes().get('data') if c['status'] == 'READY']

        return [{
            'cube': cube,
            'measures': self.kylin_client.cube_desc(cube['name']),
            'sql': self.kylin_client.cube_sql(cube['name'])['data']['sql'].replace('DEFAULT.', '')
        } for cube in cubes]

    def fetch_cube_columns(self, cube_name):
        return self.kylin_client.get_cube_columns(cube_name).get('data')

    def fetch_cube_metrics(self, cube_name):
        _measure = self.kylin_client.get_cube_measures(cube_name).get('data')
        return [
            e for e in _measure if e['function']['expression'] in app.config.get('KAP_SUPPORT_METRICS')
        ]

    def sync_datasource(self):
        datasources = []
        columns = []
        metrics = []
        all_cubes = self.fetch_cubes()

        for cube in all_cubes:
            datasources.append(KylinDatasource(
                project_id=self.id,
                datasource_name=cube['cube']['name'],
                uuid=cube['cube']['uuid'],
                chunk=json.dumps(cube['cube']),
                sql=cube['sql'],
            ))

        for ds in datasources:
            for col in self.fetch_cube_columns(ds.datasource_name):
                columns.append(KylinColumn(
                    datasource=ds,
                    column_name=col['column_NAME'],
                    type=col['datatype'],
                    groupby=True
                ))

            for metric in self.fetch_cube_metrics(ds.datasource_name):
                metrics.append(KylinMetric(
                    datasource=ds,
                    metric_name=metric['name'],
                    verbose_name=metric['name'],
                    metric_type=metric['function']['expression'],
                    expression="{}({})".format(
                        metric['function']['expression'],
                        metric['function']['parameter']['value'].replace('.', '_')
                    )
                ))

        session = db.session
        for _ in itertools.chain(datasources, columns, metrics):
            session.add(_)

        session.commit()


class KylinDatasource(Model, BaseDatasource):

    __tablename__ = 'kylin_datasources'

    type = "kylin"
    query_language = 'sql'
    column_class = KylinColumn
    metric_class = KylinMetric
    project_class = KylinProject

    datasource_name = Column(String(255), unique=True)
    uuid = Column(String(40), nullable=False)
    last_modified = Column(TIMESTAMP, nullable=False)
    active = Column(Boolean, default=True)
    project_id = Column(Integer, ForeignKey('kylin_projects.id'), nullable=False)
    project = relationship(
        'KylinProject',
        backref=backref('kylin_datasources', cascade='all, delete-orphan'),
        foreign_keys=[project_id]
    )
    chunk = Column(Text)
    sql = Column(Text)

    export_fields = (
        'datasource_name', 'main_dttm_col', 'description', 'default_endpoint',
        'database_id', 'offset', 'cache_timeout', 'schema',
        'sql', 'params')

    baselink = "kylindatasourcemodelview"

    def __repr__(self):
        return self.name

    @property
    def database(self):
        return self.project

    @property
    def connection(self):
        return str(self.database)

    @property
    def name(self):
        if not self.schema:
            return self.datasource_name
        return "{}.{}".format(self.schema, self.datasource_name)

    @property
    def cube(self):
        name = escape(self.datasource_name)
        return Markup(
            '<a href="{self.explore_url}">{name}</a>'.format(**locals()))

    @property
    def data(self):
        return super(KylinDatasource, self).data

    @property
    def link(self):
        name = escape(self.name)
        return Markup(
            '<a href="{self.explore_url}">{name}</a>'.format(**locals()))

    def values_for_column(self, column_name, limit=10000):
        pass

    def get_from_clause(self, template_processor=None, db_engine_spec=None):
        if self.sql:
            from_sql = self.sql
            if template_processor:
                from_sql = template_processor.process_template(from_sql)
            if db_engine_spec:
                from_sql = db_engine_spec.escape_sql(from_sql)
            return TextAsFrom(sa.text(from_sql), []).alias('expr_qry')
        return self.get_sqla_table()

    def get_template_processor(self, **kwargs):
        return get_template_processor(
            table=self, database=self.database, **kwargs)

    def get_sqla_query(  # sqla
            self,
            groupby, metrics,
            granularity,
            from_dttm, to_dttm,
            filter=None,  # noqa
            is_timeseries=True,
            timeseries_limit=15,
            timeseries_limit_metric=None,
            row_limit=None,
            inner_from_dttm=None,
            inner_to_dttm=None,
            orderby=None,
            extras=None,
            columns=None,
            form_data=None,
            order_desc=True,
            prequeries=None,
            is_prequery=False,
        ):
        """Querying any sqla table from this common interface"""
        template_kwargs = {
            'from_dttm': from_dttm,
            'groupby': groupby,
            'metrics': metrics,
            'row_limit': row_limit,
            'to_dttm': to_dttm,
            'form_data': form_data,
        }
        template_processor = self.get_template_processor(**template_kwargs)
        db_engine_spec = self.database.db_engine_spec

        orderby = orderby or []

        # For backward compatibility
        if granularity not in self.dttm_cols:
            granularity = self.main_dttm_col

        # Database spec supports join-free timeslot grouping
        time_groupby_inline = db_engine_spec.time_groupby_inline

        cols = {col.column_name: col for col in self.columns}
        metrics_dict = {m.metric_name: m for m in self.metrics}

        if not granularity and is_timeseries:
            raise Exception(_(
                "Datetime column not provided as part table configuration "
                "and is required by this type of chart"))
        if not groupby and not metrics and not columns:
            raise Exception(_("Empty query?"))
        for m in metrics:
            if m not in metrics_dict:
                raise Exception(_("Metric '{}' is not valid".format(m)))
        metrics_exprs = [metrics_dict.get(m).sqla_col for m in metrics]
        if metrics_exprs:
            main_metric_expr = metrics_exprs[0]
        else:
            main_metric_expr = literal_column("COUNT(*)").label("ccount")

        select_exprs = []
        groupby_exprs = []

        if groupby:
            select_exprs = []
            inner_select_exprs = []
            inner_groupby_exprs = []
            for s in groupby:
                col = cols[s]
                outer = col.sqla_col
                inner = col.sqla_col.label(col.column_name + '__')

                groupby_exprs.append(outer)
                select_exprs.append(outer)
                inner_groupby_exprs.append(inner)
                inner_select_exprs.append(inner)
        elif columns:
            for s in columns:
                select_exprs.append(cols[s].sqla_col)
            metrics_exprs = []

        # if granularity:
        #     dttm_col = cols[granularity]
        #     time_grain = extras.get('time_grain_sqla')
        #     time_filters = []
        #
        #     if is_timeseries:
        #         timestamp = dttm_col.get_timestamp_expression(time_grain)
        #         select_exprs += [timestamp]
        #         groupby_exprs += [timestamp]
        #
        #     # Use main dttm column to support index with secondary dttm columns
        #     if db_engine_spec.time_secondary_columns and \
        #             self.main_dttm_col in self.dttm_cols and \
        #             self.main_dttm_col != dttm_col.column_name:
        #         time_filters.append(cols[self.main_dttm_col].
        #                             get_time_filter(from_dttm, to_dttm))
        #     time_filters.append(dttm_col.get_time_filter(from_dttm, to_dttm))

        select_exprs += metrics_exprs
        qry = sa.select(select_exprs)

        tbl = self.get_from_clause(template_processor, db_engine_spec)

        if not columns:
            qry = qry.group_by(*groupby_exprs)

        where_clause_and = []
        having_clause_and = []
        for flt in filter:
            if not all([flt.get(s) for s in ['col', 'op', 'val']]):
                continue
            col = flt['col']
            op = flt['op']
            eq = flt['val']
            col_obj = cols.get(col)
            if col_obj:
                if op in ('in', 'not in'):
                    values = []
                    for v in eq:
                        # For backwards compatibility and edge cases
                        # where a column data type might have changed
                        if isinstance(v, basestring):
                            v = v.strip("'").strip('"')
                            if col_obj.is_num:
                                v = utils.string_to_num(v)

                        # Removing empty strings and non numeric values
                        # targeting numeric columns
                        if v is not None:
                            values.append(v)
                    cond = col_obj.sqla_col.in_(values)
                    if op == 'not in':
                        cond = ~cond
                    where_clause_and.append(cond)
                else:
                    if col_obj.is_num:
                        eq = utils.string_to_num(flt['val'])
                    if op == '==':
                        where_clause_and.append(col_obj.sqla_col == eq)
                    elif op == '!=':
                        where_clause_and.append(col_obj.sqla_col != eq)
                    elif op == '>':
                        where_clause_and.append(col_obj.sqla_col > eq)
                    elif op == '<':
                        where_clause_and.append(col_obj.sqla_col < eq)
                    elif op == '>=':
                        where_clause_and.append(col_obj.sqla_col >= eq)
                    elif op == '<=':
                        where_clause_and.append(col_obj.sqla_col <= eq)
                    elif op == 'LIKE':
                        where_clause_and.append(col_obj.sqla_col.like(eq))
        if extras:
            where = extras.get('where')
            if where:
                where = template_processor.process_template(where)
                where_clause_and += [sa.text('({})'.format(where))]
            having = extras.get('having')
            if having:
                having = template_processor.process_template(having)
                having_clause_and += [sa.text('({})'.format(having))]
        # if granularity:
        #     qry = qry.where(and_(*(time_filters + where_clause_and)))
        # else:
        qry = qry.where(and_(*where_clause_and))
        qry = qry.having(and_(*having_clause_and))

        if not orderby and not columns:
            orderby = [(main_metric_expr, not order_desc)]

        for col, ascending in orderby:
            direction = asc if ascending else desc
            print('-='*20)
            print([col, ascending])
            print('-='*20)
            qry = qry.order_by(direction(col))

        if row_limit:
            qry = qry.limit(row_limit)

        if is_timeseries and \
                timeseries_limit and groupby and not time_groupby_inline:
            # some sql dialects require for order by expressions
            # to also be in the select clause -- others, e.g. vertica,
            # require a unique inner alias
            inner_main_metric_expr = main_metric_expr.label('mme_inner__')
            inner_select_exprs += [inner_main_metric_expr]
            subq = select(inner_select_exprs)
            subq = subq.select_from(tbl)
            inner_time_filter = dttm_col.get_time_filter(
                inner_from_dttm or from_dttm,
                inner_to_dttm or to_dttm,
            )
            subq = subq.where(and_(*(where_clause_and + [inner_time_filter])))
            subq = subq.group_by(*inner_groupby_exprs)

            ob = inner_main_metric_expr
            if timeseries_limit_metric:
                timeseries_limit_metric = metrics_dict.get(timeseries_limit_metric)
                ob = timeseries_limit_metric.sqla_col
            direction = desc if order_desc else asc
            subq = subq.order_by(direction(ob))
            subq = subq.limit(timeseries_limit)

            on_clause = []
            for i, gb in enumerate(groupby):
                on_clause.append(
                    groupby_exprs[i] == column(gb + '__'))

            tbl = tbl.join(subq.alias(), and_(*on_clause))

        return qry.select_from(tbl)

    def get_query_str(self, query_obj):
        engine = self.database.get_sqla_engine()
        qry = self.get_sqla_query(**query_obj)
        sql = six.text_type(
            qry.compile(
                engine,
                compile_kwargs={'literal_binds': True},
            ),
        )
        logging.info(sql)
        sql = sqlparse.format(sql, reindent=True)
        if query_obj['is_prequery']:
            query_obj['prequeries'].append(sql)
        return sql

    def get_df(self, sql, schema):
        sql = sql.strip().strip(';')
        eng = self.database.get_sqla_engine(schema=schema)
        df = pd.read_sql(sql, eng)

        def needs_conversion(df_series):
            if df_series.empty:
                return False
            if isinstance(df_series[0], (list, dict)):
                return True
            return False

        for k, v in df.dtypes.iteritems():
            if v.type == numpy.object_ and needs_conversion(df[k]):
                df[k] = df[k].apply(utils.json_dumps_w_dates)
        return df

    def query(self, query_obj):
        qry_start_dttm = datetime.datetime.now()
        sql = self.get_query_str(query_obj)
        status = QueryStatus.SUCCESS
        error_message = None
        df = None
        try:
            df = self.get_df(sql, self.schema)
        except Exception as e:
            status = QueryStatus.FAILED
            logging.exception(e)
            error_message = (
                self.database.db_engine_spec.extract_error_message(e))

        return QueryResult(
            status=status,
            df=df,
            duration=datetime.datetime.now() - qry_start_dttm,
            query=sql,
            error_message=error_message)