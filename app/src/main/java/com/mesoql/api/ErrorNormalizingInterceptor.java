package com.mesoql.api;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Strips {@code locations} and {@code extensions} from all GraphQL errors so that
 * every error in the response contains only a {@code message} field.
 *
 * <p>This makes integration-test assertions predictable regardless of whether the
 * error originates from application code or from graphql-java schema validation.
 */
@Component
public class ErrorNormalizingInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        return chain.next(request).map(ErrorNormalizingInterceptor::normalizeErrors);
    }

    private static WebGraphQlResponse normalizeErrors(WebGraphQlResponse response) {
        final List<ResponseError> errors = response.getErrors();
        if (errors.isEmpty()) {
            return response;
        }
        final List<GraphQLError> normalized = errors.stream()
            .<GraphQLError>map(error -> new MessageOnlyError(error.getMessage()))
            .toList();
        return response.transform(builder -> builder.errors(normalized));
    }

    /**
     * A {@link GraphQLError} that serializes to {@code {"message": "..."}} only.
     */
    private record MessageOnlyError(String message) implements GraphQLError {

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public List<SourceLocation> getLocations() {
            return null;
        }

        @Override
        public ErrorClassification getErrorType() {
            return null;
        }

        @Override
        public Map<String, Object> getExtensions() {
            return null;
        }
    }
}
