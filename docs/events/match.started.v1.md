# match.started.v1

## Contexto
Publicado pelo `partidas-service` quando o organizador inicia uma partida
agendada (transição AGENDADA → EM_ANDAMENTO). Completa o ciclo de vida da
partida no SPEC.md §2.

## Produtor
`partidas-service` — via transactional outbox, gravado na mesma transação
que muda o status da `Partida` para `EM_ANDAMENTO`.

## Consumidores
- Nenhum consumidor obrigatório no MVP. Candidatos futuros: notificações
  ("a partida começou"), painel ao vivo com placar parcial.

## Chave de partição
`aggregate_id` (= `match_id`).

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-16T12:00:00Z",
  "aggregate_id": "uuid da partida",
  "type": "match.started.v1",
  "payload": {
    "match_id": "uuid",
    "championship_id": "uuid",
    "group_id": "uuid | null",
    "started_at": "2026-07-16T12:00:00Z"
  }
}
```

## Regras
- Só pode ser publicado a partir de uma partida `AGENDADA` (iniciar duas
  vezes é rejeitado com erro de estado).
- A partir deste evento, o registro de resultado passa a ser aceito;
  antes dele, é rejeitado.

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumer é idempotente — N/A, sem consumidor no MVP
- [x] Teste de contrato validando o schema do payload (`partidas-service`)
