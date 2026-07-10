# CLAUDE.md — contexto para o Claude Code

Este arquivo é lido automaticamente pelo Claude Code no início de cada sessão
neste repositório. Mantenha atualizado conforme o projeto evolui.

## Sobre o projeto
Plataforma de campeonato orientada a eventos com agente MCP integrado.
Ver `SPEC.md` para o detalhamento funcional e `ROADMAP.md` para o plano por semana.

## Stack
- Java 21 + Spring Boot 3.x
- Kafka (ou Redpanda em dev) para mensageria
- Postgres por serviço (um schema por bounded context, sem banco compartilhado)
- MCP SDK (Java ou TypeScript, a definir na semana 1) para o `mcp-agent-service`
- OpenTelemetry para tracing, Langfuse para observabilidade de chamadas LLM
- Docker Compose para orquestração local

## Convenções do repositório
- Estrutura: um diretório por serviço na raiz (`inscricoes-service/`, `partidas-service/`, `ranking-service/`, `mcp-agent-service/`, `web-dashboard/`)
- Eventos versionados: todo payload Kafka tem sufixo `.v1`, schema descrito em `docs/events/`
- Nomenclatura de commits: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`)
- Todo novo endpoint precisa de teste (unitário no mínimo; contrato quando envolver eventos)
- Não subir segredos/credenciais — usar `.env.example` como referência

## Como rodar localmente
```bash
npm run dev         # sobe TUDO: infra + 3 servicos + dashboard (idempotente)
npm run dev:build   # idem, recompilando os jars antes
npm run down        # desliga tudo
# Git Bash/WSL: bash scripts/dev-up.sh [--build]
```
Dashboard em http://localhost:5173; serviços em 8081-8083; Kafka UI em 8090.

## Prioridades ao gerar código
1. Consistência do domínio antes de otimização
2. Idempotência em qualquer consumer Kafka (ver padrão em `docs/idempotency.md` quando existir)
3. Sempre instrumentar novo código com trace/span do OpenTelemetry
4. Preferir eventos pequenos e específicos a eventos genéricos "catch-all"

## O que NÃO fazer
- Não criar chamadas síncronas entre `partidas-service` e `ranking-service` — a comunicação é sempre via Kafka
- Não commitar código sem passar `./mvnw test`
- Não expor tools MCP sem validação de input

## Skills disponíveis neste projeto
Ver `.claude/skills/`:
- `kafka-event-design` — como desenhar tópicos, schemas e garantir idempotência
- `mcp-tool-builder` — como estruturar tools MCP com guardrails
- `spring-microservice` — scaffolding padrão de um serviço novo
- `observability-langfuse` — como instrumentar OpenTelemetry + Langfuse
