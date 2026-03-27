package com.mesoql.executor;

import java.util.List;
import java.util.Map;

/**
 * Holds the output of a MesoQL query execution, including hits and any LLM-generated content.
 */
public record QueryResult(
    List<Map<String, Object>> hits,
    String synthesis,
    List<String> explanations,
    String clusters
) {}
