package com.mesoql.search;

import com.mesoql.MesoQLException;
import com.mesoql.ast.QueryAST;
import com.mesoql.config.MesoQLConfig;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.indices.IndicesStatsResponse;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring service that manages OpenSearch index creation, hybrid k-NN search, and bulk indexing.
 */
@Service
public class OpenSearchService {

    private final OpenSearchClient client;
    private final ApacheHttpClient5Transport transport;

    /**
     * Constructs the service, initialising the OpenSearch REST client from the given config.
     */
    public OpenSearchService(MesoQLConfig config) {
        this.transport = ApacheHttpClient5TransportBuilder.builder(
            HttpHost.create(URI.create(config.getOpensearchUrl()))
        ).setMapper(new JacksonJsonpMapper()).build();
        this.client = new OpenSearchClient(transport);
    }

    /**
     * Creates the {@code storm_events} index if it does not already exist.
     */
    public void createStormEventsIndex() throws IOException {
        createIndex("storm_events");
    }

    /**
     * Creates the {@code forecast_discussions} index if it does not already exist.
     */
    public void createForecastDiscussionsIndex() throws IOException {
        createIndex("forecast_discussions");
    }

    private void createIndex(String index) throws IOException {
        boolean exists = client.indices().exists(e -> e.index(index)).value();
        if (exists) return;
        client.indices().create(c -> c
            .index(index)
            .settings(s -> s
                .knn(true)
                .knnAlgoParamEfSearch(100)
            )
            .mappings(m -> {
                if ("storm_events".equals(index)) {
                    return m.properties(stormEventsProperties());
                }
                if ("forecast_discussions".equals(index)) {
                    return m.properties(forecastDiscussionProperties());
                }
                throw new MesoQLException("Unknown index mapping: " + index);
            })
        );
    }

    /**
     * Executes a hybrid k-NN plus boolean-filter search against the specified index.
     */
    @SuppressWarnings("unchecked")
    public SearchResponse<Map> hybridSearch(
            String index,
            float[] queryVector,
            List<QueryAST.Filter> filters,
            int topK) throws IOException {

        final String vecField = vectorField(index);
        final Class<Map> docClass = (Class<Map>) (Class<?>) Map.class;
        final List<Float> vectorList = toFloatList(queryVector);

        if (filters.isEmpty()) {
            return client.search(s -> s
                .index(index)
                .query(q -> q.knn(knn -> knn
                    .field(vecField)
                    .vector(vectorList)
                    .k(topK)
                ))
                .size(topK),
                docClass
            );
        }

        // Combine k-NN with bool filter using a bool query with knn + structured filters.
        final List<Query> filterQueries = filters.stream()
            .map(this::filterToQuery)
            .toList();

        return client.search(s -> s
            .index(index)
            .query(q -> q.bool(b -> b
                .must(m -> m.knn(knn -> knn
                    .field(vecField)
                    .vector(vectorList)
                    .k(topK)
                ))
                .filter(filterQueries)
            ))
            .size(topK),
            docClass
        );
    }

    /**
     * Returns true if a document with the given ID already exists in the specified index.
     */
    public boolean documentExists(String index, String docId) throws IOException {
        return client.exists(e -> e.index(index).id(docId)).value();
    }

    /**
     * Submits a bulk indexing request and returns the OpenSearch bulk response.
     */
    public BulkResponse bulkIndex(List<BulkOperation> operations) throws IOException {
        return client.bulk(BulkRequest.of(b -> b.operations(operations)));
    }

    /**
     * Returns index-level statistics for the specified index name.
     */
    public IndicesStatsResponse indexStats(String index) throws IOException {
        return client.indices().stats(s -> s.index(index));
    }

    private static List<Float> toFloatList(float[] arr) {
        final List<Float> list = new ArrayList<>(arr.length);
        for (final float v : arr) {
            list.add(v);
        }
        return list;
    }

    private Query filterToQuery(QueryAST.Filter filter) {
        return switch (filter) {
            case QueryAST.InFilter f -> Query.of(q -> q
                .terms(t -> t.field(f.field()).terms(tv -> tv
                    .value(f.values().stream().map(FieldValue::of).toList())
                ))
            );
            case QueryAST.BetweenFilter f -> Query.of(q -> q
                .range(r -> r.field(f.field()).gte(JsonData.of(f.low())).lte(JsonData.of(f.high())))
            );
            case QueryAST.ComparisonFilter f -> comparisonToQuery(f);
        };
    }

    private Query comparisonToQuery(QueryAST.ComparisonFilter f) {
        return switch (f.op()) {
            case "=" -> Query.of(q -> q.term(t -> t.field(f.field()).value(FieldValue.of(f.value()))));
            case "!=" -> Query.of(q -> q.bool(b -> b.mustNot(
                Query.of(q2 -> q2.term(t -> t.field(f.field()).value(FieldValue.of(f.value()))))
            )));
            case ">", ">=", "<", "<=" -> {
                final double val = Double.parseDouble(f.value());
                yield Query.of(q -> q.range(r -> {
                    r.field(f.field());
                    switch (f.op()) {
                        case ">"  -> r.gt(JsonData.of(val));
                        case ">=" -> r.gte(JsonData.of(val));
                        case "<"  -> r.lt(JsonData.of(val));
                        case "<=" -> r.lte(JsonData.of(val));
                        default -> throw new MesoQLException("Unknown operator: " + f.op());
                    }
                    return r;
                }));
            }
            default -> throw new MesoQLException("Unknown operator: " + f.op());
        };
    }

    private String vectorField(String source) {
        return switch (source) {
            case "storm_events"         -> "narrative_vector";
            case "forecast_discussions" -> "text_vector";
            default -> throw new MesoQLException("Unknown source: " + source);
        };
    }

    private static Map<String, Property> stormEventsProperties() {
        return Map.of(
            "event_id", Property.of(p -> p.keyword(k -> k)),
            "state", Property.of(p -> p.keyword(k -> k)),
            "event_type", Property.of(p -> p.keyword(k -> k)),
            "year", Property.of(p -> p.integer(i -> i)),
            "begin_date", Property.of(p -> p.date(d -> d)),
            "fatalities", Property.of(p -> p.integer(i -> i)),
            "damage_property", Property.of(p -> p.long_(l -> l)),
            "narrative", Property.of(p -> p.text(t -> t)),
            "narrative_vector", vectorProperty()
        );
    }

    private static Map<String, Property> forecastDiscussionProperties() {
        return Map.of(
            "discussion_id", Property.of(p -> p.keyword(k -> k)),
            "office", Property.of(p -> p.keyword(k -> k)),
            "region", Property.of(p -> p.keyword(k -> k)),
            "issuance_time", Property.of(p -> p.date(d -> d)),
            "season", Property.of(p -> p.keyword(k -> k)),
            "text", Property.of(p -> p.text(t -> t)),
            "text_vector", vectorProperty()
        );
    }

    private static Property vectorProperty() {
        return Property.of(p -> p.knnVector(v -> v
            .dimension(768)
            .method(m -> m
                .name("hnsw")
                .engine("lucene")
                .parameters("m", JsonData.of(16))
                .parameters("ef_construction", JsonData.of(128))
            )
        ));
    }
}
