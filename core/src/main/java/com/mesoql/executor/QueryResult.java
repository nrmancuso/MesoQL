package com.mesoql.executor;

import java.util.List;
import java.util.Map;

public record QueryResult(
    List<Map<String, Object>> hits,
    String synthesis,
    List<String> explanations,
    String clusters
) {}
