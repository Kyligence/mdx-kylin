package io.kylin.mdx.insight.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
//import java.nio.channels.Channels;
//import java.nio.channels.ReadableByteChannel;
//import java.nio.channels.WritableByteChannel;

public class IOUtils {

//    private static final int BUF_LEN = 128 * 1024;

//    private static final ThreadLocal<ByteBuffer> BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocate(BUF_LEN));

    /**
     * @param file 目标文件
     * @return InputStream
     */
    public static FileInputStream open(File file) {
        FileInputStream is;
        try {
            is = new FileInputStream(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return is;
    }

//    /**
//     * @throws IOException 读取失败
//     */
//    public static void transfer(InputStream inputStream, OutputStream outputStream)
//            throws IOException {
//        ByteBuffer buffer = BUFFER.get();
//        ReadableByteChannel readChannel = Channels.newChannel(inputStream);
//        WritableByteChannel writeChannel = new DirectWritableByteChannel(outputStream);
//        while (true) {
//            buffer.clear();
//            int totalRead = 0;
//            while (true) {
//                int readLen = readChannel.read(buffer);
//                if (readLen <= 0) {
//                    break;
//                }
//                totalRead += readLen;
//                if (readLen == buffer.capacity()) {
//                    break;
//                }
//            }
//            if (totalRead <= 0) {
//                break;
//            }
//            buffer.flip();
//            int totalWrite = 0;
//            while (totalWrite < totalRead) {
//                totalWrite += writeChannel.write(buffer);
//            }
//        }
//    }
//
//    private static class DirectWritableByteChannel implements WritableByteChannel {
//
//        private final OutputStream outputStream;
//
//        public DirectWritableByteChannel(OutputStream outputStream) {
//            this.outputStream = outputStream;
//        }
//
//        @Override
//        public int write(ByteBuffer src) throws IOException {
//            int len = src.remaining();
//            outputStream.write(src.array(), src.position(), src.limit());
//            return len;
//        }
//
//        @Override
//        public boolean isOpen() {
//            return true;
//        }
//
//        @Override
//        public void close() {
//        }
//
//    }

}
