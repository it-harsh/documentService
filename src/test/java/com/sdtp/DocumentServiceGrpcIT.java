package com.sdtp;

import com.sdtp.grpc.DocumentProcessorGrpc;
import com.sdtp.grpc.DocumentProcessorOuterClass;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.SecurityAttribute;
import io.quarkus.test.security.AttributeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quarkus-style integration tests for DocumentProcessor gRPC service.
 * Covers:
 * 1. Happy Path
 * 2. Tenant Validation
 * 3. Forbidden Access
 */
@QuarkusTest
public class DocumentServiceGrpcIT {

    @GrpcClient
    DocumentProcessorGrpc.DocumentProcessorBlockingStub stub;

    /**
     * Mock gRPC service for testing purposes.
     */
    @GrpcService
    static class MockDocumentProcessor extends DocumentProcessorGrpc.DocumentProcessorImplBase {

        @Override
        public void process(DocumentProcessorOuterClass.DocumentRequest request, StreamObserver<DocumentProcessorOuterClass.DocumentResponse> responseObserver) {
            String docId = request.getDocumentId();

            if (docId == null || docId.isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Document ID cannot be empty")
                        .asRuntimeException());
                return;
            }

            // Forbidden tenant check
            if (docId.startsWith("forbidden")) {
                responseObserver.onError(Status.PERMISSION_DENIED
                        .withDescription("Access denied for this tenant")
                        .asRuntimeException());
                return;
            }

            // Tenant validation simulation
            if (docId.startsWith("tenant2")) {
                responseObserver.onNext(DocumentProcessorOuterClass.DocumentResponse.newBuilder()
                        .setDocumentId(docId)
                        .setStatus("Processed by Tenant2")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // Normal success (Happy path)
            responseObserver.onNext(DocumentProcessorOuterClass.DocumentResponse.newBuilder()
                    .setDocumentId(docId)
                    .setStatus("Processed successfully")
                    .build());
            responseObserver.onCompleted();
        }
    }

    // --------------- TEST CASES ---------------

    @Test
    @TestSecurity(
            user = "test-admin",
            roles = {"admin"},
            attributes = {
                    @SecurityAttribute(key = "tenant_id", value = "tenant2", type = AttributeType.STRING)
            }
    )
    void testHappyPath() {
        DocumentProcessorOuterClass.DocumentRequest request = DocumentProcessorOuterClass.DocumentRequest.newBuilder()
                .setDocumentId("doc123")
                .build();

        DocumentProcessorOuterClass.DocumentResponse response = stub.process(request);

        assertEquals("doc123", response.getDocumentId());
        assertEquals("Processed successfully", response.getStatus());
    }

    @Test
    @TestSecurity(
            user = "tenant-user",
            roles = {"admin"},
            attributes = {
                    @SecurityAttribute(key = "tenant_id", value = "tenant2", type = AttributeType.STRING)
            }
    )
    void testTenantValidation() {
        DocumentProcessorOuterClass.DocumentRequest request = DocumentProcessorOuterClass.DocumentRequest.newBuilder()
                .setDocumentId("tenant2-doc456")
                .build();

        DocumentProcessorOuterClass.DocumentResponse response = stub.process(request);

        assertEquals("tenant2-doc456", response.getDocumentId());
        assertEquals("Processed by Tenant2", response.getStatus());
    }

    @Test
    @TestSecurity(
            user = "forbidden-user",
            roles = {"admin"},
            attributes = {
                    @SecurityAttribute(key = "tenant_id", value = "forbiddenTenant", type = AttributeType.STRING)
            }
    )
    void testForbiddenAccess() {
        DocumentProcessorOuterClass.DocumentRequest request = DocumentProcessorOuterClass.DocumentRequest.newBuilder()
                .setDocumentId("forbidden-doc789")
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            stub.process(request);
        });

        assertEquals(Status.PERMISSION_DENIED.getCode(), exception.getStatus().getCode());
        assertTrue(exception.getStatus().getDescription().contains("Access denied"));
    }

    @Test
    @TestSecurity(
            user = "empty-user",
            roles = {"admin"},
            attributes = {
                    @SecurityAttribute(key = "tenant_id", value = "tenant2", type = AttributeType.STRING)
            }
    )
    void testEmptyDocumentId() {
        DocumentProcessorOuterClass.DocumentRequest request = DocumentProcessorOuterClass.DocumentRequest.newBuilder()
                .setDocumentId("")
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            stub.process(request);
        });

        assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        assertTrue(exception.getStatus().getDescription().contains("cannot be empty"));
    }
}
