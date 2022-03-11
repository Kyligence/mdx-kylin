package io.kylin.mdx.insight.common.http;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.ErrorCode;
import org.apache.http.*;
import org.apache.http.params.HttpParams;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class HttpUtilTest {

    static {
        System.setProperty("converter.mock", "true");
        System.setProperty("MDX_HOME", "src/test/resources/");
        System.setProperty("MDX_CONF", "src/test/resources/conf");
    }

    @Test
    public void testPreHandleHttpResponse() {
        MockHttpResponse response1 = new MockHttpResponse(401, "{\"msg\":\"user test locked\"}");
        try {
            HttpUtil.preHandleHttpResponse(response1, "test");
        } catch (SemanticException e) {
            Assert.assertEquals(e, new SemanticException("user test locked", ErrorCode.USER_LOCKED));
        }

        MockHttpResponse response2 = new MockHttpResponse(401, "{\"msg\":\"User is disabled\"}");
        try {
            HttpUtil.preHandleHttpResponse(response2, "test");
        } catch (SemanticException e) {
            Assert.assertEquals(e, new SemanticException("User is disabled", ErrorCode.USER_DISABLE));
        }

        MockHttpResponse response3 = new MockHttpResponse(401, "{\"msg\":\"The license has expired\"}");
        try {
            HttpUtil.preHandleHttpResponse(response3, "test");
        } catch (SemanticException e) {
            Assert.assertEquals(e, new SemanticException("The license has expired", ErrorCode.EXPIRED_LICENSE));
        }

        MockHttpResponse response4 = new MockHttpResponse(401, "{\"msg\":\"Invalid user or password!\"}");
        try {
            HttpUtil.preHandleHttpResponse(response4, "test");
        } catch (SemanticException e) {
            Assert.assertEquals(e, new SemanticException("Invalid user or password!", ErrorCode.USER_OR_PASSWORD_ERROR));
        }

        MockHttpResponse response5 = new MockHttpResponse(401, "{\"msg\":\"User not found!\"}");
        try {
            HttpUtil.preHandleHttpResponse(response5, "test");
        } catch (SemanticException e) {
            Assert.assertEquals(e, new SemanticException("User not found!", ErrorCode.USER_NOT_FOUND));
        }

        MockHttpResponse response6 = new MockHttpResponse(401, "{\"msg\":\"\"}");
        try {
            HttpUtil.preHandleHttpResponse(response6, "test");
        } catch (SemanticException e) {
            Assert.assertEquals(e, new SemanticException("{\"msg\":\"\"}", ErrorCode.UNKNOWN_ERROR));
        }

        MockHttpResponse response7 = new MockHttpResponse(401, "{\"msg\":\"Can't find project test\"}");
        SemanticException expect7 = new SemanticException("Can't find project test", ErrorCode.KYLIN_NOT_FIND_PROJECT);
        try {
            HttpUtil.preHandleHttpResponse(response7, "test");
        } catch (SemanticException e) {
            Assert.assertEquals(expect7, e);
        }
    }

    static class MockHttpResponse implements HttpResponse {

        StatusLine statusLine;

        MockHttpEntity httpEntity;

        public MockHttpResponse(int statusCode, String content) {
            MockStatusLine mockStatusLine = new MockStatusLine(statusCode);
            this.statusLine = mockStatusLine;
            this.httpEntity = new MockHttpEntity(content);
        }

        @Override
        public StatusLine getStatusLine() {
            return statusLine;
        }

        @Override
        public void setStatusLine(StatusLine statusLine) {

        }

        @Override
        public void setStatusLine(ProtocolVersion protocolVersion, int i) {

        }

        @Override
        public void setStatusLine(ProtocolVersion protocolVersion, int i, String s) {

        }

        @Override
        public void setStatusCode(int i) throws IllegalStateException {

        }

        @Override
        public void setReasonPhrase(String s) throws IllegalStateException {

        }

        @Override
        public HttpEntity getEntity() {
            return httpEntity;
        }

        @Override
        public void setEntity(HttpEntity httpEntity) {

        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public void setLocale(Locale locale) {

        }

        @Override
        public ProtocolVersion getProtocolVersion() {
            return null;
        }

        @Override
        public boolean containsHeader(String s) {
            return false;
        }

        @Override
        public Header[] getHeaders(String s) {
            return new Header[0];
        }

        @Override
        public Header getFirstHeader(String s) {
            return null;
        }

        @Override
        public Header getLastHeader(String s) {
            return null;
        }

        @Override
        public Header[] getAllHeaders() {
            return new Header[0];
        }

        @Override
        public void addHeader(Header header) {

        }

        @Override
        public void addHeader(String s, String s1) {

        }

        @Override
        public void setHeader(Header header) {

        }

        @Override
        public void setHeader(String s, String s1) {

        }

        @Override
        public void setHeaders(Header[] headers) {

        }

        @Override
        public void removeHeader(Header header) {

        }

        @Override
        public void removeHeaders(String s) {

        }

        @Override
        public HeaderIterator headerIterator() {
            return null;
        }

        @Override
        public HeaderIterator headerIterator(String s) {
            return null;
        }

        @Override
        public HttpParams getParams() {
            return null;
        }

        @Override
        public void setParams(HttpParams httpParams) {

        }
    }

    private static class MockStatusLine implements StatusLine {

        int statusCode;

        public MockStatusLine(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public ProtocolVersion getProtocolVersion() {
            return null;
        }

        @Override
        public int getStatusCode() {
            return this.statusCode;
        }

        @Override
        public String getReasonPhrase() {
            return null;
        }
    }

    private static class MockHttpEntity implements HttpEntity {

        String content;

        public MockHttpEntity(String content) {
            this.content = content;
        }

        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public boolean isChunked() {
            return false;
        }

        @Override
        public long getContentLength() {
            return 0;
        }

        @Override
        public Header getContentType() {
            return null;
        }

        @Override
        public Header getContentEncoding() {
            return null;
        }

        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            return new ByteArrayInputStream(this.content.getBytes());
        }

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {

        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        public void consumeContent() throws IOException {

        }
    }

}
