package com.mesoql.search;

/**
 * Type classification for a field in an index schema.
 */
enum FieldType {
    /**
     * Exact-match string field.
     */
    KEYWORD,
    /**
     * 32-bit integer field.
     */
    INTEGER,
    /**
     * 64-bit integer field.
     */
    LONG,
    /**
     * ISO-8601 date field.
     */
    DATE
}
