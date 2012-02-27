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
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.YubicoResponse;
import com.yubico.client.v2.YubicoResponseStatus;

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
	public static final String OPTION_YUBICO_CLIENT_KEY         = "clientKey";
	public static final String OPTION_YUBICO_ID_REALM			= "id_realm";
	public static final String OPTION_YUBICO_SOFT_FAIL_NO_OTPS	= "soft_fail_on_no_otps";
	public static final String OPTION_YUBICO_WSAPI_URLS			= "wsapi_urls";
	public static final String OPTION_YUBICO_USERMAP_CLASS      = "usermap_class";
	public static final String OPTION_YUBICO_SYNC_POLICY        = "sync_policy";

	/* JAAS stuff */
	private Subject subject;
	private CallbackHandler callbackHandler;

	/* YubicoClient settings */
	private Integer clientId;
	private YubicoClient yc;

	private boolean soft_fail_on_no_otps;

	private YubikeyToUserMap ykmap;
	private String idRealm;

	private final Logger log = LoggerFactory.getLogger(YubikeyLoginModule.class);

	private ArrayList<YubikeyPrincipal> principals = new ArrayList<YubikeyPrincipal>();


	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#abort()
	 */
	public boolean abort() throws LoginException {
		log.trace("In abort()");
		for (YubikeyPrincipal p : this.principals) {
			this.subject.getPrincipals().remove(p);
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#commit()
	 */
	public boolean commit() throws LoginException {
		log.trace("In commit()");
		for (YubikeyPrincipal p : this.principals) {
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
		for (YubikeyPrincipal p : this.principals) {
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
		this.yc = YubicoClient.getClient(this.clientId);
		
		if(options.get(OPTION_YUBICO_CLIENT_KEY) != null) {
			yc.setKey(options.get(OPTION_YUBICO_CLIENT_KEY).toString());
		}

		/* Realm of principals added after authentication */
		if (options.get(OPTION_YUBICO_ID_REALM) != null) {
			this.idRealm = options.get(OPTION_YUBICO_ID_REALM).toString();
		}

		/* Should this JAAS module be ignored when no OTPs are supplied? */
		if (options.get(OPTION_YUBICO_SOFT_FAIL_NO_OTPS) != null) {
			if ("true".equals(options.get(OPTION_YUBICO_SOFT_FAIL_NO_OTPS).toString())) {
				this.soft_fail_on_no_otps = true;
			}
		}

		/* User-provided URLs to the Yubico validation service, separated by "|". */
		if (options.get(OPTION_YUBICO_WSAPI_URLS) != null) {
			String in = options.get(OPTION_YUBICO_WSAPI_URLS).toString();
			String l[] = in.split("\\|");
			this.yc.setWsapiUrls(l);
		}
		
		if (options.get(OPTION_YUBICO_SYNC_POLICY) != null) {
			this.yc.setSync(options.get(OPTION_YUBICO_SYNC_POLICY).toString());
		}

		/* Instantiate the specified usermap implementation. */
                String usermap_class_name = null;
                if (options.get(OPTION_YUBICO_USERMAP_CLASS) != null) {
                    usermap_class_name = options.get(OPTION_YUBICO_USERMAP_CLASS).toString();
                } else {
                    usermap_class_name = "com.yubico.jaas.impl.YubikeyToUserMapImpl"; // Default implementation
                }
                try {
                    log.debug("Trying to instantiate {}",usermap_class_name);
                    this.ykmap = (YubikeyToUserMap) Class.forName(usermap_class_name).newInstance();
                    this.ykmap.setOptions(options);
                } catch (ClassNotFoundException ex) {
                    log.error("Could not create usermap from class " + usermap_class_name, ex);
                } catch (InstantiationException ex) {
                    log.error("Could not create usermap from class " + usermap_class_name, ex);
                } catch (IllegalAccessException ex) {
                    log.error("Could not create usermap from class " + usermap_class_name, ex);
                }
	}

	/* (non-Javadoc)
	 * @see javax.security.auth.spi.LoginModule#login()
	 */
	public boolean login() throws LoginException {
		NameCallback nameCb = new NameCallback("Enter username: ");

		log.debug("Begin OTP login");

		if (callbackHandler == null) {
			throw new LoginException("No callback handler available in login()");
		}

		List<String> otps = get_tokens(nameCb);
		if (otps.size() == 0) {
			if (this.soft_fail_on_no_otps) {

				log.debug("No OTPs found, and soft-fail is on. Making JAAS ignore this module.");
				return false;
			}
			throw new LoginException("YubiKey OTP authentication failed - no OTPs supplied");
		}

		if (validate_otps(otps, nameCb)) {
			return true;
		}

		log.info("None out of {} possible YubiKey OTPs for user {} validated successful",
					otps.size(), nameCb.getName());

		throw new LoginException("YubiKey OTP authentication failed");
	}

	/**
	 * Try to validate all the OTPs provided.
	 * @param otps  Possible YubiKey OTPs
	 * @param nameCb  JAAS callback to get authenticating username
	 * @return  true if one or more of the OTPs validated OK, otherwise false
	 */
	private boolean validate_otps(List<String> otps, NameCallback nameCb) {
		boolean validated = false;

		for (String otp : otps) {
			log.trace("Checking OTP {}", otp);

			YubicoResponse ykr = this.yc.verify(otp);
			if (ykr != null) {
				log.trace("OTP {} verify result : {}", otp, ykr.getStatus().toString());
				if (ykr.getStatus() == YubicoResponseStatus.OK) {
					String publicId = YubicoClient.getPublicId(otp);
					log.info("OTP verified successfully (YubiKey id {})", publicId);
					if (is_right_user(nameCb.getName(), publicId)) {
						this.principals.add(new YubikeyPrincipal(publicId, this.idRealm));
						/* Don't just return here, we want to "consume" all OTPs if
						 * more than one is provided.
						 */
						validated = true;
					}
				} else {
					log.debug("OTP validation returned {}", ykr.getStatus().toString());
				}
			} else {
				log.trace("null YubicoResponse");
			}
		}

		return validated;
	}

	/**
	 * After validation of an OTP, check that it came from a YubiKey that actually
	 * belongs to the user trying to authenticate.
	 *
	 * @param username  Username to match against YubiKey publicId.
	 * @param publicId  The public ID of the authenticated YubiKey.
	 * @return true if the username matched the YubiKey, false otherwise
	 */
	private boolean is_right_user(String username, String publicId) {
		log.debug("Check if YubiKey {} belongs to user {}", publicId, username);
		return this.ykmap.is_right_user(username, publicId);
	}

	/**
	 * Get username and token(s) from the application, using the
	 * javax.security.auth.callback.CallbackHandler passed to our initialize()
	 * function.
	 *
	 * The tokens returned have been identified as plausible YubiKey OTPs.
	 *
	 * @param nameCb
	 * @return list of possible YubiKey OTPs
	 * @throws LoginException
	 */
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
}
