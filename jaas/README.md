These are JAAS-plugins for authentication using one time password tokens
(YubiKeys primarily).
For information about JAAS configuration, see
 http://download.oracle.com/javase/1.5.0/docs/api/javax/security/auth/login/Configuration.html

YubikeyLoginModule :

This JAAS plugin authenticates OTPs against the online Yubico validation
servers. Client id and API key can be fetched from
https://upgrade.yubico.com/getapikey/

 Parameters :

   clientId             Your Client API id for the validation service.
   clientKey            Your Client API key for the validaiton service.
   id2name_textfile     Filename with "public_id<SP>username" info about which
                        user owns what key.
   verify_yubikey_owner default: "true". Only set to "false" in pre-production
                        environments, otherwise ANY Yubikey will be accepted
                        for ANY user!
   auto_provision_owner default: "false". If set to "true", we will
                        automatically record any new Yubikeys used as belonging
                        to the user that first logged in with them.
   id_realm             Something to append to the Yubikey public id when we
                        construct principals (e.g.
                        "@my-validation-service.example.org").
   soft_fail_on_no_otps default: false. Should the JAAS login module return
                        failure or asked to be ignored in case no OTPs are
                        provided for validation?
   wsapi_urls           default: the YubiCloud validation URL. A "|" delimeted
                        list of ykval wsapi 2.0 URLs to use for OTP validation.
   sync_policy          default: none, let the server decide. a value between 0
                        and 100 indicating the percentage of synchronization
                        required by the client.
   jacc			default: false, if true module picks up the otp from j_otp
   			FORM authentication too

  Example configuration :

    YourApplicationAuth {
        com.yubico.jaas.YubikeyLoginModule required
    	    clientId="4711";
    };	
  

HttpOathOtpLoginModule :

  This JAAS plugin validates OATH OTPs using HTTP. The username and password
  entered in your application will be used to attempt a HTTP Basic Auth login
  to an URL you specify, and if that succeeds and the resulting response contains
  an expected string, authentication is granted.

  One tested backend solution for validation of the HOTPs is the Apache mod_authn_otp :

    http://code.google.com/p/mod-authn-otp/

  Parameters :

     protectedUrl     (required) The URL you have protected with OATH-HOTP HTTP
                      Basic Auth.
     expectedOutput   Default is "Authenticated OK".
     minLength        Default is 6.
     maxLength        Default is 12 (6-8 bytes HOTP and 4 bytes PIN).
     requireAllDigits Default is "true".
     id_realm         Something to append to the username when we construct
                      principals (e.g. "@my-validation-service.example.org").


  Example configuration :

    YourApplicationAuth {
    	com.yubico.jaas.HTTPOathHotpLoginModule sufficient
	    protectedUrl = "http://auth.example.com/oath-protected/"
	    expectedOutput = "User authenticated OK";
    };
