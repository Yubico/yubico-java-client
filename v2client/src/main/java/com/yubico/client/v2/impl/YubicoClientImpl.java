package com.yubico.client.v2.impl;

import com.yubico.client.v2.Signature;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.YubicoResponse;
import com.yubico.client.v2.YubicoResponseStatus;
import com.yubico.client.v2.YubicoValidationService;
import com.yubico.client.v2.YubicoValidationTimeout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/* Copyright (c) 2011, Linus Widströmer.  All rights reserved.
   Copyright (c) 2011, Simon Buckle.  All rights reserved.

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

public class YubicoClientImpl extends YubicoClient {
    private static Logger logger = LoggerFactory.getLogger(YubicoClientImpl.class);

    /** {@inheritDoc} */
    public YubicoClientImpl(Integer id) {
        this.clientId=id;
    }
    
    public YubicoClientImpl(Integer id, String key) {
    	this.clientId = id;
    	setKey(key);
    }
    
    public YubicoClientImpl(Integer id, String key, String sync) {
    	this.clientId = id;
    	setKey(key);
    	setSync(sync);
    }

    /** {@inheritDoc} */
    public YubicoResponse verify(String otp) {
    	if (!isValidOTPFormat(otp)) {
    		throw new IllegalArgumentException("The OTP is not a valid format");
    	}
        try {
            String nonce=java.util.UUID.randomUUID().toString().replaceAll("-","");
            String syncParam = "";
            if(sync != null) {
            	syncParam = String.format("&sl=%s", sync);
            }
            String paramStr = String.format("id=%d&nonce=%s&otp=%s%s&timestamp=%s", clientId, nonce, otp, syncParam, "1");
        	        	
            if (key != null) {
            	String s = Signature.calculate(paramStr.toString(), key).replaceAll("\\+", "%2B");
            	paramStr += "&h="; paramStr += s;
            }
            
            String[] wsapiUrls = this.getWsapiUrls();
            List<String> validationUrls = new ArrayList<String>();
            for (int i = 0, len = wsapiUrls.length; i < len; i++) {
            	validationUrls.add(wsapiUrls[i] + "?" + paramStr);
            }
            
            YubicoResponse response = new YubicoValidationService().fetch(validationUrls);
            if(response == null) {
            	throw new YubicoValidationTimeout("Timeout reached while waiting for valid answer.");
            }
            
            // Verify the signature
            if (key != null) {
            	StringBuffer keyValueStr = new StringBuffer();
            	for (Map.Entry<String, String> entry : response.getKeyValueMap().entrySet()) {
            		if ("h".equals(entry.getKey())) { continue; }
            		if (keyValueStr.length() > 0) { keyValueStr.append("&"); }
            		keyValueStr.append(entry.getKey()).append("=").append(entry.getValue());
            	}
            	String signature = Signature.calculate(keyValueStr.toString(), key).trim();
            	if (!response.getH().equals(signature)) {
            		logger.warn("Signatures do not match");
            		return null;
            	}
            }
            
            // NONCE/OTP fields are not returned to the client when sending error codes.
            // If there is an error response, don't need to check them.
            if (!isError(response.getStatus())) {
            	if (response.getOtp() == null || !otp.equals(response.getOtp())) {
                    logger.warn("OTP mismatch in response, is there a man-in-the-middle?");
                    return null;
                }
            	if (response.getNonce() == null || !nonce.equals(response.getNonce())) {
                    logger.warn("Nonce mismatch in response, is there a man-in-the-middle?");
                    return null;
                }
            }
            
            return response;
        } catch (Exception e) {
            logger.warn("Got exception when parsing response from server.", e);
            return null;
        }
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
