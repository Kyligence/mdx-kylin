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


package io.kylin.mdx.insight.core.manager;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author
 */
public interface ProjectManager {
    /**
     * fetch all projects
     *
     * @return Set
     * @throws SemanticException
     */
    Set<String> getAllProject() throws SemanticException;

    /**
     * init Load ProjectList
     *
     * @throws SemanticException
     */
    void initLoadProjectList() throws SemanticException;

    /**
     * verify Project List Change
     *
     * @throws SemanticException
     */
    void verifyProjectListChange() throws SemanticException;

    /**
     * get Actual ProjectSet By Admin
     *
     * @return
     * @throws SemanticException
     */
    Set<String> getActualProjectSetByAdmin() throws SemanticException;

    /**
     * get Actual Project Set
     *
     * @param connInfo
     * @return
     * @throws SemanticException
     */
    Set<String> getActualProjectSet(ConnectionInfo connInfo) throws SemanticException;

    /**
     * getUser Access Projects
     *
     * @param connInfo
     * @return
     * @throws SemanticException
     */
    Map<String, String> getUserAccessProjects(ConnectionInfo connInfo) throws SemanticException;

    /**
     * do Project Acl
     *
     * @param realUser
     * @param project
     * @throws SemanticException
     */
    void doProjectAcl(String realUser, String project) throws SemanticException;

    /**
     * get Project Names By Cache
     *
     * @return
     * @throws SemanticException
     */
    List<String> getProjectNamesByCache() throws SemanticException;
}
