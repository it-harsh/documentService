package com.sdtp.service;

import com.sdtp.model.Document;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.*;

@ApplicationScoped
public class DocumentService {

    private final Map<UUID, Document> documentStore = new HashMap<>();

    /**
     * Create a new document (RBAC + ABAC enforced)
     */
    public Uni<Document> createDocument(Document doc, JsonWebToken jwt) {
        return Uni.createFrom().item(() -> {
            String tenantId = jwt.getClaim("tenant_id");
            Set<String> roles = jwt.getGroups();
            String createdBy = jwt.getName();

            if (!roles.contains("admin")) {
                throw new ForbiddenException("Only admin users can create documents");
            }

            doc.setTenantId(tenantId);
            doc.setCreatedBy(createdBy);

            Document newDoc = new Document(doc.getTitle(), doc.getContent(), tenantId, createdBy);
            documentStore.put(newDoc.getId(), newDoc);
            return newDoc;
        });
    }

    /**
     * Fetch a document by ID (RBAC + ABAC enforced)
     */
    public Uni<Document> getDocumentById(UUID id, JsonWebToken jwt) {
        return Uni.createFrom().item(() -> {
            Document doc = documentStore.get(id);
            if (doc == null) {
                throw new NotFoundException("Document not found");
            }

            String tenantId = jwt.getClaim("tenant_id");
            Set<String> roles = jwt.getGroups();

            if (!tenantId.equals(doc.getTenantId())) {
                throw new ForbiddenException("Cannot access documents from other tenants");
            }

            if (!roles.contains("admin") && !roles.contains("viewer")) {
                throw new ForbiddenException("User does not have permission to read documents");
            }

            return doc;
        });
    }

    /**
     * Fetch all documents for current tenant (RBAC + ABAC enforced)
     */
    public Uni<List<Document>> getAllDocumentsForTenant(JsonWebToken jwt) {
        return Uni.createFrom().item(() -> {
            String tenantId = jwt.getClaim("tenant_id");
            Set<String> roles = jwt.getGroups();

            List<Document> list = new ArrayList<>();
            for (Document doc : documentStore.values()) {
                if (!tenantId.equals(doc.getTenantId())) continue;

                if (roles.contains("admin") || roles.contains("viewer")) {
                    list.add(doc);
                }
            }
            return list;
        });
    }

    /**
     * Fetch documents created by current user (RBAC + ABAC enforced)
     */
    public Uni<List<Document>> getDocumentsByUser(JsonWebToken jwt) {
        return Uni.createFrom().item(() -> {
            String tenantId = jwt.getClaim("tenant_id");
            String username = jwt.getName();
            Set<String> roles = jwt.getGroups();

            List<Document> list = new ArrayList<>();
            for (Document doc : documentStore.values()) {
                if (!tenantId.equals(doc.getTenantId())) continue;

                if (roles.contains("admin") || username.equals(doc.getCreatedBy())) {
                    list.add(doc);
                }
            }
            return list;
        });
    }
}




//package com.sdtp.service;
//
//import com.sdtp.model.Document;
//import org.eclipse.microprofile.jwt.JsonWebToken;
//
//import jakarta.enterprise.context.ApplicationScoped;
//import jakarta.ws.rs.ForbiddenException;
//import jakarta.ws.rs.NotFoundException;
//import java.util.*;
//
//@ApplicationScoped
//public class DocumentService {
//
//    private final Map<UUID, Document> documentStore = new HashMap<>();
//
//    /**
//     * Create a new document. Only "admin" role can create.
//     * ABAC: TenantId is enforced from JWT.
//     */
//    public Document createDocument(Document doc, JsonWebToken jwt) {
//        String tenantId = jwt.getClaim("tenant_id");
//        Set<String> roles = jwt.getGroups();
//        String createdBy = jwt.getName();
//
//        // RBAC: Only admin can create
//        if (!roles.contains("admin")) {
//            throw new ForbiddenException("Only admin users can create documents");
//        }
//
//        // Force tenantId from JWT
//        doc.setTenantId(tenantId);
//        doc.setCreatedBy(createdBy);
//
//        Document newDoc = new Document(doc.getTitle(), doc.getContent(), tenantId, createdBy);
//        documentStore.put(newDoc.getId(), newDoc);
//        return newDoc;
//    }
//
//    /**
//     * Fetch a document by ID
//     * RBAC: admin or viewer
//     * ABAC: document must belong to same tenant
//     */
//    public Document getDocumentById(UUID id, JsonWebToken jwt) {
//        Document doc = documentStore.get(id);
//        if (doc == null) {
//            throw new NotFoundException("Document not found");
//        }
//
//        String tenantId = jwt.getClaim("tenant_id");
//        Set<String> roles = jwt.getGroups();
//
//        // ABAC: Tenant check
//        if (!tenantId.equals(doc.getTenantId())) {
//            throw new ForbiddenException("Cannot access documents from other tenants");
//        }
//
//        // RBAC: Only admin or viewer can read
//        if (!roles.contains("admin") && !roles.contains("viewer")) {
//            throw new ForbiddenException("User does not have permission to read documents");
//        }
//
//        return doc;
//    }
//
//    /**
//     * Get all documents for current tenant
//     * RBAC: admin sees all, viewer sees all tenant documents
//     */
//    public List<Document> getAllDocumentsForTenant(JsonWebToken jwt) {
//        String tenantId = jwt.getClaim("tenant_id");
//        Set<String> roles = jwt.getGroups();
//
//        List<Document> list = new ArrayList<>();
//        for (Document doc : documentStore.values()) {
//            // ABAC: Tenant match
//            if (!tenantId.equals(doc.getTenantId())) continue;
//
//            // RBAC: admin or viewer can read
//            if (roles.contains("admin") || roles.contains("viewer")) {
//                list.add(doc);
//            }
//        }
//        return list;
//    }
//
//    /**
//     * Get documents created by current user
//     * RBAC: admin sees all tenant documents, viewer sees only their own
//     */
//    public List<Document> getDocumentsByUser(JsonWebToken jwt) {
//        String tenantId = jwt.getClaim("tenant_id");
//        String username = jwt.getName();
//        Set<String> roles = jwt.getGroups();
//
//        List<Document> list = new ArrayList<>();
//        for (Document doc : documentStore.values()) {
//            // ABAC: Tenant check
//            if (!tenantId.equals(doc.getTenantId())) continue;
//
//            // RBAC + ABAC: admin sees all, viewer sees only own
//            if (roles.contains("admin") || username.equals(doc.getCreatedBy())) {
//                list.add(doc);
//            }
//        }
//        return list;
//    }
//}
