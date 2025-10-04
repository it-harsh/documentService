package com.sdtp.model;

import java.util.UUID;

public class Document {

  private UUID id;
  private String title;
  private String content;
  private String tenantId;
  private String createdBy;

  public Document() {
    // default constructor for JSON serialization
  }

  public Document(String title, String content, String tenantId, String createdBy) {
    this.id = UUID.randomUUID();
    this.title = title;
    this.content = content;
    this.tenantId = tenantId;
    this.createdBy = createdBy;
  }

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }
}
