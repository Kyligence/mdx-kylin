/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.xmla.impl;

import mondrian.xmla.*;

import java.io.OutputStream;

/**
 * Default implementation of {@link mondrian.xmla.XmlaResponse}.
 *
 * @author Gang Chen
 */
public class DefaultXmlaResponse implements XmlaResponse {

    private final SaxWriter writer;

    public DefaultXmlaResponse(OutputStream outputStream, String encoding,
                               Enumeration.ResponseMimeType responseMimeType) {
        this.writer = XmlaResult.newSaxWriter(outputStream, encoding, responseMimeType, false);
    }

    @Override
    public XmlaResult getResult() {
        return null;
    }

    public SaxWriter getWriter() {
        return writer;
    }

    public void error(Throwable t) {
        writer.completeBeforeElement("root");
        @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
        Throwable throwable = XmlaUtil.rootThrowable(t);
        writer.startElement("Messages");
        writer.startElement(
                "Error",
                "ErrorCode", throwable.getClass().getName(),
                "Description", throwable.getMessage(),
                "Source", "Mondrian",
                "Help", "");
        writer.endElement();
        writer.endElement();
    }

}

// End DefaultXmlaResponse.java
