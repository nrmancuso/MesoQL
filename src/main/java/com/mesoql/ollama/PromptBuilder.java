package com.mesoql.ollama;

import java.util.List;
import java.util.stream.IntStream;

public class PromptBuilder {

    public static String synthesizePrompt(String question, List<String> narratives) {
        StringBuilder records = new StringBuilder();
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

    public static String explainPrompt(String queryText, String narrative) {
        return """
            You are an expert meteorologist. A user searched for weather events matching the following description:

            "%s"

            The following event was retrieved as a match:

            %s

            In one sentence, explain why this event is semantically relevant to the user's search."""
            .formatted(queryText, narrative);
    }

    public static String clusterPrompt(String queryText, List<String> narratives) {
        StringBuilder records = new StringBuilder();
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
