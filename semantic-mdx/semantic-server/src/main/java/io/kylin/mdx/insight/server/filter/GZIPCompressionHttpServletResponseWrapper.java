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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

class GZIPCompressionHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private final GZIPServletOutputStream gzipOutputStream;
    private PrintWriter gzipPrinterWriter;

    public GZIPCompressionHttpServletResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
        this.gzipOutputStream = new GZIPServletOutputStream(response.getOutputStream());
        this.gzipPrinterWriter = null;
    }

    private void initGZIPPrinterWriter() {
        if (gzipPrinterWriter == null) {
            gzipPrinterWriter = new PrintWriter(new OutputStreamWriter(gzipOutputStream));
        }
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return gzipOutputStream;
    }

    @Override
    public PrintWriter getWriter() {
        initGZIPPrinterWriter();
        return gzipPrinterWriter;
    }

    @Override
    public void flushBuffer() throws IOException {
        gzipOutputStream.flush();
        super.flushBuffer();
    }

    public void close() throws IOException {
        if (gzipPrinterWriter != null) {
            gzipPrinterWriter.close();
        }
        if (gzipOutputStream != null) {
            gzipOutputStream.close();
        }
    }

}
