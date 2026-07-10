import { z } from "zod";
import { fetchMatchResult } from "../clients/partidasServiceClient.js";
import { isValidUuid } from "../guardrails/sanitize.js";
import { fail, ok, ToolResult } from "./types.js";
import type { MatchResult } from "../clients/partidasServiceClient.js";

export const getLastMatchResultInputSchema = {
  match_id: z.string().describe("UUID da partida"),
};

export async function getLastMatchResult(input: { match_id: string }): Promise<ToolResult<MatchResult>> {
  if (!isValidUuid(input.match_id)) {
    return fail("match_id invalido: esperado UUID");
  }

  const match = await fetchMatchResult(input.match_id);
  if (!match) {
    return fail(`partida nao encontrada: ${input.match_id}`);
  }

  return ok(match);
}
