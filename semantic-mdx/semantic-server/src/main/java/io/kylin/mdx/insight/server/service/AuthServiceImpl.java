package io.kylin.mdx.insight.server.service;

import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.insight.common.MdxGlobalContext;
import io.kylin.mdx.insight.common.MdxInstance;
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.SessionUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.core.support.PermissionUtils;
import io.kylin.mdx.insight.core.support.UserOperResult;
import io.kylin.mdx.insight.server.support.WebUtils;
import lombok.extern.slf4j.Slf4j;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.olap4j.impl.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserService userService;

    public static final String SESSION_KEY = MdxInstance.getInstance().getSessionName();

    private static final String HEADER_NAME_GATEWAY = "GatewayAuth";

    private static final SemanticConfig SEMANTIC_CONFIG = SemanticConfig.getInstance();

    public static final int COOKIE_AGE = SemanticConfig.getInstance().getCookieAge();

    private final SemanticAdapter semanticAdapter = SemanticAdapter.INSTANCE;

    private static final int AUTH_INFO_LENGTH = 2;

    @Override
    public Pair<String, String> getUserInfoFromRequest(HttpServletRequest request) {
        //if there are parameters for username and password or not
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null) {
            // use authorization secondly
            String authorization = request.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
            String gatewayAuth = request.getHeader(HEADER_NAME_GATEWAY);
            if (authorization != null) {
                Pair<String, String> userInfo = parseAuthInfo(authorization);
                username = userInfo.getLeft();
                password = userInfo.getRight();
            } else if (gatewayAuth != null) {
                Pair<String, String> userInfo = parseAuthInfo(gatewayAuth);
                username = userInfo.getLeft();
                password = userInfo.getRight();
            } else {
                Cookie cookie = getAuthCookie(request);
                if (cookie != null) {
                    try {
                        Pair<String, String> pair = getUserInfoFromCookie(cookie);
                        username = pair.getLeft();
                        password = pair.getRight();
                    } catch (PwdDecryptException ignored) {
                    }
                }
            }
        }
        if (username == null || password == null) {
            throw new SemanticException(ErrorCode.MISSING_AUTH_INFO);
        }

        return Pair.of(upperAdminName(username), password);
    }

    public Pair<String, String> getUserInfoFromCookie(Cookie cookie) throws PwdDecryptException {
        String username = null;
        String password = null;
        String cookieValue = cookie.getValue();
        if (StringUtils.isNotBlank(cookieValue)) {
            Triple<String, String, Long> triple = SessionUtils.decodeValue(cookieValue);
            username = triple.getLeft();
            long timestamp = triple.getRight();
            if ((Utils.currentTimeStamp() - timestamp) > COOKIE_AGE) {
                throw new SemanticException(ErrorCode.COOKIE_OVERTIME);
            }

            UserInfo userInfoDB = userService.selectOne(username);
            password = userInfoDB.getDecryptedPassword();
            String token = SessionUtils.encodeToken(timestamp, username, password);
            if (!StringUtils.equals(triple.getMiddle(), token)) {
                throw new SemanticException(ErrorCode.INVALID_COOKIE_AUTH_INFO);
            }
        }
        return Pair.of(username, password);
    }

    @Override
    public UserOperResult login(String username, String password) {
        return userService.login(Utils.buildBasicAuth(username, password));
    }

    @Override
    public void loginMdx(String username, String password, String project, String delegateUser) {
        UserOperResult result;
        try {
            result = userService.loginForMDX(username, password, project, delegateUser);
        } catch (Exception ex) {
            throw new SemanticException(ErrorCode.AUTH_ERROR, ex, username, delegateUser, project);
        }
        if (result != UserOperResult.LOGIN_SUCCESS) {
            throw new SemanticException(ErrorCode.AUTH_ERROR, delegateUser, project);
        }
    }

    @Override
    public String getCurrentUser() {
        HttpServletRequest request = WebUtils.getRequest();
        Cookie cookie = getAuthCookie(request);
        if (cookie != null) {
            String cookieValue = cookie.getValue();
            Triple<String, String, Long> pair = SessionUtils.decodeValue(cookieValue);
            return pair.getLeft();
        }
        String basicAuth = request.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
        return StringUtils.isBlank(basicAuth) ? "" : Utils.decodeBasicAuth(basicAuth)[0];
    }

    @Override
    public void addSessionCookie(String username, String password, HttpServletResponse response) {
        long timestamp = Utils.currentTimeStamp();
        String token = SessionUtils.encodeToken(timestamp, username, password);
        String value = SessionUtils.encodeValue(timestamp, username, token);

        Cookie cookie = new Cookie(SESSION_KEY, value);
        cookie.setMaxAge(COOKIE_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    @Override
    public void removeSessionCookie(HttpServletResponse response) {
        HttpServletRequest request = WebUtils.getRequest();
        Cookie cookie = getAuthCookie(request);
        if (cookie != null) {
            cookie.setValue(null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
    }

    private static Pair<String, String> parseAuthInfo(String authorization) {
        String[] basicAuthInfos = authorization.split("\\s");
        if (basicAuthInfos.length < AUTH_INFO_LENGTH) {
            throw new SemanticException(ErrorCode.INVALID_BASIC_AUTH_INFO);
        }
        String basicAuth = new String(Base64.decode(basicAuthInfos[1]));
        String[] authInfos = basicAuth.split(":", 2);
        if (authInfos.length < AUTH_INFO_LENGTH) {
            throw new SemanticException(ErrorCode.INVALID_BASIC_AUTH_INFO);
        }
        return Pair.of(authInfos[0], authInfos[1]);
    }

    private static Cookie getAuthCookie(HttpServletRequest request) {
        Cookie[] cs = request.getCookies();
        if (cs == null) {
            return null;
        }
        return Arrays.stream(cs).
                filter(cookie -> SESSION_KEY.equals(cookie.getName())).findFirst().orElse(null);
    }

    private static String upperAdminName(String username) {
        if (SemanticConstants.ADMIN.equalsIgnoreCase(username) && SEMANTIC_CONFIG.isUpperAdminName()) {
            return username.toUpperCase();
        }
        if (SEMANTIC_CONFIG.isUpperUserName()) {
            return username.toUpperCase();
        }
        return username;
    }

    @Override
    public boolean validPassword(String username, String password) {
        String oldPassword = MdxGlobalContext.getPassword(username);
        if (StringUtils.isNotBlank(oldPassword) && oldPassword.equals(password)) {
            return true;
        } else {
            XmlaRequestContext.getContext().invalidPassword = true;
            return false;
        }
    }

    @Override
    public boolean hasAdminPermission(ConnectionInfo connInfo, boolean global) {
        boolean accessFlag = false;
        List<String> authorities = semanticAdapter.getUserAuthority(connInfo);
        if (authorities.contains(SemanticConstants.ROLE_ADMIN)) {
            accessFlag = true;
        }
        if (global) {
            return accessFlag;
        }
        if (!accessFlag) {
            Set<String> projects = semanticAdapter.getActualProjectSet(connInfo);
            ConnectionInfo newConnInfo = new ConnectionInfo(connInfo);
            for (String project : projects) {
                newConnInfo.setProject(project);
                String accessInfo = semanticAdapter.getAccessInfo(newConnInfo);
                if (PermissionUtils.hasAdminPermission(accessInfo)) {
                    accessFlag = true;
                    break;
                }
            }
        }
        return accessFlag;
    }

    @Override
    public void hasAdminPermission() {
        String user = getCurrentUser();
        if (StringUtils.isEmpty(user)) {
            HttpServletRequest request = WebUtils.getRequest();
            String basicAuth = request.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
            if (StringUtils.isNotBlank(basicAuth)) {
                String[] userPwd = Utils.decodeBasicAuth(basicAuth);
                user = userPwd[0];
            }
        }
        UserInfo userInfo = userService.selectOne(user);
        if (userInfo == null) {
            throw new SemanticException(ErrorCode.MISSING_AUTH_INFO);
        }

        String userPwd;
        try {
            userPwd = Utils.buildBasicAuth(user, userInfo.getDecryptedPassword());
        } catch (PwdDecryptException p) {
            throw new SemanticException(ErrorCode.PASSWORD_DECRYPTION_ERROR, p);
        }
        if (SemanticConfig.getInstance().isConvertorMock()) {
            return;
        }

        boolean accessFlag = hasAdminPermission(new ConnectionInfo(userPwd), true);
        if (!accessFlag) {
            throw new SemanticException(ErrorCode.NOT_ADMIN_USER, user);
        }
    }

    @Override
    public String getBasicAuthInfo(String user) throws PwdDecryptException {
        UserInfo userInfo = userService.selectOne(user);
        return Utils.buildBasicAuth(user, userInfo.getDecryptedPassword());
    }
}
