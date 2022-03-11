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

import java.util.Set;

/**
 * @author qi.wu
 */
public interface SegmentManager {
    /**
     * fetch Segment from remote, only kylin now
     * @param project mdx project name
     * @return  Set<String>
     * @throws SemanticException
     */
    Set<String> getSegmentByKylin(String project) throws SemanticException;

    /**
     * fetch Segment from remote, only kylin now
     * @param project mdx project name
     * @return  Set<String>
     * @throws SemanticException
     */
    Set<String> getSegmentByCache(String project) throws SemanticException;

    /**
     * save Segment to cache remote
     * @param project mdx project name
     * @return  null
     * @throws SemanticException
     */
    void saveSegment(String project, Set<String> segments) throws SemanticException;

}
