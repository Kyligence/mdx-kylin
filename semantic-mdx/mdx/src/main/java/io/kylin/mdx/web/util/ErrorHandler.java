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


package io.kylin.mdx.web.util;

import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.ErrorCodeSupplier;
import io.kylin.mdx.ExceptionUtils;
import io.kylin.mdx.core.MdxConfig;
import mondrian.xmla.Enumeration;
import mondrian.xmla.SaxWriter;
import mondrian.xmla.XmlaException;
import mondrian.xmla.XmlaRequestContext;
import mondrian.xmla.impl.DefaultSaxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static mondrian.xmla.XmlaConstants.*;

public class ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final String SOAP_PREFIX = "SOAP-ENV";

    private static final String SERVER_FAULT_FC = "Server";

    private static final MdxConfig config = MdxConfig.getInstance();

    public static void handleXmlaFault(HttpServletRequest request, HttpServletResponse response, Exception e) {
        Enumeration.ResponseMimeType mimeType = Enumeration.ResponseMimeType.SOAP;
        byte[][] responseSoapParts = new byte[2][];
        response.setCharacterEncoding(DEFAULT_ENCODING);
        response.setContentType(mimeType.getMimeType());
        handleFault(request, response, responseSoapParts, e);
        marshallSoapMessage(response, responseSoapParts, mimeType);
    }

    private static void handleFault(HttpServletRequest request, HttpServletResponse response,
            byte[][] responseSoapParts, Exception e) {
        // Regardless of whats been put into the response so far, clear
        // it out.
        XmlaRequestContext context = XmlaRequestContext.getContext();
        if (context == null || !context.fromGateway) {
            response.reset();
        }

        // NOTE: if you can think of better/other status codes to use
        // for the various phases, please make changes.
        // I think that XMLA faults always returns OK.
        if (config.isStressTestMode()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        String faultCode = XmlaException.formatFaultCode("server", UNKNOWN_ERROR_CODE);
        String faultString = ExceptionUtils.getRootCause(e);

        if (e instanceof ErrorCodeSupplier) {
            faultString = ExceptionUtils.getFormattedErrorMsg(faultString, ((ErrorCodeSupplier) e).getErrorCode());

            if (ErrorCode.MISSING_AUTH_INFO == ((ErrorCodeSupplier) e).getErrorCode()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                String host = request.getServerName();
                response.setHeader("WWW-Authenticate", "Basic realm=\"" + host + "\"");
            }
        }

        if (context != null && context.runningStatistics.mdxTimeout) {
            faultString = faultString + ". We recommend that you narrow your query and try again.";
        }

        if (faultString != null && context != null && context.runningStatistics.queryID != null) {
            faultString += " [MDX Query Id " + context.runningStatistics.queryID + "]";
        }

//        String detail = XmlaException.formatDetail(e.getMessage());

        String encoding = response.getCharacterEncoding();
        ByteArrayOutputStream osBuf = new ByteArrayOutputStream();
        try {
            SaxWriter writer = new DefaultSaxWriter(osBuf, encoding);
            writer.startDocument();
            writer.startElement(SOAP_PREFIX + ":Fault");

            // The faultcode element is intended for use by software to provide
            // an algorithmic mechanism for identifying the fault. The faultcode
            // MUST be present in a SOAP Fault element and the faultcode value
            // MUST be a qualified name
            writer.startElement("faultcode");
            writer.characters(faultCode);
            writer.endElement();

            // The faultstring element is intended to provide a human readable
            // explanation of the fault and is not intended for algorithmic
            // processing.
            writer.startElement("faultstring");
            writer.characters(faultString);
            writer.endElement();

            // The faultactor element is intended to provide information about
            // who caused the fault to happen within the message path
            writer.startElement("faultactor");
            writer.characters(FAULT_ACTOR);
            writer.endElement();

            // The detail element is intended for carrying application specific
            // error information related to the Body element. It MUST be present
            // if the contents of the Body element could not be successfully
            // processed. It MUST NOT be used to carry information about error
            // information belonging to header entries. Detailed error
            // information belonging to header entries MUST be carried within
            // header entries.
            writer.startElement("detail");
            writer.startElement("Error", "Description", faultString, "ErrorCode", "3238658052", "Source",
                    "Microsoft SQL Server 2012 Analysis Services");
            writer.endElement(); // error
            writer.endElement(); // detail

            writer.endElement(); // </Fault>
            writer.endDocument();
        } catch (UnsupportedEncodingException uee) {
            logger.warn("This should be handled at begin of processing request", uee);
        } catch (Exception e1) {
            logger.error("Unexcepted runtime exception when handing SOAP fault :(", e1);
        }

        responseSoapParts[1] = osBuf.toByteArray();
    }

    private static void marshallSoapMessage(HttpServletResponse response, byte[][] responseSoapParts,
            Enumeration.ResponseMimeType responseMimeType) throws XmlaException {
        try {
            switch (responseMimeType) {
            case JSON:
                response.setContentType("application/json");
                break;
            case SOAP:
            default:
                response.setContentType("text/xml");
                break;
            }

            // The setCharacterEncoding, setContentType, or setLocale method
            // must be called BEFORE getWriter or getOutputStream and before
            // committing the response for the character encoding to be used.
            //
            // See javax.servlet.ServletResponse
            OutputStream outputStream = response.getOutputStream();

            byte[] soapHeader = responseSoapParts[0];
            byte[] soapBody = responseSoapParts[1];

            Object[] byteChunks = null;
            XmlaRequestContext context = XmlaRequestContext.getContext();
            boolean gateway = context.fromGateway;
            try {
                switch (responseMimeType) {
                case JSON:
                    byteChunks = new Object[] { soapBody, };
                    break;

                case SOAP:
                default:
                    String s0 = "<" + SOAP_PREFIX
                            + ":Envelope xmlns:" + SOAP_PREFIX + "=\"" + NS_SOAP_ENV_1_1 + "\" " + SOAP_PREFIX
                            + ":encodingStyle=\"" + NS_SOAP_ENC_1_1 + "\" >" + "\n<" + SOAP_PREFIX + ":Header>\n";
                    if (!gateway) {
                        s0 = "<?xml version=\"1.0\" encoding=\"" + DEFAULT_ENCODING + "\"?>\n" + s0;
                    }
                    String s2 = "</" + SOAP_PREFIX + ":Header>\n<" + SOAP_PREFIX + ":Body>\n";
                    String s4 = "\n</" + SOAP_PREFIX + ":Body>\n</" + SOAP_PREFIX + ":Envelope>\n";

                    byteChunks = new Object[] { s0.getBytes(DEFAULT_ENCODING), soapHeader,
                            s2.getBytes(DEFAULT_ENCODING), soapBody, s4.getBytes(DEFAULT_ENCODING), };
                    break;
                }
            } catch (UnsupportedEncodingException uee) {
                logger.warn("This should be handled at begin of processing request", uee);
            }

            try (WritableByteChannel wch = Channels.newChannel(outputStream)) {
                int bufferSize = 4096;
                ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                for (Object byteChunk : byteChunks) {
                    if (byteChunk == null || ((byte[]) byteChunk).length == 0) {
                        continue;
                    }
                    try (ReadableByteChannel rch = Channels.newChannel(new ByteArrayInputStream((byte[]) byteChunk))) {
                        int readSize;
                        do {
                            buffer.clear();
                            readSize = rch.read(buffer);
                            buffer.flip();

                            int writeSize = 0;
                            while ((writeSize += wch.write(buffer)) < readSize) {
                            }
                        } while (readSize == bufferSize);
                    }
                }
                outputStream.flush();
            } catch (IOException ioe) {
                logger.warn("Exception when transferring bytes over sockets", ioe);
            }
        } catch (XmlaException xex) {
            throw xex;
        } catch (Exception ex) {
            throw new XmlaException(SERVER_FAULT_FC, MSM_UNKNOWN_CODE, MSM_UNKNOWN_FAULT_FS, ex);
        }
    }
}
