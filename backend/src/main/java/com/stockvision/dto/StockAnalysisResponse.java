package com.stockvision.dto;

import java.util.ArrayList;
import java.util.List;

public class StockAnalysisResponse {

    private String symbol;
    private String analysis;
    private List<String> keyFactors = new ArrayList<>();
    private List<String> risks = new ArrayList<>();
    private String disclaimer;
    private List<KnowledgeSourceResponse> sources = new ArrayList<>();

    public StockAnalysisResponse() {}

    public StockAnalysisResponse(String symbol, String analysis, List<String> keyFactors,
                                 List<String> risks, String disclaimer, List<KnowledgeSourceResponse> sources) {
        this.symbol = symbol;
        this.analysis = analysis;
        this.keyFactors = keyFactors != null ? keyFactors : new ArrayList<>();
        this.risks = risks != null ? risks : new ArrayList<>();
        this.disclaimer = disclaimer;
        this.sources = sources != null ? sources : new ArrayList<>();
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    public List<String> getKeyFactors() { return keyFactors; }
    public void setKeyFactors(List<String> keyFactors) { this.keyFactors = keyFactors; }

    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public List<KnowledgeSourceResponse> getSources() { return sources; }
    public void setSources(List<KnowledgeSourceResponse> sources) { this.sources = sources; }
}