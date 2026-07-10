import Anthropic from "@anthropic-ai/sdk";
import { Langfuse } from "langfuse";
import type { MatchResult } from "../clients/partidasServiceClient.js";
import { config } from "../config.js";
import { DATA_NOT_INSTRUCTION_PREAMBLE, wrapAsData } from "../guardrails/sanitize.js";

const MODEL = "claude-sonnet-5";

// Langfuse e opcional: sem chaves, a geracao funciona sem observabilidade
// (skill observability-langfuse: toda chamada LLM registrada quando possivel).
const langfuse =
  config.langfuse.publicKey && config.langfuse.secretKey
    ? new Langfuse({
        publicKey: config.langfuse.publicKey,
        secretKey: config.langfuse.secretKey,
        baseUrl: config.langfuse.host,
      })
    : null;

const SYSTEM_PROMPT =
  "Voce escreve recaps curtos (3-4 frases) de partidas de campeonato, em portugues, " +
  `tom neutro e factual. ${DATA_NOT_INSTRUCTION_PREAMBLE}`;

/**
 * Gera o texto do recap a partir dos dados da partida. Usado pela tool MCP
 * e pelo script de eval (mesmo caminho de prompt = eval representativa).
 */
export async function generateRecapText(match: MatchResult): Promise<string> {
  const client = new Anthropic({ apiKey: config.anthropicApiKey });

  // Dados de dominio delimitados, nunca tratados como instrucao
  // (skill mcp-tool-builder — protege contra prompt injection via nomes).
  const userMessage = `Gere um recap da partida abaixo.\n\n${wrapAsData("match_data", JSON.stringify(match))}`;

  const trace = langfuse?.trace({
    name: "generate_match_recap",
    input: { match_id: match.match_id },
    metadata: { championship_id: match.championship_id },
  });
  const generation = trace?.generation({
    name: "recap-llm-call",
    model: MODEL,
    input: [
      { role: "system", content: SYSTEM_PROMPT },
      { role: "user", content: userMessage },
    ],
  });

  const startedAt = Date.now();
  try {
    const response = await client.messages.create({
      model: MODEL,
      max_tokens: 300,
      system: SYSTEM_PROMPT,
      messages: [{ role: "user", content: userMessage }],
    });

    const textBlock = response.content.find((block) => block.type === "text");
    if (!textBlock || textBlock.type !== "text") {
      throw new Error("resposta do modelo nao continha texto");
    }

    generation?.end({
      output: textBlock.text,
      usage: {
        input: response.usage.input_tokens,
        output: response.usage.output_tokens,
      },
      metadata: { latency_ms: Date.now() - startedAt },
    });
    trace?.update({ output: textBlock.text });

    return textBlock.text;
  } catch (error) {
    generation?.end({ level: "ERROR", statusMessage: error instanceof Error ? error.message : String(error) });
    throw error;
  } finally {
    await langfuse?.flushAsync().catch(() => undefined);
  }
}
