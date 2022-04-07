package io.kylin.mdx.insight.core.config;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration
@SpringBootConfiguration
public class SecurityConfigTest {

    @BeforeClass
    public static void init() {
        System.setProperty("MDX_HOME", "../mdx/src/test/resources/");
        System.setProperty("insight.semantic.enable.aad", "true");
    }

    @Test
    public void test() {
    }

    @AfterClass
    public static void clearProperty() {
        System.clearProperty("insight.semantic.enable.aad");
    }


}
