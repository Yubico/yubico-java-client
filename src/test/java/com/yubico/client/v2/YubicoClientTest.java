package com.yubico.client.v2;

import com.yubico.client.v2.impl.YubicoClientImpl;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

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

public class YubicoClientTest {

    private YubicoClient client=null;

    @Before
    public void setup() {
        client=YubicoClient.getClient();
        client.setClientId(4711);
    }

    @Test
    public void verifyContruct() {
        assertTrue(client instanceof YubicoClientImpl);
    }

    @Test
    public void testBadOTP() {
        String otp="kaka";
        YubicoResponse response = client.verify(otp);
        assertNotNull(response);
        assertEquals(otp, response.getOtp());
        assertTrue(response.getStatus() == YubicoResponseStatus.BAD_OTP);
    }

    @Test
    public void testReplayedOTP() {
        String otp="cccccccfhcbelrhifnjrrddcgrburluurftrgfdrdifj";
        YubicoResponse response = client.verify(otp);
        assertNotNull(response);
        assertEquals(otp, response.getOtp());
        assertTrue(response.getStatus() == YubicoResponseStatus.REPLAYED_OTP);
    }

}
