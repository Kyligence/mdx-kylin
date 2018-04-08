from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals

from flask import Blueprint
from flask_appbuilder.security.manager import AUTH_REMOTE_USER

from collections import OrderedDict
from one.kylin_auth import KylinSecurityManager

# --------------------------------------------------
# used for debug mode restart
import kylinpy
# --------------------------------------------------

# --------------------------------------------------
# Modules, datasources and middleware to be registered
# --------------------------------------------------
DEFAULT_MODULE_DS_MAP = OrderedDict([
    ('superset.connectors.sqla.models', ['SqlaTable']),
    # ('superset.connectors.druid.models', ['DruidDatasource']),
])
ADDITIONAL_MODULE_DS_MAP = {
    'one.kylin.models': ['KylinDatasource']
}
SQLALCHEMY_DATABASE_URI = 'mysql://root:root@127.0.0.1/superset'

LANGUAGES = {
    'en': {'flag': 'us', 'name': 'English'},
    'zh': {'flag': 'cn', 'name': 'Chinese'},
}

MAPBOX_API_KEY = 'pk.eyJ1IjoieW9uZ2ppZXpoYW8iLCJhIjoiY2pjMXM3ZW1zMGNjMzMzczRxcHQ3bWI0OCJ9.KjGWpO7N37z6f2odGNbP5w'

# Roles that are controlled by the API / Superset and should not be changes
# by humans.
# ROBOT_PERMISSION_ROLES = ['Public', 'Gamma', 'Alpha', 'Admin', 'sql_lab']

# Integrate external Blueprints to the app by passing them to your
# configuration. These blueprints will get integrated in the app
# one = Blueprint('one', __name__, template_folder='templates')
# BLUEPRINTS = [one]

#################################################################
# KAP CONFIG
#################################################################
KAP_HOST = "sandbox"
KAP_PORT = 7070
KAP_ADMIN = 'ADMIN'
KAP_CREDENTIAL= 'KYLIN'
KAP_ENDPOINT = '/kylin/api'

KAP_SUPPORT_METRICS = [
    'SUM',
    'COUNT',
]

CUSTOM_SECURITY_MANAGER = KylinSecurityManager
AUTH_TYPE = AUTH_REMOTE_USER
KAP_PERMISSION_ROLES = ['Query', 'Operation', 'Admin', 'Management']
