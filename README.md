== yubico-java-client
This repository contains a Java library with an accompanying demo server, as well as a 
https://github.com/Yubico/yubico-java-client/tree/master/jaas[JAAS module],
to validate YubiKey OTPs (One-Time Passwords).

By default, this library uses the Yubico YubiCloud validation platform,
but it can be configured for another validation server.

NOTE: For more details on how to use a YubiKey OTP library, visit
https://developers.yubico.com/OTP[developers.yubico.com/OTP].

=== Usage

Add this to your pom.xml:

[source,xml]
 <dependency>
   <groupId>com.yubico</groupId>
   <artifactId>yubico-validation-client2</artifactId>
   <version>3.0.1</version>
 </dependency>

[source,java]
----
// clientId and secretKey are retrieved from https://upgrade.yubico.com/getapikey
YubicoClient client = YubicoClient.getClient(clientId, secretKey);

// otp is the OTP from the YubiKey
VerificationResponse response = client.verify(otp);
assert response.isOk();
----

After validating the OTP you should make sure that the publicId part belongs to
the correct user. For example:

[source,java]
YubicoClient.getPublicId(otp)
    .equals(/* Yubikey ID associated with the user */);

For a complete example, see the https://github.com/Yubico/yubico-java-client/tree/master/demo-server[demo server].

=== Logging
The validation client depends on slf4j-api for logging. To get the actual logs
and not receive warnings on System.out you will need to depend on a slf4j logger
binding, for example slf4j-log4j with the following Maven configuration:

[source,xml]
 <dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-log4j</artifactId>
  <version>1.6.1</version>
 </dependency>

=== Read more
For more complete descriptions of methods and failure states, please see
the https://developers.yubico.com/yubico-java-client[JavaDoc].

NOTE: If you want the client for the legacy version 1 of the API, it can be found https://github.com/Yubico/yubico-java-client/tree/master/v1client[here].
