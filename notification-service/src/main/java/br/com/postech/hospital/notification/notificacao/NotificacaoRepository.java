package br.com.postech.hospital.notification.notificacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    boolean existsByEventoId(UUID eventoId);

    List<Notificacao> findByPacienteId(UUID pacienteId);
}
