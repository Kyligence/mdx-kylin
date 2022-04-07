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

import mondrian.xmla.SaxWriter;
import org.olap4j.xmla.server.impl.ArrayStack;
import org.olap4j.xmla.server.impl.StringEscaper;
import org.olap4j.xmla.server.impl.Util;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link SaxWriter}.
 *
 * @author jhyde
 * @author Gang Chen
 * @since 27 April, 2003
 */
public class DefaultSaxWriter implements SaxWriter {

    /**
     * Inside the tag of an element.
     */
    private static final int STATE_IN_TAG = 0;
    /**
     * After the tag at the end of an element.
     */
    private static final int STATE_END_ELEMENT = 1;
    /**
     * After the tag at the start of an element.
     */
    private static final int STATE_AFTER_TAG = 2;
    /**
     * After a burst of character data.
     */
    private static final int STATE_CHARACTERS = 3;

    private final Appendable buf;
    private final boolean compact;

    private final String indentStr = "  ";
    private final ArrayStack<String> stack = new ArrayStack<>();
    private int indent;
    private int state = STATE_END_ELEMENT;

    public DefaultSaxWriter(OutputStream stream, String encoding) throws UnsupportedEncodingException {
        this(new BufferedWriter(new OutputStreamWriter(stream, encoding)), 0, false);
    }

    public DefaultSaxWriter(OutputStream stream, String encoding, boolean compact) throws UnsupportedEncodingException {
        this(new BufferedWriter(new OutputStreamWriter(stream, encoding)), 0, compact);
    }

    public DefaultSaxWriter(Appendable buf, int initialIndent) {
        this(buf, initialIndent, false);
    }

    /**
     * Creates a DefaultSaxWriter without indentation.
     *
     * @param buf String builder to write to
     */
    public DefaultSaxWriter(Appendable buf) {
        this(buf, 0, false);
    }

    /**
     * Creates a DefaultSaxWriter.
     *
     * @param buf           String builder to write to
     * @param initialIndent Initial indent (0 to write on single line)
     * @param compact       Compact Result (false to format result)
     */
    public DefaultSaxWriter(Appendable buf, int initialIndent, boolean compact) {
        this.buf = buf;
        this.indent = initialIndent;
        this.compact = compact;
    }

    private void _startElement(String qName, Attributes attrs) throws IOException {
        _checkTag();
        if (!compact) {
            if (indent > 0) {
                buf.append(Util.nl);
            }
            for (int i = 0; i < indent; i++) {
                buf.append(indentStr);
            }
            indent++;
        }
        buf.append('<');
        buf.append(qName);
        final int length = attrs.getLength();
        for (int i = 0; i < length; i++) {
            String val = attrs.getValue(i);
            if (val != null) {
                buf.append(' ');
                buf.append(attrs.getQName(i));
                buf.append("=\"");
                StringEscaper.XML_NUMERIC_ESCAPER.appendEscapedString(val, buf);
                buf.append("\"");
            }
        }
        state = STATE_IN_TAG;
        assert qName != null;
        stack.add(qName);
    }

    private void _checkTag() throws IOException {
        if (state == STATE_IN_TAG) {
            state = STATE_AFTER_TAG;
            buf.append('>');
        }
    }

    private void _endElement() throws IOException {
        String qName = stack.pop();
        if (!compact) {
            indent--;
        }
        if (state == STATE_IN_TAG) {
            buf.append("/>");
        } else {
            if (!compact) {
                if (state != STATE_CHARACTERS) {
                    buf.append(Util.nl);
                    for (int i = 0; i < indent; i++) {
                        buf.append(indentStr);
                    }
                }
            }
            buf.append("</");
            buf.append(qName);
            buf.append('>');
        }
        state = STATE_END_ELEMENT;
    }

    private void _characters(StringEscaper se, String s, boolean nowrap, boolean pure) throws IOException {
        _checkTag();
        se.appendEscapedString(s, buf, nowrap, pure);
        state = STATE_CHARACTERS;
    }

    // Simplifying methods

    @Override
    public boolean isCompact() {
        return compact;
    }

    @Override
    public void characters(String s) {
        try {
            _characters(StringEscaper.XML_NUMERIC_ESCAPER, s, false, false);
        } catch (IOException e) {
            throw new RuntimeException("Error while appending XML", e);
        }
    }

    @Override
    public void textCharacters(String s) {
        try {
            _characters(StringEscaper.XML_NUMERIC_ESCAPER, s, true, false);
        } catch (IOException e) {
            throw new RuntimeException("Error while appending XML", e);
        }
    }

    @Override
    public void pureCharacters(String s) {
        try {
            _characters(StringEscaper.PLAIN_TEXT_ESCAPER, s, false, true);
        } catch (IOException e) {
            throw new RuntimeException("Error while appending XML", e);
        }
    }

    @Override
    public void startSequence(String name, String subName) {
        if (name != null) {
            startElement(name);
        } else {
            stack.push(null);
        }
    }

    @Override
    public void endSequence() {
        if (stack.peek() == null) {
            stack.pop();
        } else {
            endElement();
        }
    }

    @Override
    public final void textElement(String name, Object data) {
        try {
            _startElement(name, EmptyAttributes);

            // Replace line endings with spaces. IBM's DOM implementation keeps
            // line endings, whereas Sun's does not. For consistency, always
            // strip them.
            //
            // REVIEW: It would be better to enclose in CDATA, but some clients
            // might not be expecting this.
            textCharacters(data.toString());
            _endElement();
        } catch (IOException e) {
            throw new RuntimeException("Error while appending XML", e);
        }
    }

    @Override
    public void element(String tagName, Object... attributes) {
        startElement(tagName, attributes);
        endElement();
    }

    @Override
    public void startElement(String tagName) {
        try {
            _startElement(tagName, EmptyAttributes);
        } catch (IOException e) {
            throw new RuntimeException("Error while appending XML", e);
        }
    }

    @Override
    public void startElement(String tagName, Object... attributes) {
        try {
            _startElement(tagName, new StringAttributes(attributes));
        } catch (IOException e) {
            throw new RuntimeException("Error while appending XML", e);
        }
    }

    @Override
    public void endElement() {
        try {
            _endElement();
        } catch (IOException e) {
            throw new RuntimeException("Error while appending XML", e);
        }
    }

    @Override
    public void startDocument() {
        if (stack.size() != 0) {
            throw new IllegalStateException("Document already started");
        }
    }

    @Override
    public void endDocument() {
        if (stack.size() != 0) {
            throw new IllegalStateException("Document may have unbalanced elements");
        }
        flush();
    }

    @Override
    public void completeBeforeElement(String tagName) {
        if (!stack.contains(tagName)) {
            return;
        }

        String currentTagName = stack.peek();
        while (!tagName.equals(currentTagName)) {
            try {
                _endElement();
            } catch (IOException e) {
                throw new RuntimeException("Error while appending XML", e);
            }
            currentTagName = stack.peek();
        }
    }

    @Override
    public void verbatim(String text) {
        try {
            _checkTag();
            buf.append(text);
        } catch (IOException e) {
            throw new RuntimeException("Error while appending XML", e);
        }
    }

    @Override
    public void flush() {
        if (buf instanceof Writer) {
            try {
                ((Writer) buf).flush();
            } catch (IOException e) {
                throw new RuntimeException("Error while flushing XML", e);
            }
        }
    }

    private static final Attributes EmptyAttributes = new Attributes() {
        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public String getURI(int index) {
            return null;
        }

        @Override
        public String getLocalName(int index) {
            return null;
        }

        @Override
        public String getQName(int index) {
            return null;
        }

        @Override
        public String getType(int index) {
            return null;
        }

        @Override
        public String getValue(int index) {
            return null;
        }

        @Override
        public int getIndex(String uri, String localName) {
            return 0;
        }

        @Override
        public int getIndex(String qName) {
            return 0;
        }

        @Override
        public String getType(String uri, String localName) {
            return null;
        }

        @Override
        public String getType(String qName) {
            return null;
        }

        @Override
        public String getValue(String uri, String localName) {
            return null;
        }

        @Override
        public String getValue(String qName) {
            return null;
        }
    };

    /**
     * List of SAX attributes based upon a string array.
     */
    public static class StringAttributes implements Attributes {
        private final Object[] strings;

        public StringAttributes(Object[] strings) {
            this.strings = strings;
        }

        @Override
        public int getLength() {
            return strings.length / 2;
        }

        @Override
        public String getURI(int index) {
            return null;
        }

        @Override
        public String getLocalName(int index) {
            return null;
        }

        @Override
        public String getQName(int index) {
            return (String) strings[index * 2];
        }

        @Override
        public String getType(int index) {
            return null;
        }

        @Override
        public String getValue(int index) {
            return stringValue(strings[index * 2 + 1]);
        }

        @Override
        public int getIndex(String uri, String localName) {
            return -1;
        }

        @Override
        public int getIndex(String qName) {
            final int count = strings.length / 2;
            for (int i = 0; i < count; i++) {
                String string = (String) strings[i * 2];
                if (string.equals(qName)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public String getType(String uri, String localName) {
            return null;
        }

        @Override
        public String getType(String qName) {
            return null;
        }

        @Override
        public String getValue(String uri, String localName) {
            return null;
        }

        @Override
        public String getValue(String qName) {
            final int index = getIndex(qName);
            if (index < 0) {
                return null;
            } else {
                return stringValue(strings[index * 2 + 1]);
            }
        }

        private static String stringValue(Object s) {
            return s == null ? null : s.toString();
        }
    }
}

// End DefaultSaxWriter.java
