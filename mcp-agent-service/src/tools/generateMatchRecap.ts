import { z } from "zod";
import { fetchMatchResult } from "../clients/partidasServiceClient.js";
import { config } from "../config.js";
import { isValidUuid } from "../guardrails/sanitize.js";
import { generateRecapText } from "../llm/recapGenerator.js";
import { fail, ok, ToolResult } from "./types.js";

export const generateMatchRecapInputSchema = {
  match_id: z.string().describe("UUID da partida"),
};

interface MatchRecap {
  match_id: string;
  recap: string;
}

export async function generateMatchRecap(input: { match_id: string }): Promise<ToolResult<MatchRecap>> {
  if (!isValidUuid(input.match_id)) {
    return fail("match_id invalido: esperado UUID");
  }
  if (!config.anthropicApiKey) {
    return fail("ANTHROPIC_API_KEY nao configurada");
  }

  const match = await fetchMatchResult(input.match_id);
  if (!match) {
    return fail(`partida nao encontrada: ${input.match_id}`);
  }

  const recap = await generateRecapText(match);
  return ok({ match_id: input.match_id, recap });
}
