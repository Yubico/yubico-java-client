package com.yubico.client.v2;

import com.yubico.client.v2.impl.YubicoClientImpl;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: lwid
 * Date: 1/25/11
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class YubicoClientTest {

    private YubicoClient client=null;

    @Before
    public void setup() {
        client=YubicoClient.getClient();
        client.setClientId(4711);
    }

    @Test
    public void verifyContruct() {
        assertTrue(client instanceof YubicoClientImpl);
    }

    @Test
    public void testBadOTP() {
        String otp="kaka";
        YubicoResponse response = client.verify(otp);
        assertNotNull(response);
        assertEquals(otp, response.getOtp());
        assertTrue(response.getStatus() == YubicoResponseStatus.BAD_OTP);
    }

    @Test
    public void testReplayedOTP() {
        String otp="cccccccfhcbelrhifnjrrddcgrburluurftrgfdrdifj";
        YubicoResponse response = client.verify(otp);
        assertNotNull(response);
        assertEquals(otp, response.getOtp());
        assertTrue(response.getStatus() == YubicoResponseStatus.REPLAYED_OTP);
    }

}
