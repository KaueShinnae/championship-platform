# match.result.corrected.v1

## Contexto
Publicado pelo `partidas-service` quando o organizador **corrige** um resultado
já registrado (typo, placar trocado). Diferente de `match.finished.v1`, este
evento carrega o placar **anterior** e o **corrigido**, para que o consumidor
aplique a diferença (reverter o antigo, aplicar o novo) sem contar duas vezes —
a projeção de classificação do ranking é acumulativa.

A guarda de dependência (não corrigir depois que a fase avançou ou o próximo
jogo começou) é aplicada no `partidas-service` antes de emitir; quando um
resultado de mata-mata muda de vencedor e ainda é seguro, o bracket é
repropagado localmente (novo `match.scheduled.v1` do confronto seguinte).

## Produtor
`partidas-service` — via transactional outbox, na mesma transação da correção.

## Consumidores
- `ranking-service` — reverte o placar anterior e aplica o corrigido na
  classificação do grupo; publica `ranking.updated.v1`. Partidas sem
  `group_id` (mata-mata) não afetam a classificação e são ignoradas.

## Chave de partição
`aggregate_id` (= `match_id`) — ordena correções da mesma partida.

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-17T14:32:00Z",
  "aggregate_id": "uuid da partida",
  "type": "match.result.corrected.v1",
  "payload": {
    "match_id": "uuid",
    "championship_id": "uuid",
    "group_id": "uuid | null (mata-mata = null)",
    "previous_home": { "team_id": "uuid", "name": "string", "score": 3 },
    "previous_away": { "team_id": "uuid", "name": "string", "score": 1 },
    "corrected_home": { "team_id": "uuid", "name": "string", "score": 1 },
    "corrected_away": { "team_id": "uuid", "name": "string", "score": 3 },
    "wo": false,
    "corrected_at": "2026-07-17T14:32:00Z"
  }
}
```

## Regras
- `event_id` obrigatório e único (deduplicação no consumer).
- O consumidor deve ser idempotente: reprocessar o mesmo `event_id` não pode
  reverter/aplicar de novo (garantido por `processed_events`).
- `previous_*` sempre reflete o placar imediatamente anterior à correção — o
  ranking depende disso para o delta ser exato.

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumer é idempotente (`processed_events` no ranking-service)
- [x] Teste de contrato validando o schema do payload (`partidas-service`)
