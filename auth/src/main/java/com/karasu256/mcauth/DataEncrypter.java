package com.karasu256.mcauth;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class DataEncrypter {
    private final String key;

    public DataEncrypter(String key) {
        this.key = key;
    }

    public static String createKey(String password, String transformation) {
         return password;
    }

    public String encrypt(String data) {
         try {
            byte[] salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            byte[] iv = new byte[16];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getKeyFromPassword(key, salt), new IvParameterSpec(iv));

            byte[] encrypted = cipher.doFinal(data.getBytes());

            byte[] encryptedWithSaltAndIv = new byte[salt.length + iv.length + encrypted.length];
            System.arraycopy(salt, 0, encryptedWithSaltAndIv, 0, salt.length);
            System.arraycopy(iv, 0, encryptedWithSaltAndIv, salt.length, iv.length);
            System.arraycopy(encrypted, 0, encryptedWithSaltAndIv, salt.length + iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(encryptedWithSaltAndIv);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String data) {
        try {
            byte[] decodedData = Base64.getDecoder().decode(data);

            byte[] salt = new byte[16];
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[decodedData.length - salt.length - iv.length];
            System.arraycopy(decodedData, 0, salt, 0, salt.length);
            System.arraycopy(decodedData, salt.length, iv, 0, iv.length);
            System.arraycopy(decodedData, salt.length + iv.length, encrypted, 0, encrypted.length);

            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, getKeyFromPassword(key, salt), new IvParameterSpec(iv));
            return new String(c.doFinal(encrypted));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SecretKeySpec getKeyFromPassword(String password, byte[] salt) {
        try {
            return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(password.toCharArray(), salt, 65536, 256)).getEncoded(), "AES");
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
