package com.yubico.client.v2;

/* 	Copyright (c) 2011, Simon Buckle.  All rights reserved.

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

	Written by Simon Buckle <simon@webteq.eu>, September 2011.
*/

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.yubico.client.v2.exceptions.YubicoSignatureException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;

public class Signature {

	private final static String HMAC_SHA1 = "HmacSHA1";

	public static String calculate(String data, byte[] key)
			throws YubicoSignatureException {
		try {
			SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA1);
			Mac mac = Mac.getInstance(HMAC_SHA1);
			mac.init(signingKey);        
			byte[] raw = mac.doFinal(data.getBytes("UTF-8"));
			// Base64 encode the result, use old API call to work on android
			return new String(Base64.encodeBase64(raw));
		} catch (NoSuchAlgorithmException e) {
			throw new YubicoSignatureException("No such algorithm (HMAC_SHA1?)", e);
		} catch (InvalidKeyException e) {
			throw new YubicoSignatureException("Invalid key in signature.", e);
		} catch (IllegalStateException e) {
			throw new YubicoSignatureException("Illegal state in signature", e);
		} catch (UnsupportedEncodingException e) {
			throw new YubicoSignatureException("Unsupported encoding (utf8?)", e);
		}
	}
}
