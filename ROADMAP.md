# ROADMAP — Plataforma de Campeonato Orientada a Eventos

## Semana 1 — Fundação
- Scaffolding dos 4 serviços (skill `spring-microservice`)
- Docker Compose com Postgres + Kafka/Redpanda subindo localmente
- Modelagem de entidades: Time, Jogador, Campeonato, Inscrição
- Definir schema dos primeiros eventos (`team.registered.v1`, `enrollment.confirmed.v1`)
- Escolher SDK MCP (Java ou TypeScript) para o `mcp-agent-service`

**Entregável**: `docker-compose up` sobe tudo, `inscricoes-service` cria time e publica evento no Kafka.

## Semana 2 — Fluxo de partidas e ranking (CQRS)
- `partidas-service`: registro de resultado + transactional outbox
- `ranking-service`: consumer de `match.finished.v1` recalculando classificação (read model)
- Consumers idempotentes (skill `kafka-event-design`)
- Teste de contrato dos eventos

**Entregável**: lançar um resultado de partida atualiza o ranking sem chamada síncrona.

## Semana 3 — Agente MCP
- `mcp-agent-service` expõe `get_group_standings`, `get_last_match_result`
- Guardrails de input e prompt injection (skill `mcp-tool-builder`)
- Testar via Claude Desktop/Code consumindo o servidor MCP local
- Primeira versão de `generate_match_recap`

**Entregável**: perguntar "quem lidera o grupo B?" via MCP retorna dado correto e atualizado.

## Semana 4 — Observabilidade e polimento
- OpenTelemetry com propagação de trace através do Kafka
- Langfuse instrumentado nas chamadas do `generate_match_recap`
- Dataset de eval para qualidade do recap
- README com diagrama de arquitetura, GIF/print de uso, e post pronto pro LinkedIn

**Entregável**: repo apresentável, com trace ponta a ponta visível e eval documentada.

## Semana 5 — Dashboard web
- `web-dashboard/` (Vite + React + TS): painel de classificação, painel de
  partidas com registro de resultado, e feed de eventos consumidos/publicados
- Endpoint de listagem de partidas no `partidas-service` e feed de eventos
  recentes no `ranking-service`
- Proxy do Vite em dev (sem CORS espalhado, sem BFF); nginx servindo o build
  no docker-compose como polimento
- Polling de 2s primeiro; SSE no `ranking-service` como evolução opcional

**Entregável**: demo em uma tela — resultado lançado na UI → ranking atualiza
ao vivo com os eventos visíveis no feed; GIF disso no README.

### Semana 5b — Área do organizador
- Endpoints de leitura no `inscricoes-service` (`GET /campeonatos`,
  `GET /campeonatos/{id}/inscricoes` com jogadores e status)
- UI do organizador: criar campeonato, inscrever time com jogadores
  (saga visível: PENDENTE → CONFIRMADA sem refresh), agendar partida
  escolhendo times confirmados

**Entregável**: fluxo completo de organizador na UI, do campeonato vazio ao
ranking populado, sem tocar em curl.

## Depois (se quiser expandir)
- Saga orquestrada explícita para o fluxo de inscrição + pagamento mock
- Segundo agente (multi-agente com LangGraph) para gerar recap + post de rede social junto
- Deploy simples (Railway/Fly.io) para demo ao vivo no link do LinkedIn
