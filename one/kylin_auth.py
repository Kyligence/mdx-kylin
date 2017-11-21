# -*- coding: utf-8 -*-

from flask import redirect, request, g, redirect, flash

from flask_appbuilder import expose
from flask_appbuilder.forms import DynamicForm
from flask_appbuilder.security.views import AuthView, UserModelView
from flask_appbuilder.security.sqla.manager import SecurityManager
from flask_appbuilder._compat import as_unicode

from wtforms import StringField, BooleanField, PasswordField
from wtforms.validators import DataRequired
from flask_babel import lazy_gettext

from flask_login import login_user, logout_user

from kylinpy import Kylinpy
from kylinpy.errors import KylinUnauthorizedError, KylinUserDisabled

import logging


class KylinLoginForm(DynamicForm):
    username = StringField(lazy_gettext('User Name'), validators=[DataRequired()])
    password = PasswordField(lazy_gettext('Password'), validators=[DataRequired()])


class KylinAuthUserView(AuthView):
    login_template = 'security/login_kylin.html'

    @expose('/login/', methods=['GET', 'POST'])
    def login(self):
        if g.user is not None and g.user.is_authenticated():
            return redirect(self.appbuilder.get_url_for_index)

        form = KylinLoginForm()
        if form.validate_on_submit():
            user = self.appbuilder.sm.auth_user_kylin(form.username.data, form.password.data)
            if not user:
                flash(as_unicode(self.invalid_login_message), 'warning')
                return redirect(self.appbuilder.get_url_for_login)

            login_user(user, remember=False)
            return redirect(self.appbuilder.get_url_for_index)

        return self.render_template(self.login_template,
                                    title=self.title,
                                    form=form,
                                    appbuilder=self.appbuilder)


class KylinUserModelView(UserModelView):
    pass


class KylinSecurityManager(SecurityManager):
    authremoteuserview = KylinAuthUserView
    userremoteusermodelview = KylinUserModelView

    def __init__(self, appbuilder):
        super(KylinSecurityManager, self).__init__(appbuilder)

    def auth_user_kylin(self, username, password):
        try:
            auth_info = Kylinpy(
                host=self.appbuilder.app.config.get('KAP_HOST'),
                port=self.appbuilder.app.config.get('KAP_PORT'),
                username=username,
                password=password,
                version='v2'
            ).authentication()
            username = auth_info['data']['username']
            password = auth_info['data']['password']
        except (KylinUnauthorizedError, KylinUserDisabled) as e:
            logging.warn(e)
            user = self.find_user(username=username)
            if user:
                user.active = False
                self.update_user(user)
            return None

        user = self.find_user(username=username)
        if user and not user.is_active():
            # reborn user
            user.active = True
            self.update_user(user)
            return user
        if not user:
            # save a kap user, then setting admin role
            user = self.add_user(
                username=username,
                first_name=username,
                last_name=username,
                email=username,
                role=self.find_role(self.auth_role_admin),
                password=password
            )
            if not user:
                logging.warn("Error creating kylin user %s" % username)
                return None

        return user
