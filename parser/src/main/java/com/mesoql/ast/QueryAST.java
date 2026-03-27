package com.mesoql.ast;

import java.util.List;

public class QueryAST {

    public sealed interface Node permits Query, SearchClause, WhereClause,
        SemanticClause, Filter, OutputClause {}

    public record Query(SearchClause search, WhereClause where, List<OutputClause> outputs) implements Node {}
    public record SearchClause(String source) implements Node {}
    public record WhereClause(SemanticClause semantic, List<Filter> filters) implements Node {}
    public record SemanticClause(String text) implements Node {}

    public sealed interface Filter extends Node permits InFilter, BetweenFilter, ComparisonFilter {}
    public record InFilter(String field, List<String> values) implements Filter {}
    public record BetweenFilter(String field, double low, double high) implements Filter {}
    public record ComparisonFilter(String field, String op, String value) implements Filter {}

    public sealed interface OutputClause extends Node permits SynthesizeClause, ClusterClause, ExplainClause, LimitClause {}
    public record SynthesizeClause(String question) implements OutputClause {}
    public record ClusterClause() implements OutputClause {}
    public record ExplainClause() implements OutputClause {}
    public record LimitClause(int n) implements OutputClause {}
}
