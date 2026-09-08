package br.com.postech.hospital.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class JwtPropertiesTest {

    @Test
    void deveAceitarSegredoComTrintaEDoisCaracteresOuMais() {
        assertThatNoException().isThrownBy(() -> new JwtProperties("01234567890123456789012345678901", 60));
    }

    @Test
    void deveRejeitarSegredoMenorQueTrintaEDoisCaracteres() {
        assertThatThrownBy(() -> new JwtProperties("segredo-curto", 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 caracteres");
    }

    @Test
    void deveRejeitarSegredoNulo() {
        assertThatThrownBy(() -> new JwtProperties(null, 60))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarExpiracaoNaoPositiva() {
        assertThatThrownBy(() -> new JwtProperties("01234567890123456789012345678901", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiration-minutes");

        assertThatThrownBy(() -> new JwtProperties("01234567890123456789012345678901", -5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
