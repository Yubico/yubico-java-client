package com.yubico.jaas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HttpOathOtpLoginModuleTest {

    private static class LocalOathOtpLoginModule extends HttpOathOtpLoginModule {
        @Override
        BufferedReader attemptAuthentication(String authStringEnc) throws IOException {
            if ("Pz8_Pz8_Pz8_Pz8_OmNjY2NjY2dldGJraGJramlkZnZuZ3J0a2tpdWV2YmJnbGt0dWpkdWtjbnZs".equals(authStringEnc)) {
                return new BufferedReader(new StringReader("Authenticated OK"));
            } else {
                return new BufferedReader(new StringReader("Authentication not OK"));
            }
        }
    }

    @Test
    public void testVerifyBadOtp() {
        assertFalse(
            new LocalOathOtpLoginModule()
                .verify_otp("????????????", "ccccccgetbkhtdelccclkdtugcljfjbjikbvhlbkhllb")
        );
    }

    @Test
    public void testVerifyGoodOtp() {
        assertTrue(
            new LocalOathOtpLoginModule()
                .verify_otp("????????????", "ccccccgetbkhbkjidfvngrtkkiuevbbglktujdukcnvl")
        );
    }

}
