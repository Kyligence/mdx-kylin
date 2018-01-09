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
import sqlalchemy as sqla
import sqlalchemy as sa
from sqlalchemy import asc, and_, desc, select, or_, MetaData, Table
from sqlalchemy.sql.expression import TextAsFrom
from sqlalchemy.orm import backref, relationship
from sqlalchemy.sql import table, literal_column, text, column
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

    # @classmethod
    # def import_obj(cls, i_column):
    #     def lookup_obj(lookup_column):
    #         return db.session.query(KylinColumn).filter(
    #             KylinColumn.table_id == lookup_column.table_id,
    #             KylinColumn.column_name == lookup_column.column_name).first()
    #     return import_util.import_simple_obj(db.session, i_column, lookup_obj)


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

    # def __repr__(self):
    #     return self.verbose_name if self.verbose_name else self.project

    @property
    def backend(self):
        url = make_url(self.sqlalchemy_uri_decrypted)
        return url.get_backend_name()

    @property
    def data(self):
        return
        # d = super(KylinDatasource, self).data
        # if self.type == 'table':
        #     grains = self.database.grains() or []
        #     if grains:
        #         grains = [(g.name, g.name) for g in grains]
        #     d['granularity_sqla'] = utils.choicify(self.dttm_cols)
        #     d['time_grain_sqla'] = grains
        # return d

    @property
    def sqlalchemy_uri_decrypted(self):
        # import ipdb; ipdb.set_trace()
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

    # def get_columns(self, table_name, schema=None):
    #     return self.inspector.get_columns(table_name, schema)

    # def get_table(self, table_name, schema=None):
    #     extra = self.get_extra()
    #     meta = MetaData(**extra.get('metadata_params', {}))
    #     return Table(
    #         table_name, meta,
    #         schema=schema or None,
    #         autoload=True,
    #         autoload_with=self.get_sqla_engine())

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

    # @property
    # def inspector(self):
    #     engine = self.get_sqla_engine()
    #     return sqla.inspect(engine)

    @property
    def db_engine_spec(self):
        return db_engine_specs.engines.get(
            self.backend, db_engine_specs.BaseEngineSpec)

    # def authentication(self):
    #     self.connection.authentication()

    @property
    def kylin_client(self):
        _client = kylinpy.Kylinpy(
            host=app.config.get('KAP_HOST'),
            port=app.config.get('KAP_PORT'),
            username=app.config.get('KAP_ADMIN'),
            password=app.config.get('KAP_CREDENTIAL'),
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

    def get_df(self, sql, schema):
        sql = sql.strip().strip(';')
        eng = self.get_sqla_engine(schema=schema)
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


class KylinDatasource(Model, BaseDatasource):

    __tablename__ = 'kylin_datasources'

    type = "kylin"
    query_language = 'sql'
    column_class = KylinColumn
    metric_class = KylinMetric
    project_class = KylinProject

    datasource_name = Column(String(255), unique=True)
    # model_name = Column(String(255), unique=True)
    # status = Column(String(40), nullable=False)
    # cube_size = Column(String(40), nullable=False)

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

    # @property
    # def table_name(self):
    #     return self.datasource_name

    @property
    def name(self):
        if not self.schema:
            return self.datasource_name
        return "{}.{}".format(self.schema, self.datasource_name)

    # def get_table(self, datasource_name, schema=None):
    #     extra = self.get_extra()
    #     meta = MetaData(**extra.get('metadata_params', {}))
    #     return Table(
    #         datasource_name, meta,
    #         schema=schema or None,
    #         autoload=True,
    #         autoload_with=self.get_sqla_engine())

    # @property
    # def table_names(self):
    #     # pylint: disable=no-member
    #     return ", ".join(
    #         {"{}".format(s.datasource.full_name) for s in self.slices})

    @property
    def cube(self):
        name = escape(self.datasource_name)
        return Markup(
            '<a href="{self.explore_url}">{name}</a>'.format(**locals()))

    # @property
    # def sqla_col(self):
    #     name = self.column_name
    #     if not self.expression:
    #         col = column(self.column_name).label(name)
    #     else:
    #         col = literal_column(self.expression).label(name)
    #     return col

    # @property
    # def data(self):
    #     d = super(KylinDatasource, self).data
    #     if self.type == 'table':
    #         grains = self.database.grains() or []
    #         if grains:
    #             grains = [(g.name, g.name) for g in grains]
    #         d['granularity_sqla'] = utils.choicify(self.dttm_cols)
    #         d['time_grain_sqla'] = grains
    #     return d

    # def get_col(self, col_name):
    #     columns = self.columns
    #     for col in columns:
    #         if col_name == col.column_name:
        # Supporting arbitrary SQL statements in place of tables
    #             return col

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
order_desc=True):
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
        # from pprint import pprint; import ipdb; ipdb.set_trace()
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
        # todo
        # self.columns
        # from sqlalchemy import create_engine
        # engine = create_engine(self.database.sqlalchemy_uri_decrypted)
        engine = self.database.get_sqla_engine()
        qry = self.get_sqla_query(**query_obj)

        sql = str(
            qry.compile(
                engine,
                compile_kwargs={"literal_binds": True}
            )
        )
        logging.info(sql)
        sql = sqlparse.format(sql, reindent=True)
        return sql

    def query(self, query_obj):
        qry_start_dttm = datetime.datetime.now()
        sql = self.get_query_str(query_obj)
        status = QueryStatus.SUCCESS
        error_message = None
        df = None
        try:
            df = self.database.get_df(sql, self.schema)
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

    # def get_sqla_table_object(self):
    #     return self.database.get_table(self.table_name, schema=self.schema)

    # def fetch_metadata(self):
    #     """Fetches the metadata for the table and merges it in"""
    #     try:
    #         table = self.get_sqla_table_object()
    #     except Exception:
    #         raise Exception(_(
    #             "Table [{}] doesn't seem to exist in the specified database, "
    #             "couldn't fetch column information").format(self.table_name))
    #
    #     M = KylinMetric  # noqa
    #     metrics = []
    #     any_date_col = None
    #     db_dialect = self.database.get_dialect()
    #     dbcols = (
    #         db.session.query(KylinColumn)
    #         .filter(KylinColumn.datasource == self)
    #         .filter(or_(KylinColumn.column_name == col.name
    #                     for col in table.columns)))
    #     dbcols = {dbcol.column_name: dbcol for dbcol in dbcols}
    #
    #     for col in table.columns:
    #         try:
    #             datatype = col.type.compile(dialect=db_dialect).upper()
    #         except Exception as e:
    #             datatype = "UNKNOWN"
    #             logging.error(
    #                 "Unrecognized data type in {}.{}".format(table, col.name))
    #             logging.exception(e)
    #         dbcol = dbcols.get(col.name, None)
    #         if not dbcol:
    #             dbcol = KylinColumn(column_name=col.name, type=datatype)
    #             dbcol.groupby = dbcol.is_string
    #             dbcol.filterable = dbcol.is_string
    #             dbcol.sum = dbcol.is_num
    #             dbcol.avg = dbcol.is_num
    #             dbcol.is_dttm = dbcol.is_time
    #         self.columns.append(dbcol)
    #         if not any_date_col and dbcol.is_time:
    #             any_date_col = col.name
    #
    #         quoted = str(col.compile(dialect=db_dialect))
    #         if dbcol.sum:
    #             metrics.append(M(
    #                 metric_name='sum__' + dbcol.column_name,
    #                 verbose_name='sum__' + dbcol.column_name,
    #                 metric_type='sum',
    #                 expression="SUM({})".format(quoted)
    #             ))
    #         if dbcol.avg:
    #             metrics.append(M(
    #                 metric_name='avg__' + dbcol.column_name,
    #                 verbose_name='avg__' + dbcol.column_name,
    #                 metric_type='avg',
    #                 expression="AVG({})".format(quoted)
    #             ))
    #         if dbcol.max:
    #             metrics.append(M(
    #                 metric_name='max__' + dbcol.column_name,
    #                 verbose_name='max__' + dbcol.column_name,
    #                 metric_type='max',
    #                 expression="MAX({})".format(quoted)
    #             ))
    #         if dbcol.min:
    #             metrics.append(M(
    #                 metric_name='min__' + dbcol.column_name,
    #                 verbose_name='min__' + dbcol.column_name,
    #                 metric_type='min',
    #                 expression="MIN({})".format(quoted)
    #             ))
    #         if dbcol.count_distinct:
    #             metrics.append(M(
    #                 metric_name='count_distinct__' + dbcol.column_name,
    #                 verbose_name='count_distinct__' + dbcol.column_name,
    #                 metric_type='count_distinct',
    #                 expression="COUNT(DISTINCT {})".format(quoted)
    #             ))
    #         dbcol.type = datatype
    #
    #     metrics.append(M(
    #         metric_name='count',
    #         verbose_name='COUNT(*)',
    #         metric_type='count',
    #         expression="COUNT(*)"
    #     ))
    #
    #     dbmetrics = db.session.query(M).filter(M.table_id == self.id).filter(
    #         or_(M.metric_name == metric.metric_name for metric in metrics))
    #     dbmetrics = {metric.metric_name: metric for metric in dbmetrics}
    #     for metric in metrics:
    #         metric.table_id = self.id
    #         if not dbmetrics.get(metric.metric_name, None):
    #             db.session.add(metric)
    #     if not self.main_dttm_col:
    #         self.main_dttm_col = any_date_col
    #     db.session.merge(self)
    #     db.session.commit()

    # @classmethod
    # def import_obj(cls, i_datasource, import_time=None):
    #     """Imports the datasource from the object to the database.
    #
    #      Metrics and columns and datasource will be overrided if exists.
    #      This function can be used to import/export dashboards between multiple
    #      superset instances. Audit metadata isn't copies over.
    #     """
    #     def lookup_sqlatable(table):
    #         return db.session.query(KylinDatasource).join(Database).filter(
    #             KylinDatasource.table_name == table.table_name,
    #             KylinDatasource.schema == table.schema,
    #             Database.id == table.database_id,
    #         ).first()
    #
    #     def lookup_database(table):
    #         return db.session.query(Database).filter_by(
    #             database_name=table.params_dict['database_name']).one()
    #     return import_util.import_datasource(
    #         db.session, i_datasource, lookup_database, lookup_sqlatable,
    #         import_time)
    #
    # @classmethod
    # def query_datasources_by_name(
    #         cls, session, database, datasource_name, schema=None):
    #     query = (
    #         session.query(cls)
    #         .filter_by(database_id=database.id)
    #         .filter_by(table_name=datasource_name)
    #     )
    #     if schema:
    #         query = query.filter_by(schema=schema)
    #     return query.all()
