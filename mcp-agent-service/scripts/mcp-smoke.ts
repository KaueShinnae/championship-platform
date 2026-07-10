/**
 * Smoke test do servidor MCP de ponta a ponta: sobe o servidor via stdio
 * (como o Claude Desktop/Code faria), lista as tools e as invoca contra os
 * servicos reais (ranking-service:8083, partidas-service:8082).
 *
 * Uso: npx tsx scripts/mcp-smoke.ts <group_id> <match_id>
 */
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const [groupId, matchId] = process.argv.slice(2);
if (!groupId || !matchId) {
  console.error("uso: npx tsx scripts/mcp-smoke.ts <group_id> <match_id>");
  process.exit(1);
}

const transport = new StdioClientTransport({
  command: process.execPath,
  args: ["--import", "tsx", "src/index.ts"],
});

const client = new Client({ name: "mcp-smoke", version: "0.1.0" });
await client.connect(transport);

const { tools } = await client.listTools();
console.log("tools expostas:", tools.map((tool) => tool.name).join(", "));

async function call(name: string, args: Record<string, string>) {
  const result = await client.callTool({ name, arguments: args });
  const first = Array.isArray(result.content) ? result.content[0] : undefined;
  const text = first && first.type === "text" ? first.text : JSON.stringify(result);
  console.log(`\n=== ${name}(${JSON.stringify(args)}) isError=${result.isError ?? false}`);
  console.log(text);
  return { isError: result.isError ?? false, text };
}

let failures = 0;

// 1. caminho feliz: classificacao do grupo real
const standings = await call("get_group_standings", { group_id: groupId });
if (standings.isError || !standings.text.includes("standings")) failures++;

// 2. caminho feliz: resultado da partida real
const match = await call("get_last_match_result", { match_id: matchId });
if (match.isError || !match.text.includes("match_id")) failures++;

// 3. guardrail: input invalido nunca chega ao backend
const invalid = await call("get_group_standings", { group_id: "'; DROP TABLE ranking; --" });
if (!invalid.isError || !invalid.text.includes("group_id invalido")) failures++;

// 4. grupo inexistente vira erro estruturado, nao excecao
const missing = await call("get_group_standings", { group_id: "00000000-0000-0000-0000-000000000000" });
if (!missing.isError || !missing.text.includes("grupo nao encontrado")) failures++;

// 5. recap: com API key gera texto; sem, retorna erro estruturado claro
const recap = await call("generate_match_recap", { match_id: matchId });
if (recap.isError && !recap.text.includes("ANTHROPIC_API_KEY")) failures++;

await client.close();
console.log(failures === 0 ? "\nMCP_SMOKE_OK" : `\nMCP_SMOKE_FAILURES=${failures}`);
process.exit(failures === 0 ? 0 : 1);
