package io.kylin.mdx.insight.server.controller;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.server.bean.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IndicatorControllerTest extends BaseEnvSetting {

    @Mock
    private AuthService authService;

    @BeforeClass
    public static void setup() {
        System.setProperty("MDX_HOME", "../mdx/src/test/resources/");
    }

    @Test
    public void getIndicator() {
        when(authService.getCurrentUser()).thenReturn("ADMIN");
        IndicatorController indicatorController = new IndicatorController();
        Response<String> response = indicatorController.getIndicator(0, 1, "");
        Assert.assertEquals("", response.getData());
    }

    @AfterClass
    public static void clearProperty() {
        System.clearProperty("MDX_HOME");
    }

}
