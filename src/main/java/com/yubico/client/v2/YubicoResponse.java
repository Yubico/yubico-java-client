package com.yubico.client.v2;

/**
 * Created by IntelliJ IDEA.
 * User: lwid
 * Date: 1/25/11
 * Time: 3:39 PM
 * To change this template use File | Settings | File Templates.
 */
public interface YubicoResponse {
    String getH();
    String getT();
    YubicoResponseStatus getStatus();
    String getTimestamp();
    String getSessioncounter();
    String getSessionuse();
    String getSl();
    String getOtp();
    public String getNonce();
}
