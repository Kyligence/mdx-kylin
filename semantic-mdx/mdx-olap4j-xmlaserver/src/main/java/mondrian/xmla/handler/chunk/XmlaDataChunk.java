package mondrian.xmla.handler.chunk;

import java.io.IOException;
import java.io.OutputStream;

public interface XmlaDataChunk {

    long count();

    void write(OutputStream outputStream) throws IOException;

}