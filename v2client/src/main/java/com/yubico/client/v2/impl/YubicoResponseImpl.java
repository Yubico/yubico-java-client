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
import com.yubico.client.v2.YubicoResponse;
import com.yubico.client.v2.YubicoResponseStatus;
import com.yubico.client.v2.exceptions.YubicoInvalidResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

public class YubicoResponseImpl implements YubicoResponse {

    private String h;
    private String t;
    private YubicoResponseStatus status;
    private String timestamp;
    private String sessioncounter;
    private String sessionuse;
    private String sl;
    private String otp;
    private String nonce;
    
    private Map<String, String> keyValueMap = new TreeMap<String, String>();

    public YubicoResponseImpl(InputStream inStream) throws IOException, YubicoInvalidResponse {
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
                this.setH(val);
            } else if ("t".equals(key)) {
            	this.setT(val);
            } else if ("otp".equals(key)) {
            	this.setOtp(val);
            } else if ("status".equals(key))  {
                this.setStatus(YubicoResponseStatus.valueOf(val));
            } else if ("timestamp".equals(key)) {
                this.setTimestamp(val);
            } else if ("sessioncounter".equals(key)) {
                this.setSessioncounter(val);
            } else if ("sessionuse".equals(key)) {
                this.setSessionuse(val);
            } else if ("sl".equals(key)) {
                this.setSl(val);
            } else if ("nonce".equals(key)) {
                this.setNonce(val);
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

    public String getH() {
        return h;
    }

    public void setH(String h) {
        this.h = h;
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public YubicoResponseStatus getStatus() {
        return status;
    }

    public void setStatus(YubicoResponseStatus status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessioncounter() {
        return sessioncounter;
    }

    public void setSessioncounter(String sessioncounter) {
        this.sessioncounter = sessioncounter;
    }

    public String getSessionuse() {
        return sessionuse;
    }

    public void setSessionuse(String sessionuse) {
        this.sessionuse = sessionuse;
    }

    public String getSl() {
        return sl;
    }

    public void setSl(String sl) {
        this.sl = sl;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

	public String getPublicId() {
		return YubicoClient.getPublicId(otp);
	}
}
