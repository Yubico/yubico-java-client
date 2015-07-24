package com.yubico.client.v2;

import com.yubico.client.v2.exceptions.YubicoVerificationException;
import com.yubico.client.v2.exceptions.YubicoVerificationFailure;
import org.junit.Before;
import org.junit.Test;

import static com.yubico.client.v2.OtpUtils.getPublicId;
import static com.yubico.client.v2.OtpUtils.isValidOTPFormat;
import static org.junit.Assert.*;

/* Copyright (c) 2011, Linus Widströmer.  All rights reserved.
   Copyright (c) 2012, Yubico AB. All rights reserved.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:
  
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
  
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following
     disclaimer in the documentation and/or other materials provided
     with the distribution.
 
   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
   CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
   INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
   MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
   TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
   ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
   TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
   THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
   SUCH DAMAGE.
 
   Written by Linus Widströmer <linus.widstromer@it.su.se>, January 2011.
*/

public class YubicoClientTest {

    private YubicoClient client = null;

    /*
     * API key for signing/verifying request/response. Don't reuse this one (or
     * you will have zero security), get your own at
     * https://upgrade.yubico.com/getapikey/
     */
    private final int clientId = 21188;
    private final String apiKey = "p38Z7DuEB/JC/LbDkkjmvMRB5GI=";

    @Before
    public void setup() {
        client = new YubicoClientBuilder()
                .clientId(clientId)
                .key(apiKey)
                .build();
    }

    // YubiCloud is known to reply with bad signatures when given a bad OTP.
    // See https://github.com/Yubico/yubikey-val/issues/8
    @Test(expected = YubicoVerificationFailure.class)
    public void testBadOTP() throws YubicoVerificationException, YubicoVerificationFailure {
    	String otp="11111111111111111111111111111111111";
        client.verify(otp);
    }
    
    @Test
    public void testReplayedOTP() throws YubicoVerificationException, YubicoVerificationFailure {
        String otp = "cccccccfhcbelrhifnjrrddcgrburluurftrgfdrdifj";
        VerificationResponse response = client.verify(otp);
        assertNotNull(response);
        assertEquals(otp, response.getOtp().get());
        assertEquals(ResponseStatus.REPLAYED_OTP, response.getStatus());
    }

    @Test
    public void testSignature() throws YubicoVerificationException, YubicoVerificationFailure {
        String otp = "cccccccfhcbelrhifnjrrddcgrburluurftrgfdrdifj";

        VerificationResponse response = client.verify(otp);
        assertNotNull(response);
        assertEquals(otp, response.getOtp().get());
        assertEquals(ResponseStatus.REPLAYED_OTP, response.getStatus());
    }

    @Test
    public void testBadSignature() throws YubicoVerificationException, YubicoVerificationFailure {
        String otp = "cccccccfhcbelrhifnjrrddcgrburluurftrgfdrdifj";
        client = new YubicoClientBuilder()
                .clientId(clientId)
                .key("bAX9u78e8BRHXPGDVV3lQUm4yVw=")
                .build();
        VerificationResponse response = client.verify(otp);
        assertEquals(ResponseStatus.BAD_SIGNATURE, response.getStatus());
    }
    
    @Test
    public void testUnPrintableOTP() {
    	String otp = new String(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
    	assertFalse(isValidOTPFormat(otp));
    }
    
    @Test
    public void testShortOTP() {
    	String otp = "cccccc";
    	assertFalse(isValidOTPFormat(otp));
    }
    
    @Test
    public void testLongOTP() {
    	String otp = "cccccccccccccccccccccccccccccccccccccccccccccccccc";
    	assertFalse(isValidOTPFormat(otp));
    }
    
    @Test
    public void testTwoQueries() throws YubicoVerificationException, YubicoVerificationFailure {
    	String otp = "cccccccfhcbelrhifnjrrddcgrburluurftrgfdrdifj";
    	VerificationResponse response = client.verify(otp);
    	assertEquals(ResponseStatus.REPLAYED_OTP, response.getStatus());
    	VerificationResponse response2 = client.verify(otp);
    	assertEquals(ResponseStatus.REPLAYED_OTP, response2.getStatus());
    }
    
    @Test(expected=YubicoVerificationException.class)
    public void testBadUrls() throws YubicoVerificationException, YubicoVerificationFailure {
    	String otp = "cccccccfhcbelrhifnjrrddcgrburluurftrgfdrdifj";
        client = new YubicoClientBuilder()
                .clientId(clientId)
                .key(apiKey)
                .addApiUrls("http://www.example.com/wsapi/2.0/verify")
                .addApiUrls("http://api2.example.com/wsapi/2.0/verify")
                .build();
    	client.verify(otp);
    }
    
    @Test
    public void testGoodAndBadUrls() throws Exception {
    	String otp = "cccccccfhcbelrhifnjrrddcgrburluurftrgfdrdifj";
        client = new YubicoClientBuilder()
                .clientId(clientId)
                .key(apiKey)
                .addApiUrls("http://api.example.com/wsapi/2.0/verify")
                .addApiUrls("http://www.example.com/wsapi/2.0/verify")
                .addApiUrls("http://api3.yubico.com/wsapi/2.0/verify")
                .build();
    	VerificationResponse response = client.verify(otp);
    	assertEquals(ResponseStatus.REPLAYED_OTP, response.getStatus());
    }
    
    @Test(expected=NullPointerException.class)
    public void testNullOTPPublicId() {    	
    	getPublicId(null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyOTPPublicId() {
        getPublicId("");
    }
    
    @Test
    public void testValidOTPPublicId() {
    	String testOtp = "cccccccfhcbelrhifnjrrddcgrburluurftrgfdrdifj";
    	String testPublicId = "cccccccfhcbe";
    	String resultPublicId = getPublicId(testOtp);
    	assertEquals(testPublicId, resultPublicId);
    }
}
