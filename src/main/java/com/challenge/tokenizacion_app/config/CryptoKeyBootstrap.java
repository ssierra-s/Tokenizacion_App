package com.challenge.tokenizacion_app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CryptoKeyBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CryptoKeyBootstrap.class);

    @Value("${crypto.aes-gcm.key-base64:}")
    private String keyB64;

    @Override
    public void run(ApplicationArguments args) {
        if (keyB64 != null && !keyB64.isBlank()) {
            System.setProperty("AES_GCM_KEY_BASE64", keyB64);  // <-- el converter la verá
            log.info("AES_GCM_KEY_BASE64 inicializada desde application.yml");
        } else {
            log.warn("AES_GCM_KEY_BASE64 NO establecida. Configúrala por ENV o en application.yml (solo DEV).");
        }
    }
}
