package br.com.postech.hospital.history.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Traduz exceções de domínio para erros GraphQL com o {@code errorType} correto, em vez do INTERNAL_ERROR genérico padrão. */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment environment) {
        if (ex instanceof AccessDeniedException) {
            return GraphqlErrorBuilder.newError(environment)
                    .errorType(ErrorType.FORBIDDEN)
                    .message(ex.getMessage())
                    .build();
        }
        return null;
    }
}
