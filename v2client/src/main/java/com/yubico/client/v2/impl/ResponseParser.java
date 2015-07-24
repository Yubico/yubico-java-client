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

package com.yubico.client.v2.impl;

import com.google.common.collect.ImmutableSortedMap;
import com.yubico.client.v2.ImmutableVerificationResponse;
import com.yubico.client.v2.ResponseStatus;
import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.exceptions.YubicoInvalidResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResponseParser {

    public VerificationResponse parse(InputStream inStream) throws YubicoInvalidResponse, IOException {

        ImmutableSortedMap.Builder<String, String> keyValueBuilder = ImmutableSortedMap.naturalOrder();
        ImmutableVerificationResponse.Builder responseBuilder = ImmutableVerificationResponse.builder();

        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
            int ix=inputLine.indexOf("=");
            if(ix==-1) continue; // Invalid line
            String key=inputLine.substring(0,ix);
            String val=inputLine.substring(ix+1);

            switch (key) {
                case "h":
                    responseBuilder.h(val);
                    break;
                case "t":
                    responseBuilder.t(val);
                    break;
                case "otp":
                    responseBuilder.otp(val);
                    break;
                case "status":
                    responseBuilder.status(ResponseStatus.valueOf(val));
                    break;
                case "timestamp":
                    responseBuilder.timestamp(val);
                    break;
                case "sessioncounter":
                    responseBuilder.sessioncounter(val);
                    break;
                case "sessionuse":
                    responseBuilder.sessionuse(val);
                    break;
                case "sl":
                    responseBuilder.sl(val);
                    break;
                case "nonce":
                    responseBuilder.nonce(val);
                    break;
            }
            keyValueBuilder.put(key, val);
        }
        in.close();

        try {
            return responseBuilder.keyValueMap(keyValueBuilder.build()).build();
        } catch (IllegalStateException e) {
            throw new YubicoInvalidResponse("Invalid response.");
        }
    }
}
