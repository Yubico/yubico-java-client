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
package com.yubico.jaas.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yubico.jaas.YubikeyLoginModule;
import com.yubico.jaas.YubikeyToUserMap;

/**
 * Class to verify that a user is the rightful user of a YubiKey.
 * 
 * The current implementation is very rudimentary and uses a plain text file
 * as a backend, expecting lines like "yk.vvcccfhcb = alice". If "bob" tries
 * to authenticate with YubiKey "vvcccfhcb" he should be denied.
 *
 * There is also support for auto provisioning (default: false). If the option
 * auto_provision_owner is set to "true", a user logging in without a prior
 * YubiKey association will have the YubiKey they are using associated with them.
 * This should probably only be used during an initial time window when YubiKey
 * tokens are distributed to users, after which the auto provisioning should be
 * turned off again.
 *  
 * @author Fredrik Thulin <fredrik@yubico.com>
 *
 */
public class YubikeyToUserMapImpl extends YubikeyToUserMap {
	/* Options for this class */
	public static final String OPTION_YUBICO_AUTO_PROVISION		= "auto_provision_owner";
	public static final String OPTION_YUBICO_ID2NAME_TEXTFILE	= "id2name_textfile";
	public static final String OPTION_YUBICO_VERIFY_YK_OWNER	= "verify_yubikey_owner";
	
	private String id2name_textfile;
	private boolean auto_provision_owners = false;
	private boolean verify_yubikey_owner = true;

	private final Logger log = LoggerFactory.getLogger(YubikeyLoginModule.class);

	public YubikeyToUserMapImpl(Map<String, ?> options) {
		/* Is verification of YubiKey owners enabled? */
		this.verify_yubikey_owner = true;
		if (options.get(OPTION_YUBICO_VERIFY_YK_OWNER) != null) {
			if ("false".equals(options.get(OPTION_YUBICO_VERIFY_YK_OWNER).toString())) {
				this.verify_yubikey_owner = false;
			}
		}	
		
		/* id2name text file */
		if (options.get(OPTION_YUBICO_ID2NAME_TEXTFILE) != null) {
			this.id2name_textfile = options.get(OPTION_YUBICO_ID2NAME_TEXTFILE).toString();
		}

		/* should we automatically assign new yubikeys to users? */
		if (options.get(OPTION_YUBICO_AUTO_PROVISION) != null) {
			if ("true".equals(options.get(OPTION_YUBICO_AUTO_PROVISION).toString())) {
				this.auto_provision_owners = true;
			}
		}			
	}

	public boolean is_right_user(String username, String publicId) {
		if (this.id2name_textfile == null) {
			log.debug("No id2name configuration. Defaulting to {}.", this.verify_yubikey_owner);
			return this.verify_yubikey_owner;
		}

		Scanner sc = null;
		File file = new File(this.id2name_textfile);
		try {
			sc = new Scanner(file);
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.startsWith("yk." + publicId + ".user")) {
					String ykuser = line.split("=")[1].trim();

					if (! ykuser.equals(username)) {
						log.info("YubiKey " + publicId + " registered to user " + ykuser
								+ ", not " + username);
						return false;
					}
					return true;
				}
			}
		} catch (FileNotFoundException ex) {
			log.error("Yubikey to username textfile {} not found", this.id2name_textfile);
			return false;
		} finally {
			if (sc != null) {
				sc.close();	
			}
		}	    

		if (this.auto_provision_owners) {
			log.info("Registering new YubiKey " + publicId + " as belonging to {}", username);

			try {
				FileWriter writer = new FileWriter(file, true);
				writer.write("yk." + publicId + ".user = " + username
						+ System.getProperty("line.separator"));
				writer.close();	
			} catch (IOException ex) {
				log.error("Failed appending entry to file {}", this.id2name_textfile, ex);
			}
			return true;
		}	    

		return false;
	}
}
