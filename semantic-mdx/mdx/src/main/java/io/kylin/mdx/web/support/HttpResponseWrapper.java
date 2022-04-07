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



package io.kylin.mdx.web.support;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class HttpResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    private final PrintWriter printWriter = new PrintWriter(outputStream);

    public HttpResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return new ServletOutputStream() {
            @Override
            public void write(int b) {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                outputStream.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                outputStream.flush();
            }
        };
    }

    @Override
    public PrintWriter getWriter() {
        return printWriter;
    }

    public byte[] getContent() {
        printWriter.close();
        return outputStream.toByteArray();
    }

}
