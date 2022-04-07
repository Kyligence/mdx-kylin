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


package io.kylin.mdx.insight.engine.service;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.util.NetworkUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ZookeeperRegistryServiceImpl implements Watcher {

    private final SemanticConfig semanticConfig = SemanticConfig.getInstance();

    private ZooKeeper zk;

    private static final String CLUSTER = "/cluster";

    private static final String LOCALHOST = "localhost";

    private static final String HOST_IP = "127.0.0.1";

    private String registerAddress = "";

    private String hostIp;

    private String mdxPort;

    private String clusterId;

    public ZookeeperRegistryServiceImpl() {
        if (semanticConfig.isZookeeperEnable()) {
            try {
                zk = new ZooKeeper(semanticConfig.getZookeeperAddress(), semanticConfig.getZookeeperTimeout(), this);
            } catch (IOException e) {
                log.error("connect zookeeper failed", e);
            }
        }
    }

    @PostConstruct
    public void init() {
        if (semanticConfig.isZookeeperEnable()) {
            register();
        }
    }

    public void register() {
        try {
            log.info("connected to zookeeper");
            String nodePath = semanticConfig.getZookeeperNodePath() + CLUSTER;
            StringBuilder temNode = new StringBuilder("");
            List<String> nodeArray = Arrays.asList(nodePath.split("/"));
            for (String node : nodeArray) {
                if (StringUtils.isBlank(node)) {
                    continue;
                }
                temNode.append("/").append(node);
                if (zk.exists(temNode.toString(), false) == null) {
                    zk.create(temNode.toString(), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            }
            hostIp = semanticConfig.getMdxHost();
            if (StringUtils.isBlank(hostIp) || LOCALHOST.equals(hostIp) || HOST_IP.equals(hostIp)) {
                hostIp = NetworkUtils.getLocalIP();
            }
            mdxPort = semanticConfig.getMdxPort();
            clusterId = hostIp + "_" + mdxPort;
            registerAddress = temNode.append("/").append(clusterId).toString();
            if (zk.exists(registerAddress, false) == null) {
                zk.create(registerAddress, getRegisterInfo().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                log.info("create service node:{}", registerAddress);
            }
        } catch (Exception e) {
            log.error("create node failure", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
    }

    @PreDestroy
    public void destroy() {
        try {
            if (semanticConfig.isZookeeperEnable()) {
                zk.delete(registerAddress, -1);
                log.info("delete service node:{}", registerAddress);
            }
        } catch (Exception e) {
            log.error("delete service node failed!", e);
            Thread.currentThread().interrupt();
        }
    }

    @Scheduled(cron = "0/30 * * * * ?")
    public void reconnectZKScheduled() {
        try {
            if (!semanticConfig.isZookeeperEnable()) {
                return;
            }
            if (zk.getState() == ZooKeeper.States.CLOSED || zk.getState() == ZooKeeper.States.NOT_CONNECTED) {
                zk = new ZooKeeper(semanticConfig.getZookeeperAddress(), semanticConfig.getZookeeperTimeout(), this);
            }
            register();
        } catch (Exception e) {
            log.error("reconnect zk and create service node failure", e);
        }
    }

    public String getRegisterInfo() {
        String pattern = "{\"name\":\"cluster\",\"id\":\"<ID>\",\"address\":\"<IP>\",\"port\":<PORT>,\"sslPort\":null," +
                "\"payload\":{\"@class\":\"org.springframework.cloud.zookeeper.discovery.ZookeeperInstance\"," +
                "\"id\":\"cluster:prod,custom:<PORT>\",\"name\":\"cluster\",\"metadata\":{\"instance_status\":\"UP\"}}," +
                "\"registrationTimeUTC\":1616640377000,\"serviceType\":\"STATIC\",\"uriSpec\":{\"parts\":[{\"value\":\"scheme\"," +
                "\"variable\":true},{\"value\":\"://\",\"variable\":false},{\"value\":\"address\",\"variable\":true},{\"value\":\":\"," +
                "\"variable\":false},{\"value\":\"port\",\"variable\":true}]},\"enabled\":true}";
        String registerInfo = pattern.replace("<ID>", clusterId).replace("<IP>", hostIp).replace("<PORT>", mdxPort);
        return registerInfo;
    }
}
