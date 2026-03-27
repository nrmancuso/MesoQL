package com.mesoql.search;

import com.mesoql.MesoQLException;
import com.mesoql.ast.QueryAST;
import com.mesoql.config.MesoQLConfig;
import org.apache.http.HttpHost;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.indices.IndicesStatsResponse;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

@Service
public class OpenSearchService {

    private final OpenSearchClient client;

    private final org.opensearch.client.RestClient restClient;

    public OpenSearchService(MesoQLConfig config) {
        this.restClient = org.opensearch.client.RestClient.builder(
            HttpHost.create(config.getOpensearchUrl())
        ).build();
        final RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        this.client = new OpenSearchClient(transport);
    }

    public void createStormEventsIndex() throws IOException {
        createIndexFromJson("storm_events", STORM_EVENTS_MAPPING);
    }

    public void createForecastDiscussionsIndex() throws IOException {
        createIndexFromJson("forecast_discussions", FORECAST_DISCUSSIONS_MAPPING);
    }

    private void createIndexFromJson(String index, String json) throws IOException {
        boolean exists = client.indices().exists(e -> e.index(index)).value();
        if (exists) return;
        final org.opensearch.client.Request req = new org.opensearch.client.Request("PUT", "/" + index);
        req.setJsonEntity(json);
        restClient.performRequest(req);
    }

    @SuppressWarnings("unchecked")
    public SearchResponse<Map> hybridSearch(
            String index,
            float[] queryVector,
            List<QueryAST.Filter> filters,
            int topK) throws IOException {

        final String vecField = vectorField(index);
        final Class<Map> docClass = (Class<Map>) (Class<?>) Map.class;

        if (filters.isEmpty()) {
            return client.search(s -> s
                .index(index)
                .query(q -> q.knn(knn -> knn
                    .field(vecField)
                    .vector(queryVector)
                    .k(topK)
                ))
                .size(topK),
                docClass
            );
        }

        // Combine k-NN with bool filter: use script_score or nested bool approach
        // since opensearch-java 2.6.0 doesn't have a native hybrid query builder.
        // We use bool must with knn + filter as a pragmatic approach.
        final List<Query> filterQueries = filters.stream()
            .map(this::filterToQuery)
            .toList();

        return client.search(s -> s
            .index(index)
            .query(q -> q.bool(b -> b
                .must(m -> m.knn(knn -> knn
                    .field(vecField)
                    .vector(queryVector)
                    .k(topK)
                ))
                .filter(filterQueries)
            ))
            .size(topK),
            docClass
        );
    }

    public boolean documentExists(String index, String docId) throws IOException {
        return client.exists(e -> e.index(index).id(docId)).value();
    }

    public BulkResponse bulkIndex(List<BulkOperation> operations) throws IOException {
        return client.bulk(BulkRequest.of(b -> b.operations(operations)));
    }

    public IndicesStatsResponse indexStats(String index) throws IOException {
        return client.indices().stats(s -> s.index(index));
    }

    public OpenSearchClient getClient() {
        return client;
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

    private static final String STORM_EVENTS_MAPPING = """
        {
          "settings": {
            "index": { "knn": true, "knn.algo_param.ef_search": 100 }
          },
          "mappings": {
            "properties": {
              "event_id":        { "type": "keyword" },
              "state":           { "type": "keyword" },
              "event_type":      { "type": "keyword" },
              "year":            { "type": "integer" },
              "begin_date":      { "type": "date" },
              "fatalities":      { "type": "integer" },
              "damage_property": { "type": "long" },
              "narrative":       { "type": "text" },
              "narrative_vector": {
                "type": "knn_vector",
                "dimension": 768,
                "method": {
                  "name": "hnsw",
                  "engine": "lucene",
                  "parameters": { "m": 16, "ef_construction": 128 }
                }
              }
            }
          }
        }
        """;

    private static final String FORECAST_DISCUSSIONS_MAPPING = """
        {
          "settings": {
            "index": { "knn": true, "knn.algo_param.ef_search": 100 }
          },
          "mappings": {
            "properties": {
              "discussion_id":   { "type": "keyword" },
              "office":          { "type": "keyword" },
              "region":          { "type": "keyword" },
              "issuance_time":   { "type": "date" },
              "season":          { "type": "keyword" },
              "text":            { "type": "text" },
              "text_vector": {
                "type": "knn_vector",
                "dimension": 768,
                "method": {
                  "name": "hnsw",
                  "engine": "lucene",
                  "parameters": { "m": 16, "ef_construction": 128 }
                }
              }
            }
          }
        }
        """;
}
