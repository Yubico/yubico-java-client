package demo;

import com.google.common.collect.HashMultimap;
import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.YubicoClientBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.yubico.client.v2.OtpUtils.getPublicId;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class Resource {

    public static final String NAVIGATION = "<p>Navigation: <a href='/loginIndex'>Login</a> | <a href='/registerIndex'>Register</a></p>";

    // Don't use this client ID in production. Instead, get your own from https://upgrade.yubico.com/getapikey
    public static final int CLIENT_ID = 21188;
    public static final String API_KEY = "p38Z7DuEB/JC/LbDkkjmvMRB5GI=";

    private final YubicoClient client = new YubicoClientBuilder()
            .clientId(CLIENT_ID)
            .key(API_KEY)
            .build();
    private final HashMultimap<String, String> yubikeyIds = HashMultimap.create();

    @Path("registerIndex")
    @GET
    public Response registerIndex() {
        return Response.ok()
                .entity(Resource.class.getResourceAsStream("registerIndex.html"))
                .build();
    }

    @Path("register")
    @POST
    public String register(@FormParam("username") String username, @FormParam("otp") String otp) throws Exception {
        VerificationResponse response = client.verify(otp);
        if (response.isOk()) {
            String yubikeyId = getPublicId(otp);
            yubikeyIds.put(username, yubikeyId);
            return "Successfully registered YubiKey!" + NAVIGATION;
        }
        return "Invalid OTP: " + response;
    }

    @Path("loginIndex")
    @GET
    public Response loginIndex() {
        return Response.ok()
                .entity(Resource.class.getResourceAsStream("loginIndex.html"))
                .build();
    }

    @Path("login")
    @POST
    public String login(@FormParam("username") String username, @FormParam("otp") String otp) throws Exception {
        VerificationResponse response = client.verify(otp);
        if (response.isOk()) {
            String yubikeyId = getPublicId(otp);
            if(yubikeyIds.get(username).contains(yubikeyId)) {
                return "Success fully logged in " + username + "!" + NAVIGATION;
            }
            return "No such username and YubiKey combination.";
        }
        return "Invalid OTP: " + response;
    }
}
