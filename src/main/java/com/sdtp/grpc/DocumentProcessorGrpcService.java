package com.sdtp.grpc;

import com.sdtp.model.Document;
import com.sdtp.service.DocumentService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;
import java.util.UUID;

@GrpcService
public class DocumentProcessorGrpcService extends DocumentProcessorGrpc.DocumentProcessorImplBase {

    @Inject
    DocumentService documentService;

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    @Override
    public void process(
            DocumentProcessorOuterClass.DocumentRequest request,
            StreamObserver<DocumentProcessorOuterClass.DocumentResponse> responseObserver) {

        // Parse UUID
        UUID uuid;
        try {
            uuid = UUID.fromString(request.getDocumentId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("Invalid UUID format").asRuntimeException());
            return;
        }

        // Reactive identity fetch
        currentIdentityAssociation.getDeferredIdentity()
                .flatMap(identity -> buildJwtAdapter(identity))
                .flatMap(jwt -> documentService.getDocumentById(uuid, jwt))
                .subscribe().with(
                        doc -> {
                            // Success
                            DocumentProcessorOuterClass.DocumentResponse response =
                                    DocumentProcessorOuterClass.DocumentResponse.newBuilder()
                                            .setDocumentId(doc.getId().toString())
                                            .setStatus("Processed")
                                            .build();
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        failure -> {
                            Throwable cause = (failure instanceof io.quarkus.arc.ArcUndeclaredThrowableException)
                                    ? failure.getCause()
                                    : failure;
                            Status status;
                            if (cause instanceof ForbiddenException) status = Status.PERMISSION_DENIED;
                            else if (cause instanceof NotFoundException) status = Status.NOT_FOUND;
                            else status = Status.UNKNOWN;
                            responseObserver.onError(status.withDescription(cause.getMessage()).asRuntimeException());
                        });
    }

    private Uni<JsonWebToken> buildJwtAdapter(SecurityIdentity identity) {
        JsonWebToken jwt = (JsonWebToken) identity.getPrincipal();
        String tenantId = jwt.getClaim("tenant_id");
        if (tenantId == null) {
            return Uni.createFrom().failure(new RuntimeException("JWT missing or invalid"));
        }

        JsonWebToken jwtAdapter = new JsonWebToken() {
            @Override
            public Set<String> getGroups() {
                return identity.getRoles();
            }

            @Override
            public Set<String> getClaimNames() {
                return identity.getAttributes().keySet();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getClaim(String claimName) {
                if ("tenant_id".equals(claimName)) return (T) tenantId;
                return (T) identity.getAttribute(claimName);
            }

            @Override
            public String getName() {
                return identity.getPrincipal().getName();
            }
        };

        return Uni.createFrom().item(jwtAdapter);
    }
}
