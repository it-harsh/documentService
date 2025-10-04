package com.sdtp.auth;

import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/login")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {

    public static class LoginRequest {
        public String username;
        public String password;
    }

    @POST
    public Response login(LoginRequest request) {
        var user = UserStore.getByUsername(request.username);
        if (user == null || !user.password.equals(request.password)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Invalid credentials")).build();
        }

        // Build JWT
        String token = Jwt.claim("tenant_id", user.tenantId)
                .groups(user.roles)
                .subject(user.username)
                .issuer("doc-service")
                .expiresIn(1800L)
                .sign();

        Map<String, String> resp = new HashMap<>();
        resp.put("token", token);
        return Response.ok(resp).build();
    }
}
