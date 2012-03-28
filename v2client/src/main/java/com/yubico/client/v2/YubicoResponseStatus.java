/* Copyright (c) 2011, Linus Widströmer.  All rights reserved.

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

public enum YubicoResponseStatus {
	/**The OTP is valid. */ OK,
	/**The OTP is invalid format. */ BAD_OTP,
	/**The OTP has already been seen by the service. */ REPLAYED_OTP,
	/**The HMAC signature verification failed. */ BAD_SIGNATURE,
	/**The request lacks a parameter. */ MISSING_PARAMETER,
	/**The request id does not exist. */ NO_SUCH_CLIENT,
	/**The request id is not allowed to verify OTPs. */ OPERATION_NOT_ALLOWED,
	/**Unexpected error in our server. Please contact us if you see this error. */ BACKEND_ERROR,
	/**Server could not get requested number of syncs during before timeout */ NOT_ENOUGH_ANSWERS,	
	/**Server has seen the OTP/Nonce combination before
	 * @see <a href="http://forum.yubico.com/viewtopic.php?f=3&t=701">http://forum.yubico.com/viewtopic.php?f=3&t=701</a> */ 
	REPLAYED_REQUEST
}

