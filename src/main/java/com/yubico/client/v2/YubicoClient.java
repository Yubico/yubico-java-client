package com.yubico.client.v2;

import com.yubico.client.v2.impl.YubicoClientImpl;

/**
 * Created by IntelliJ IDEA.
 * User: lwid
 * Date: 1/25/11
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class YubicoClient {
    protected Integer clientId;

    public abstract YubicoResponse verify( String otp );

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public static YubicoClient getClient() {
        return new YubicoClientImpl();
    }


}

