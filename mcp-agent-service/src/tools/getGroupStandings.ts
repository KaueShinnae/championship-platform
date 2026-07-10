import { z } from "zod";
import { fetchGroupStandings } from "../clients/rankingServiceClient.js";
import { isValidUuid } from "../guardrails/sanitize.js";
import { fail, ok, ToolResult } from "./types.js";
import type { GroupStandings } from "../clients/rankingServiceClient.js";

export const getGroupStandingsInputSchema = {
  group_id: z.string().describe("UUID do grupo do campeonato"),
};

export async function getGroupStandings(input: { group_id: string }): Promise<ToolResult<GroupStandings>> {
  if (!isValidUuid(input.group_id)) {
    return fail("group_id invalido: esperado UUID");
  }

  const standings = await fetchGroupStandings(input.group_id);
  if (!standings) {
    return fail(`grupo nao encontrado: ${input.group_id}`);
  }

  // Escopo restrito ao grupo pedido — a query no ranking-service ja filtra
  // por group_id, nunca retornando dados de outro campeonato (skill mcp-tool-builder).
  return ok(standings);
}
