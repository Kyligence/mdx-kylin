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


package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.common.SemanticException;

import java.util.Map;

/**
 * @author
 */
public interface InitService {

    /**
     * exec sync task
     *
     * @return boolean
     * @throws SemanticException
     */
    boolean sync() throws SemanticException;

    /**
     * start QueryLogPersistence thread
     *
     * @return null
     *
     */
    void startQueryLogPersistence() throws SemanticException;

    /**
     * start QueryLogHousekeep thread
     *
     * @return boolean
     * @throws SemanticException
     */
    boolean startQueryLogHousekeep() throws SemanticException;

    /**
     * fetch config info in system
     *
     * @return map
     * @throws SemanticException
     */
    Map<String, String> getConfigurations() throws SemanticException;

    /**
     * update config info in system
     *
     * @param confMap
     * @return string
     * @throws SemanticException
     */
    String updateConfigurations(Map<String, String> confMap) throws SemanticException;

    /**
     * restart sync task
     *
     * @return string
     * @throws SemanticException
     */
    String restartSync() throws SemanticException;

    /**
     * load all project lists
     *
     * @return null
     * @throws SemanticException
     */
    void loadProjectList() throws SemanticException;

}
