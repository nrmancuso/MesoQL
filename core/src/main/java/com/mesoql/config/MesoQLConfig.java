package com.mesoql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties bound under the {@code mesoql} prefix.
 */
@ConfigurationProperties(prefix = "mesoql")
public class MesoQLConfig {

    private String opensearchUrl = "http://localhost:9200";
    private String ollamaBaseUrl = "http://localhost:11434";
    private String nwsApiBaseUrl = "https://api.weather.gov";
    private String embedModel = "nomic-embed-text";
    private String generateModel = "llama3";

    /**
     * Returns the OpenSearch base URL.
     */
    public String getOpensearchUrl() { return opensearchUrl; }

    /**
     * Sets the OpenSearch base URL.
     */
    public void setOpensearchUrl(String opensearchUrl) { this.opensearchUrl = opensearchUrl; }

    /**
     * Returns the Ollama base URL.
     */
    public String getOllamaBaseUrl() { return ollamaBaseUrl; }

    /**
     * Sets the Ollama base URL.
     */
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }

    /**
     * Returns the NWS API base URL.
     */
    public String getNwsApiBaseUrl() { return nwsApiBaseUrl; }

    /**
     * Sets the NWS API base URL.
     */
    public void setNwsApiBaseUrl(String nwsApiBaseUrl) { this.nwsApiBaseUrl = nwsApiBaseUrl; }

    /**
     * Returns the name of the embedding model.
     */
    public String getEmbedModel() { return embedModel; }

    /**
     * Sets the name of the embedding model.
     */
    public void setEmbedModel(String embedModel) { this.embedModel = embedModel; }

    /**
     * Returns the name of the text-generation model.
     */
    public String getGenerateModel() { return generateModel; }

    /**
     * Sets the name of the text-generation model.
     */
    public void setGenerateModel(String generateModel) { this.generateModel = generateModel; }
}
