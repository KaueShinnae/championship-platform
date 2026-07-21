import { config } from "../config.js";

export interface MatchResult {
  match_id: string;
  championship_id: string;
  home_team: { team_id: string; name: string; score: number };
  away_team: { team_id: string; name: string; score: number };
  played_at: string;
  status: string;
}

export async function fetchMatchResult(matchId: string): Promise<MatchResult | null> {
  const response = await fetch(`${config.partidasServiceUrl}/matches/${matchId}`);
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`partidas-service respondeu ${response.status}`);
  }
  return (await response.json()) as MatchResult;
}
