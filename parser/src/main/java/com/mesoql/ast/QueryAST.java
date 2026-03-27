package com.mesoql.ast;

import java.util.List;

/**
 * Container for all AST node types produced by the MesoQL parser.
 */
public class QueryAST {

    /**
     * Base sealed interface for all AST nodes.
     */
    public sealed interface Node permits Query, SearchClause, WhereClause,
        SemanticClause, Filter, OutputClause {}

    /**
     * Top-level query node combining search, where, and output clauses.
     */
    public record Query(SearchClause search, WhereClause where, List<OutputClause> outputs) implements Node {
        /**
         * Creates a query node with an immutable output clause list.
         */
        public Query {
            outputs = outputs == null ? List.of() : List.copyOf(outputs);
        }
    }
    /**
     * Identifies the data source to query.
     */
    public record SearchClause(String source) implements Node {}
    /**
     * Combines the mandatory semantic clause with optional structured filters.
     */
    public record WhereClause(SemanticClause semantic, List<Filter> filters) implements Node {
        /**
         * Creates a where clause with an immutable filter list.
         */
        public WhereClause {
            filters = filters == null ? List.of() : List.copyOf(filters);
        }
    }
    /**
     * Holds the natural-language text used for vector search.
     */
    public record SemanticClause(String text) implements Node {}

    /**
     * Sealed base interface for structured filter expressions.
     */
    public sealed interface Filter extends Node permits InFilter, BetweenFilter, ComparisonFilter {}
    /**
     * Filter that checks whether a field's value is in a list of strings.
     */
    public record InFilter(String field, List<String> values) implements Filter {
        /**
         * Creates an IN filter with immutable comparison values.
         */
        public InFilter {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }
    /**
     * Filter that checks whether a numeric field falls within a range.
     */
    public record BetweenFilter(String field, double low, double high) implements Filter {}
    /**
     * Filter that applies a comparison operator to a field value.
     */
    public record ComparisonFilter(String field, String op, String value) implements Filter {}

    /**
     * Sealed base interface for output modifier clauses.
     */
    public sealed interface OutputClause extends Node permits SynthesizeClause, ClusterClause, ExplainClause, LimitClause {}
    /**
     * Requests LLM-generated synthesis of results for a given question.
     */
    public record SynthesizeClause(String question) implements OutputClause {}
    /**
     * Requests clustering of results by theme.
     */
    public record ClusterClause() implements OutputClause {}
    /**
     * Requests an explanation of the query plan and results.
     */
    public record ExplainClause() implements OutputClause {}
    /**
     * Limits the number of results returned.
     */
    public record LimitClause(int n) implements OutputClause {}
}
