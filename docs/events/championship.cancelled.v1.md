# championship.cancelled.v1

## Contexto
O organizador cancelou o torneio (housekeeping: criado por engano, de teste,
ou o evento não vai mais acontecer). É um estado **terminal** — diferente de
"reabrir inscrições", que é reversível. O campeonato passa a `CANCELADO` no
`inscricoes-service`, e os outros serviços purgam tudo que guardam dele.

Cancelar um torneio já `ENCERRADO` (com campeão) não é permitido — o histórico
do campeão é preservado.

## Produtor
`inscricoes-service` — via transactional outbox, na mesma transação em que o
campeonato vira `CANCELADO`. Só o **dono** cancela (ação destrutiva).

## Consumidores
- `partidas-service` — purga as partidas, o chaveamento (`torneio_chaveamento`)
  e os slots do bracket (`chave_slot`) do campeonato.
- `ranking-service` — purga a classificação projetada (`group_standing`) do
  campeonato (Monitoramento / tool MCP deixam de exibi-lo).

Cascade **sempre via Kafka** — nunca há chamada síncrona entre serviços
(CLAUDE.md "o que não fazer").

## Chave de partição
`aggregate_id` (= `championship_id`).

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-18T14:32:00Z",
  "aggregate_id": "uuid do campeonato",
  "type": "championship.cancelled.v1",
  "payload": {
    "championship_id": "uuid"
  }
}
```

## Regras
- `event_id` é obrigatório e único (chave de deduplicação nos consumers).
- Idempotente: reentrega não faz nada além da primeira purga (a purga é
  naturalmente idempotente — deletar o que não existe mais é no-op).
- Janela de propagação: por segundos após o cancelamento, o torneio pode ainda
  aparecer no Monitoramento/bracket até o consumer processar — inofensivo, o
  campeonato já está marcado `CANCELADO` na origem.

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumers são idempotentes (tabela `processed_events` em partidas e ranking)
- [x] Teste de contrato validando o schema do payload (`inscricoes-service`)
