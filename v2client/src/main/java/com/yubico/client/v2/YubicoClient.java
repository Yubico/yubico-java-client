/* Copyright (c) 2011, Linus Widströmer.  All rights reserved.
   Copyright (c) 2011-2012, Yubico AB.  All rights reserved.
   Copyright (c) 2011, Simon Buckle.  All rights reserved.

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

package com.yubico.client.v2;

import com.yubico.client.v2.exceptions.YubicoVerificationException;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.impl.YubicoClientImpl;
import org.apache.commons.codec.binary.Base64;

/**
 * Base class for doing YubiKey validations using version 2 of the validation protocol.
 */

public abstract class YubicoClient {
    protected Integer clientId;
    protected byte[] key;
    protected Integer sync;
    protected String wsapi_urls[] = {
               "https://api.yubico.com/wsapi/2.0/verify",
               "https://api2.yubico.com/wsapi/2.0/verify",
               "https://api3.yubico.com/wsapi/2.0/verify",
               "https://api4.yubico.com/wsapi/2.0/verify",
               "https://api5.yubico.com/wsapi/2.0/verify"
    		};
    
    protected String userAgent = "yubico-java-client/" + Version.version +
            " (" + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + ")";

    /**
     * Validate an OTP using a webservice call to one or more ykval validation servers.
     *
     * @param otp YubiKey OTP
     * @return result of the webservice validation operation
     * @throws com.yubico.client.v2.exceptions.YubicoVerificationException for validation errors, like unreachable servers
     * @throws YubicoValidationFailure for validation failures, like non matching OTPs in request and response
     * @throws IllegalArgumentException for arguments that are not correctly formatted OTP strings.
     */
    public abstract VerificationResponse verify(String otp) throws YubicoVerificationException, YubicoValidationFailure;

    /**
     * Get the ykval client identifier used to identify the application.
     * @return ykval client identifier
     * @see YubicoClient#setClientId(Integer)
     */
    public Integer getClientId() {
        return clientId;
    }

    /**
     * Set the ykval client identifier, used to identify the client application to
     * the validation servers. Such validation is only required for non-https-v2.0
     * validation queries, where the clientId tells the server what API key (shared
     * secret) to use to validate requests and sign responses.
     *
     * You can get a clientId and API key for the YubiCloud validation service at
     * https://upgrade.yubico.com/getapikey/
     *
     * @param clientId  ykval client identifier
     */
    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    /**
     * Set api key to be used for signing requests
     * @param key ykval client key
     * @see YubicoClient#setClientId(Integer)
     */
    public void setKey(String key) {
        this.key = Base64.decodeBase64(key.getBytes());
    }
    
    /**
     * Get the api key that is used for signing requests
     * @return ykval client key
     * @see YubicoClient#setClientId(Integer)
     */
    public String getKey() {
        return new String(Base64.encodeBase64(this.key));
    }
    
    /**
     * Set the sync percentage required for a successful auth.
     * Default is to let the server decide.
     * @param sync percentage or strings 'secure' or 'fast'
     */
    public void setSync(Integer sync) {
    	this.sync = sync;
    }
    
    /**
     * Get the list of URLs that will be used for validating OTPs.
     * @return list of base URLs
     */
    public String[] getWsapiUrls() {
		return wsapi_urls;
	}

    /**
     * Configure what URLs to use for validating OTPs. These URLs will have
     * all the necessary parameters appended to them. Example :
     * {"https://api.yubico.com/wsapi/2.0/verify"}
     * @param wsapi  list of base URLs
     */
	public void setWsapiUrls(String[] wsapi) {
		this.wsapi_urls = wsapi;
	}
	
	/**
	 * Set user agent to be used in request to validation server
	 * @param userAgent the user agent used in requests
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	
	/**
	 * Instantiate a YubicoClient object.
     *
     * @param clientId Retrieved from https://upgrade.yubico.com/getapikey
	 * @return  client that can be used to validate YubiKey OTPs
	 */
	public static YubicoClient getClient(Integer clientId, String key) {
        return new YubicoClientImpl(clientId, key);
    }

    /**
	 * Extract the public ID of a YubiKey from an OTP it generated.
	 *
	 * @param otp	The OTP to extract ID from, in modhex format.
	 *
	 * @return string	Public ID of YubiKey that generated otp. Between 0 and 12 lower-case characters.
	 * 
	 * @throws IllegalArgumentException for arguments that are null or too short to be valid OTP strings. 
	 */
	public static String getPublicId(String otp) {
		if ((otp == null) || (otp.length() < OTP_MIN_LEN)){
			//not a valid OTP format, throw an exception
			throw new IllegalArgumentException("The OTP is too short to be valid");
		}
		
		Integer len = otp.length();

		/* The OTP part is always the last 32 bytes of otp. Whatever is before that
		 * (if anything) is the public ID of the YubiKey. The ID can be set to ''
		 * through personalization.
		 */
		return otp.substring(0, len - 32).toLowerCase();
	}
	
	private static final Integer OTP_MIN_LEN = 32;
	private static final Integer OTP_MAX_LEN = 48;
	/**
	 * Determines whether a given OTP is of the correct length
	 * and only contains printable characters, as per the recommendation.
	 *
	 * @param otp The OTP to validate
	 * @return boolean Returns true if it's valid; false otherwise
	 * 
	 */
	public static boolean isValidOTPFormat(String otp) {
		if (otp == null){
			return false;
		}		
		int len = otp.length();
		for (char c : otp.toCharArray()) {
			if (c < 0x20 || c > 0x7E) {
				return false;
			}
		}
		return OTP_MIN_LEN <= len && len <= OTP_MAX_LEN;
	}
}
