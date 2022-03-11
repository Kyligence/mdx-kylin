package io.kylin.mdx.insight.server.controller;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.SemanticOmitDetailException;
import org.junit.Test;

import java.lang.reflect.UndeclaredThrowableException;

public class ExceptionCatchAdviceTest {

    @Test
    public void testExceptionCatchAdvice(){
        ExceptionCatchAdvice exceptionCatchAdvice = new ExceptionCatchAdvice();
        exceptionCatchAdvice.handle(new SemanticException());
        exceptionCatchAdvice.handle(new SemanticOmitDetailException());
        exceptionCatchAdvice.othersErrorHandler(new UndeclaredThrowableException(new Throwable()));
        exceptionCatchAdvice.othersErrorHandler(new Exception());
    }

}
