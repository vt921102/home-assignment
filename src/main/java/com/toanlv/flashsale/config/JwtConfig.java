package com.toanlv.flashsale.config;

import com.toanlv.flashsale.common.config.ApplicationProperties;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

@Configuration
public class JwtConfig {

    private final ApplicationProperties properties;

    public JwtConfig(ApplicationProperties properties) {
        this.properties = properties;
    }

    @Bean
    public KeyStore keyStore() {
        var jwt = properties.jwt();
        try (InputStream in = Files.newInputStream(Path.of(jwt.keystorePath()))) {
            var ks = KeyStore.getInstance("PKCS12");
            ks.load(in, jwt.keystorePassword().toCharArray());
            return ks;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load JWT keystore from: " + jwt.keystorePath(), e);
        }
    }

    @Bean
    public PrivateKey jwtPrivateKey(KeyStore keyStore) {
        var jwt = properties.jwt();
        try {
            var key = keyStore.getKey(
                    jwt.keyAlias(),
                    jwt.keystorePassword().toCharArray());
            if (!(key instanceof PrivateKey pk)) {
                throw new IllegalStateException(
                        "Alias '" + jwt.keyAlias() + "' is not a PrivateKey");
            }
            return pk;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT private key", e);
        }
    }

    @Bean
    public PublicKey jwtPublicKey(KeyStore keyStore) {
        var jwt = properties.jwt();
        try {
            var cert = keyStore.getCertificate(jwt.keyAlias());
            if (cert == null) {
                throw new IllegalStateException(
                        "No certificate found for alias: " + jwt.keyAlias());
            }
            return cert.getPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key", e);
        }
    }

    @Bean
    public JwtParser jwtParser(PublicKey publicKey) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(properties.jwt().issuer())
                .build();
    }
}
