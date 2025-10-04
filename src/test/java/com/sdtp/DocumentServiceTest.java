package com.sdtp;

import com.sdtp.model.Document;
import com.sdtp.service.DocumentService;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    DocumentService documentService;
    JsonWebToken jwt;

    @BeforeEach
    void setup() {
        documentService = new DocumentService();
        jwt = mock(JsonWebToken.class);
    }

    // ----- createDocument tests -----
    @Test
    void createDocument_asAdmin_success() {
        when(jwt.getGroups()).thenReturn(Set.of("admin"));
        when(jwt.getName()).thenReturn("alice");
        when(jwt.getClaim("tenant_id")).thenReturn("tenant1");

        Document doc = new Document();
        doc.setTitle("Test");
        doc.setContent("Content");

        Document created = documentService.createDocument(doc, jwt).await().indefinitely();
        assertNotNull(created.getId());
        assertEquals("tenant1", created.getTenantId());
        assertEquals("alice", created.getCreatedBy());
    }

    @Test
    void createDocument_nonAdmin_forbidden() {
        when(jwt.getGroups()).thenReturn(Set.of("viewer"));
        when(jwt.getName()).thenReturn("bob");
        when(jwt.getClaim("tenant_id")).thenReturn("tenant1");

        Document doc = new Document();
        doc.setTitle("Test");

        assertThrows(ForbiddenException.class, () -> {
            documentService.createDocument(doc, jwt).await().indefinitely();
        });
    }

    // ----- getDocumentById tests -----
    @Test
    void getDocumentById_success() {
        // Step 1: create document as admin
        when(jwt.getGroups()).thenReturn(Set.of("admin"));
        when(jwt.getName()).thenReturn("alice");
        when(jwt.getClaim("tenant_id")).thenReturn("tenant1");

        Document doc = new Document();
        doc.setTitle("Doc1");
        doc.setContent("Content1");
        Document created = documentService.createDocument(doc, jwt).await().indefinitely();

        // Step 2: switch JWT to viewer for fetching
        when(jwt.getGroups()).thenReturn(Set.of("viewer"));
        when(jwt.getName()).thenReturn("bob");
        when(jwt.getClaim("tenant_id")).thenReturn("tenant1");

        Document fetched = documentService.getDocumentById(created.getId(), jwt).await().indefinitely();

        assertEquals(created.getId(), fetched.getId());
        assertEquals("Doc1", fetched.getTitle());
    }

    @Test
    void getDocumentById_wrongTenant_forbidden() {
        when(jwt.getGroups()).thenReturn(Set.of("admin"));
        when(jwt.getName()).thenReturn("alice");
        when(jwt.getClaim("tenant_id")).thenReturn("tenant1");

        Document doc = new Document();
        doc.setTitle("Doc1");
        Document created = documentService.createDocument(doc, jwt).await().indefinitely();

        // Change JWT tenant
        when(jwt.getClaim("tenant_id")).thenReturn("tenant2");

        assertThrows(ForbiddenException.class, () -> {
            documentService.getDocumentById(created.getId(), jwt).await().indefinitely();
        });
    }

    // ----- getAllDocumentsForTenant tests -----
    @Test
    void getAllDocumentsForTenant_asAdmin() {
        when(jwt.getGroups()).thenReturn(Set.of("admin"));
        when(jwt.getName()).thenReturn("alice");
        when(jwt.getClaim("tenant_id")).thenReturn("tenant1");

        // create docs
        Document d1 = new Document(); d1.setTitle("D1");
        Document d2 = new Document(); d2.setTitle("D2");
        documentService.createDocument(d1, jwt).await().indefinitely();
        documentService.createDocument(d2, jwt).await().indefinitely();

        List<Document> docs = documentService.getAllDocumentsForTenant(jwt).await().indefinitely();
        assertEquals(2, docs.size());
    }

    @Test
    void getAllDocumentsForTenant_asViewer() {
        when(jwt.getGroups()).thenReturn(Set.of("viewer"));
        when(jwt.getName()).thenReturn("bob");
        when(jwt.getClaim("tenant_id")).thenReturn("tenant1");

        // create docs as admin
        when(jwt.getGroups()).thenReturn(Set.of("admin"));
        when(jwt.getName()).thenReturn("alice");
        Document d1 = new Document(); d1.setTitle("D1");
        Document d2 = new Document(); d2.setTitle("D2");
        documentService.createDocument(d1, jwt).await().indefinitely();
        documentService.createDocument(d2, jwt).await().indefinitely();

        // switch to viewer
        when(jwt.getGroups()).thenReturn(Set.of("viewer"));
        when(jwt.getName()).thenReturn("bob");

        List<Document> docs = documentService.getAllDocumentsForTenant(jwt).await().indefinitely();
        assertEquals(2, docs.size());
    }

    // ----- getDocumentsByUser tests -----
    @Test
    void getDocumentsByUser_admin_seesAll() {
        when(jwt.getGroups()).thenReturn(Set.of("admin"));
        when(jwt.getName()).thenReturn("alice");
        when(jwt.getClaim("tenant_id")).thenReturn("tenant1");

        Document d1 = new Document(); d1.setTitle("D1");
        Document d2 = new Document(); d2.setTitle("D2");
        documentService.createDocument(d1, jwt).await().indefinitely();
        documentService.createDocument(d2, jwt).await().indefinitely();

        List<Document> docs = documentService.getDocumentsByUser(jwt).await().indefinitely();
        assertEquals(2, docs.size());
    }


}
