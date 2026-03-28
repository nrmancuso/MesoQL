package com.mesoql.api;

import graphql.GraphQLError;
import graphql.scalars.ExtendedScalars;
import graphql.schema.TypeResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Registers the {@code Long} extended scalar, the {@code SearchHit} union type resolver,
 * and the exception resolver that propagates {@link GraphQLError} exceptions as GraphQL errors.
 */
@Configuration
public class GraphQLConfig {

    /**
     * Wires in the Long scalar and the SearchHit union TypeResolver.
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        final TypeResolver searchHitResolver = env -> {
            final Object object = env.getObject();
            if (object instanceof StormEventHit) {
                return env.getSchema().getObjectType("StormEventHit");
            }
            return env.getSchema().getObjectType("ForecastDiscussionHit");
        };

        return wiringBuilder -> wiringBuilder
            .scalar(ExtendedScalars.GraphQLLong)
            .type("SearchHit", typeWiring -> typeWiring.typeResolver(searchHitResolver));
    }

    /**
     * Propagates exceptions that implement {@link GraphQLError} (e.g. {@code GraphqlErrorException})
     * as GraphQL errors rather than letting Spring wrap them in {@code INTERNAL_ERROR}.
     */
    @Bean
    public DataFetcherExceptionResolver graphQLErrorExceptionResolver() {
        return (ex, env) -> {
            if (ex instanceof GraphQLError error) {
                return Mono.just(List.of(error));
            }
            return Mono.empty();
        };
    }
}
