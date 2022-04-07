package mondrian.xmla.impl;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SessionManagerTest {

    @Test
    public void testSession() {
        Map<String, Object> context = new HashMap<>();
        String sessionId = SessionManager.generateSessionId(context, Collections.emptyList());
        SessionManager.addSession(sessionId);
        Assert.assertTrue(SessionManager.existsSession(sessionId));
        SessionManager.removeSession(sessionId);
        Assert.assertFalse(SessionManager.existsSession(sessionId));
    }

}