package io.kylin.mdx.insight.common;

import io.kylin.mdx.ErrorCode;
import org.junit.Assert;
import org.junit.Test;

public class SemanticExceptionTest {

    @Test
    public void testErrorMsg() {
        Exception exception0 = new SemanticException(ErrorCode.NO_AVAILABLE_DATASET, "test_project");
        Assert.assertEquals("There is no dataset in project test_project. Please check and try again.",
                exception0.getMessage());
        Exception exception1 = new SemanticException(ErrorCode.ALL_DATASET_BROKEN, "test_project");
        Assert.assertEquals("Datasets are broken in the project test_project. Please fix and try again.",
                exception1.getMessage());
        Exception exception2 = new SemanticException(ErrorCode.USER_NO_ACCESS_DATASET, "test", "test_project");
        Assert.assertEquals("The user test has no access to any datasets in this project.",
                exception2.getMessage());
        Exception exception3 = new SemanticException(ErrorCode.INVALIDATE_SYNC_INFO);
        Assert.assertEquals("The connection user information or password maybe empty or has been changed, please contact system admin to update in Configuration page under Management.",
                exception3.getMessage());
    }

    @Test
    public void testHashCode() {
        int hashCode1 = new SemanticException(ErrorCode.USER_NO_ACCESS_DATASET, "test", "test_project").hashCode();
        Assert.assertEquals(hashCode1, new SemanticException(ErrorCode.USER_NO_ACCESS_DATASET, "test", "test_project").hashCode());

        int hashCode2 = new SemanticException(ErrorCode.USER_NO_ACCESS_DATASET, "test2", "test_project").hashCode();
        Assert.assertNotEquals(hashCode1, hashCode2);
    }

    @Test
    public void testEqual() {
        SemanticException s1 = new SemanticException(ErrorCode.USER_NO_ACCESS_DATASET, "test", "test_project");
        SemanticException s2 = new SemanticException(ErrorCode.USER_NO_ACCESS_DATASET, "test", "test_project");
        SemanticException s3 = new SemanticException();
        SemanticException s4 = new SemanticException("test");
        SemanticException s5 = new SemanticException(new Throwable());
        SemanticException s6 = new SemanticException(new Throwable(), ErrorCode.USER_NO_ACCESS_DATASET);
        SemanticException s7 = new SemanticException("test", new Throwable(), ErrorCode.USER_NO_ACCESS_DATASET);
        Assert.assertEquals(s1, s1);
        Assert.assertEquals(s1, s2);
    }

}
