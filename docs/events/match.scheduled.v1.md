# match.scheduled.v1

## Contexto
Publicado pelo `partidas-service` quando uma partida é registrada/agendada
entre dois times de um campeonato, antes do resultado existir. Também é
**reemitido quando a partida é remarcada** (`POST /matches/{id}/schedule`),
com novo `event_id` e o `scheduled_at` atualizado — para um mesmo
`aggregate_id`, vale o evento mais recente.

## Produtor
`partidas-service` — via transactional outbox, gravado na mesma transação
da criação da `Partida`.

## Consumidores
- Nenhum consumidor obrigatório no MVP (evento existe para completude do
  domínio e para consumidores futuros, ex: notificações). `ranking-service`
  não precisa dele — só reage a `match.finished.v1`.

## Decisão de modelagem: nomes de time denormalizados
`partidas-service` não faz chamada síncrona a `inscricoes-service` para
resolver nome de time (ver CLAUDE.md "o que não fazer" — comunicação
sempre via Kafka/assíncrona). Para o MVP, quem cria a partida informa
`team_id` e `team_name` no payload da requisição (event-carried state
transfer simplificado). `partidas-service` nunca é dono desse dado — só
o repassa.

## Chave de partição
`aggregate_id` (= `match_id`).

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-16T10:00:00Z",
  "aggregate_id": "uuid da partida",
  "type": "match.scheduled.v1",
  "payload": {
    "match_id": "uuid",
    "championship_id": "uuid",
    "group_id": "uuid | null",
    "home_team": { "team_id": "uuid", "name": "string" },
    "away_team": { "team_id": "uuid", "name": "string" },
    "scheduled_at": "2026-07-16T10:00:00Z | null"
  }
}
```

`scheduled_at` é `null` quando o horário ainda está "a definir" (partidas
geradas pelo sorteio nascem sem horário; o organizador define depois e o
evento é reemitido com o valor preenchido).

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumer é idempotente — N/A, sem consumidor no MVP
- [x] Teste de contrato validando o schema do payload (`partidas-service`)
