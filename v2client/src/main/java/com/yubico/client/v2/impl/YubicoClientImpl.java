/* Copyright (c) 2011, Linus Widströmer.  All rights reserved.

   Copyright (c) 2011, Simon Buckle.  All rights reserved.
   
   Copyright (c) 2012, Yubico AB.  All rights reseved.

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
   
   Modified by Simon Buckle <simon@webteq.eu>, September 2011.
    - Added support for generating and validating signatures
*/

package com.yubico.client.v2.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.yubico.client.v2.Signature;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.YubicoResponse;
import com.yubico.client.v2.YubicoResponseStatus;
import com.yubico.client.v2.YubicoValidationService;
import com.yubico.client.v2.exceptions.YubicoSignatureException;
import com.yubico.client.v2.exceptions.YubicoValidationException;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;

public class YubicoClientImpl extends YubicoClient {
	private YubicoValidationService validationService;
	
	public YubicoClientImpl() {
		validationService = new YubicoValidationService();
	}
	
    public YubicoClientImpl(Integer id) {
    	this();
        this.clientId=id;
    }
    
    public YubicoClientImpl(Integer id, String key) {
    	this(id);
    	setKey(key);
    }
    
    public YubicoClientImpl(Integer id, String key, String sync) {
    	this(id, key);
    	setSync(sync);
    }

    /** {@inheritDoc} */
    public YubicoResponse verify(String otp) throws YubicoValidationException, YubicoValidationFailure {
    	if (!isValidOTPFormat(otp)) {
    		throw new IllegalArgumentException("The OTP is not a valid format");
    	}
    	Map<String,String> requestMap = new TreeMap<String, String>();
    	String nonce=java.util.UUID.randomUUID().toString().replaceAll("-","");
    	requestMap.put("nonce", nonce);
    	requestMap.put("id", clientId.toString());
    	requestMap.put("otp", otp);
    	requestMap.put("timestamp", "1");
    	if(sync != null) {
    		requestMap.put("sl", sync);
    	}
    	String paramStr = "";
    	for(Entry<String,String> entry : requestMap.entrySet()) {
    		if(!paramStr.isEmpty()) {
    			paramStr += "&";
    		}
    		try {
				paramStr += entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new YubicoValidationException("Failed to encode parameter.", e);
			}
    	}
    	
    	if (key != null) {
    		String s;
			try {
				s = URLEncoder.encode(Signature.calculate(paramStr.toString(), key), "UTF-8");
			} catch (YubicoSignatureException e) {
				throw new YubicoValidationException("Failed signing of request", e);
			} catch (UnsupportedEncodingException e) {
				throw new YubicoValidationException("Failed to encode signature", e);
			}
    		paramStr += "&h=" + s;
    	}

    	String[] wsapiUrls = this.getWsapiUrls();
    	List<String> validationUrls = new ArrayList<String>();
    	for(String wsapiUrl : wsapiUrls) {
    		validationUrls.add(wsapiUrl + "?" + paramStr);
    	}

    	YubicoResponse response = validationService.fetch(validationUrls, userAgent);

    	// Verify the signature
    	if (key != null) {
    		StringBuffer keyValueStr = new StringBuffer();
    		for (Map.Entry<String, String> entry : response.getKeyValueMap().entrySet()) {
    			if ("h".equals(entry.getKey())) { continue; }
    			if (keyValueStr.length() > 0) { keyValueStr.append("&"); }
    			keyValueStr.append(entry.getKey()).append("=").append(entry.getValue());
    		}
			try {
				String signature = Signature.calculate(keyValueStr.toString(), key).trim();
				if (!response.getH().equals(signature) &&
						!response.getStatus().equals(YubicoResponseStatus.BAD_SIGNATURE)) {
					// don't throw a ValidationFailure if the server said bad signature, in that
					//  case we probably have the wrong key/id and want to check it.
	    			throw new YubicoValidationFailure("Signatures do not match");
	    		}
			} catch (YubicoSignatureException e) {
				throw new YubicoValidationException("Failed to calculate the response signature.", e);
			}
    		
    	}

    	// NONCE/OTP fields are not returned to the client when sending error codes.
    	// If there is an error response, don't need to check them.
    	if (!isError(response.getStatus())) {
    		if (response.getOtp() == null || !otp.equals(response.getOtp())) {
    			throw new YubicoValidationFailure("OTP mismatch in response, is there a man-in-the-middle?");
    		}
    		if (response.getNonce() == null || !nonce.equals(response.getNonce())) {
    			throw new YubicoValidationFailure("Nonce mismatch in response, is there a man-in-the-middle?");
    		}
    	}

    	return response;
    }
    
    /**
     * Function is used to determine if the response status is an error or not.
     * 
     * @param status
     * @return boolean
     */
    private boolean isError(YubicoResponseStatus status) 
    {
    	return (YubicoResponseStatus.BACKEND_ERROR.equals(status) || 
    			YubicoResponseStatus.BAD_OTP.equals(status) || 
    			YubicoResponseStatus.BAD_SIGNATURE.equals(status) ||
    			YubicoResponseStatus.NO_SUCH_CLIENT.equals(status) ||
    			YubicoResponseStatus.MISSING_PARAMETER.equals(status));
    }
}
