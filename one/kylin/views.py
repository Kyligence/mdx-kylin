import logging

from past.builtins import basestring

from flask import Markup, flash, redirect, Response
from flask_appbuilder import CompactCRUDMixin, expose
from flask_appbuilder.models.sqla.interface import SQLAInterface
from flask_appbuilder.security.decorators import has_access_api
import sqlalchemy as sa

from flask_babel import lazy_gettext as _
from flask_babel import gettext as __

from superset import appbuilder, app, db, utils, security, sm
from superset.utils import has_access
from superset.connectors.base.views import DatasourceModelView
from superset.connectors.connector_registry import ConnectorRegistry
from superset.views.base import (
    api, BaseSupersetView,
    SupersetModelView, ListWidgetWithCheckboxes, DeleteMixin, DatasourceFilter,
    get_datasource_exist_error_mgs,
)

from one.kylin.models import KylinProject
from . import models

from kylinpy import Kylinpy


class KylinColumnInlineView(CompactCRUDMixin, SupersetModelView):  # noqa
    datamodel = SQLAInterface(models.KylinColumn)

    list_title = _('List Columns')
    show_title = _('Show Column')
    add_title = _('Add Column')
    edit_title = _('Edit Column')

    can_delete = False
    # list_widget = ListWidgetWithCheckboxes
    list_columns = ['column_name', 'verbose_name', 'type']
    label_columns = {
        'column_name': _("Column"),
        'verbose_name': _("Verbose Name"),
        'description': _("Description"),
        'groupby': _("Groupable"),
        'filterable': _("Filterable"),
        'table': _("Table"),
        'count_distinct': _("Count Distinct"),
        'sum': _("Sum"),
        'min': _("Min"),
        'max': _("Max"),
        'expression': _("Expression"),
        'is_dttm': _("Is temporal"),
        'python_date_format': _("Datetime Format"),
        'database_expression': _("Database Expression"),
        'type': _('Type'),
    }


appbuilder.add_view_no_menu(KylinColumnInlineView)


class KylinMetricInlineView(CompactCRUDMixin, SupersetModelView):  # noqa
    datamodel = SQLAInterface(models.KylinMetric)

    list_title = _('List Metrics')
    show_title = _('Show Metric')
    add_title = _('Add Metric')
    edit_title = _('Edit Metric')

    add_columns = ['metric_name', 'expression']


appbuilder.add_view_no_menu(KylinMetricInlineView)


class KylinProjectModelView(CompactCRUDMixin, SupersetModelView):
    datamodel = SQLAInterface(models.KylinProject)

    list_title = _('List Instance')
    show_title = _('Show Instance')
    add_title = _('Add Instance')
    edit_title = _('Edit Instance')

    list_columns = []
    add_columns = []
    edit_columns = []

    def pre_add(self, kylin_instance):
        try:
            kylin_instance.authentication()
        except Exception as e:
            raise (Exception(str(e)))

    def post_add(self, kylin_instance):
        kylin_instance.sync()


# appbuilder.add_view(
#     KylinProjectModelView,
#     "Kylin Project",
#     label=__("Kylin Project"),
#     category="Sources",
#     category_label=__("Sources"),
#     icon='fa-cubes',
# )


class KylinDatasourceModelView(SupersetModelView):  # noqa
    datamodel = SQLAInterface(models.KylinDatasource)

    list_title = _('List Kylin Cubes')
    show_title = _('Show Kylin Cubes')
    add_title = _('Add Kylin Cubes')
    edit_title = _('Edit Kylin Cubes')

    list_columns = ['cube', 'project', 'last_modified']
    add_columns = []
    edit_columns = []
    show_columns = []

    related_views = [KylinColumnInlineView, KylinMetricInlineView]


appbuilder.add_view(
    KylinDatasourceModelView,
    "Kylin Cubes",
    label=__("Kylin Cubes"),
    category="Sources",
    category_label=__("Sources"),
    icon='fa-cubes',
)


class Kylin(BaseSupersetView):
    @has_access
    @expose("/refresh_datasources/")
    def refresh_datasources(self):
        _client = Kylinpy(
            host=app.config.get('KAP_HOST'),
            port=app.config.get('KAP_PORT'),
            username=app.config.get('KAP_ADMIN'),
            password=app.config.get('KAP_CREDENTIAL'),
            version='v2'
        )

        local_projects = db.session.query(KylinProject).all()
        for project in _client.projects()['data']:
            has_project = (
                db.session.query(KylinProject)
                .filter(KylinProject.uuid == project['uuid'])
                .first()
            )
            if has_project:
                local_projects.remove(has_project)
                if str(project['last_modified']) != has_project.last_modified:
                    has_project.refresh_kylin_project(project)
                else:
                    continue
            else:
                KylinProject.create_kylin_project(project)

        for inaction_project in local_projects:
            inaction_project.inaction()
            # todo disable cube in this project

        for project in (
            db.session.query(KylinProject).filter(KylinProject.active)
        ):
            project.sync_datasource()

        return redirect("/kylindatasourcemodelview/list/")

    @api
    @has_access_api
    @expose("/checkbox/<model_view>/<id_>/<attr>/<value>", methods=['GET'])
    def checkbox(self, model_view, id_, attr, value):
        pass
        # modelview_to_model = {
        #     'TableColumnInlineView':
        #         ConnectorRegistry.sources['table'].column_class,
        # }
        # model = modelview_to_model[model_view]
        # obj = db.session.query(model).filter_by(id=id_).first()
        # if obj:
        #     setattr(obj, attr, value == 'true')
        #     db.session.commit()
        # return json_success("OK")


appbuilder.add_view_no_menu(Kylin)


appbuilder.add_link(
    "Refresh Kylin",
    label=__("Refresh Kylin"),
    href='/kylin/refresh_datasources/',
    category='Sources',
    category_label=__("Sources"),
    category_icon='fa-database',
    icon="fa-cog")

appbuilder.add_separator("Sources")
