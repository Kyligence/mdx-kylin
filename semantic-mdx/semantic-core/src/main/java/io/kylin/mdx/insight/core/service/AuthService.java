package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.support.UserOperResult;
import org.apache.commons.lang3.tuple.Pair;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AuthService {

    Pair<String, String> getUserInfoFromRequest(HttpServletRequest request);

    UserOperResult login(String username, String password);

    void loginMdx(String username, String password, String project, String delegateUser);

    String getCurrentUser();

    void addSessionCookie(String username, String password, HttpServletResponse response);

    void removeSessionCookie(HttpServletResponse response);

    boolean validPassword(String user, String password);

    boolean hasAdminPermission(ConnectionInfo connInfo, boolean global);

    void hasAdminPermission();

    String getBasicAuthInfo(String user) throws PwdDecryptException;
}
