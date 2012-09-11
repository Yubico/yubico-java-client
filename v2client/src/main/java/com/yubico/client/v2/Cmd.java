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

public class Cmd {

    public static void main (String args []) throws Exception
    {
        if (args.length != 2) {
            System.err.println("\n*** Test your Yubikey against Yubico OTP validation server ***");
            System.err.println("\nUsage: java -jar client.jar Auth_ID OTP");
            System.err.println("\nEg. java -jar client.jar 28 vvfucnlcrrnejlbuthlktguhclhvegbungldcrefbnku");
            System.err.println("\nTouch Yubikey to generate the OTP. Visit Yubico.com for more details.");
            return;
        }

        int authId = Integer.parseInt(args[0]);
        String otp = args[1];

        YubicoClient yc = YubicoClient.getClient(authId);
        YubicoResponse response = yc.verify(otp);

        if(response!=null && response.getStatus() == YubicoResponseStatus.OK) {
            System.out.println("\n* OTP verified OK");
        } else {
            System.out.println("\n* Failed to verify OTP");
        }

        System.out.println("\n* Last response: " + response);
        System.exit(0);

    } // End of main

}
