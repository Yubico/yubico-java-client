package com.yubico.client.v2.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.exceptions.YubicoInvalidResponse;

/* Copyright (c) 2011, Linus Widströmer.  All rights reserved.

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

public class VerificationResponseImplTest {

    @Test
    public void testParserForNullArg() throws YubicoInvalidResponse {
        try {
			new VerificationResponseImpl(null);
            fail("Expected an IOException to be thrown.");
        } catch (IOException ignored) {
        }
    }

    @Test
    public void testToString() throws YubicoInvalidResponse {
        String testData=    "h=lPuwrWh8/5ZuRBN1q+v7/pCOfYo=\n" +
                            "t=2011-01-26T11:48:21Z0323\n" +
                            "otp=cccccccfhcbeceeiinhjfjhfjutfvrjetfkjlhbduvdd\n" +
                            "nonce=askjdnkagfdgdgdgggggggddddddddd\n" +
                            "timestamp=4711\n" +
                            "sessioncounter=42\n" +
                            "sessionuse=666\n" +
                            "sl=foo\n" +
                            "status=REPLAYED_OTP\n";

        try {
            VerificationResponse response = new VerificationResponseImpl(new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8)));
            assertTrue(response.toString().contains("REPLAYED_OTP"));
            assertTrue(response.toString().contains("cccccccfhcbeceeiinhjfjhfjutfvrjetfkjlhbduvdd"));
        } catch (IOException ioe) {
            fail("Encountered an exception");
        }
    }

    @Test
    public void testParser() throws YubicoInvalidResponse {
        String testData=    "h=lPuwrWh8/5ZuRBN1q+v7/pCOfYo=\n" +
                            "t=2011-01-26T11:48:21Z0323\n" +
                            "otp=cccccccfhcbeceeiinhjfjhfjutfvrjetfkjlhbduvdd\n" +
                            "nonce=askjdnkagfdgdgdgggggggddddddddd\n" +
                            "timestamp=4711\n" +
                            "sessioncounter=42\n" +
                            "sessionuse=666\n" +
                            "sl=foo\n" +
                            "status=REPLAYED_OTP\n";

        try {
            VerificationResponse response = new VerificationResponseImpl(new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8)));
            assertEquals("2011-01-26T11:48:21Z0323",response.getT());
            assertEquals("lPuwrWh8/5ZuRBN1q+v7/pCOfYo=", response.getH());
            assertEquals("REPLAYED_OTP", response.getStatus().toString());
            assertEquals("cccccccfhcbeceeiinhjfjhfjutfvrjetfkjlhbduvdd", response.getOtp());
            assertEquals("askjdnkagfdgdgdgggggggddddddddd", response.getNonce());
            assertEquals("4711", response.getTimestamp());
            assertEquals("42", response.getSessioncounter());
            assertEquals("foo", response.getSl());
            assertEquals("666", response.getSessionuse());
            assertEquals("cccccccfhcbe", response.getPublicId());

        } catch (IOException ioe) {
            fail("Encountered an exception");
        }
    }
    
    @Test(expected=YubicoInvalidResponse.class)
    public void testBrokenResponse() throws YubicoInvalidResponse {
    	String testData = 	"foo=bar\n" +
    						"kaka=blahonga\n";
    	try {
    		new VerificationResponseImpl(new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8)));
    	} catch (IOException ioe) {
    		fail("Encountered an exception");
    	}
    }
}

