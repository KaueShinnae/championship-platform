package com.championship.partidas.application;

import com.championship.partidas.domain.Partida;
import com.championship.partidas.infrastructure.messaging.DomainEventWriter;
import com.championship.partidas.infrastructure.persistence.PartidaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidaServiceTest {

    @Mock
    private PartidaRepository partidaRepository;

    @Mock
    private DomainEventWriter domainEventWriter;

    @Mock
    private ChaveamentoService chaveamentoService;

    @InjectMocks
    private PartidaService partidaService;

    private Partida partida() {
        return Partida.agendar(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Timaço FC", UUID.randomUUID(), "Rival FC", null);
    }

    @Test
    void listarSemFiltroRetornaTodasOrdenadas() {
        when(partidaRepository.findAllByOrderByScheduledAtDesc()).thenReturn(List.of(partida(), partida()));

        List<Partida> resultado = partidaService.listar(null);

        assertThat(resultado).hasSize(2);
        verify(partidaRepository).findAllByOrderByScheduledAtDesc();
    }

    @Test
    void listarComGroupIdFiltraPorGrupo() {
        UUID groupId = UUID.randomUUID();
        when(partidaRepository.findByGroupIdOrderByScheduledAtDesc(groupId)).thenReturn(List.of(partida()));

        List<Partida> resultado = partidaService.listar(groupId);

        assertThat(resultado).hasSize(1);
        verify(partidaRepository).findByGroupIdOrderByScheduledAtDesc(groupId);
    }
}
