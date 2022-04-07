/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2006 Pentaho and others
// All Rights Reserved.
*/
package mondrian.xmla;

/**
 * XML/A response interface.
 *
 * @author Gang Chen
 */
public interface XmlaResponse {

    /**
     * Report XML/A error (not SOAP fault).
     */
    void error(Throwable t);

    XmlaResult getResult();

}

// End XmlaResponse.java
