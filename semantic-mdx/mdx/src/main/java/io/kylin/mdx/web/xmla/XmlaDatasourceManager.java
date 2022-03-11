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


package io.kylin.mdx.web.xmla;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.DatasourceUtils;
import io.kylin.mdx.insight.common.util.Utils;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;


public class XmlaDatasourceManager implements Closeable {

    public static final AtomicReference<XmlaDatasourceManager> INSTANCE = new AtomicReference<>();

    private final String rootPath;

    private final ConcurrentHashMap<String, byte[]> schemaHashCodes;

    private XmlaDatasourceManager(String rootPath) {
        this.rootPath = rootPath;
        this.schemaHashCodes = new ConcurrentHashMap<>();
    }

    public static void newInstance(String rootPath) {
        INSTANCE.set(new XmlaDatasourceManager(rootPath));
    }

    public static XmlaDatasourceManager getInstance() {
        return Objects.requireNonNull(INSTANCE.get(), "No XmlaDatasourceManager!");
    }

    @Override
    public void close() {
        File rootFile = new File(rootPath);
        File[] datasources = rootFile.listFiles();
        if (datasources != null) {
            for (File datasource : datasources) {
                if (datasource.isFile() && isDatasourceSchema(datasource)) {
                    FileUtils.deleteQuietly(datasource);
                } else if (datasource.isDirectory() && "schema".equals(datasource.getName())) {
                    Utils.deleteDir(datasource.getAbsolutePath());
                }
            }
        }
    }

    public XmlaDatasource newDatasource(String username, String password, String project,
                                        String delegate, boolean forceRefresh) {
        return new XmlaDatasource(rootPath, username, password, project, delegate, forceRefresh);
    }

    void checkEqualsAndWrite(String project, String filePath, byte[] newHashCode, String content,
                             String exceptionTemplate, String exceptionArg) {
        schemaHashCodes.compute(project + '$' + filePath, (k, previousHashCode) -> {
            if (!Arrays.equals(newHashCode, previousHashCode)) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    writer.write(content);
                } catch (IOException e) {
                    throw new SemanticException(String.format(exceptionTemplate, exceptionArg), e);
                }
            }
            return newHashCode;
        });
    }

    byte[] getFileHashCode(String fileName) {
        return schemaHashCodes.get(fileName);
    }

    public void clearSchemaHashCodes(String project) {
        Iterator<String> iterator = schemaHashCodes.keySet().iterator();
        String key;
        while (iterator.hasNext()) {
            key = iterator.next();
            int splitter = key.indexOf('$');
            String currentProject = key.substring(0, splitter);
            if (project.equals(currentProject)) {
                iterator.remove();
            }
        }
    }

    public void clearSchemaHashCodes() {
        schemaHashCodes.clear();
    }

    public boolean isSchemaExist(String username, String project, String delegate) {
        String filePath = DatasourceUtils.getDatasourcePath(rootPath, username, project, delegate);
        return new File(filePath).exists();
    }

    private boolean isDatasourceSchema(File file) {
        return file.getName().endsWith("_datasources.xml");
    }

}
