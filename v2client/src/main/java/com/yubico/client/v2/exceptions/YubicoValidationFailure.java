/* Copyright (c) 2012, Yubico AB.  All rights reserved.

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
*/

package com.yubico.client.v2.exceptions;

/**
 * YubicoValidationFailure is thrown for validation failures<br>
 *  * OTP in request and response isn't matching, could mean a man-in-the-middle<br>
 *  * nonce in request and response isn't matching, could mean a man-in-the-middle<br>
 *  * response signature verification failed
 */

public class YubicoValidationFailure extends Exception {

	private static final long serialVersionUID = 1L;

	public YubicoValidationFailure(String message) {
		super(message);
	}

	public YubicoValidationFailure(Throwable cause) {
		super(cause);
	}

	public YubicoValidationFailure(String message, Throwable cause) {
		super(message, cause);
	}
}
