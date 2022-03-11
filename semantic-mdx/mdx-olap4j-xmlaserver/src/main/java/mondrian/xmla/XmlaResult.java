package mondrian.xmla;

import mondrian.xmla.handler.chunk.XmlaDataChunk;
import mondrian.xmla.impl.DefaultSaxWriter;
import mondrian.xmla.impl.JsonSaxWriter;
import org.olap4j.xmla.server.impl.Util;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class XmlaResult {

    private static final String MSG_ENCODING_ERROR = "Encoding unsupported: ";

    public Enumeration.ResponseMimeType responseMimeType;

    public byte[] header;

    public XmlaDataChunk body;

    public static SaxWriter newSaxWriter(OutputStream outputStream, String encoding,
                                         Enumeration.ResponseMimeType responseMimeType, boolean compact) {
        try {
            switch (responseMimeType) {
                case JSON:
                    return new JsonSaxWriter(outputStream);
                case SOAP:
                default:
                    return new DefaultSaxWriter(outputStream, encoding, compact);
            }
        } catch (UnsupportedEncodingException uee) {
            throw Util.newError(uee, MSG_ENCODING_ERROR + encoding);
        }
    }

}
