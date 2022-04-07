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


package io.kylin.mdx.insight.common;

import io.kylin.mdx.insight.common.constants.ConfigConstants;
import io.kylin.mdx.insight.common.constants.ErrorConstants;
import io.kylin.mdx.insight.common.constants.SystemConstants;

import java.util.TimeZone;

public class SemanticConstants implements ConfigConstants, ErrorConstants, SystemConstants {

    public final static String DOT = ".";

    public final static String COMMA = ",";

    public final static String EQUAL = "=";

    public final static String COLON = ":";

    public final static String SEMICOLON = ";";


    public final static String FUNCTION_CONSTANT_TYPE = "constant";

    public final static String MEASURE_CONSTANT = "1";

    public final static String BASIC_AUTH_HEADER_KEY = "Authorization";

    // data type

    public final static String DATA_TYPE_INTEGER = "integer";

    public final static String DATA_TYPE_DATE = "date";

    public final static String DATA_TYPE_REAL = "real";

    public final static String DATA_TYPE_STRING = "String";

    // join type

    public final static String JOIN_TYPE_JOIN = "join";

    public final static String JOIN_TYPE_TABLE = "table";

    // Kylin response status

    public final static String STATUS_KEY = "code";

    public final static String STATUS_SUC = "000";

    public final static String SUC_DATA = "data";

    // http response status

    public final static String RESP_SUC = "success";

    public final static String RESP_FAIL = "failure";

    public final static int RESP_STATUS_SUC = 0;

    // date

    public final static String MAX_TIME_DAY_STR = "23:59:59";

    public final static TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("GMT+0.00");

    public final static long MILLISECOND_UNIT = 1000;


    // auth

    public final static String BASIC_AUTH_PREFIX = "Basic ";


    // user type

    public final static String USER = "user";

    public final static String ROLE = "role";

    public final static String GROUP = "group";

    public final static String ADMIN = "ADMIN";

    // role

    public final static String DEFAULT_ROLE = "Admin";

    // health

    public final static String CODE_FAIL = "999";

    public final static String CODE_SUCCESS = "000";

    public final static String DATA_NORMAL = "NORMAL";

    public final static String DATA_WARN = "WARNING";

    // KYLIN group

    public final static String ROLE_ADMIN = "ROLE_ADMIN";

}
