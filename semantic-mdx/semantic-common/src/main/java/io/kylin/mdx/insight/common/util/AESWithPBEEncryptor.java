package io.kylin.mdx.insight.common.util;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

@Slf4j
public class AESWithPBEEncryptor implements StringEncryptor {

    /**
     * The key must be the same as in the properties file
     */
    private static final String JASYPT_ENCRYPT_KEY = "Ee9TlFUNJzNuzRev";

    private static final String ALGORITHM = "PBEWithHMACSHA512AndAES_256";

    private static final StandardPBEStringEncryptor AES_ENCRYPTOR = new StandardPBEStringEncryptor();

    static {
        AES_ENCRYPTOR.setPassword(JASYPT_ENCRYPT_KEY);
        AES_ENCRYPTOR.setAlgorithm(ALGORITHM);
        AES_ENCRYPTOR.setIvGenerator(new RandomIvGenerator());
    }

    @Override
    public String encrypt(String message) {
        return AES_ENCRYPTOR.encrypt(message);
    }

    @Override
    public String decrypt(String encryptedMessage) {
        return AES_ENCRYPTOR.decrypt(encryptedMessage);
    }
}
