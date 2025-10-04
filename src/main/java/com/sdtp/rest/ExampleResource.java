package com.sdtp.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @GET
    @Path("/public")
    public String publicHello() {
        return "Hello from public endpoint";
    }

    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    public String adminHello() {
        return "Hello from ADMIN endpoint";
    }
}
