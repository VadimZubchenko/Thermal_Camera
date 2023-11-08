package com.example.heatcam.PrivateKeyboard.Helpers;

import org.apache.commons.net.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CipherDecrypt {
    private static final Cipher encryptCipher;
    private static final Cipher decryptCipher;

    public static final byte[] KEY = {'P', 'R', 'I', 'V', 'A', 'T', 'E', 'K', 'E', 'Y', 'B', 'O', 'A', 'R', 'D', 'S'};

    static {
        try {
            encryptCipher = Cipher.getInstance("AES");
            SecretKeySpec eSpec = new SecretKeySpec(KEY, "AES");
            encryptCipher.init(Cipher.ENCRYPT_MODE, eSpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            decryptCipher = Cipher.getInstance("AES");
            SecretKeySpec dSpec = new SecretKeySpec(KEY, "AES");
            decryptCipher.init(Cipher.DECRYPT_MODE, dSpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String Encrypt(String text) {
        byte[] b1 = text.getBytes();
        byte[] encryptedValue;
        try {
            encryptedValue = encryptCipher.doFinal(b1);
            byte[] encodedBytes = Base64.encodeBase64(encryptedValue);
            return new String(encodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String Decrypt(String encryptedValue) {
        byte[] decryptedValue = Base64.decodeBase64(encryptedValue.getBytes());
        byte[] decValue;
        try {
            decValue = decryptCipher.doFinal(decryptedValue);
            return new String(decValue);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}