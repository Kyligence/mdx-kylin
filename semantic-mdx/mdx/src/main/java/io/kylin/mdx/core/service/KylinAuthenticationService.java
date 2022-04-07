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


package io.kylin.mdx.core.service;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.core.support.SpringHolder;
import io.kylin.mdx.insight.core.support.UserOperResult;
import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.core.MdxException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class KylinAuthenticationService {

    private static class KylinAuthenticationServiceHolder {
        private static final KylinAuthenticationService INSTANCE = new KylinAuthenticationService();
    }

    public static KylinAuthenticationService getInstance() {
        return KylinAuthenticationServiceHolder.INSTANCE;
    }

    public UserOperResult authenticate(String user, String password) {
        UserService userService = SpringHolder.getBean(UserService.class);
        return userService.login(Utils.buildBasicAuth(user, password));
    }

    public boolean authenticate(String user, String password, String project, String delegate) throws MdxException {
        UserService userService = SpringHolder.getBean(UserService.class);
        UserOperResult result;
        try {
            result = userService.loginForMDX(user, password, project, delegate);
        } catch (SemanticException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MdxException(ex.getMessage() + " [" + project + "]", ex, ErrorCode.AUTH_FAILED);
        }
        if (UserOperResult.LOGIN_SUCCESS == result) {
            return true;
        } else {
            throw new MdxException(result.getMessage() + " [" + project + "]", ErrorCode.AUTH_FAILED);
        }
    }

}
