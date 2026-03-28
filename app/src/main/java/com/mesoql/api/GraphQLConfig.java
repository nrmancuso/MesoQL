package com.mesoql.api;

import graphql.scalars.ExtendedScalars;
import graphql.schema.TypeResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Registers the {@code Long} extended scalar and the {@code SearchHit} union type resolver.
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
}
