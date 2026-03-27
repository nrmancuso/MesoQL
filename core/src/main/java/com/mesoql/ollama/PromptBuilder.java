package com.mesoql.ollama;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Builds LLM prompt strings for SYNTHESIZE, EXPLAIN, and CLUSTER BY THEME output clauses
 */
public class PromptBuilder {

    /**
     * Returns a prompt that asks the model to answer a question using the given weather records.
     */
    public static String synthesizePrompt(String question, List<String> narratives) {
        final StringBuilder records = new StringBuilder();
        for (int i = 0; i < narratives.size(); i++) {
            records.append("[").append(i + 1).append("] ").append(narratives.get(i)).append("\n");
        }
        return """
            You are an expert meteorologist. The following are real weather event records retrieved from the NOAA Storm Events Database.

            --- RECORDS ---
            %s--- END RECORDS ---

            Based only on the records above, answer the following question:
            %s

            Be specific. Cite record numbers where relevant.""".formatted(records, question);
    }

    /**
     * Returns a prompt that asks the model to explain why a retrieved narrative matches the query.
     */
    public static String explainPrompt(String queryText, String narrative) {
        return """
            You are an expert meteorologist. A user searched for weather events matching the following description:

            "%s"

            The following event was retrieved as a match:

            %s

            In one sentence, explain why this event is semantically relevant to the user's search."""
            .formatted(queryText, narrative);
    }

    /**
     * Returns a prompt that asks the model to group the given narratives into thematic clusters.
     */
    public static String clusterPrompt(String queryText, List<String> narratives) {
        final StringBuilder records = new StringBuilder();
        for (int i = 0; i < narratives.size(); i++) {
            records.append("[").append(i + 1).append("] ").append(narratives.get(i)).append("\n");
        }
        return """
            You are an expert meteorologist. The following weather records were retrieved for the query:

            "%s"

            --- RECORDS ---
            %s--- END RECORDS ---

            Group these records into 2-5 thematic clusters based on their meteorological characteristics.
            For each cluster, provide:
            - A short label (3-5 words)
            - The record numbers in that cluster
            - One sentence describing what the records in the cluster have in common"""
            .formatted(queryText, records);
    }
}
