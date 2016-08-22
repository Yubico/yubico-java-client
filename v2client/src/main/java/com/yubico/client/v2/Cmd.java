/* Copyright (c) 2016, Nicholas Sushkin.  All rights reserved.

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
 
   Written by Nicholas Sushkin <nsushkin@openfinance.com>, August 2016.
*/

package com.yubico.client.v2;

import com.yubico.client.v2.exceptions.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class Cmd {

    static Options options = new Options()
        .addOption("h", "help", false, "Display this help screen")
        .addOption("V", "version", false, "Display version information")
        .addOption("d", "debug", false, "Print debugging information")
        .addOption(Option.builder("a").longOpt("apikey").hasArg().desc("API key for HMAC validation of request/response").argName("key").build())
        .addOption(Option.builder("u").longOpt("url").hasArg().desc("Yubikey validation service URL").argName("ykvalurl").build());

    static void printUsage() {
        (new HelpFormatter()).printHelp("java -jar client.jar (--help|--version|--apikey <key>) [--debug] [--url <ykvalurl>]* CLIENTID YUBIKEYOTP\n" +
                                        "or: java -jar client.jar [--debug] [--url <ykvalurl>]* CLIENTID key YUBIKEYOTP",
                                        "Validate the YUBIKEYOTP one-time-password against the YubiCloud using CLIENTID as the client identifier\n\n",
                                        options,
                                        "\nExit status is 0 on success, 1 if there is a hard failure, 2 if the OTP was replayed, 3 for other soft OTP-related failures.");
    }

    static String getVersion() {
        return Cmd.class.getName() + " " + Version.version;
    }

    static void log(String description, String value) {
        if (value != null) {
            System.err.printf("%14s: %s\n", description, value);
        }
    }
    
    public static void main (String args[]) throws Exception {
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cli = parser.parse(options, args, true);

            String[] leftoverArgs = cli.getArgs();

            if ( cli.hasOption("h") ) {
                printUsage();
                return;
            }

            if ( cli.hasOption("V") ) {
                System.out.println(getVersion());
                return;
            }
                          
            if ( ( leftoverArgs.length < 2 )
                 || ( leftoverArgs.length > 3 )
                 ) {
                printUsage();
                return;
            }

            boolean legacyUsage = (leftoverArgs.length == 3);
            
            if ( ! legacyUsage && ! cli.hasOption("a") ) {
                System.err.println("Specify API key using --apikey key option");
                System.exit(1);
            }

            int clientId = -1;
            try {
                clientId = Integer.parseInt(leftoverArgs[0]);
            } catch (NumberFormatException e) {
                System.err.println("error: client identity must be a positive integer.");
                System.exit(1);
            }

            if (clientId <= 0) {
                System.err.println("error: client identity must be a positive integer.");
                System.exit(1);
            }

            String apiKey = legacyUsage ? leftoverArgs[1] : cli.getOptionValue("a");
            String otp = legacyUsage ? leftoverArgs[2] : leftoverArgs[1];
            
            YubicoClient yc = YubicoClient.getClient(clientId, apiKey);

            yc.setUserAgent(getVersion());

            if ( cli.hasOption("u") ) {
                yc.setWsapiUrls( cli.getOptionValues("u") );
            }
            
            if (otp.length() < 32) {
                System.err.println("error: modhex encoded token must be at least 32 characters");
                System.exit(1);
            }
                
            if ( cli.hasOption("d") ) {
                System.err.printf("%s:\n", "Input");
                for ( String url: yc.getWsapiUrls() )
                {
                    System.err.printf("%14s: %s\n", "validation URL", url);
                }
                log("client id", Integer.toString(yc.getClientId()));
                log("token", otp);
                log("api key", yc.getKey());
            }

            try {
                VerificationResponse response = yc.verify(otp);
                ResponseStatus status = response.getStatus();

                if ( cli.hasOption("d") ) {
                    System.err.printf("\n%s: %s\n", "Response", response);
                    
                    if (status != null) {                      
                        System.err.printf("%14s: (%d) %s\n", "status", status.ordinal(), status);
                    }

                    log("otp", response.getOtp());
                    log("nonce", response.getNonce());
                    log("t", response.getT());
                    log("timestamp", response.getTimestamp());
                    log("sessioncounter", response.getSessioncounter());
                    log("sessionuse", response.getSessionuse());
                    log("sl", response.getSl());
                }

                if (response.isOk()) {
                    System.exit(0);
                } else if (status == ResponseStatus.REPLAYED_OTP)  {
                    System.exit(2);
                } else {
                    System.exit(3);
                }
            }
            catch (YubicoVerificationException e) {
                System.err.println("Validation Error: " + e);
                System.exit(3);
            }
            catch (YubicoValidationFailure f) {
                System.err.println("Validation Failure: " + f);
                System.exit(3);
            }
            catch (IllegalArgumentException a) {
                System.err.println("Incorrectly formatted OTP string: " + a);
                System.exit(3);
            }
            
            return;
        }
        catch (ParseException pe) {
            System.err.println("Error parsing command line: " + pe.getMessage());
            System.exit(1);
        }
    }
}
