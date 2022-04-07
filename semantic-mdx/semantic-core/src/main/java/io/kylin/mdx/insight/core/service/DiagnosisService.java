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

import io.kylin.mdx.insight.core.support.PackageThread;

import java.util.*;

public abstract class DiagnosisService {

    public static final String UNKNOWN = "UNKNOWN";

    public static final long SEVEN_DAY = 7 * 24 * 3600 * 1000L;

    public abstract void logProcessInfo();

    public abstract void logStackInfo();

    public abstract void logHeapInfo();

    public abstract void logTopInfo();

    public abstract boolean retrieveClusterInfo(Map<String, String> resultMap);

    public abstract PackageThread extractSpecifiedData(Long startTimeStamp,
                                                       Long endTimeStamp,
                                                       String rootPath,
                                                       String tmpPath,
                                                       String queryTime,
                                                       String ip,
                                                       String port,
                                                       Map<String, String> mapDatasetName2Json);


    public abstract boolean clearOldPackage(String queryTime, String semanticMdxPath);

    public abstract boolean clearOldTempFiles(String queryTime, String mdxPath, String tmpPath);

    public abstract String getPackageState(PackageThread packageThread);

    public abstract void stopPackaging(PackageThread packageThread, String rootPath);
}
