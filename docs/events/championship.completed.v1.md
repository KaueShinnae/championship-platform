# championship.completed.v1

## Contexto
Publicado pelo `partidas-service` quando o campeão do torneio é definido:
na final do mata-mata (formatos `PLAYOFFS` e `GRUPOS_PLAYOFFS`) ou quando a
última partida dos pontos corridos é finalizada. Fecha o ciclo de vida do
campeonato sem ação manual do organizador — o `inscricoes-service` consome e
marca o campeonato como `ENCERRADO`.

## Produtor
`partidas-service` — via transactional outbox, gravado na mesma transação do
registro do resultado que definiu o campeão (`ChaveamentoService`).

## Consumidores
- `inscricoes-service`: transiciona o campeonato `EM_ANDAMENTO -> ENCERRADO`
  e registra `campeao_time_id` / `campeao_nome`.

## Chave de partição
`aggregate_id` (= `championship_id`).

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-16T12:00:00Z",
  "aggregate_id": "uuid do campeonato",
  "type": "championship.completed.v1",
  "payload": {
    "championship_id": "uuid",
    "champion": { "team_id": "uuid", "name": "string" },
    "completed_at": "2026-07-16T12:00:00Z"
  }
}
```

## Regras
- Emitido no máximo uma vez por campeonato no fluxo normal (a final só é
  finalizada uma vez); a idempotência do consumer cobre reentregas.
- O campeão é decidido pelo `partidas-service` com os próprios resultados:
  vencedor da final no mata-mata, ou líder da classificação nos pontos
  corridos (desempate: pontos, vitórias, saldo, gols pró, ordem do sorteio).

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumer é idempotente (`inscricoes-service`, tabela `processed_events`)
- [x] Teste de contrato validando o schema do payload (`partidas-service`)
