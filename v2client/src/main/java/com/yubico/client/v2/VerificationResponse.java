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

import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

/**
 * Object built from server response, detailing the status of validation.
 *
 */
@Value.Immutable
public interface VerificationResponse {

    /**
     * Whether the response status from the server was OK, representing a valid OTP.
     *
     * @return true if the response status was OK, false otherwise
     */
    default boolean isOk() {
        return getStatus() == ResponseStatus.OK;
    }
    default Boolean isReplayed() {
        return getStatus() == ResponseStatus.REPLAYED_REQUEST;
    }

    /**
     * Returns the public id of the returned OTP
     *
     * @return public id
     */
    default Optional<String> getPublicId() {
        return getOtp().map(YubicoClient::getPublicId);
    }

	/**
	 * Signature of the response, with the same API key as the request.
	 * 
	 * @return response signature
	 */
    String getH();
    
    /**
     * UTC timestamp from the server when response was processed.
     * 
     * @return server UTC timestamp
     */
    String getT();
    
    /**
     * Server response to the request.
     * 
     * @see ResponseStatus
     * @return response status
     */
    ResponseStatus getStatus();
    
    /**
     * Returns the internal timestamp from the YubiKey 8hz timer.
     * 
     * @return yubikey internal timestamp
     */
    Optional<String> getTimestamp();
    
    /**
     * Returns the non-volatile counter that is incremented on power-up.
     * 
     * @return session counter
     */
    Optional<String> getSessioncounter();
    
    /**
     * Returns the volatile counter that is incremented on each button-press,
     * starts at 0 after power-up.
     * 
     * @return session use counter
     */
    Optional<String> getSessionuse();
    
    /**
     * Returns the amount of sync the server achieved before sending the
     * response.
     * 
     * @return sync, in procent
     */
    Optional<String> getSl();
    
    /**
     * Echos back the OTP from the request, should match.
     * 
     * @return otp
     */
    Optional<String> getOtp();
    
    /**
     * Echos back the nonce from the request. Should match. 
     * @return nonce
     */
    Optional<String> getNonce();
    
    /**
     * Returns all parameters from the response as a Map
     * 
     * @return map of all values
     */
    Map<String, String> getKeyValueMap();
}
