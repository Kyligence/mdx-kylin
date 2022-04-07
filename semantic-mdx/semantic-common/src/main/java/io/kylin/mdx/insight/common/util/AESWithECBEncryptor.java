/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.insight.common.util;

import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class AESWithECBEncryptor {

    private static final String SECRET_KEY = SemanticConfig.getInstance().getSecretKey();

    private static final String KEY_ALGORITHM = "AES";

    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    private static final Key KEY;

    static {
        try {
            KEY = toKey(Hex.decodeHex(SECRET_KEY));
        } catch (DecoderException e) {
            throw new Error("Load AES key error", e);
        }
    }

    public static byte[] initSecretKey() {
        KeyGenerator kg;
        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);
            kg.init(128);
            SecretKey secretKey = kg.generateKey();
            return secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            log.error("no such encryption algorithm", e);
            return new byte[0];
        }
    }

    private static Key toKey(byte[] key) {
        return new SecretKeySpec(key, KEY_ALGORITHM);
    }

    public static String encrypt(String plainText) {
        byte[] bytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] encryptBytes = encrypt(bytes);
        return Hex.encodeHexString(encryptBytes);
    }

    private static byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(AESWithECBEncryptor.DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, AESWithECBEncryptor.KEY);
            return cipher.doFinal(data);
        } catch (Exception ex) {
            log.error("Password Encryption error", ex);
            return data;
        }
    }

    public static String decrypt(String cipherHexText) throws PwdDecryptException {
        byte[] cipherBytes;
        try {
            cipherBytes = Hex.decodeHex(cipherHexText);
        } catch (Exception e) {
            throw new PwdDecryptException("Password decodeHex error", e);
        }

        byte[] clearBytes = decrypt(cipherBytes);
        return new String(clearBytes, StandardCharsets.UTF_8);
    }

    private static byte[] decrypt(byte[] data) throws PwdDecryptException {
        try {
            Cipher cipher = Cipher.getInstance(AESWithECBEncryptor.DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, AESWithECBEncryptor.KEY);
            return cipher.doFinal(data);
        } catch (Exception ex) {
            throw new PwdDecryptException("Password Decryption error", ex);
        }
    }
}
