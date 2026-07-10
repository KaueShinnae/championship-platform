# SPEC — Plataforma de Campeonato Orientada a Eventos com Agente MCP

## 1. Visão geral

Plataforma de gestão de campeonatos/torneios construída em arquitetura de
microsserviços orientada a eventos, com um agente de IA exposto via MCP capaz
de responder perguntas sobre o estado do campeonato em tempo real e gerar
recaps automáticos de partidas.

**Objetivo do projeto**: portfólio técnico demonstrando event-driven
architecture, mensageria (Kafka), microsserviços (Spring Boot), CQRS/saga,
observabilidade (OpenTelemetry + Langfuse) e agentes de IA (MCP + RAG).

## 2. Escopo (MVP)

### Dentro do escopo
- Cadastro de campeonato, times e jogadores
- Inscrição de times em um campeonato (com confirmação assíncrona)
- Ciclo de vida da partida: agendada (com horário de início marcado) →
  em andamento (iniciada pelo organizador) → finalizada (com placar);
  registro de resultado só é aceito com a partida em andamento
- Cálculo de classificação/ranking por grupo (recalculado via eventos)
- Consulta de estado do campeonato via agente MCP (ex: "quem lidera o grupo B?")
- Geração de recap textual de uma partida via agente de IA
- Observabilidade de ponta a ponta (traces + métricas de LLM)
- Aplicação web (`web-dashboard`) multi-página com dois perfis:
  - **Público (leitura)**: página do torneio (classificação ao vivo, partidas,
    feed de eventos) e página de detalhe de partida (status, horários, times
    e jogadores inscritos) — nenhuma ação de escrita visível
  - **Organizador (escrita)**: área restrita por chave de acesso onde se
    cria campeonato, inscreve time com jogadores (saga visível:
    PENDENTE → CONFIRMADA), agenda partida (mandante/visitante escolhidos
    entre times já inscritos — nunca reinscrever), inicia partida e registra
    resultado
- Validação de unicidade: um time não pode ser inscrito duas vezes no mesmo
  campeonato (nome comparado sem diferenciar maiúsculas); rejeição clara na
  API para não poluir a contagem de pontos da classificação

### Fora do escopo (por enquanto)
- Pagamentos reais (mock/stub apenas)
- Autenticação multi-tenant completa (JWT simples é suficiente)
- Autenticação real de usuário final: a área do organizador usa uma chave
  de acesso simples no cliente (suficiente para demo local; JWT nos serviços
  fica como expansão futura)
- Estatísticas por jogador (gols, faltas) — exigiria `match.finished.v2`
  com eventos por jogador; candidata a expansão no ROADMAP

## 3. Domínio e eventos

### Bounded contexts / serviços
| Serviço | Responsabilidade | Banco |
|---|---|---|
| `inscricoes-service` | Times, jogadores, inscrição em campeonato | Postgres |
| `partidas-service` | Registro de partidas e resultados | Postgres |
| `ranking-service` | Projeção de classificação (read model) | Postgres |
| `mcp-agent-service` | Exposição de tools MCP + geração de recap | — (consome projeções) |
| `web-dashboard` | UI de demonstração (SPA React) — camada de apresentação, **não** é bounded context | — (consome APIs via proxy) |

### Eventos de domínio (tópicos Kafka)
- `team.registered.v1`
- `enrollment.confirmed.v1`
- `match.scheduled.v1`
- `match.started.v1` → partida entrou em andamento
- `match.finished.v1` → dispara recálculo de ranking
- `ranking.updated.v1` → consumido pelo mcp-agent-service para atualizar cache/projeção

### Padrões arquiteturais a aplicar
- **Transactional Outbox** em `partidas-service` para publicar `match.finished.v1` de forma consistente com a escrita no banco
- **CQRS**: `ranking-service` mantém um read model dedicado, populado só por eventos
- **Saga coreografada** para o fluxo de inscrição (times → confirmação → notificação)
- **Idempotência de consumers** (chave de deduplicação por `event_id`)

## 4. Agente MCP

O `mcp-agent-service` expõe um servidor MCP com tools:
- `get_group_standings(group_id)` — lê a projeção de ranking
- `get_last_match_result(match_id)` — lê dados da partida
- `generate_match_recap(match_id)` — gera texto de resumo via LLM a partir dos dados da partida

Guardrails: validar `group_id`/`match_id` de entrada, nunca expor dados de
outros campeonatos sem filtro, sanitizar prompt injection em dados vindos de
nomes de times/jogadores (aplicar o mesmo padrão que você já usou no Repo
Triage Agent).

## 5. Requisitos não funcionais
- Cada serviço deve ter tracing distribuído via OpenTelemetry (correlação end-to-end desde a chamada MCP até a escrita no Postgres)
- Chamadas ao LLM instrumentadas com Langfuse (custo, latência, avaliação de qualidade do recap)
- Testes: unitários por serviço + teste de contrato de eventos (schema do payload Kafka)
- Docker Compose sobe: Postgres, Kafka (ou Redpanda), os 3 serviços + mcp-agent-service

## 6. Critérios de aceite do MVP
- [ ] Um time se inscreve → evento percorre a saga → aparece como confirmado
- [ ] Um resultado de partida é lançado → ranking do grupo é atualizado sem chamada síncrona direta
- [ ] Uma pergunta ao agente MCP sobre "quem lidera o grupo X" retorna dado correto e atualizado
- [ ] Um recap de partida é gerado e é factualmente consistente com o resultado registrado
- [ ] Trace completo de uma requisição é visível no backend de observability escolhido
- [ ] Registrar resultado pela UI → classificação atualiza na tela sem refresh
      e sem chamada síncrona entre serviços, com os eventos visíveis no feed
