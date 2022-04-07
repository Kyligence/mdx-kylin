/*
  Copyright (C) 2021 Kyligence Inc. All rights reserved.

  http://kyligence.io

  This software is the confidential and proprietary information of
  Kyligence Inc. ("Confidential Information"). You shall not disclose
  such Confidential Information and shall use it only in accordance
  with the terms of the license agreement you entered into with
  Kyligence Inc.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
import React, { PureComponent, Fragment } from 'react';
import PropTypes from 'prop-types';
import { Button, Input, Form, Tabs, Transfer, MessageBox, Loading } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { messageHelper, dataHelper } from '../../utils';
import { validator, getInitialForm } from './handler';
import { strings, configs } from '../../constants';
import CollapseList from '../../components/CollapseList/CollapseList';
import PaginationTable from '../../components/PaginationTable/PaginationTable';

export default
@Connect({
  mapState: {
    datasetRoleList: state => state.data.datasetRoleList,
    userList: state => state.data.userList,
    datasetRole: state => state.workspace.datasetRole,
  },
})
@InjectIntl()
class ListDatasetRole extends PureComponent {
  static propTypes = {
    boundDatasetRoleActions: PropTypes.object.isRequired,
    boundUserActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    datasetRole: PropTypes.object.isRequired,
    datasetRoleList: PropTypes.object.isRequired,
    userList: PropTypes.object.isRequired,
  };

  $form = React.createRef();

  state = {
    selectedRole: getInitialForm(),
    isEditing: false,
    isLoadingDetail: false,
    isSubmitLoading: false,
    filter: {
      roleName: '',
      userName: '',
    },
    form: getInitialForm(),
  };

  constructor(props) {
    super(props);
    this.handleClickTab = this.handleClickTab.bind(this);
    this.handleEditRole = this.handleEditRole.bind(this);
    this.handleClickRole = this.handleClickRole.bind(this);
    this.handleDeleteRole = this.handleDeleteRole.bind(this);
    this.handleFilterRole = this.handleFilterRole.bind(this);
    this.handleCreateRole = this.handleCreateRole.bind(this);
    this.handleCancelEdit = this.handleCancelEdit.bind(this);
    this.handleSubmitForm = this.handleSubmitForm.bind(this);
    this.handleAssignUsers = this.handleAssignUsers.bind(this);
    this.handleFilterUser = this.handleFilterUser.bind(this);
    this.handleFilterUserInTable = this.handleFilterUserInTable.bind(this);
  }

  async componentDidMount() {
    const { boundDatasetRoleActions, boundUserActions } = this.props;
    await Promise.all([
      boundDatasetRoleActions.getAllRoles(),
      boundUserActions.getAllUsers(),
    ]);
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { datasetRole: oldDatasetRole } = this.props;
    const { datasetRole: newDatasetRole } = nextProps;

    if (oldDatasetRole !== newDatasetRole) {
      this.setState({ selectedRole: newDatasetRole });
    }
  }

  componentDidUpdate(prevProps) {
    const { datasetRoleList: oldDatasetRoleList } = prevProps;
    const { datasetRoleList: newDatasetRoleList } = this.props;

    if (!oldDatasetRoleList.data.length && newDatasetRoleList.data.length) {
      this.setFirstSelectRole();
    }
  }

  componentWillUnmount() {
    const { boundDatasetRoleActions } = this.props;
    boundDatasetRoleActions.initDatasetRole();
  }

  get currentRole() {
    const { isEditing, selectedRole, form } = this.state;
    return isEditing ? form : selectedRole;
  }

  get rules() {
    return {
      name: [{ required: true, validator: validator.name(this.props), trigger: 'blur' }],
      description: [{ validator: validator.description(this.props), trigger: 'blur' }],
    };
  }

  get userOptions() {
    const { userList } = this.props;
    return userList.data.map(({ name }) => ({ key: name, label: name, type: 'user' }));
  }

  get inbuildRoles() {
    const { datasetRoleList } = this.props;
    return datasetRoleList.data.filter(item => configs.inbuildDatasetRoles.includes(item.name));
  }

  get customRoles() {
    const { datasetRoleList } = this.props;
    return datasetRoleList.data.filter(item => !configs.inbuildDatasetRoles.includes(item.name));
  }

  get listActions() {
    const { intl } = this.props;
    return [{
      label: intl.formatMessage(strings.EDIT),
      icon: 'icon-superset-table_edit',
      handler: async row => {
        await this.handleClickRole(row);
        this.handleEditRole();
      },
    }];
  }

  get inbuildRolesDescription() {
    const { intl } = this.props;
    return {
      Admin: intl.formatMessage(strings.DATASET_ADMIN_DESC),
    };
  }

  setFirstSelectRole() {
    const { datasetRoleList } = this.props;
    const { data } = datasetRoleList;

    if (data[0]) {
      const datasetRoleId = data[0].id;
      this.getRoleDetail(datasetRoleId);
    }
  }

  async getRoleDetail(datasetRoleId) {
    const { boundDatasetRoleActions } = this.props;

    this.setState({ isLoadingDetail: true });
    await boundDatasetRoleActions.getRoleDetail({ datasetRoleId });
    this.setState({ isEditing: false, isLoadingDetail: false });
  }

  showExitToLostEditWarning() {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.EXIT_LOST_EDIT);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  showRoleChangeWarning() {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.SWITCH_LOST_EDIT_ROLE);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  showRoleDeleteWarning(datasetRole) {
    const { intl } = this.props;
    const params = { roleName: datasetRole.name };
    const messageContent = intl.formatMessage(strings.CONFIRM_DELETE_DATASET_ROLE, params);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  toggleSubmitLoading(isSubmitLoading) {
    this.setState({ isSubmitLoading });
  }

  handleCreateRole() {
    this.setState({ isEditing: true, form: getInitialForm() });
  }

  handleEditRole() {
    const { datasetRole } = this.props;
    this.setState({ isEditing: true, form: datasetRole });
  }

  async handleCancelEdit() {
    await this.showExitToLostEditWarning();
    this.setState({ isEditing: false, form: getInitialForm() });
  }

  handleClickTab(instance) {
    this.setState({ activeTabName: instance.props.name });
  }

  handleInput(key, value) {
    const { form: oldForm } = this.state;
    const form = { ...oldForm, [key]: value };
    this.setState({ form });
  }

  async handleClickRole(row) {
    const { isEditing } = this.state;
    if (isEditing) {
      await this.showRoleChangeWarning();
    }
    await this.getRoleDetail(row.id);
  }

  handleFilterRole(value) {
    const { filter: oldFilter } = this.state;
    const filter = { ...oldFilter, roleName: value };
    this.setState({ filter });
  }

  handleFilterUserInTable(value) {
    const { filter: oldFilter } = this.state;
    const filter = { ...oldFilter, userName: value };
    this.setState({ filter });
  }

  /* eslint-disable class-methods-use-this */
  handleFilterUser(query, item) {
    const username = item.label.toLowerCase();
    return username.includes(query.toLowerCase());
  }

  handleAssignUsers(userIds) {
    const assignedUsers = this.userOptions
      .filter(option => userIds.includes(option.key))
      .map(option => ({ id: option.key, name: option.label, type: 'user' }));

    const { form: oldForm } = this.state;
    const form = { ...oldForm, contains: assignedUsers };
    this.setState({ form });
  }

  showDeleteSuccess(datasetRole) {
    const { intl } = this.props;
    const params = { roleName: datasetRole.name };
    const message = intl.formatMessage(strings.DELETE_DATASET_ROLE_SUCCESS, params);
    messageHelper.notifySuccess(message);
  }

  async handleDeleteRole(datasetRole) {
    const { boundDatasetRoleActions } = this.props;
    await this.showRoleDeleteWarning(datasetRole);
    await boundDatasetRoleActions.deleteRole(datasetRole.id);
    this.showDeleteSuccess(datasetRole);
  }

  showModifySuccess(newForm) {
    const { intl } = this.props;
    const params = { roleName: newForm.name };
    const message = intl.formatMessage(strings.MODIFY_DATASET_ROLE_SUCCESS, params);
    messageHelper.notifySuccess(message);
  }

  showCreateSuccess(newForm) {
    const { intl } = this.props;
    const params = { roleName: newForm.name };
    const message = intl.formatMessage(strings.CREATE_DATASET_ROLE_SUCCESS, params);
    messageHelper.notifySuccess(message);
  }

  async handleSubmitForm() {
    const { boundDatasetRoleActions } = this.props;
    const { form } = this.state;
    let datasetRoleId = form.id;

    this.$form.current.validate(async valid => {
      if (valid) {
        this.toggleSubmitLoading(true);
        try {
          const newForm = { ...form, name: form.name.trim() };
          if (datasetRoleId) {
            await boundDatasetRoleActions.editRole({ datasetRoleId }, newForm, false);
            this.showModifySuccess(newForm);
          } else {
            datasetRoleId = await boundDatasetRoleActions.createRole({}, newForm, false);
            this.showCreateSuccess(newForm);
          }
          this.setState({ isEditing: false, form: getInitialForm() });
          await this.getRoleDetail(datasetRoleId);
        } catch (e) {
          this.toggleSubmitLoading(false);
        }
        this.toggleSubmitLoading(false);
      }
    });
  }

  render() {
    const { datasetRoleList, userList, intl } = this.props;
    const {
      filter,
      isEditing,
      form,
      isLoadingDetail,
      isSubmitLoading,
      activeTabName,
    } = this.state;
    const {
      $form,
      rules,
      userOptions,
      currentRole,
      customRoles,
      inbuildRoles,
      inbuildRolesDescription,
    } = this;

    const isInbuildRole = inbuildRoles.some(r => r.name === currentRole.name);

    return (
      <div className="layout-wrapper list-dataset-role clearfix mdx-it-dataset-roles-page">
        <div className="layout-sidebar">
          <div className="layout-sidebar-block">
            <Button
              plain
              className="add-role mdx-it-add-role-button"
              type="primary"
              onClick={this.handleCreateRole}
              disabled={isEditing}
              icon="icon-superset-add_2"
            >
              {intl.formatMessage(strings.DATASET_ROLE)}
            </Button>
            <Input
              style={{ marginBottom: '10px' }}
              value={filter.roleName}
              prefixIcon="icon-superset-search"
              onChange={this.handleFilterRole}
              placeholder={intl.formatMessage(strings.SEARCH)}
            />
            <Loading className="default-roles mdx-it-default-roles" loading={datasetRoleList.isLoading}>
              <CollapseList
                title={intl.formatMessage(strings.DEFAULT_ROLE)}
                rowKey="name"
                rows={inbuildRoles}
                onClick={this.handleClickRole}
                filter={filter.roleName}
                activeItem={currentRole}
                actions={this.listActions}
              />
            </Loading>
            <Loading className="custom-roles" loading={datasetRoleList.isLoading}>
              <CollapseList
                title={intl.formatMessage(strings.CUSTOM_ROLE)}
                rowKey="name"
                rows={customRoles}
                onClick={this.handleClickRole}
                filter={filter.roleName}
                activeItem={currentRole}
                actions={this.listActions}
              />
            </Loading>
          </div>
        </div>
        <div className="layout-right-content">
          <div className="form-content">
            <Loading loading={isLoadingDetail}>
              <Form scrollToError ref={$form} model={form} rules={rules} labelPosition="left">
                {!isEditing ? (
                  <div className="title-sm font-medium mdx-it-current-dataset-role">
                    {intl.formatMessage(strings.DATASET_ROLE_C)}
                    {currentRole.name}
                  </div>
                ) : (
                  <Form.Item className="role-name-input mdx-it-role-name-input" label={intl.formatMessage(strings.ROLE)} prop="name">
                    <Input
                      value={currentRole.name}
                      onChange={value => this.handleInput('name', value)}
                      disabled={!dataHelper.isEmpty(currentRole.id)}
                    />
                  </Form.Item>
                )}
                {!isEditing ? (
                  <div className="actions">
                    <Button
                      type="primary"
                      icon="icon-superset-table_edit"
                      onClick={() => this.handleEditRole(currentRole)}
                    >
                      {intl.formatMessage(strings.EDIT)}
                    </Button>
                    <Button
                      icon="icon-superset-table_delete"
                      disabled={isInbuildRole}
                      onClick={() => this.handleDeleteRole(currentRole)}
                    >
                      {intl.formatMessage(strings.DELETE)}
                    </Button>
                  </div>
                ) : null}
                <Tabs activeName="users" type="card" onTabClick={this.handleClickTab}>
                  <Tabs.Pane label={intl.formatMessage(strings.USER)} name="users">
                    <div className="tab-content">
                      <Loading loading={userList.isLoading}>
                        {userList.isFullLoading}
                        {!isEditing ? (
                          <Fragment>
                            <Input
                              className="filter-username"
                              placeholder={intl.formatMessage(strings.FILTER_USERS)}
                              value={filter.userName}
                              onChange={this.handleFilterUserInTable}
                            />
                            <PaginationTable
                              className={activeTabName}
                              columns={[{ label: intl.formatMessage(strings.ASSIGNED_USERS), prop: 'name' }]}
                              data={currentRole.contains}
                              filters={[{ name: filter.userName }]}
                            />
                          </Fragment>
                        ) : (
                          <Transfer
                            isLazy
                            filterable
                            titles={[
                              intl.formatMessage(strings.UNASSIGNED_USERS),
                              intl.formatMessage(strings.ASSIGNED_USERS),
                            ]}
                            filterMethod={this.handleFilterUser}
                            filterPlaceholder={intl.formatMessage(strings.SEARCH)}
                            value={Array.from(new Set(currentRole.contains.map(user => user.name)))}
                            data={userOptions}
                            onChange={this.handleAssignUsers}
                          />
                        )}
                      </Loading>
                    </div>
                  </Tabs.Pane>
                  <Tabs.Pane label={intl.formatMessage(strings.DESCRIPTION)} name="description">
                    <div className="tab-content">
                      <Form.Item prop="description">
                        <Input
                          className="mdx-it-dataset-role-description"
                          type="textarea"
                          value={inbuildRolesDescription[this.currentRole.name] ||
                            this.currentRole.description}
                          disabled={!isEditing || isInbuildRole}
                          autosize={{ minRows: 10 }}
                          onChange={value => this.handleInput('description', value)}
                        />
                      </Form.Item>
                    </div>
                  </Tabs.Pane>
                </Tabs>
              </Form>
            </Loading>
          </div>
          {isEditing ? (
            <div className="layout-footer clearfix">
              <div className="pull-right">
                <Button plain onClick={this.handleCancelEdit} disabled={isSubmitLoading}>
                  {intl.formatMessage(strings.CANCEL)}
                </Button>
                <Button type="primary" onClick={this.handleSubmitForm} loading={isSubmitLoading}>
                  {intl.formatMessage(strings.OK)}
                </Button>
              </div>
            </div>
          ) : null}
        </div>
      </div>
    );
  }
}
