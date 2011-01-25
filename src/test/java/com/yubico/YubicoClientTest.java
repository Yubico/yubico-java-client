/**
 * 
 */
package com.yubico;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author ft
 *
 */
public class YubicoClientTest {

	/**
	 * Test method for {@link com.yubico.YubicoClient#YubicoClient(int)}.
	 */
	@Test
	public void testYubicoClient() {
		YubicoClient c = new YubicoClient(4711);

		assertTrue(YubicoClient.YUBICO_AUTH_SRV_URL.contains("yubico.com"));
	}

	/**
	 * Test method for {@link com.yubico.YubicoClient#getId()}.
	 */
	@Test
	public void testGetId() {
		YubicoClient c = new YubicoClient(4711);

		assertEquals(4711, c.getId());
	}

}
