package io.kylin.mdx.insight.server.security;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.crypto.codec.Hex;

import static io.kylin.mdx.insight.common.constants.ConfigConstants.AAD_AUTHENTICATION_CALLBACK_URL;
import static io.kylin.mdx.insight.common.constants.ConfigConstants.AAD_AUTHENTICATION_CODE_URL;

public class HexInternalOauth2KeyGeneratorTest extends BaseEnvSetting {

    @Test
    public void testGenerateKey() {
        SemanticConfig semanticConfig = SemanticConfig.getInstance();
        semanticConfig.getProperties().put(AAD_AUTHENTICATION_CALLBACK_URL, "http://mdx/callback_url");
        semanticConfig.getProperties().put(AAD_AUTHENTICATION_CODE_URL, "http://mdx/login/oauth2/code/");
        HexInternalOauth2KeyGenerator generator = new HexInternalOauth2KeyGenerator("mock-session-id");
        String state = generator.generateKey();
        String hex = "5f63616c6c6261636b5f75726c3d687474703a2f2f6d64782f63616c6c6261636b5f75726c265f636f64655f757" +
                "26c3d687474703a2f2f6d64782f6c6f67696e2f6f61757468322f636f64652f265f73657373696f6e5f69643d6d6f636b" +
                "2d73657373696f6e2d6964";
        Assert.assertEquals(hex, state);
        String decodedState = new String(Hex.decode(hex));
        Assert.assertEquals("_callback_url=http://mdx/callback_url&_code_url=http://mdx/login/oauth2/code/&" +
                "_session_id=mock-session-id", decodedState);
    }
}