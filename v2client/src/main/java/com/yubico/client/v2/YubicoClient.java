/* Copyright (c) 2011, Linus Widströmer.  All rights reserved.

   Copyright (c) 2011, Simon Buckle.  All rights reserved.
   
   Copyright (c) 2014, Yubico AB.  All rights reseved.

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
   
   Modified by Simon Buckle <simon@webteq.eu>, September 2011.
    - Added support for generating and validating signatures
*/

package com.yubico.client.v2;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.yubico.client.v2.exceptions.YubicoSignatureException;
import com.yubico.client.v2.exceptions.YubicoVerificationException;
import com.yubico.client.v2.exceptions.YubicoVerificationFailure;
import org.immutables.builder.Builder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.yubico.client.v2.HttpUtils.toQueryString;
import static com.yubico.client.v2.OtpUtils.isValidOTPFormat;
import static com.yubico.client.v2.ResponseStatus.BAD_SIGNATURE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

public class YubicoClient {

    private static final Set<String> DEFAULT_API_URLS = ImmutableSet.of(
            "https://api.yubico.com/wsapi/2.0/verify",
            "https://api2.yubico.com/wsapi/2.0/verify",
            "https://api3.yubico.com/wsapi/2.0/verify",
            "https://api4.yubico.com/wsapi/2.0/verify",
            "https://api5.yubico.com/wsapi/2.0/verify"
    );

    private final VerificationRequester validationService;
    private final Optional<Integer> sync;
    private final Set<String> apiUrls;
    private final Integer clientId;
    private final byte[] key;

    /**
     * Configure what URLs to use for validating OTPs. These URLs will have
     * all the necessary parameters appended to them. Example :
     * {"https://api.yubico.com/wsapi/2.0/verify"}
     * @param wsapi  list of base URLs
     */

    /**
     * Instantiate a YubicoClient object.
     *
     * @param clientId Retrieved from https://upgrade.yubico.com/getapikey
     * @return  client that can be used to validate YubiKey OTPs
     */
    @Builder.Factory
    public static YubicoClient yubicoClient(Integer clientId,
                                            String key,
                                            Optional<Integer> syncLevel,
                                            Set<String> apiUrls) {
        return new YubicoClient(clientId, key, syncLevel, apiUrls.isEmpty() ? DEFAULT_API_URLS: apiUrls);
    }

    private final String userAgent = "yubico-java-client/" + Version.version +
            " (" + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + ")";

    /**
     * Creates a YubicoClient that will be using the given Client ID and API key.
     *
     * @param clientId Retrieved from https://upgrade.yubico.com/getapikey
     * @param apiKey Retrieved from https://upgrade.yubico.com/getapikey
     * @param sync A value 0 to 100 indicating percentage of syncing required by client, or strings "fast" or "secure"
     *             to use server-configured values; if absent, let the server decide
     */
    public YubicoClient(Integer clientId, String apiKey, Optional<Integer> sync, Set<String> apiUrls) {
        validationService = new VerificationRequester();
        this.clientId = clientId;
        this.key = decodeBase64(apiKey.getBytes());
        this.sync = sync;
        this.apiUrls = apiUrls;
    }

    /**
     * Validate an OTP using a webservice call to one or more ykval validation servers.
     *
     * @param otp YubiKey OTP
     * @return result of the webservice validation operation
     * @throws YubicoVerificationException for validation errors, like unreachable servers
     * @throws YubicoVerificationFailure for validation failures, like non matching OTPs in request and response
     * @throws IllegalArgumentException for arguments that are not correctly formatted OTP strings.
     */
    public VerificationResponse verify(String otp) throws YubicoVerificationException, YubicoVerificationFailure {
        checkArgument(isValidOTPFormat(otp), "The OTP is not a valid format");

        String nonce = UUID.randomUUID().toString().replaceAll("-", "");
        ImmutableSortedMap.Builder<String, String> requestMapBuilder = ImmutableSortedMap.<String, String>naturalOrder()
                .put("nonce", nonce)
                .put("id", clientId.toString())
                .put("otp", otp)
                .put("timestamp", "1");
        sync.ifPresent(sync -> requestMapBuilder.put("sl", sync.toString()));

        String queryString = sign(toQueryString(requestMapBuilder.build()));

        Set<String> validationUrls = apiUrls.stream()
                .map(url -> url + "?" + queryString)
                .collect(toSet());

        Optional<VerificationResponse> responseOpt = validationService.fetch(validationUrls, userAgent);
        VerificationResponse response = responseOpt.orElseThrow(
                () -> new YubicoVerificationException("No server returned a valid response. See log for details."));

        verifySignature(response);

        // NONCE/OTP fields are not returned to the client when sending error codes.
        // If there is an error response, don't need to check them.
        if (!response.getStatus().isError()) {
            if (!response.getOtp().filter(otp::equals).isPresent()) {
                throw new YubicoVerificationFailure("OTP mismatch in response, is there a man-in-the-middle?");
            }
            if (!response.getNonce().filter(nonce::equals).isPresent()) {
                throw new YubicoVerificationFailure("Nonce mismatch in response, is there a man-in-the-middle?");
            }
        }
        return response;
    }

    private void verifySignature(VerificationResponse response) throws YubicoVerificationFailure, YubicoVerificationException {
        String keyValueStr = response.getKeyValueMap().entrySet().stream()
                .filter(e -> !"h".equals(e.getKey()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining("&"));

        try {
            String signature = Signature.calculate(keyValueStr, key).trim();
            if (!response.getH().equals(signature) && response.getStatus() != BAD_SIGNATURE) {
                // don't throw a ValidationFailure if the server said bad signature, in that
                // case we probably have the wrong key/id and want to check it.
                throw new YubicoVerificationFailure("Signatures do not match");
            }
        } catch (YubicoSignatureException e) {
            throw new YubicoVerificationException("Failed to calculate the response signature.", e);
        }
    }

    private String sign(String queryString) throws YubicoSignatureException {
        String signature = Signature.calculate(queryString, key);
        return queryString + "&h=" + urlFormParameterEscaper().escape(signature);
    }
}
