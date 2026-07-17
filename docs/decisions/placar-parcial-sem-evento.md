# Decisão: placar parcial ao vivo sem evento Kafka

**Data:** 2026-07-16 · **Status:** aceita (revisada com o product owner na rodada 4)

## Contexto
O placar parcial (`POST /matches/{id}/score`) é a ferramenta de contagem do
organizador durante a partida — pontos genéricos, vale para qualquer esporte.
O dashboard lê via polling; visitantes acompanham em ~2s de atraso.

## Decisão
O placar parcial **não** gera evento de domínio. Ele é estado operacional
mutável (uma contagem em andamento, corrigível a qualquer momento); o fato de
domínio continua sendo apenas o `match.finished.v1`, emitido no encerramento
com o placar final. Segue a prioridade do projeto de "eventos pequenos e
específicos" — um `score.updated` por clique de +1 seria ruído para todos os
consumers sem nenhum caso de uso downstream hoje.

## Consequências (dívida consciente)
- Atualizações parciais **não aparecem no Monitoramento** nem no feed de
  eventos — o suporte não tem trilha de auditoria de disputas de contagem
  durante a partida (só o resultado final).
- Se surgir caso de uso real (auditoria de disputa, timeline da partida,
  notificações ao vivo), a evolução natural é um `match.score.updated.v1`
  com outbox + consumer idempotente — **não** reaproveitar `match.finished.v1`.

## Guardas relacionadas
- Empate em eliminatória é bloqueado no encerramento em três camadas:
  `Partida.registrarResultado` (backend), botão "Encerrar partida" (desabilita
  com dica) e formulário manual de placar (valida antes de enviar).
- O placar parcial em si permite empate — a exigência de vencedor vale só no
  encerramento.
