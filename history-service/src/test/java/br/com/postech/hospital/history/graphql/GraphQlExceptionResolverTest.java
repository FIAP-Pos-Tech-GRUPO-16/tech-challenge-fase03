package br.com.postech.hospital.history.graphql;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class GraphQlExceptionResolverTest {

    private final GraphQlExceptionResolver resolver = new GraphQlExceptionResolver();

    @Test
    void deveConverterAccessDeniedExceptionParaErroForbidden() {
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class, RETURNS_DEEP_STUBS);

        GraphQLError erro = resolver.resolveToSingleError(new AccessDeniedException("acesso negado"), environment);

        assertThat(erro).isNotNull();
        assertThat(erro.getMessage()).isEqualTo("acesso negado");
        assertThat(erro.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void deveRetornarNuloParaOutrasExcecoesDeixandoOTratamentoPadraoAgir() {
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class, RETURNS_DEEP_STUBS);

        GraphQLError erro = resolver.resolveToSingleError(new RuntimeException("erro qualquer"), environment);

        assertThat(erro).isNull();
    }

    @Test
    void deveConverterArgumentoInvalidoParaBadRequest() {
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class, RETURNS_DEEP_STUBS);

        GraphQLError erro = resolver.resolveToSingleError(
                new IllegalArgumentException("pacienteId deve ser um UUID"), environment);

        assertThat(erro).isNotNull();
        assertThat(erro.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    void deveEnxergarArgumentoInvalidoMesmoEnvolvidoEmOutrasExcecoes() {
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class, RETURNS_DEEP_STUBS);
        Throwable encadeado = new RuntimeException("falha ao vincular argumento",
                new IllegalStateException("conversao falhou",
                        new IllegalArgumentException("Invalid UUID string: abc")));

        GraphQLError erro = resolver.resolveToSingleError(encadeado, environment);

        assertThat(erro).isNotNull();
        assertThat(erro.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    void naoDeveEntrarEmLacoComExcecaoQueApontaParaSiMesma() {
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class, RETURNS_DEEP_STUBS);

        GraphQLError erro = resolver.resolveToSingleError(new RuntimeException("erro sem causa"), environment);

        assertThat(erro).isNull();
    }
}
