package com.mesoql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mesoql")
public class MesoQLConfig {

    private String opensearchUrl = "http://localhost:9200";
    private String ollamaBaseUrl = "http://localhost:11434";
    private String embedModel = "nomic-embed-text";
    private String generateModel = "llama3";

    public String getOpensearchUrl() { return opensearchUrl; }
    public void setOpensearchUrl(String opensearchUrl) { this.opensearchUrl = opensearchUrl; }

    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }

    public String getEmbedModel() { return embedModel; }
    public void setEmbedModel(String embedModel) { this.embedModel = embedModel; }

    public String getGenerateModel() { return generateModel; }
    public void setGenerateModel(String generateModel) { this.generateModel = generateModel; }
}
