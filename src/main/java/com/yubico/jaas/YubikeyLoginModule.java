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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yubico.client.v1.YubicoClient;
import com.yubico.jaas.impl.YubikeyToUserMapImpl;

/**
 * A JAAS module for verifying OTPs (One Time Passwords) against a
 * Yubikey Validation Service.
 *
 * @author Fredrik Thulin <fredrik@yubico.com>
 *
 */
public class YubikeyLoginModule implements LoginModule {
	/* Options for this class.
	 * Note that the options map is shared with other classes, like YubikeyToUserMap.
	 */
	public static final String OPTION_YUBICO_CLIENT_ID			= "clientId";
	public static final String OPTION_YUBICO_ID_REALM			= "id_realm";

	/* JAAS stuff */
	private Subject subject;
	private CallbackHandler callbackHandler;

	/* YubicoClient settings */
	private Integer clientId;
	private YubicoClient yc;

	private YubikeyToUserMapImpl ykmap;
	private String idRealm;

	private final Logger log = LoggerFactory.getLogger(YubikeyLoginModule.class);

	private ArrayList<YubicoPrincipal> principals = new ArrayList<YubicoPrincipal>();

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#abort()
	 */
	public boolean abort() throws LoginException {
		log.trace("In abort()");
		for (YubicoPrincipal p : this.principals) {
			this.subject.getPrincipals().remove(p);			
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#commit()
	 */
	public boolean commit() throws LoginException {
		log.trace("In commit()");
		for (YubicoPrincipal p : this.principals) {
			log.debug("Committing principal {}", p);
			this.subject.getPrincipals().add(p);
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#logout()
	 */
	public boolean logout() throws LoginException {
		log.trace("In logout()");
		for (YubicoPrincipal p : this.principals) {
			this.subject.getPrincipals().remove(p);	
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
	 */
	public void initialize(Subject newSubject, CallbackHandler newCallbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {

		log.debug("Initializing YubikeyLoginModule");
		this.subject = newSubject;
		this.callbackHandler = newCallbackHandler;

		/* Yubico verification client */
		this.clientId = Integer.parseInt(options.get(OPTION_YUBICO_CLIENT_ID).toString());
		this.yc = new YubicoClient(this.clientId);

		/* Realm of principals added after authentication */
		if (options.get(OPTION_YUBICO_ID_REALM) != null) {
			this.idRealm = options.get(OPTION_YUBICO_ID_REALM).toString();
		}
		
		this.ykmap = new YubikeyToUserMapImpl(options);
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#login()
	 */
	public boolean login() throws LoginException {
		boolean validated = false;
		NameCallback nameCb = new NameCallback("Enter username: ");

		log.debug("Begin OTP login");

		if (callbackHandler == null) {
			throw new LoginException("No callback handler available in login()");
		}

		List<String> otps = get_tokens(nameCb);

		for (String otp : otps) {
			log.trace("Checking OTP {}", otp);

			if (this.yc.verify(otp)) {
				String publicId = this.yc.getPublicId(otp);
				log.info("OTP verified successfully (YubiKey {})", publicId);
				if (is_right_user(nameCb.getName(), publicId)) {
					this.principals.add(new YubicoPrincipal(publicId, this.idRealm));
					/* Don't just return here, we want to "consume" all OTPs if 
					 * more than one is provided.
					 */
					validated = true;
				}
			}
			if (! validated) {
				log.info("OTP did NOT verify");
			}
		}
		return validated;
	}

	private boolean is_right_user(String username, String publicId) {
		log.debug("Check if YubiKey {} belongs to user {}", publicId, username);
		return this.ykmap.is_right_user(username, publicId);
	}

	private List<String> get_tokens(NameCallback nameCb) throws LoginException {
		MultiValuePasswordCallback mv_passCb = new MultiValuePasswordCallback("Enter authentication tokens: ", false);
		List<String> result = new ArrayList<String>();

		try {
			/* Fetch a password using the callbackHandler */
			callbackHandler.handle(new Callback[] { nameCb, mv_passCb });

			for (char[] c : mv_passCb.getSecrets()) {
				String s = new String(c);
				/* Check that OTP is at least 32 chars before we verify it. User might have entered
				 * some other password instead of an OTP, and we don't want to send that, possibly
				 * in clear text, over the network.
				 */
				if (s.length() < 32) {
					log.debug("Skipping token, not a valid YubiKey OTP (too short, {} < 32)", s.length());
				} else {
					result.add(s);
				}
			}
		} catch (UnsupportedCallbackException ex) {
			log.error("Callback type not supported", ex);
		} catch (IOException ex) {
			log.error("CallbackHandler failed", ex);
		}

		return result;
	}

	/**
	 * A class that extends PasswordCallback to keep a list of all values
	 * set using setPassword(). If the application using this JAAS plugin
	 * wants to pass us multiple authentication factors, it just calls
	 * setPassword() more than once in the CallbackHandler.
	 */
	public class MultiValuePasswordCallback extends PasswordCallback {
		private static final long serialVersionUID = 5362005708680822656L;
		private ArrayList<char[]> secrets = new ArrayList<char[]>();

		public MultiValuePasswordCallback(String prompt, boolean echoOn) {
			super(prompt, echoOn);
		}

		/**
		 * @return Returns all the secrets.
		 */
		public List<char[]> getSecrets() {
			return secrets;
		}

		/**
		 * @param password A secret to add to our list.
		 */
		public void setPassword(char[] password) {
			this.secrets.add(password);
		}

		/**
		 * Tries to clear all the passwords from memory.
		 */
		public void clearPassword() {
			for (char pw[] : this.secrets) {
				for (int i = 0; i < pw.length; i++) {
					pw[i] = 0;
				}
			}

			/* Now discard the list. */
			this.secrets = new ArrayList<char []>();
		}
	}
}
