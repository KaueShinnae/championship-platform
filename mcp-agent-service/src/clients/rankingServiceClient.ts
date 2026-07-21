import { config } from "../config.js";

export interface StandingEntry {
  team_id: string;
  team_name: string;
  points: number;
  wins: number;
  draws: number;
  losses: number;
}

export interface GroupStandings {
  group_id: string;
  updated_at: string;
  standings: StandingEntry[];
}

export async function fetchGroupStandings(groupId: string): Promise<GroupStandings | null> {
  const response = await fetch(`${config.rankingServiceUrl}/groups/${groupId}/standings`);
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`ranking-service respondeu ${response.status}`);
  }
  return (await response.json()) as GroupStandings;
}
