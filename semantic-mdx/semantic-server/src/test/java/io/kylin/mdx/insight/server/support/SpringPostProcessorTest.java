package io.kylin.mdx.insight.server.support;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.core.service.InitService;
import io.kylin.mdx.insight.core.support.SpringHolder;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpringPostProcessorTest extends BaseEnvSetting {

    @Mock
    private ApplicationStartedEvent applicationStartedEvent;

    @Mock
    private ConfigurableApplicationContext applicationContext;

    @Mock
    private ServletContext servletContext;

    @Mock
    private InitService initService;

    @Test
    public void onApplicationEvent() throws IOException {
        new SpringHolder().setApplicationContext(applicationContext);
        when(applicationStartedEvent.getApplicationContext()).thenReturn(applicationContext);
        when(applicationContext.getBean(InitService.class)).thenReturn(initService);
        when(applicationContext.getBean(ServletContext.class)).thenReturn(servletContext);

        String mdxHome = "src/test/resources/";
        when(servletContext.getRealPath("/")).thenReturn(mdxHome + "server");
        File dir = new File(mdxHome + "server/WEB-INF");
        FileUtils.copyDirectory(new File(mdxHome + "datasource"), dir);

        SpringPostProcessor processor = new SpringPostProcessor();
        processor.onApplicationEvent(applicationStartedEvent);
        Assert.assertTrue(dir.exists());
    }

}
