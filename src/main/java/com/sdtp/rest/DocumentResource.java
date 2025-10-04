package com.sdtp.rest;

import com.sdtp.model.Document;
import com.sdtp.service.DocumentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

@Path("/documents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @Inject
    DocumentService documentService;

    @Inject
    JsonWebToken jwt;

    @POST
    @RolesAllowed("admin") // RBAC: only admin can call
    public Uni<Response> createDocument(Document doc) {
        return documentService.createDocument(doc, jwt)
                .onItem().transform(created ->
                        Response.status(Response.Status.CREATED).entity(created).build()
                );
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"admin", "viewer"}) // RBAC: admin or viewer
    public Uni<Response> getDocument(@PathParam("id") String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid UUID format").build()
            );
        }

        return documentService.getDocumentById(uuid, jwt)
                .onItem().transform(doc -> Response.ok(doc).build());
    }

    @GET
    @Path("/tenant")
    @RolesAllowed({"admin", "viewer"})
    public Uni<Response> getAllTenantDocuments() {
        return documentService.getAllDocumentsForTenant(jwt)
                .onItem().transform(docs -> Response.ok(docs).build());
    }

    @GET
    @Path("/user")
    @RolesAllowed({"admin", "viewer"})
    public Uni<Response> getUserDocuments() {
        return documentService.getDocumentsByUser(jwt)
                .onItem().transform(docs -> Response.ok(docs).build());
    }
}
