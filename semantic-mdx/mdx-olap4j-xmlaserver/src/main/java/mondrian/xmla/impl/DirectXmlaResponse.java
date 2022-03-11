package mondrian.xmla.impl;

import mondrian.xmla.Enumeration;
import mondrian.xmla.SaxWriter;
import mondrian.xmla.XmlaResponse;
import mondrian.xmla.XmlaResult;

public class DirectXmlaResponse implements XmlaResponse {

    private final XmlaResult result;

    public boolean compact;

    public String encoding;

    public Enumeration.ResponseMimeType mimeType;

    public DirectXmlaResponse(XmlaResult result) {
        this.result = result;
    }

    @Override
    public void error(Throwable t) {
    }

    @Override
    public XmlaResult getResult() {
        return result;
    }

}
