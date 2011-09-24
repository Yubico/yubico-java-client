package com.yubico.client.v2.impl;

import com.yubico.client.v2.Signature;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.YubicoResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.TreeMap;

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

    /** {@inheritDoc} */
    public YubicoResponse verify(String otp) {
    	if (!isValidOTPFormat(otp)) {
    		throw new IllegalArgumentException("The OTP is not a valid format");
    	}
        try {
            String nonce=java.util.UUID.randomUUID().toString().replaceAll("-","");
            
            Map<String, String> paramsMap = new TreeMap<String, String>();
            paramsMap.put("id", clientId.toString());
            paramsMap.put("nonce", nonce);
            paramsMap.put("timestamp", "1");
            paramsMap.put("otp", otp);
            
            StringBuffer paramStr = new StringBuffer();
        	for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
        		if (paramStr.length() > 0) { paramStr.append("&"); }
        		paramStr.append(entry.getKey()).append("=").append(entry.getValue());
        	}
            
            if (key != null) {
            	String s = Signature.calculate(paramStr.toString(), key).replaceAll("\\+", "%2B");
            	paramStr.append("&h=").append(s);
            }
            
            /* XXX we only use the first wsapi URL - not a real validation v2.0 client yet */
            URL srv = new URL(wsapi_urls[0] + "?" + paramStr.toString());
            URLConnection conn = srv.openConnection();
            YubicoResponse response = new YubicoResponseImpl(conn.getInputStream());

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
            
            // Verify the result
            if (response.getOtp() != null) {
            	if(!otp.equals(response.getOtp())) {
                    logger.warn("OTP mismatch in response, is there a man-in-the-middle?");
                    return null;
                }
            }
            
            if (response.getNonce() != null) {
            	if(!nonce.equals(response.getNonce())) {
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
}
