package com.mesoql.executor;

import com.mesoql.ollama.OllamaClient;
import com.mesoql.ollama.PromptBuilder;
import com.mesoql.search.OpenSearchService;
import com.mesoql.search.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executes a {@link SearchRequest} against OpenSearch and Ollama.
 */
@Service
public class QueryExecutor {

    private final OpenSearchService searchService;
    private final OllamaClient ollamaClient;

    /**
     * Constructs the executor with its required search and LLM collaborators.
     */
    public QueryExecutor(OpenSearchService searchService, OllamaClient ollamaClient) {
        this.searchService = searchService;
        this.ollamaClient = ollamaClient;
    }

    /**
     * Executes the given request and returns the combined result.
     */
    @SuppressWarnings("unchecked")
    public QueryResult execute(SearchRequest request) {
        final String source = request.source();
        final String semanticText = request.semantic();
        final int topK = request.limit();

        final float[] queryVector = ollamaClient.embed(semanticText);

        final SearchResponse<Map> response;
        try {
            response = searchService.hybridSearch(source, queryVector, request.filters(), topK);
        } catch (IOException e) {
            throw new com.mesoql.MesoQLException("Search failed", e);
        }

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> hits = response.hits().hits().stream()
            .map(h -> (Map<String, Object>) h.source())
            .toList();

        final String narrativeField = switch (source) {
            case "storm_events" -> "narrative";
            case "forecast_discussions" -> "text";
            default -> "narrative";
        };

        final List<String> narratives = hits.stream()
            .map(h -> h.getOrDefault(narrativeField, "").toString())
            .toList();

        String synthesis = null;
        List<String> explanations = null;
        String clusters = null;

        if (request.synthesizePrompt().isPresent()) {
            final String prompt = PromptBuilder.synthesizePrompt(
                request.synthesizePrompt().get(), narratives);
            synthesis = ollamaClient.generate(prompt);
        }

        if (request.explain()) {
            explanations = new ArrayList<>();
            for (final String narrative : narratives) {
                final String prompt = PromptBuilder.explainPrompt(semanticText, narrative);
                explanations.add(ollamaClient.generate(prompt));
            }
        }

        if (request.clusterByTheme()) {
            final String prompt = PromptBuilder.clusterPrompt(semanticText, narratives);
            clusters = ollamaClient.generate(prompt);
        }

        return new QueryResult(hits, synthesis, explanations, clusters);
    }
}
