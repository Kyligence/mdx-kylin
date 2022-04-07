package io.kylin.mdx.insight.engine.service;

import io.kylin.mdx.insight.common.util.BeanUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ZookeeperRegistryServiceTest {

    @Mock
    private WatchedEvent watchedEvent;

    @BeforeClass
    public static void before() {
        System.setProperty("converter.mock", "true");
        System.setProperty("MDX_HOME", "src/test/resources/kylin");
        System.setProperty("MDX_CONF", "src/test/resources/kylin/conf");
    }

    @Test
    public void testZookeeperRegistryService() throws IOException {
        ZookeeperRegistryServiceImpl zookeeperRegistryService = new ZookeeperRegistryServiceImpl();
        MockZK mockZK = new MockZK();
        BeanUtils.setField(zookeeperRegistryService, ZooKeeper.class, mockZK);
        zookeeperRegistryService.init();
        zookeeperRegistryService.process(watchedEvent);
        zookeeperRegistryService.destroy();
        zookeeperRegistryService.getRegisterInfo();
    }

    @Test
    public void testReconnectZKScheduled() throws IOException {
        ZookeeperRegistryServiceImpl zookeeperRegistryService = new ZookeeperRegistryServiceImpl();
        MockZK mockZK = new MockZK();
        BeanUtils.setField(zookeeperRegistryService, ZooKeeper.class, mockZK);
        zookeeperRegistryService.reconnectZKScheduled();
    }

    static class MockZK extends ZooKeeper {

        public MockZK() throws IOException {
            super("localhost:2182", 1000, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                }
            });
        }

        @Override
        public Stat exists(String path, boolean watch) {
            return  null;
        }

        @Override
        public String create(final String path, byte data[], List<ACL> acl,
                             CreateMode createMode) {
            return "";
        }

        @Override
        public void delete(final String path, int version){
            return;
        }

        @Override
        public States getState() {
            return ZooKeeper.States.CONNECTING;
        }

    }

}
