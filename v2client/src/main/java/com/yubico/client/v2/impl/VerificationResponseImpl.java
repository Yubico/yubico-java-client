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

package com.yubico.client.v2.impl;

import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.ResponseStatus;
import com.yubico.client.v2.exceptions.YubicoInvalidResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

public class VerificationResponseImpl implements VerificationResponse {

    private String h;
    private String t;
    private ResponseStatus status;
    private String timestamp;
    private String sessioncounter;
    private String sessionuse;
    private String sl;
    private String otp;
    private String nonce;
    
    private final Map<String, String> keyValueMap = new TreeMap<String, String>();

    public VerificationResponseImpl(InputStream inStream) throws IOException, YubicoInvalidResponse {
        if(inStream == null) {
            throw new IOException("InputStream argument was null");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            int ix=inputLine.indexOf("=");
            if(ix==-1) continue; // Invalid line
            String key=inputLine.substring(0,ix);
            String val=inputLine.substring(ix+1);

            if ("h".equals(key)) {
                this.h = val;
            } else if ("t".equals(key)) {
                this.t = val;
            } else if ("otp".equals(key)) {
                this.otp = val;
            } else if ("status".equals(key))  {
                this.status = ResponseStatus.valueOf(val);
            } else if ("timestamp".equals(key)) {
                this.timestamp = val;
            } else if ("sessioncounter".equals(key)) {
                this.sessioncounter = val;
            } else if ("sessionuse".equals(key)) {
                this.sessionuse = val;
            } else if ("sl".equals(key)) {
                this.sl = val;
            } else if ("nonce".equals(key)) {
                this.nonce = val;
            }
            
            keyValueMap.put(key, val);
        }
        in.close();
        
        if(status == null) {
        	throw new YubicoInvalidResponse("Invalid response, contains no status.");
        }
    }

    public Map<String, String> getKeyValueMap() {
    	return keyValueMap;
    }
    
    public String toString() {
        return otp + ":" + status;
    }

    public boolean isOk() {
        return getStatus() == ResponseStatus.OK;
    }

    public String getH() {
        return h;
    }

    public String getT() {
        return t;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSessioncounter() {
        return sessioncounter;
    }

    public String getSessionuse() {
        return sessionuse;
    }

    public String getSl() {
        return sl;
    }

    public String getOtp() {
        return otp;
    }

    public String getNonce() {
        return nonce;
    }

    public String getPublicId() {
		return YubicoClient.getPublicId(otp);
	}
}
