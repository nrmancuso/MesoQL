package com.mesoql.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.PrintWriter;
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
        print(result, new PrintWriter(System.out, true));
    }

    public void print(QueryResult result, PrintWriter out) {
        if (json) {
            printJson(result, out);
        } else {
            printHuman(result, out);
        }
    }

    private void printHuman(QueryResult result, PrintWriter out) {
        final List<Map<String, Object>> hits = result.hits();
        for (int i = 0; i < hits.size(); i++) {
            final Map<String, Object> hit = hits.get(i);
            out.printf("%n--- [%d] ---%n", i + 1);
            hit.forEach((key, value) -> {
                if (!key.endsWith("_vector") && !key.equals("episode_narrative")) {
                    out.printf("  %-20s %s%n", key + ":", value);
                }
            });
            if (result.explanations() != null && i < result.explanations().size()) {
                out.printf("  %-20s %s%n", "explanation:", result.explanations().get(i));
            }
        }

        if (result.synthesis() != null) {
            out.printf("%n=== Synthesis ===%n%s%n", result.synthesis());
        }
        if (result.clusters() != null) {
            out.printf("%n=== Clusters ===%n%s%n", result.clusters());
        }
    }

    private void printJson(QueryResult result, PrintWriter out) {
        try {
            final Map<String, Object> output = new LinkedHashMap<>();
            output.put("hits", result.hits());
            if (result.synthesis() != null) output.put("synthesis", result.synthesis());
            if (result.explanations() != null) output.put("explanations", result.explanations());
            if (result.clusters() != null) output.put("clusters", result.clusters());
            out.println(mapper.writeValueAsString(output));
        } catch (Exception e) {
            out.println("Error serializing results: " + e.getMessage());
        }
    }
}
