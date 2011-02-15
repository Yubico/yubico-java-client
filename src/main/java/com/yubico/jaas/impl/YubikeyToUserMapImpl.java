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
 * as a backend, expecting lines like "yk.vvcccfhcb.user = alice". If "bob"
 * tries to authenticate with YubiKey "vvcccfhcb" he should be denied.
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

	/*
	 * Verify that there is a known connection between username and publicId.
	 * If auto-provisioning is enabled and no connection is found, one is registered.
	 *
	 * @param username username to match to YubiKey id
	 * @publicId modhex encoded public id of a YubiKey (e.g. "vvcccccfhc")
	 *
	 */
	public boolean is_right_user(String username, String publicId) {
		if (! this.verify_yubikey_owner) {
			log.debug("YubiKey owner verification disabled, returning 'true'");
			return true;
		}
		if (this.id2name_textfile == null) {
			log.debug("No id2name configuration. Defaulting to {}.", this.verify_yubikey_owner);
			return this.verify_yubikey_owner;
		}

		String ykuser;
		try {
			ykuser = get_username_for_id(publicId, this.id2name_textfile);
		} catch (FileNotFoundException ex) {
			log.error("Yubikey to username textfile {} not found", this.id2name_textfile);
			return false;
		}

		if (ykuser != null) {
			if (! ykuser.equals(username)) {
				log.info("YubiKey " + publicId + " registered to user {}, not {}", ykuser, username);
				return false;
			}
			return true;
		} else {
			if (this.auto_provision_owners) {
				log.info("Registering new YubiKey " + publicId + " as belonging to {}", username);

				add_yubikey_to_user(publicId, username, this.id2name_textfile);
				return true;
			}
			log.debug("No record of YubiKey {} found. Returning 'false'.", publicId);
			return false;
		}
	}

	/**
	 * Given publicId "vvcccccfhc", scans filename for a line like "yk.vvcccccfhc.user = alice"
	 * and returns "alice" if found. Null is returned in case there is no matching line in file.
	 *
	 * @param publicId
	 * @param filename
	 * @return String username
	 * @throws FileNotFoundException
	 */
	private String get_username_for_id(String publicId, String filename) throws FileNotFoundException {
		Scanner sc = null;
		File file = new File(filename);
		try {
			sc = new Scanner(file);
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.startsWith("yk." + publicId + ".user")) {
					String ykuser = line.split("=")[1].trim();

					return ykuser;
				}
			}
		} finally {
			if (sc != null) {
				sc.close();
			}
		}
		return null;
	}

	/**
	 * Stores an association between username and YubiKey publicId in filename.
	 *
	 * @param publicId
	 * @param username
	 * @param filename
	 */
	private void add_yubikey_to_user(String publicId, String username, String filename) {
		try {
			File file = new File(filename);
			FileWriter writer = new FileWriter(file, true);
			writer.write("yk." + publicId + ".user = " + username
					+ System.getProperty("line.separator"));
			writer.close();
		} catch (IOException ex) {
			log.error("Failed appending entry to file {}", filename, ex);
		}
	}
}
