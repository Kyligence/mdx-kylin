package io.kylin.mdx;

import io.kylin.mdx.insight.data.command.LogFilterCommand;
import org.junit.Test;

public class LogFilterCommandTest {

    private static final String semanticLogPath = "src/test/resources/mdxDir/newMdx/logs/semantic.log";

    private static final String gcLogPath = "src/test/resources/mdxDir/newMdx/logs/gc.log";

    @Test
    public void testProcessTrim() throws Exception {
        String[] semanticArgs = new String[]{"log", semanticLogPath, "1615271247000", "1615616847000"};
        String[] gcArgs = new String[]{"log", gcLogPath, "1615271247000", "1615616847000"};
        String[] errorArgs = new String[]{"log", semanticLogPath, "1615271247000", "1615616847000",""};
        LogFilterCommand logFilterCommand = new LogFilterCommand();
        logFilterCommand.execute(semanticArgs);
        logFilterCommand.execute(gcArgs);
        logFilterCommand.execute(errorArgs);
    }

}
