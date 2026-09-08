package br.com.postech.hospital.scheduling.usuario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioSeederTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void deveCriarUsuariosDeDemonstracaoQuandoTabelaEstaVazia() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-fake");

        new UsuarioSeeder(usuarioRepository, passwordEncoder).run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Usuario>> captor = ArgumentCaptor.forClass(List.class);
        verify(usuarioRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(4);
        assertThat(captor.getValue()).extracting(Usuario::getUsername)
                .containsExactlyInAnyOrder("medica.ana", "enfermeiro.bruno", "paciente.joao", "paciente.maria");
    }

    @Test
    void naoDeveCriarUsuariosQuandoJaExisteAlgumCadastrado() {
        when(usuarioRepository.count()).thenReturn(1L);

        new UsuarioSeeder(usuarioRepository, passwordEncoder).run();

        verify(usuarioRepository, never()).saveAll(org.mockito.ArgumentMatchers.<List<Usuario>>any());
    }
}
