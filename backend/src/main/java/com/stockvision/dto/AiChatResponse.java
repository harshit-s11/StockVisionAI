package com.stockvision.dto;

import java.util.ArrayList;
import java.util.List;

public class AiChatResponse {

    private String answer;
    private String disclaimer;
    private List<KnowledgeSourceResponse> sources = new ArrayList<>();

    public AiChatResponse() {}

    public AiChatResponse(String answer, String disclaimer, List<KnowledgeSourceResponse> sources) {
        this.answer = answer;
        this.disclaimer = disclaimer;
        this.sources = sources != null ? sources : new ArrayList<>();
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public List<KnowledgeSourceResponse> getSources() { return sources; }
    public void setSources(List<KnowledgeSourceResponse> sources) { this.sources = sources; }
}