package io.kylin.mdx.insight.common.util.io;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public class CountInputStream extends InputStream implements Countable {

    private final InputStream inputStream;

    private long count;

    public CountInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public long getCount() {
        return count;
    }

    @Override
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
        int l = inputStream.read(b, off, len);
        if (l > 0) {
            count += l;
        }
        return l;
    }

    @Override
    public int read(@Nonnull byte[] b) throws IOException {
        int l = inputStream.read(b);
        if (l > 0) {
            count += l;
        }
        return l;
    }

    @Override
    public int read() throws IOException {
        int b = inputStream.read();
        if (b > -1) {
            count++;
        }
        return b;
    }

}
