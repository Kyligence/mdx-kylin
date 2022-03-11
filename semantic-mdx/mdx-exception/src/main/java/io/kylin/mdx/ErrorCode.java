/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package io.kylin.mdx;


public enum ErrorCode {

    INTERNAL_ERROR("MDX-00000001"),

    UNKNOWN_ERROR("MDX-00000002"),

    // =================== 01 System Service ======================

    // 0101 KYLIN License
    LICENSE_FORMAT_ERROR("MDX-01010001"),
    EXPIRED_LICENSE("MDX-01010002"),
    LICENSE_USER_EXCEED_LIMIT("MDX-01010003"),

    // 0102 System Config
    SYSTEM_CONFIG_NO_ADMIN_USER("MDX-01020001"),
    NO_AVAILABLE_CONVERTER("MDX-01020002"),
    PROPERTY_PARSE_EXCEPTION("MDX-01020003"),

    // 0103 Healthy Check
    SERVICE_UNAVAILABLE("MDX-01030001"),

    // 0104 Diagnose Package
    GENERATE_DIAGNOSE_PACKAGE_ERROR("MDX-01040001"),
    DOWNLOAD_DIAGNOSE_PACKAGE_ERROR("MDX-01040002"),
    REQUEST_NODE_INCONSISTENCY_ERROR("MDX-01040003"),

    // 0105 Synchronization Task
    DISCONNECT_TO_KYLIN("MDX-01050001"),
    INVALIDATE_SYNC_INFO("MDX-01050002", "The connection user information or password maybe empty or has been changed, please contact system admin to update in Configuration page under Management."),

    // 0106 User / Group / Role
    ILLEGAL_ROLE_NAME("MDX-01060001"),
    DUPLICATE_ROLE_NAME("MDX-01060002"),
    USER_NOT_FOUND("MDX-01060003"),
    ROLE_ID_MISMATCH_NAME("MDX-01060004"),
    ROLE_NOT_FOUND("MDX-01060005"),
    INSERT_MORE_THAN_ONE_USER("MDX-01060006"),
    DELETE_MORE_THAN_ONE_USER("MDX-01060007"),
    USE_ALREADY_EXISTS("MDX-01060008"),
    ROLE_MUST_NOT_EMPTY("MDX-01060009"),

    // 0107 Database
    DB_OPERATION_ERROR("MDX-01070001"),

    // 0108 System Tool
    ENCRYPT_PARAMETER_LENGTH_ERROR("MDX-01080001"),

    // =================== 02 Semantic Service ======================

    // 0201 KYLIN API CALL
    FETCH_KYLIN_MODEL_LIST_ERROR("MDX-02010001"),
    FETCH_KYLIN_DIMS_ERROR("MDX-02010002"),
    FETCH_KYLIN_MODEL_INFO_ERROR("MDX-02010003"),
    FETCH_KYLIN_PROJECT_LIST_ERROR("MDX-02010004"),
    FETCH_KYLIN_USER_LIST_ERROR("MDX-02010005"),
    FETCH_KYLIN_USER_INFO_ERROR("MDX-02010006"),
    FETCH_KYLIN_ACCESS_INFO_ERROR("MDX-02010007"),


    // 0202 Dataset
    GET_DATASET_LIST_ERROR("MDX-02020001"),
    DATASET_NOT_EXISTS("MDX-02020002"),
    DATASET_ALREADY_EXISTS("MDX-02020003"),
    HIERARCHY_LEVEL_NOT_IN_DIMTABLE("MDX-02020005"),
    DIMENSION_NAME_NOT_UNIQUE("MDX-02020006"),
    MEASURE_NAME_NOT_UNIQUE("MDX-02020007"),
    CALCULATED_MEASURE_NAME_NOT_UNIQUE("MDX-02020008"),
    NAMEDSET_NAME_NOT_UNIQUE("MDX-02020009"),
    FOLDER_NAME_FORMAT_ERROR("MDX-02020010"),
    CALCULATED_MEASURE_VALIDATE_ERROR("MDX-02020011"),
    NAMEDSET_VALIDATE_ERROR("MDX-02020012"),
    DEFAULT_MEMBER_VALIDATE_ERROR("MDX-02020013"),
    DATASET_TYPE_NOT_SUPPORTED("MDX-02020014"),
    DATASET_CONNECT_CHECK_ERROR("MDX-02020015"),
    UNSUPPORTED_MODEL_RELATION_TYPE("MDX-02020016"),
    MULTI_TABLES_IN_HIERARCHY("MDX-02020017"),
    DIMENSION_NOT_FOUND("MDX-02020018"),
    DIMENSION_COL_NOT_FOUND("MDX-02020019"),
    MEASURE_NOT_FOUND("MDX-02020020"),
    CALCULATED_MEASURE_NOT_FOUND("MDX-02020021"),
    NAMED_SET_NOT_FOUND("MDX-02020022"),
    DATASET_INVALID_IN_PROJECT("MDX-02020023"),
    KYLIN_NOT_FIND_PROJECT("MDX-02020024"),
    DATASET_ORDER_ERROR("MDX-02020025"),
    ZIP_PACKAGE_ERROR("MDX-02020026"),
    IMPORT_TYPE_NOT_FOUNT("MDX-02020027"),

    // =================== 03 MDX Service ======================

    // 0301 Schema
    TABLE_JOIN_RELATION_NOT_FOUND("MDX-03010001"),
    TABLE_PRIMARY_KEY_NOT_FOUND("MDX-03010002"),
    DATASOURCE_FILE_NOT_FOUND("MDX-03010003",
            "Datasource definition file [%s] not found. Please check whether the datasource is normal."),

    // 0302 XMLA Protocol
    UNSUPPORTED_XMLA_METHOD("MDX-03020001"),
    DRILLTHROUGH_ONLY_SUPPORT_TABULAR_FORMAT("MDX-03020002"),
    DRILLTHROUGH_ERROR("MDX-03020003"),
    UNMARSHALLING_MESSAGE_ERROR("MDX-03020004"),
    UNSUPPORTED_XMLA_REQUEST_PATH("MDX-03020005"),

    // 0303 Query
    // see mondrian/resource/MondrianResource.xml

    // 0304 Rewrite / Reject
    UNSUPPORTED_PATTERN_IN_MDX("MDX-03040001"),
    REDIRECT_ADDRESS_NO_CONFIG("MDX-03040002",
            "Configuration insight.semantic.reject.redirect-address is not set. Please check it."),
    CANNOT_REDIRECT_MDX_SERVER("MDX-03040003",
            "Unable to redirect mdx address by %s. Please check the request URL or close query streaming."),
    CLUSTER_NODES_NO_CONFIG("MDX-03040004",
            "Configuration insight.mdx.cluster.nodes is not set. Please check it."),

    //0305 Excel Repair tool
    UPLOAD_EXCEL_FILE_OVERSIZE("MDX-03050001"),

    // =================== 04 Authentication & Authorization ======================

    // 0401 Authentication
    MISSING_AUTH_INFO("MDX-04010001"),
    USER_OR_PASSWORD_ERROR("MDX-04010002"),
    USER_LOCKED("MDX-04010003"),
    ACCESS_DENIED("MDX-04010004"),
    AUTH_FAILED("MDX-04010005"),
    USER_DISABLE("MDX-04010006"),

    // 0402 Authorization
    NOT_ADMIN_USER("MDX-04020001", "The user [%s] is not a system administrator."),
    INACTIVE_USER("MDX-04020002"),
    WITHOUT_PROJECT_PERMISSION("MDX-04020003"),
    NO_AVAILABLE_DATASET("MDX-04020004",
            "There is no dataset in project %s. Please check and try again."),
    ALL_DATASET_BROKEN("MDX-04020005",
            "Datasets are broken in the project %s. Please fix and try again."),
    USER_NO_ACCESS_DATASET("MDX-04020006",
            "The user %s has no access to any datasets in this project."),



    AUTH_ERROR("MDX-04010001",
            "Authentication error. User \"%1$s\", Delegate \"%2$s\", Project \"%3$s\"."),
    INVALID_BASIC_AUTH_INFO("MDX-04010002",
            "Invalid basic authentication information."),
    INVALID_COOKIE_AUTH_INFO("MDX-04010003",
            "Invalid cookie authentication information."),
    COOKIE_OVERTIME("MDX-04010010",
            "The session has expired. Please log in again."),
    PASSWORD_DECRYPTION_ERROR("MDX-01080001",
            "Can't decrypt user's password. Please check and try again."),

    // User Delegate
    EXECUTE_PARAMETER_NOT_ENABLED("MDX-04020011",
            "Can't use the parameter \"EXECUTE_AS_USER_ID\" now. Please contact your system admin to enable it in MDX for Kylin."),
    EXECUTE_PARAMETER_CANNOT_EMPTY("MDX-04020012",
            "The parameter \"EXECUTE_AS_USER_ID\" can't be empty. Please contact your system admin and check its value in the connection string for Kylin."),
    EXECUTE_PARAMETER_TOO_LONG("MDX-04020013",
            "The value of parameter EXECUTE_AS_USER_ID can't exceed 1024 characters. Please shorten your user id value."),
    EXECUTE_PARAMETER_NOT_FOUND("MDX-04020014",
            "Can't find the user specified in parameter \"EXECUTE_AS_USER_ID\" in Kylin. Please contact your system admin and make sure you have access to MDX for Kylin."),
    EXECUTE_NOT_ENOUGH_PERMISSION("MDX-04020015",
            "The user does not have enough permission to perform user delegation. Please make the user is at least a project admin in Kylin or Kyligence Cloud, and have all access to the dataset in MDX for Kylin.");

    private final String code;

    private final String errorMsg;

    ErrorCode(String code) {
        this.code = code;
        this.errorMsg = "Internal Error!";
    }

    ErrorCode(String code, String errorMsg) {
        this.code = code;
        this.errorMsg = errorMsg;
    }

    public String getCode() {
        return this.code;
    }

    public String getFormattedCode() {
        return "[" + this.code + "]";
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public String getErrorMsg(String... parameters) {
        return String.format(errorMsg, (Object[]) parameters);
    }

}
