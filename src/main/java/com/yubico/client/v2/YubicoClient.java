package com.yubico.client.v2;

import com.yubico.client.v2.impl.YubicoClientImpl;

/* Copyright (c) 2011, Linus Widströmer.  All rights reserved.
   Copyright (c) 2011, Yubico AB.  All rights reserved.

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

public abstract class YubicoClient {
    protected Integer clientId;

    public abstract YubicoResponse verify( String otp );

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public static YubicoClient getClient() {
        return new YubicoClientImpl();
    }

    /**
	 * Extract the public ID of a Yubikey from an OTP it generated.
	 *
	 * @param otp	The OTP to extract ID from, in modhex format.
	 *
	 * @return string	Public ID of Yubikey that generated otp. Between 0 and 12 characters.
	 */
	public static String getPublicId(String otp) {
		Integer len = otp.length();

		/* The OTP part is always the last 32 bytes of otp. Whatever is before that
		 * (if anything) is the public ID of the Yubikey. The ID can be set to ''
		 * through personalization.
		 */
		return otp.substring(0, len - 32);
	}
}
