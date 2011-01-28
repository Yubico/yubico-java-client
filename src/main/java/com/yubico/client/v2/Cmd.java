package com.yubico.client.v2;

/**
 * Created by IntelliJ IDEA.
 * User: lwid
 * Date: 1/28/11
 * Time: 2:07 PM
 * To change this template use File | Settings | File Templates.
 */
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

        YubicoClient yc = YubicoClient.getClient();
        yc.setClientId(authId);
        YubicoResponse response = yc.verify(otp);

        if(response.getStatus() == YubicoResponseStatus.OK) {
            System.out.println("\n* OTP verified OK");
        } else {
            System.out.println("\n* Failed to verify OTP");
        }

        System.out.println("\n* Last response: " + response);
        System.out.println("\n");

    } // End of main

}
