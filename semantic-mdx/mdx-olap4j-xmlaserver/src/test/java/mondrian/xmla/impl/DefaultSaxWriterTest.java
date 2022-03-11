package mondrian.xmla.impl;

import mondrian.xmla.SaxWriter;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class DefaultSaxWriterTest {

    @Test
    public void test() throws UnsupportedEncodingException {
        String compactResult = "<People><Name>John</Name><Sex>0</Sex><Age>18</Age></People>";
        Assert.assertEquals(compactResult, serialize(true));
        String prettyResult = "<People>\n" +
                "  <Name>John</Name>\n" +
                "  <Sex>0</Sex>\n" +
                "  <Age>18</Age>\n" +
                "</People>";
        Assert.assertEquals(prettyResult, serialize(false));
    }

    private String serialize(boolean compact) throws UnsupportedEncodingException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SaxWriter writer = new DefaultSaxWriter(outputStream, "UTF-8", compact);
        writer.startDocument();
        writer.startElement("People");
        writer.textElement("Name", "John");
        writer.textElement("Sex", 0);
        writer.textElement("Age", 18);
        writer.endElement();
        writer.endDocument();
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

}