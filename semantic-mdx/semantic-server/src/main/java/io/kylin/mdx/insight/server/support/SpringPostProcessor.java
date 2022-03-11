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


package io.kylin.mdx.insight.server.support;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.service.InitService;
import io.kylin.mdx.web.rejecter.RejectRuleManager;
import io.kylin.mdx.web.xmla.XmlaDatasourceManager;
import lombok.extern.slf4j.Slf4j;
import mondrian.olap.MondrianProperties;
import mondrian.xmla.PropertyDefinition;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import javax.servlet.ServletContext;

@Slf4j
public class SpringPostProcessor implements ApplicationListener<ApplicationStartedEvent> {

    private final SemanticConfig semanticConfig = SemanticConfig.getInstance();

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ApplicationContext appCtx = event.getApplicationContext();
        try {
            initProperties();
            initDatasource(appCtx.getBean(ServletContext.class));
            initMetadata(appCtx.getBean(InitService.class));
            RejectRuleManager.newInstance();
        } catch (SemanticException e) {
            throw new RuntimeException("Semantic initialization error at startup, please check it!", e);
        }
    }

    private void initProperties() {
        // MondrianProperties 初始化处，请勿删除
        MondrianProperties properties = MondrianProperties.instance();
        // 配置相关属性
        PropertyDefinition.MdpropMdxSubqueries.setValue(String.valueOf(properties.MdpropMdxSubqueries.get()));
    }

    private void initDatasource(ServletContext servletContext) {
        String webRoot = servletContext.getRealPath("/");
        String rootPath = Utils.endWithSlash(webRoot) + "WEB-INF";
        XmlaDatasourceManager.newInstance(rootPath);
        if (SemanticConfig.getInstance().isClearOnStartupEnable()) {
            XmlaDatasourceManager.getInstance().close();
        }
    }

    private void initMetadata(InitService initService) {
        if (semanticConfig.isSyncOnStartupEnable()) {
            initService.sync();
        }
        initService.startQueryLogPersistence();
        initService.startQueryLogHousekeep();
    }

}
