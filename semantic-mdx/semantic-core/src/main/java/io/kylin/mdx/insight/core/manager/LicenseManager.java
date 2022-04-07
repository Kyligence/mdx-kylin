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

/**
 * @author
 */
public interface LicenseManager {
    /**
     * init License
     *
     * @throws SemanticException
     */
    void init() throws SemanticException;

    /**
     * update Mdx version
     *
     * @param mdxVersion
     * @throws SemanticException
     */
    void updateMdxVersion(String mdxVersion) throws SemanticException;

    /**
     * fetch file content
     *
     * @param filename
     * @return string
     * @throws SemanticException
     */
    String getFileContent(String filename) throws SemanticException;

    int getUserLimit();

    String getKiType();

    String getKiVersion();

    String getCommitId();

    String[] getAnalyticTypes();

    String getDefaultLiveDateRange();

    String getLiveDateRange();

    // TODO: remove this
    // Date getKiLicenseUnderline() throws SemanticException;
}
