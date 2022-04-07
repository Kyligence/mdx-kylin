package mondrian.xmla.handler.chunk;

import java.io.IOException;
import java.io.OutputStream;

public class ByteArrayDataChunk implements XmlaDataChunk {

    private final byte[] content;

    private long count;

    public ByteArrayDataChunk(byte[] content) {
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public long count() {
        return count;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        if (content != null) {
            outputStream.write(content);
            count += content.length;
        }
    }

}