/**
 * Copyright (c) 2011, Yubico AB.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 *  CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 *  TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 *  THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 */
package com.yubico.jaas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JAAS module for verifying OATH OTPs (One Time Passwords) using
 * HTTP Basic Auth to a backend server doing the real authentication.
 *
 * @author Fredrik Thulin (fredrik@yubico.com)
 *
 */
public class HttpOathOtpLoginModule implements LoginModule {

	/* Options */
	public static final String OPTION_HTTPOATHOTP_PROTECTED_URL			= "protectedUrl";
	public static final String OPTION_HTTPOATHOTP_EXPECTED_OUTPUT		= "expectedOutput";
	public static final String OPTION_HTTPOATHOTP_MIN_LENGTH			= "minLength";
	public static final String OPTION_HTTPOATHOTP_MAX_LENGTH			= "maxLength";
	public static final String OPTION_HTTPOATHOTP_REQUIRE_ALL_DIGITS	= "requireAllDigits";
	public static final String OPTION_HTTPOATHOTP_ID_REALM				= "id_realm";

	public String protectedUrl;
	public String expectedOutput = "Authenticated OK";
	public int minLength = 6;
	public int maxLength = 12;
	public boolean requireAllDigits = true;
	public String idRealm;

	/* JAAS stuff */
	private Subject subject;
	private CallbackHandler callbackHandler;

	private final Logger log = LoggerFactory.getLogger(HttpOathOtpLoginModule.class);

	private YubikeyPrincipal principal;

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#abort()
	 */
	public boolean abort() throws LoginException {
		subject.getPrincipals().remove(this.principal);
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#commit()
	 */
	public boolean commit() throws LoginException {
		log.trace("In commit()");
		subject.getPrincipals().add(this.principal);
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
	 */
	public void initialize(Subject newSubject, CallbackHandler newCallbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {

		log.trace("Initializing HTTP OATH OTP LoginModule");

		this.subject = newSubject;
		this.callbackHandler = newCallbackHandler;

		this.protectedUrl = options.get(OPTION_HTTPOATHOTP_PROTECTED_URL).toString();

		if (options.get(OPTION_HTTPOATHOTP_EXPECTED_OUTPUT) != null) {
			this.expectedOutput = options.get(OPTION_HTTPOATHOTP_EXPECTED_OUTPUT).toString();
		}

		if (options.get(OPTION_HTTPOATHOTP_MIN_LENGTH) != null) {
			this.minLength = Integer.parseInt(options.get(OPTION_HTTPOATHOTP_MIN_LENGTH).toString());
		}
		if (options.get(OPTION_HTTPOATHOTP_MAX_LENGTH) != null) {
			this.maxLength = Integer.parseInt(options.get(OPTION_HTTPOATHOTP_MAX_LENGTH).toString());
		}
		if (options.get(OPTION_HTTPOATHOTP_REQUIRE_ALL_DIGITS) != null) {
			String s = options.get(OPTION_HTTPOATHOTP_REQUIRE_ALL_DIGITS).toString();
			if (s.equals("true")) {
				this.requireAllDigits = true;
			} else if (s.equals("false")) {
				this.requireAllDigits = false;
			} else {
				log.error("Bad value for option {}", OPTION_HTTPOATHOTP_REQUIRE_ALL_DIGITS);
			}
		}
		/* Realm of principals added after authentication */
		if (options.get(OPTION_HTTPOATHOTP_ID_REALM) != null) {
			this.idRealm = options.get(OPTION_HTTPOATHOTP_ID_REALM).toString();
		}
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#login()
	 */
	public boolean login() throws LoginException {
		log.trace("Begin OATH OTP login");

		if (callbackHandler == null) {
			throw new LoginException("No callback handler available in login()");
		}

		NameCallback nameCb = new NameCallback("Enter username: ");

		List<String> otps = get_tokens(nameCb);

		for (String otp : otps) {
			String userName = nameCb.getName();

			log.trace("Checking OATH OTP for user {}", userName);

			if (verify_otp(userName, otp)) {
				log.info("OATH OTP verified successfully");
				principal = new YubikeyPrincipal(userName, this.idRealm);
				return true;
			}
			log.info("OATH OTP did NOT verify");
		}
		throw new LoginException("OATH OTP verification failed");
	}

	/**
	 * Access protectedUrl using userName and otp for basic auth.
	 * Check if what we get back contains expectedOutput.
	 * @param userName
	 * @param otp
	 * @return boolean
	 */
	private boolean verify_otp(String userName, String otp) {
		try {
			String authString = userName + ":" + otp;
			String authStringEnc = Base64.encodeBase64URLSafeString(authString.getBytes());

			BufferedReader in = attemptAuthentication(authStringEnc);

			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.contains(expectedOutput)) {
					return true;
				}
			}
		} catch (Exception ex) {
			log.error("Failed verifying OATH OTP :", ex);
		}
		return false;
	}

	private BufferedReader attemptAuthentication(String authStringEnc) throws IOException {
		URL url = new URL(this.protectedUrl);
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
		conn.connect();

		return new BufferedReader(new InputStreamReader(
                conn.getInputStream()
		));
	}

	private List<String> get_tokens(NameCallback nameCb) throws LoginException {
		MultiValuePasswordCallback mv_passCb = new MultiValuePasswordCallback("Enter authentication tokens: ", false);
		List<String> result = new ArrayList<String>();

		try {
			/* Fetch a password using the callbackHandler */
			callbackHandler.handle(new Callback[] { nameCb, mv_passCb });

			for (char[] c : mv_passCb.getSecrets()) {
				String s = new String(c);
				/* Check that OTP looks like OATH before we verify it. User might have entered
				 * some other password instead of an OTP, and we don't want to send that, possibly
				 * in clear text, over the network.
				 */
				if (s.length() < this.minLength) {
					log.info("Skipping token, not a valid OATH OTP token (too short, {} < {})", s.length(), this.minLength);
				} else if (s.length() > this.maxLength) {
					log.info("Skipping token, not a valid OATH OTP token (too long, {} > {})", s.length(), this.maxLength);
				} else {
					if (this.requireAllDigits) {
						if (s.matches("^[0-9]+$")) {
							result.add(s);
						} else {
							log.info("Skipping token, not a valid OATH OTP token (non-digits not allowed)");
						}
					} else {
						result.add(s);
					}
				}
			}
		} catch (UnsupportedCallbackException ex) {
			log.error("Callback type not supported", ex);
		} catch (IOException ex) {
			log.error("CallbackHandler failed", ex);
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#logout()
	 */
	public boolean logout() throws LoginException {
		log.trace("In logout()");
		subject.getPrincipals().remove(this.principal);
		return false;
	}
}
