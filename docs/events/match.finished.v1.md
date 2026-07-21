# match.finished.v1

## Contexto
Publicado pelo `partidas-service` quando o resultado de uma partida é
registrado. É o gatilho do recálculo de classificação (SPEC.md §3):
`ranking-service` consome este evento e atualiza sua projeção — nunca há
chamada síncrona de `partidas-service` para `ranking-service`.

## Produtor
`partidas-service` — via transactional outbox, gravado na mesma transação
que marca a `Partida` como `FINALIZADA`.

## Consumidores
- `ranking-service`: recalcula a classificação do grupo (CQRS read model)
  e, ao final, publica `ranking.updated.v1`.

## Chave de partição
`aggregate_id` (= `match_id`).

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-16T12:00:00Z",
  "aggregate_id": "uuid da partida",
  "type": "match.finished.v1",
  "payload": {
    "match_id": "uuid",
    "championship_id": "uuid",
    "group_id": "uuid | null",
    "home_team": { "team_id": "uuid", "name": "string", "score": 0 },
    "away_team": { "team_id": "uuid", "name": "string", "score": 0 },
    "wo": false,
    "played_at": "2026-07-16T12:00:00Z"
  }
}
```

## Regras
- `group_id` pode ser `null` — nesse caso `ranking-service` ignora o
  evento para fins de classificação por grupo (não há grupo pra atualizar),
  mas ainda marca o evento como processado (idempotência).
- Critério de pontos de classificação (aplicado pelo `ranking-service`, não
  pelo produtor): vitória = 3, empate = 1 para cada lado, derrota = 0.
- `wo` (adicionado em torneios-em-geral): quando `true`, o resultado é uma
  decisão administrativa (W.O.). Conta a vitória/derrota de classificação, mas
  é **neutro no placar** — o consumidor não soma `score` a Pró/Contra/Saldo.
  Campo opcional; ausente em eventos antigos equivale a `false`.

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumer é idempotente (`ranking-service`, tabela `processed_events`)
- [x] Teste de contrato validando o schema do payload (`partidas-service` e `ranking-service`)
