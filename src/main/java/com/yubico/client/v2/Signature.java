package com.yubico.client.v2;

import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Signature {

	private final static String HMAC_SHA1 = "HmacSHA1";
	
	public static String calculate(String data, String key) 
	throws SignatureException {
		try {
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1);
	        Mac mac = Mac.getInstance(HMAC_SHA1);
	        mac.init(signingKey);        
	        byte[] raw = mac.doFinal(data.getBytes("UTF-8"));
	        // Base64 encode the result;
	        return Base64.encodeBase64String(raw);
		} catch (Exception e) {
			throw new SignatureException("Failed to generate signature: " + e.getMessage());
		}
	}
}
