# team.registered.v1

## Contexto
Publicado pelo `inscricoes-service` quando um time é cadastrado com sucesso
(antes da confirmação de inscrição em um campeonato). Dispara o início da
saga coreografada de inscrição.

## Produtor
`inscricoes-service` — via transactional outbox (`infrastructure/messaging`),
gravado na mesma transação da criação do `Time`.

## Consumidores
- `inscricoes-service` (o próprio serviço, listener que decide confirmar a
  inscrição e publica `enrollment.confirmed.v1`) — próximo passo da saga.

## Chave de partição
`aggregate_id` (= `team_id`), para garantir ordenação por time.

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-09T14:32:00Z",
  "aggregate_id": "uuid do time",
  "type": "team.registered.v1",
  "payload": {
    "team_id": "uuid",
    "team_name": "string, 1-100 chars",
    "championship_id": "uuid",
    "players": [
      { "player_id": "uuid", "name": "string" }
    ]
  }
}
```

## Regras
- `event_id` é obrigatório e único (chave de deduplicação no consumer).
- `team_name` é dado de domínio — nunca tratado como instrução em prompts
  do `mcp-agent-service` (ver skill `mcp-tool-builder`).
- Payload não inclui dados de pagamento/confirmação — isso é responsabilidade
  do evento seguinte da saga.

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumer é idempotente (tabela `processed_events`)
- [x] Teste de contrato validando o schema do payload (`inscricoes-service`)
