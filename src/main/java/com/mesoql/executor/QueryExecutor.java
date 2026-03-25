package com.mesoql.executor;

import com.mesoql.ast.QueryAST;
import com.mesoql.ollama.OllamaClient;
import com.mesoql.ollama.PromptBuilder;
import com.mesoql.planner.QueryPlanner;
import com.mesoql.search.OpenSearchService;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QueryExecutor {

    private final OpenSearchService searchService;
    private final OllamaClient ollamaClient;
    private final QueryPlanner planner;

    public QueryExecutor(OpenSearchService searchService, OllamaClient ollamaClient, QueryPlanner planner) {
        this.searchService = searchService;
        this.ollamaClient = ollamaClient;
        this.planner = planner;
    }

    @SuppressWarnings("unchecked")
    public QueryResult execute(QueryAST.Query query) {
        planner.validate(query);

        String source = query.search().source();
        String semanticText = query.where().semantic().text();
        int topK = query.outputs().stream()
            .filter(c -> c instanceof QueryAST.LimitClause)
            .map(c -> ((QueryAST.LimitClause) c).n())
            .findFirst()
            .orElse(10);

        float[] queryVector = ollamaClient.embed(semanticText);

        SearchResponse<Map> response;
        try {
            response = searchService.hybridSearch(source, queryVector, query.where().filters(), topK);
        } catch (IOException e) {
            throw new com.mesoql.MesoQLException("Search failed", e);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hits = response.hits().hits().stream()
            .map(h -> (Map<String, Object>) h.source())
            .toList();

        String narrativeField = switch (source) {
            case "storm_events" -> "narrative";
            case "forecast_discussions" -> "text";
            default -> "narrative";
        };

        List<String> narratives = hits.stream()
            .map(h -> h.getOrDefault(narrativeField, "").toString())
            .toList();

        String synthesis = null;
        List<String> explanations = null;
        String clusters = null;

        for (QueryAST.OutputClause clause : query.outputs()) {
            switch (clause) {
                case QueryAST.SynthesizeClause s -> {
                    String prompt = PromptBuilder.synthesizePrompt(s.question(), narratives);
                    synthesis = ollamaClient.generate(prompt);
                }
                case QueryAST.ExplainClause ignored -> {
                    explanations = new ArrayList<>();
                    for (String narrative : narratives) {
                        String prompt = PromptBuilder.explainPrompt(semanticText, narrative);
                        explanations.add(ollamaClient.generate(prompt));
                    }
                }
                case QueryAST.ClusterClause ignored -> {
                    String prompt = PromptBuilder.clusterPrompt(semanticText, narratives);
                    clusters = ollamaClient.generate(prompt);
                }
                case QueryAST.LimitClause ignored -> {} // already handled
            }
        }

        return new QueryResult(hits, synthesis, explanations, clusters);
    }
}
