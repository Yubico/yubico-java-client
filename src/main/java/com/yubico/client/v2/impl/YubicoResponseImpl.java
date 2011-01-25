package com.yubico.client.v2.impl;

import com.yubico.client.v2.YubicoResponse;
import com.yubico.client.v2.YubicoResponseStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: lwid
 * Date: 1/25/11
 * Time: 3:11 PM
 * To change this template use File | Settings | File Templates.
 */
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

    public YubicoResponseImpl(InputStream inStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            int ix=inputLine.indexOf("=");
            if(ix==-1) continue; // Invalid line
            String key=inputLine.substring(0,ix);
            String val=inputLine.substring(ix+1);

            if("h".equals(key))
                this.setH(val);

            if("t".equals(key))
                this.setT(val);

            if("otp".equals(key))
                this.setOtp(val);

            if("status".equals(key))  {
                this.setStatus(YubicoResponseStatus.valueOf(val));
            }

            if("timestamp".equals(key))
                this.setTimestamp(val);

            if("sessioncounter".equals(key))
                this.setSessioncounter(val);

            if("sessionuse".equals(key))
                this.setSessionuse(val);

            if("sl".equals(key))
                this.setSl(val);

            if("nonce".equals(key))
                this.setNonce(val);
        }
        in.close();
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

}
