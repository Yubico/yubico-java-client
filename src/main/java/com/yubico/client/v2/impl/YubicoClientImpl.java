package com.yubico.client.v2.impl;

import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.YubicoResponse;
import org.apache.log4j.Logger;

import java.net.URL;
import java.net.URLConnection;


/**
 * Created by IntelliJ IDEA.
 * User: lwid
 * Date: 1/25/11
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class YubicoClientImpl extends YubicoClient {
    private static Logger logger = Logger.getLogger(YubicoClientImpl.class);

    public YubicoClientImpl() {}

    public YubicoClientImpl(Integer id) {
        this.clientId=id;
    }

    public YubicoResponse verify(String otp) {
        try {
            String nonce=java.util.UUID.randomUUID().toString().replaceAll("-","");
            URL srv = new URL("http://api.yubico.com/wsapi/2.0/verify?id=" + clientId +
                    "&otp=" + otp +
                    "&timestamp=1" +
                    "&nonce=" + nonce
            );
            URLConnection conn = srv.openConnection();
            YubicoResponse response = new YubicoResponseImpl(conn.getInputStream());

            // Verify the result
            if(!otp.equals(response.getOtp())) {
                logger.warn("OTP mismatch in response, is there a man-in-the-middle?");
                return null;
            }

            if(!nonce.equals(response.getNonce())) {
                logger.warn("Nonce mismatch in response, is there a man-in-the-middle?");
                return null;
            }

            return response;
        } catch (Exception e) {
            logger.warn("Got exception when parsing response from server.", e);
            return null;
        }



    }
}

