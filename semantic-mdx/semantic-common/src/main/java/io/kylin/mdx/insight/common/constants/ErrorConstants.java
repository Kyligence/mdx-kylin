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



package io.kylin.mdx.insight.common.constants;

public interface ErrorConstants {

    String ROLE_CONTAINS_ONE_USER = "Role must and only contains one user.";

    String ROLE_NOT_CONTAINS_THIS_USER = "Role not contains this user.";

    String ROLE_ALREADY_CONTAINS_USER = "Role already contains this user.";

    String ROLE_NOT_EXISTS = "Role [xxx] not exists.";

    String USER_NOT_EXISTS = "User [xxx] not exists.";

    String FORMAT_ERROR = "Please check the data format, for example whether type is user.";

    String ROLE_ID_NOT_MATCH_NAME = "Role id not match name.";

    String DUPLICATE_ROLE_NAME = "The dataset role named [xxx] already exists.";

    String INVALID_NAME = "Invalid name. Only letters, numbers and underscore characters are supported in a valid name.";

    String ADMIN_CAN_NOT_BE_DELETED = "Admin can't be deleted.";

    String ROLE_NOT_EMPTY = "User is assigned with dataset role [xxx] which cannot be deleted.";

    String ACCESS_DENIED = "Access denied. Only Kylin system administrators or project administrators can access the MDX for Kylin UI interface.";

    String DATASET_NOT_FOUND = "This dataset:[%d] doesn't exist";

    String DATASET_NAME_NOT_FOUND = "This dataset:[%s] doesn't exist";

    String NO_ACCESS = "Access denied! There is no right!";

    String KE_USER_DISABLE = "User is disabled";

    String LICENSE_OUTDATED = "The license has expired";

}
