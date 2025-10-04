package com.sdtp;

import com.sdtp.model.Document;
import com.sdtp.service.DocumentService;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusIntegrationTest
public class DocumentServiceIT {

    DocumentService documentService;

    JsonWebToken adminJwt;
    JsonWebToken viewerJwt;

    @BeforeEach
    void setup() {
        // manually instantiate service
        documentService = new DocumentService();

        // create simple mocks for JWT representing different roles
        adminJwt = new MockJwt("alice", Set.of("admin"), "tenant1");
        viewerJwt = new MockJwt("bob", Set.of("viewer"), "tenant1");

    }

    // ----- Tenant isolation IT -----
    @Test
    void tenantIsolation_enforced() {
        Document doc1 = new Document("Doc1", "Content1", "tenant1", "alice");
        documentService.createDocument(doc1, adminJwt).await().indefinitely();

        JsonWebToken tenant2Admin = new MockJwt("charlie", Set.of("admin"), "tenant2");
        Document doc2 = new Document("Doc2", "Content2", "tenant2", "charlie");
        documentService.createDocument(doc2, tenant2Admin).await().indefinitely();

        List<Document> tenant1Docs = documentService.getAllDocumentsForTenant(viewerJwt).await().indefinitely();
        assertEquals(1, tenant1Docs.size());
        assertEquals("tenant1", tenant1Docs.get(0).getTenantId());
    }

    // ----- Multi-user end-to-end IT -----
    @Test
    void multiUserFlow_adminAndViewer() {
        Document d1 = new Document("D1", "Content1", "tenant1", "alice");
        Document d2 = new Document("D2", "Content2", "tenant1", "alice");
        documentService.createDocument(d1, adminJwt).await().indefinitely();
        documentService.createDocument(d2, adminJwt).await().indefinitely();

        List<Document> viewerDocs = documentService.getDocumentsByUser(viewerJwt).await().indefinitely();
        assertEquals(0, viewerDocs.size());

        List<Document> tenantDocs = documentService.getAllDocumentsForTenant(viewerJwt).await().indefinitely();
        assertEquals(2, tenantDocs.size());
    }

    // ----- Forbidden creation IT -----
    @Test
    void viewerCannotCreateDocument_forbidden() {
        Document doc = new Document("ViewerDoc", "Content", "tenant1", "bob");

        assertThrows(jakarta.ws.rs.ForbiddenException.class, () -> {
            documentService.createDocument(doc, viewerJwt).await().indefinitely();
        });
    }

    // ----- Cross-tenant access forbidden IT -----
    @Test
    void crossTenantAccess_forbidden() {
        Document doc = new Document("Doc1", "Content", "tenant1", "alice");
        Document created = documentService.createDocument(doc, adminJwt).await().indefinitely();

        JsonWebToken tenant2Viewer = new MockJwt("eve", Set.of("viewer"), "tenant2");

        assertThrows(jakarta.ws.rs.ForbiddenException.class, () -> {
            documentService.getDocumentById(created.getId(), tenant2Viewer).await().indefinitely();
        });
    }

    // ----- Helper Mock JWT -----
    static class MockJwt implements JsonWebToken {
        final String name;
        final Set<String> groups;
        final String tenantId;

        MockJwt(String name, Set<String> groups, String tenantId) {
            this.name = name;
            this.groups = groups;
            this.tenantId = tenantId;
        }

        @Override
        public Set<String> getGroups() { return groups; }

        @Override
        public String getName() { return name; }

        @Override
        public Object getClaim(String claimName) {
            if ("tenant_id".equals(claimName)) return tenantId;
            return null;
        }

        @Override
        public Set<String> getClaimNames() { return Set.of(); }
    }
}
