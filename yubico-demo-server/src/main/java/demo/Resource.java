package demo;

import com.google.common.collect.HashMultimap;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.YubicoResponse;
import com.yubico.client.v2.YubicoResponseStatus;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class Resource {

    public static final String NAVIGATION = "<p>Navigation: <a href='/loginIndex'>Login</a> | <a href='/registerIndex'>Register</a></p>";

    // Don't use this client ID in production. Instead, get your own from https://upgrade.yubico.com/getapikey
    public static final int CLIENT_ID = 1;
    private final YubicoClient client = YubicoClient.getClient(CLIENT_ID);
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
        YubicoResponse response = client.verify(otp);
        if (response.getStatus() == YubicoResponseStatus.OK) {
            String yubikeyId = YubicoClient.getPublicId(otp);
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
        YubicoResponse response = client.verify(otp);
        if (response.getStatus() == YubicoResponseStatus.OK) {
            String yubikeyId = YubicoClient.getPublicId(otp);
            if(yubikeyIds.get(username).contains(yubikeyId)) {
                return "Success fully logged in " + username + "!" + NAVIGATION;
            }
            return "No such username and YubiKey combination.";
        }
        return "Invalid OTP: " + response;
    }
}
