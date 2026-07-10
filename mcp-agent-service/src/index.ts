import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { generateMatchRecap, generateMatchRecapInputSchema } from "./tools/generateMatchRecap.js";
import { getGroupStandings, getGroupStandingsInputSchema } from "./tools/getGroupStandings.js";
import { getLastMatchResult, getLastMatchResultInputSchema } from "./tools/getLastMatchResult.js";
import { runTool, toMcpContent } from "./tools/types.js";

const server = new McpServer({
  name: "mcp-agent-service",
  version: "0.1.0",
});

server.tool(
  "get_group_standings",
  "Retorna a classificacao atual de um grupo do campeonato (projecao de ranking).",
  getGroupStandingsInputSchema,
  async (input) => toMcpContent(await runTool(() => getGroupStandings(input))),
);

server.tool(
  "get_last_match_result",
  "Retorna o resultado de uma partida especifica.",
  getLastMatchResultInputSchema,
  async (input) => toMcpContent(await runTool(() => getLastMatchResult(input))),
);

server.tool(
  "generate_match_recap",
  "Gera um resumo textual (recap) de uma partida a partir do resultado registrado.",
  generateMatchRecapInputSchema,
  async (input) => toMcpContent(await runTool(() => generateMatchRecap(input))),
);

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("mcp-agent-service conectado via stdio");
}

main().catch((error) => {
  console.error("falha ao iniciar mcp-agent-service", error);
  process.exit(1);
});
