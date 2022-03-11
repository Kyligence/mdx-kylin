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

package io.kylin.mdx.insight.data.deleter;


import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.core.service.MdxQueryService;
import io.kylin.mdx.insight.core.sync.MetaHousekeep;
import io.kylin.mdx.insight.data.command.CommandLineExecutor;
import io.kylin.mdx.insight.engine.service.MdxQueryServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tk.mybatis.spring.annotation.MapperScan;

import javax.sql.DataSource;
import java.util.Arrays;

@Slf4j
@EnableScheduling
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableTransactionManagement
@ComponentScan(
        basePackages = {"io.kylin.mdx.insight.data.deleter"}
)
@MapperScan("io.kylin.mdx.insight.core.dao")
public class QueryLogDeleterCommand implements CommandLineExecutor, ApplicationRunner {

    private static final SemanticConfig config = SemanticConfig.getInstance();

    @Autowired
    private MdxQueryService mdxQueryService;

    @Override
    public void execute(String[] args) {
        new SpringApplicationBuilder(QueryLogDeleterCommand.class)
                .web(WebApplicationType.NONE)
                .sources(MdxQueryService.class, MdxQueryServiceImpl.class)
                .main(QueryLogDeleterCommand.class)
                .run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!Arrays.asList(args.getSourceArgs()).contains("deleteQueryLog")){
            return;
        }
        int mdxQueryMaxRows = config.getMdxQueryHousekeepMaxRows();
        if (mdxQueryMaxRows > 0) {
            MetaHousekeep metaHousekeep = new MetaHousekeep(mdxQueryService, mdxQueryMaxRows);
            metaHousekeep.start();
        }
    }

    @Bean
    @Autowired
    public PlatformTransactionManager platformTransactionManager(
            @Qualifier("dataSource") DataSource myDataSource) {
        return new DataSourceTransactionManager(myDataSource);
    }
}
