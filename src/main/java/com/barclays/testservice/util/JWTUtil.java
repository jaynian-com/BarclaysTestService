package com.barclays.testservice.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JWTUtil {

    @Value("${jwt.secret-key}") String jwtSecretKey;
    @Value("${jwt.expiry-seconds:600}") long jwtExpirySeconds;

    public String generateToken(String username) {

        var claimsSet = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer("self")
                .expirationTime(Date.from(Instant.now().plusSeconds(jwtExpirySeconds)))
                .build();

        var signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
        );

        try {
            var signer = new MACSigner(jwtSecretKey);
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Error creating JWT", e);
        }
    }
}
