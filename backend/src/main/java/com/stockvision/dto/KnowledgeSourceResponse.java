package com.stockvision.dto;

public class KnowledgeSourceResponse {

    private String title;
    private String source;

    public KnowledgeSourceResponse() {}

    public KnowledgeSourceResponse(String title, String source) {
        this.title = title;
        this.source = source;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}