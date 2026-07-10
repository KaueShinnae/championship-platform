# Rascunho de post para o LinkedIn (ROADMAP Semana 4)

> Ajuste o tom antes de publicar; anexe o GIF da demo (registrar resultado →
> ranking + feed atualizando sozinhos) e o print do trace no Jaeger.

---

Nas últimas semanas construí uma plataforma de campeonatos para exercitar,
de ponta a ponta, os padrões que mais aparecem em sistemas distribuídos
reais — e integrei um agente de IA por cima. 🏆

O que tem dentro:

⚙️ 3 microsserviços Spring Boot (Java 21), cada um com seu Postgres —
inscrições, partidas e ranking nunca se chamam diretamente: toda
comunicação é assíncrona via Kafka.

📬 Transactional Outbox: o evento é gravado na mesma transação do banco e
publicado por um poller — consistência sem two-phase commit.

🔁 Saga coreografada na inscrição de times e consumers idempotentes
(deduplicação por event_id) em todos os pontos de consumo.

📊 CQRS: a classificação é um read model recalculado só por eventos.
No dashboard dá para VER o fluxo: registro um placar e o feed mostra
match.finished.v1 sendo consumido e ranking.updated.v1 sendo publicado.

🤖 Agente via MCP (Model Context Protocol): pergunto "quem lidera o grupo?"
no Claude e a resposta vem das projeções — com guardrails contra prompt
injection nos nomes de times cadastrados por usuários.

🔭 Observabilidade: OpenTelemetry com trace propagado através do Kafka
(visível no Jaeger) e Langfuse registrando prompt, latência e custo de
cada geração de recap por LLM — com dataset de eval para pegar alucinação
de placar.

Stack: Java 21 · Spring Boot 3 · Kafka · Postgres · React · TypeScript ·
MCP SDK · OpenTelemetry · Jaeger · Langfuse · Docker Compose

Repo: [link]

#EventDrivenArchitecture #Kafka #SpringBoot #AIEngineering #MCP #OpenTelemetry
