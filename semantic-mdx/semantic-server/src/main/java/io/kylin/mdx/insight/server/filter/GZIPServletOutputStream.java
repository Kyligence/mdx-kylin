/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.insight.server.filter;

import lombok.Getter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

class GZIPServletOutputStream extends ServletOutputStream {

    private final ServletOutputStream servletOutputStream;

    private final GZIPOutputStream gzipOutputStream;

    @Getter
    private int length;

    GZIPServletOutputStream(ServletOutputStream servletOutputStream) throws IOException {
        this.servletOutputStream = servletOutputStream;
        this.gzipOutputStream = new GZIPOutputStream(servletOutputStream);
    }

    @Override
    public boolean isReady() {
        return servletOutputStream.isReady();
    }

    @Override
    public void setWriteListener(WriteListener listener) {
        servletOutputStream.setWriteListener(listener);
    }

    @Override
    public void flush() throws IOException {
        gzipOutputStream.flush();
        servletOutputStream.flush();
    }

    @Override
    public void close() throws IOException {
        gzipOutputStream.close();
        servletOutputStream.close();
    }

    @Override
    public void write(int b) throws IOException {
        gzipOutputStream.write(b);
        length++;
    }

    @Override
    public void write(byte[] b) throws IOException {
        gzipOutputStream.write(b);
        length += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        gzipOutputStream.write(b, off, len);
        length += len;
    }
}
