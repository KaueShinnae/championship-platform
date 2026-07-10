# mcp-agent-service

Servidor MCP (TypeScript) com as tools definidas no SPEC.md §4:
`get_group_standings`, `get_last_match_result`, `generate_match_recap`.

## Status
Contrato das tools definido (schema de input, guardrails, formato de
resposta). Os endpoints que ele consome já existem desde a Semana 2:
`GET /groups/{groupId}/standings` (ranking-service, porta 8083) e
`GET /matches/{matchId}` (partidas-service, porta 8082). Falta a
instrumentação Langfuse/OTel (Semana 4) e o consumo de
`ranking.updated.v1` para cache.

## Guardrails implementados (skill `mcp-tool-builder`)
- Validação de formato de `group_id`/`match_id` (UUID) antes de qualquer query
- Dados de domínio (nome de time/jogador) sempre delimitados em blocos
  (`<match_data>...</match_data>`) antes de irem para o prompt do LLM,
  nunca interpolados soltos — protege contra prompt injection via nome
  cadastrado pelo usuário
- Resposta sempre estruturada (`{ ok, data }` ou `{ ok: false, error }`),
  nunca lança exceção crua pro cliente MCP

## Rodando local
Requer Node.js 20+.

```bash
npm install
cp ../.env.example .env   # ajuste ANTHROPIC_API_KEY etc.
npm run dev                # stdio — conecte via Claude Desktop/Code
```

## Testando
```bash
npm test
```

## Semana 3/4 (a implementar)
- Endpoints reais em `ranking-service`/`partidas-service` para as tools consumirem
- Instrumentação Langfuse em `generate_match_recap` (skill `observability-langfuse`)
- Propagação de trace OpenTelemetry desde a chamada MCP
