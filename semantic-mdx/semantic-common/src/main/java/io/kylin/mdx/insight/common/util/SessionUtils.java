package io.kylin.mdx.insight.common.util;

import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.insight.common.SemanticException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.nio.charset.StandardCharsets;

public class SessionUtils {
    public static final int SALT_LENGTH = 10;

    private static final int SESSION_ARRAY_LENGTH = 3;

    public static String encodeToken(long timestamp, String username, String password) {
        String token = username + ":" + password + ":" + timestamp;
        return Utils.genMD5(token);
    }

    public static String encodeValue(long timestamp, String username, String token) {
        String salt = RandomStringUtils.randomAlphabetic(SALT_LENGTH);
        return Base64.encodeBase64String((salt + ":" + username + ":" + token + ":" + timestamp)
                .getBytes(StandardCharsets.UTF_8));
    }

    public static Triple<String, String, Long> decodeValue(String encodedTxt) {
        String decoded = new String(Base64.decodeBase64(encodedTxt), StandardCharsets.UTF_8);
        String[] array = decoded.split(":", 4);
        if (array.length < SESSION_ARRAY_LENGTH) {
            throw new SemanticException(ErrorCode.INVALID_COOKIE_AUTH_INFO);
        }
        try {
            long timestamp = Long.parseLong(array[3]);
            return Triple.of(array[1], array[2], timestamp);
        } catch (Exception e) {
            throw new SemanticException(ErrorCode.INVALID_COOKIE_AUTH_INFO);
        }
    }
}
