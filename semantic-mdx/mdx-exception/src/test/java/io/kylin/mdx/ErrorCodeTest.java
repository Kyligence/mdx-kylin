package io.kylin.mdx;

import org.junit.Assert;
import org.junit.Test;

public class ErrorCodeTest {

    @Test
    public void getErrorMsg() {
        String msg0 = ErrorCode.EXECUTE_PARAMETER_NOT_ENABLED.getErrorMsg();
        Assert.assertEquals("Can't use the parameter \"EXECUTE_AS_USER_ID\" now. Please contact your system admin to enable it in MDX for Kylin.", msg0);
        String msg1 = ErrorCode.DATASOURCE_FILE_NOT_FOUND.getErrorMsg("test_project");
        Assert.assertEquals("Datasource definition file [test_project] not found. Please check whether the datasource is normal.", msg1);
    }

}
