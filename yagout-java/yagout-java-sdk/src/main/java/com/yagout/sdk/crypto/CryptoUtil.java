package com.yagout.sdk.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class CryptoUtil {
    private static final byte[] IV = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    private CryptoUtil() {}

    public static String encryptBase64(String plain, String keyBase64) {
        try {
            byte[] key = Base64.getDecoder().decode(keyBase64.replaceAll("\\s+", ""));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // PKCS5Padding == PKCS7 for 16-byte blocks in JCE
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(IV));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed: " + e.getMessage(), e);
        }
    }

    public static String decryptBase64(String cipherBase64, String keyBase64) {
        try {
            byte[] key = Base64.getDecoder().decode(keyBase64.replaceAll("\\s+", ""));
            byte[] bytes = Base64.getDecoder().decode(cipherBase64);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(IV));
            byte[] decrypted = cipher.doFinal(bytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt failed: " + e.getMessage(), e);
        }
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed: " + e.getMessage(), e);
        }
    }
}
