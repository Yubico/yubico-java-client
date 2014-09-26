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

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.PasswordCallback;

/**
 * A class that extends PasswordCallback to keep a list of all values
 * set using setPassword(). If the application using this JAAS plugin
 * wants to pass us multiple authentication factors, it just calls
 * setPassword() more than once in the CallbackHandler.
 * 
 * @author Fredrik Thulin (fredrik@yubico.com)
 *
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
	 * @param password  A secret to add to our list.
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
