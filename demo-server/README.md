== yubico-demo-server
A simple self-contained demo server supporting multiple YubiKeys per user. The central part is the
https://github.com/Yubico/yubico-java-client/blob/master/demo-server/src/main/java/demo/Resource.java[Resource]
class.

=== Usage
Compile using `mvn clean install` and then run using
`java -jar target/yubico-demo-server.jar server config.yml`.

Then point a web browser to
link:http://localhost:8080/registerIndex[localhost:8080/registerIndex].
