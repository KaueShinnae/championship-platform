# partidas-service

Status: Semana 2 (ROADMAP.md) implementada — registro de partidas e
resultados com transactional outbox.

## Endpoints
| Método | Rota | Descrição |
|---|---|---|
| `POST /matches` | Agenda uma partida | publica `match.scheduled.v1` |
| `POST /matches/{matchId}/result` | Registra o resultado | publica `match.finished.v1` |
| `GET /matches/{matchId}` | Consulta uma partida | usado pela tool MCP `get_last_match_result` |

## Eventos publicados
- `match.scheduled.v1` — ver [docs/events/match.scheduled.v1.md](../docs/events/match.scheduled.v1.md)
- `match.finished.v1` — ver [docs/events/match.finished.v1.md](../docs/events/match.finished.v1.md);
  dispara o recálculo de ranking no `ranking-service` (sem chamada síncrona)

Ambos via transactional outbox (tabela `outbox_event` + poller), gravados na
mesma transação da escrita de domínio (skill `kafka-event-design`).

## Decisão de modelagem
Nomes de time são denormalizados no payload de criação da partida
(event-carried state transfer simplificado) — este serviço nunca chama o
`inscricoes-service` de forma síncrona.

## Rodando local
```bash
docker-compose up -d       # a partir da raiz do repo
./mvnw spring-boot:run     # porta 8082
```

## Testes
```bash
./mvnw test
```
