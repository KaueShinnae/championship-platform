# ranking-service

Status: Semana 2 (ROADMAP.md) implementada — read model CQRS de
classificação, populado exclusivamente por eventos.

## Como funciona (CQRS)
1. Consome `match.finished.v1` (consumer idempotente via `processed_events`)
2. Atualiza a projeção `group_standing` (uma linha por time/grupo):
   vitória = 3 pts, empate = 1 pt, derrota = 0; acumula saldo de gols
3. Publica `ranking.updated.v1` via outbox na mesma transação

Nenhuma escrita chega aqui por API — só por evento. Partidas sem `group_id`
são ignoradas para classificação (mas marcadas como processadas).

## Endpoints
| Método | Rota | Descrição |
|---|---|---|
| `GET /groups/{groupId}/standings` | Classificação do grupo | consumido pela tool MCP `get_group_standings` |

Ordenação: pontos > saldo de gols > gols pró > nome do time.

## Eventos
- Consome: `match.finished.v1` — ver [docs/events/match.finished.v1.md](../docs/events/match.finished.v1.md)
- Publica: `ranking.updated.v1` — ver [docs/events/ranking.updated.v1.md](../docs/events/ranking.updated.v1.md)

## Rodando local
```bash
docker-compose up -d       # a partir da raiz do repo
./mvnw spring-boot:run     # porta 8083
```

## Testes
```bash
./mvnw test
```
Inclui teste de contrato que desserializa um `match.finished.v1` literal
(exatamente como o `partidas-service` publica) — quebra se o contrato divergir.
