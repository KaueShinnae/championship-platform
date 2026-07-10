# enrollment.confirmed.v1

## Contexto
Publicado pelo `inscricoes-service` como último passo da saga coreografada de
inscrição, depois que `team.registered.v1` é processado e a inscrição é
validada (MVP: confirmação automática; sem pagamento real, ver SPEC.md §2).

## Produtor
`inscricoes-service` — via transactional outbox, gravado na mesma transação
que atualiza o status da `Inscricao` para `CONFIRMADA`.

## Consumidores
- Futuro: serviço de notificação (fora do escopo do MVP).
- `ranking-service` pode usar este evento para saber que um time confirmado
  existe no grupo antes de partidas começarem (a confirmar na Semana 2).

## Chave de partição
`aggregate_id` (= `enrollment_id`).

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-09T14:32:05Z",
  "aggregate_id": "uuid da inscricao",
  "type": "enrollment.confirmed.v1",
  "payload": {
    "enrollment_id": "uuid",
    "team_id": "uuid",
    "championship_id": "uuid",
    "group_id": "uuid | null",
    "confirmed_at": "2026-07-09T14:32:05Z"
  }
}
```

## Regras
- `event_id` é obrigatório e único (chave de deduplicação no consumer).
- Só é publicado depois que `team.registered.v1` foi processado com sucesso
  pelo próprio `inscricoes-service` (saga coreografada, sem orquestrador
  central no MVP — ver ROADMAP.md "Depois" para versão orquestrada).
- `group_id` pode ser `null` até que o sorteio de grupos aconteça.

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumer é idempotente (tabela `processed_events`)
- [x] Teste de contrato validando o schema do payload (`inscricoes-service`)
