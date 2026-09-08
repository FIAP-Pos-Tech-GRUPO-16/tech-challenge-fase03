package br.com.postech.hospital.history.messaging;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.StatusConsulta;
import br.com.postech.hospital.history.historico.HistoricoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsultaEventListenerTest {

    @Mock
    private HistoricoService historicoService;

    @Test
    void aoReceberEventoDeveDelegarParaOServicoDeHistorico() {
        ConsultaEventListener listener = new ConsultaEventListener(historicoService);
        ConsultaEvent evento = ConsultaEvent.criada(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.now().plusDays(1), StatusConsulta.AGENDADA);

        listener.aoReceberEventoDeConsulta(evento);

        verify(historicoService).aplicar(evento);
    }
}
