package io.kylin.mdx.insight.common.util;

import io.kylin.mdx.insight.common.PwdDecryptException;
import org.junit.Assert;
import org.junit.Test;

public class AESWithECBEncryptorTest {

    @Test
    public void testEncrypt() throws PwdDecryptException {
        byte[] bytes = AESWithECBEncryptor.initSecretKey();
        Assert.assertNotNull(bytes);

        String clearTxt = "12313231";
        String encrypt = AESWithECBEncryptor.encrypt(clearTxt);
        Assert.assertEquals("ec6796e089f4bdf04f043bad057dd34f", encrypt);
        Assert.assertEquals(clearTxt, AESWithECBEncryptor.decrypt(encrypt));
    }

    @Test
    public void testDecrypt() {
        try {
            AESWithECBEncryptor.decrypt("GG");
        } catch (PwdDecryptException ignored) {
        }
        try {
            AESWithECBEncryptor.decrypt("12345678");
        } catch (PwdDecryptException ignored) {
        }
    }

    @Test
    public void testPwdDecryptException() {
        PwdDecryptException pwdDecryptException1 = new PwdDecryptException();
        PwdDecryptException pwdDecryptException2 = new PwdDecryptException(new Throwable());
    }

}
