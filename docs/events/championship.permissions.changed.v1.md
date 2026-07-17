# championship.permissions.changed.v1

## Contexto
Snapshot completo de quem gerencia um campeonato (dono + admins delegados).
Publicado sempre que a gestão muda: criação do campeonato, delegação/remoção
de admin e reivindicação de torneio legado. O `partidas-service` mantém uma
projeção local (`campeonato_permissao`) e autoriza escrita em partidas sem
chamada síncrona.

Por ser **snapshot** (estado completo, não delta), o consumer substitui todas
as linhas do campeonato a cada evento — reprocessamento e reordenação leve
não corrompem a projeção.

## Produtor
`inscricoes-service` — via transactional outbox, na mesma transação da
mudança de dono/admins. Campeonato legado (sem dono) **não** emite: ausência
de projeção no consumidor significa "regra legada" (qualquer usuário
autenticado gerencia, até alguém reivindicar).

## Consumidores
- `partidas-service` — atualiza a projeção `campeonato_permissao`
  (fail-closed: conhecendo o campeonato, só dono/admin escrevem em partidas).

## Chave de partição
`aggregate_id` (= `championship_id`) — ordenação por campeonato garante que
o último snapshot é o estado vigente.

## Schema do payload

```json
{
  "event_id": "uuid",
  "occurred_at": "2026-07-16T14:32:00Z",
  "aggregate_id": "uuid do campeonato",
  "type": "championship.permissions.changed.v1",
  "payload": {
    "championship_id": "uuid",
    "owner_id": "uuid (nunca nulo — legado sem dono não emite)",
    "admin_ids": ["uuid"]
  }
}
```

## Regras
- `event_id` é obrigatório e único (chave de deduplicação no consumer).
- Janela de propagação (segundos entre a mudança e a projeção): inofensiva
  na criação — o campeonato ainda não tem partidas; num admin recém-delegado,
  a primeira ação pode receber 403 e funciona ao tentar de novo.
- Auditoria: a reivindicação de torneio legado emite este evento — visível
  no Kafka UI e no `processed_events` do partidas-service.

## Checklist (skill `kafka-event-design`)
- [x] Nome segue a convenção `<dominio>.<evento>.v<n>`
- [x] Schema documentado neste arquivo
- [x] Producer usa outbox pattern
- [x] Consumer é idempotente (tabela `processed_events` no partidas-service)
- [x] Teste de contrato validando o schema do payload (`inscricoes-service`)
