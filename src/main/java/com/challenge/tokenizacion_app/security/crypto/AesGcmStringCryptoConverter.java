package com.challenge.tokenizacion_app.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;


@Converter
public class AesGcmStringCryptoConverter implements AttributeConverter<String, String> {

    private static final int NONCE_LEN = 12;
    private static final int TAG_LEN_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String VERSION = "v1:";

    private static final SecureRandom RNG = new SecureRandom();

    // ❌ Quita esto:
    // private static final SecretKeySpec KEY = loadKey();

    // ✅ Usa carga perezosa + cache
    private static volatile SecretKeySpec KEY;

    private static SecretKeySpec getKey() {
        SecretKeySpec local = KEY;
        if (local == null) {
            synchronized (AesGcmStringCryptoConverter.class) {
                local = KEY;
                if (local == null) {
                    KEY = local = loadKey(); // lee de ENV/propiedades del sistema
                }
            }
        }
        return local;
    }

    private static SecretKeySpec loadKey() {
        String b64 = System.getenv("AES_GCM_KEY_BASE64");
        if (b64 == null || b64.isBlank()) {
            b64 = System.getProperty("AES_GCM_KEY_BASE64", "");
        }
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException(
                    "AES_GCM_KEY_BASE64 no configurado (base64 de 16/24/32 bytes).");
        }
        byte[] keyBytes = java.util.Base64.getDecoder().decode(b64);
        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new IllegalStateException("La clave AES debe ser 128/192/256 bits (16/24/32 bytes).");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] nonce = new byte[NONCE_LEN];
            RNG.nextBytes(nonce);

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(TRANSFORMATION);
            javax.crypto.spec.GCMParameterSpec spec =
                    new javax.crypto.spec.GCMParameterSpec(TAG_LEN_BITS, nonce);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, getKey(), spec); // <-- usa getKey()

            byte[] cipherText = cipher.doFinal(attribute.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(NONCE_LEN + cipherText.length);
            bb.put(nonce);
            bb.put(cipherText);
            return VERSION + java.util.Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new IllegalStateException("Error cifrando campo", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            if (!dbData.startsWith(VERSION)) {
                throw new IllegalStateException("Versión de cifrado desconocida");
            }
            String b64 = dbData.substring(VERSION.length());
            byte[] all = java.util.Base64.getDecoder().decode(b64);

            byte[] nonce = new byte[NONCE_LEN];
            byte[] cipherText = new byte[all.length - NONCE_LEN];
            System.arraycopy(all, 0, nonce, 0, NONCE_LEN);
            System.arraycopy(all, NONCE_LEN, cipherText, 0, cipherText.length);

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(TRANSFORMATION);
            javax.crypto.spec.GCMParameterSpec spec =
                    new javax.crypto.spec.GCMParameterSpec(TAG_LEN_BITS, nonce);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, getKey(), spec); // <-- usa getKey()
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error descifrando campo", e);
        }
    }
}
