# ranking.updated.v1

## Contexto
Publicado pelo `ranking-service` sempre que termina de recalcular a
classificação de um grupo em reação a `match.finished.v1`. Consumido pelo
`mcp-agent-service` para saber quando invalidar/atualizar cache ou
projeção local (SPEC.md §3). Payload é intencionalmente enxuto — quem
quiser os dados completos chama `get_group_standings` (tool MCP) ou
`GET /groups/{group_id}/standings`.

## Produtor
`ranking-service` — via transactional outbox, gravado na mesma transação
que persiste as linhas atualizadas do read model.

## Consumidores
- `mcp-agent-service` (Semana 3/4): invalidação de cache da projeção de ranking.

## Chave de partição
`aggregate_id` (= `group_id`).

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-16T12:00:05Z",
  "aggregate_id": "uuid do grupo",
  "type": "ranking.updated.v1",
  "payload": {
    "group_id": "uuid",
    "championship_id": "uuid",
    "updated_at": "2026-07-16T12:00:05Z"
  }
}
```

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumer é idempotente — a implementar no `mcp-agent-service` (Semana 3/4)
- [x] Teste de contrato validando o schema do payload (`ranking-service`)
