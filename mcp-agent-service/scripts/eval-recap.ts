/**
 * Eval do recap de partida (skill observability-langfuse): roda o mesmo
 * caminho de geracao da tool MCP contra o dataset golden e checa
 * consistencia factual de forma deterministica (nomes, placar, e ausencia
 * de alucinacao/vazamento de prompt). Cada geracao tambem e registrada no
 * Langfuse quando as chaves estao configuradas.
 *
 * Uso: npx tsx scripts/eval-recap.ts   (requer ANTHROPIC_API_KEY)
 */
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type { MatchResult } from "../src/clients/partidasServiceClient.js";
import { config } from "../src/config.js";
import { generateRecapText } from "../src/llm/recapGenerator.js";

interface EvalExample {
  id: string;
  match: MatchResult;
  must_contain: string[];
  must_not_contain: string[];
}

if (!config.anthropicApiKey) {
  console.error("ANTHROPIC_API_KEY nao configurada — eval precisa gerar recaps reais.");
  process.exit(1);
}

const datasetPath = join(dirname(fileURLToPath(import.meta.url)), "..", "eval", "recap-dataset.json");
const dataset = JSON.parse(readFileSync(datasetPath, "utf-8")) as { examples: EvalExample[] };

let failures = 0;

for (const example of dataset.examples) {
  const recap = await generateRecapText(example.match);
  const recapLower = recap.toLowerCase();

  const missing = example.must_contain.filter((fact) => !recapLower.includes(fact.toLowerCase()));
  const forbidden = example.must_not_contain.filter((term) => recapLower.includes(term.toLowerCase()));

  const passed = missing.length === 0 && forbidden.length === 0;
  if (!passed) failures++;

  console.log(`\n=== [${passed ? "PASS" : "FAIL"}] ${example.id}`);
  console.log(recap);
  if (missing.length > 0) console.log(`  fatos ausentes: ${missing.join(" | ")}`);
  if (forbidden.length > 0) console.log(`  termos proibidos presentes: ${forbidden.join(" | ")}`);
}

console.log(`\nRESULTADO: ${dataset.examples.length - failures}/${dataset.examples.length} exemplos passaram`);
process.exit(failures === 0 ? 0 : 1);
