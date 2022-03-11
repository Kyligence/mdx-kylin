package io.kylin.mdx.insight.common.util.io;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

public class CountOutputStream extends OutputStream implements Countable {

    private final OutputStream outputStream;

    private long count;

    public CountOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public long getCount() {
        return count;
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
        count++;
    }

    @Override
    public void write(@Nonnull byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
        count += len;
    }

    @Override
    public void write(@Nonnull byte[] b) throws IOException {
        outputStream.write(b);
        count += b.length;
    }

}
