package com.mesoql.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultPrinter {

    private final boolean json;
    private final ObjectMapper mapper;

    public ResultPrinter(boolean json) {
        this.json = json;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void print(QueryResult result) {
        if (json) {
            printJson(result);
        } else {
            printHuman(result);
        }
    }

    private void printHuman(QueryResult result) {
        List<Map<String, Object>> hits = result.hits();
        for (int i = 0; i < hits.size(); i++) {
            Map<String, Object> hit = hits.get(i);
            System.out.printf("%n--- [%d] ---%n", i + 1);
            hit.forEach((key, value) -> {
                if (!key.endsWith("_vector")) {
                    System.out.printf("  %-20s %s%n", key + ":", value);
                }
            });
            if (result.explanations() != null && i < result.explanations().size()) {
                System.out.printf("  %-20s %s%n", "explanation:", result.explanations().get(i));
            }
        }

        if (result.synthesis() != null) {
            System.out.printf("%n=== Synthesis ===%n%s%n", result.synthesis());
        }
        if (result.clusters() != null) {
            System.out.printf("%n=== Clusters ===%n%s%n", result.clusters());
        }
    }

    private void printJson(QueryResult result) {
        try {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("hits", result.hits());
            if (result.synthesis() != null) output.put("synthesis", result.synthesis());
            if (result.explanations() != null) output.put("explanations", result.explanations());
            if (result.clusters() != null) output.put("clusters", result.clusters());
            System.out.println(mapper.writeValueAsString(output));
        } catch (Exception e) {
            System.err.println("Error serializing results: " + e.getMessage());
        }
    }
}
