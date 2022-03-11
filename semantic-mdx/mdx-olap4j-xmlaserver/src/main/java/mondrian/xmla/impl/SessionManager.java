package mondrian.xmla.impl;

import org.apache.commons.lang3.StringUtils;
import mondrian.xmla.XmlaRequestCallback;
import org.olap4j.impl.Olap4jUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static mondrian.xmla.XmlaConstants.XMLA_SESSION;
import static mondrian.xmla.XmlaConstants.XMLA_SESSION_ID;

public class SessionManager {

    private static final Map<String, String> sessionIdMap = new ConcurrentHashMap<>();

    /**
     * Session properties, keyed by session ID. Currently just username and password.
     */
    private static final Map<String, SessionInfo> sessionInfos = new ConcurrentHashMap<>();

    public static boolean existsSession(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return false;
        }
        return sessionIdMap.containsKey(sessionId);
    }

    public static void addSession(String sessionId) {
        if (StringUtils.isNotBlank(sessionId)) {
            sessionIdMap.put(sessionId, "");
        }
    }

    public static void removeSession(String sessionId) {
        if (StringUtils.isNotBlank(sessionId)) {
            sessionIdMap.remove(sessionId);
        }
    }

    public static String generateSessionId(Map<String, Object> context, List<XmlaRequestCallback> callbacks) {
        for (XmlaRequestCallback callback : callbacks) {
            final String sessionId = callback.generateSessionId(context);
            if (sessionId != null) {
                return sessionId;
            }
        }

        // Generate a pseudo-random new session ID.
        return Long.toString(17L * System.nanoTime() + 3L * System.currentTimeMillis(), 35);
    }

    public static String getSessionIdFromRequest(Element e, Map<String, Object> context) throws Exception {
        // extract the SessionId attrs value and put into context
        Attr attr = e.getAttributeNode(XMLA_SESSION_ID);
        if (attr == null) {
            throw new SAXException("Invalid XML/A message: " + XMLA_SESSION + " Header element with no "
                    + XMLA_SESSION_ID + " attribute");
        }

        String sessionId = attr.getValue();
        if (sessionId == null) {
            throw new SAXException("Invalid XML/A message: " + XMLA_SESSION + " Header element with " + XMLA_SESSION_ID
                    + " attribute but no attribute value");
        }
        return sessionId;
    }

    public static SessionInfo getSessionInfo(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionInfos.get(sessionId);
    }

    public static void saveSessionInfo(String username, String password, String sessionId) {
        SessionInfo sessionInfo = sessionInfos.get(sessionId);
        if (sessionInfo != null && Olap4jUtil.equal(sessionInfo.username, username)) {
            // Overwrite the password, but only if it is non-empty.
            // (Sometimes Simba sends the credentials object again
            // but without a password.)
            if (password != null && password.length() > 0) {
                sessionInfo = new SessionInfo(username, password);
                sessionInfos.put(sessionId, sessionInfo);
            }
        } else {
            // A credentials object was stored against the provided session
            // ID but the username didn't match, so create a new holder.
            sessionInfo = new SessionInfo(username, password);
            sessionInfos.put(sessionId, sessionInfo);
        }
    }

    /**
     * Holds authentication credentials of a XMLA session.
     */
    public static class SessionInfo {
        final String username;
        final String password;

        public SessionInfo(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

}
