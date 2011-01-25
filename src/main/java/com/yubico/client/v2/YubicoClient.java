package com.yubico.client.v2;

/**
 * Created by IntelliJ IDEA.
 * User: lwid
 * Date: 1/25/11
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
public interface YubicoClient {
    public YubicoResponse verify( Integer id, String otp );
}

