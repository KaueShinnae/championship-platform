// Cliente fino sobre as APIs dos servicos (via proxy do Vite — ver vite.config.ts).
// Os payloads sao snake_case, espelhando o contrato JSON dos servicos Spring.

export interface TeamView {
  team_id: string;
  name: string;
  score: number | null;
}

export interface Match {
  match_id: string;
  championship_id: string;
  group_id: string | null;
  home_team: TeamView;
  away_team: TeamView;
  status: "AGENDADA" | "EM_ANDAMENTO" | "FINALIZADA";
  scheduled_at: string;
  started_at: string | null;
  played_at: string | null;
  stage: "GRUPOS" | "PLAYOFF" | null;
  round: number | null;
  bracket_pos: number | null;
}

export interface StandingEntry {
  team_id: string;
  team_name: string;
  points: number;
  wins: number;
  draws: number;
  losses: number;
  goals_for: number;
  goals_against: number;
}

export interface GroupStandings {
  group_id: string;
  updated_at: string;
  standings: StandingEntry[];
}

export interface FeedEntry {
  kind: "CONSUMED" | "PUBLISHED";
  type: string;
  event_id: string;
  aggregate_id: string;
  trace_id: string | null;
  payload: string | null;
  at: string;
}

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`${url} respondeu ${response.status}`);
  }
  return (await response.json()) as T;
}

export function fetchMatches(): Promise<Match[]> {
  return getJson<Match[]>("/api/partidas/matches");
}

export function fetchMatch(matchId: string): Promise<Match> {
  return getJson<Match>(`/api/partidas/matches/${matchId}`);
}

export async function fetchStandings(groupId: string): Promise<GroupStandings | null> {
  const response = await fetch(`/api/ranking/groups/${groupId}/standings`);
  if (response.status === 404) return null; // grupo ainda sem resultado processado
  if (!response.ok) throw new Error(`standings respondeu ${response.status}`);
  return (await response.json()) as GroupStandings;
}

export function fetchEventFeed(): Promise<FeedEntry[]> {
  return getJson<FeedEntry[]>("/api/ranking/events/recent?limit=50");
}

// ---- area do organizador (inscricoes-service + agendamento) ----

export type ChampionshipFormat = "GRUPOS_PLAYOFFS" | "PLAYOFFS" | "PONTOS_CORRIDOS";

export interface Championship {
  id: string;
  nome: string;
  status: "ABERTO" | "SORTEADO" | "EM_ANDAMENTO" | "ENCERRADO";
  formato: ChampionshipFormat;
  campeao_nome: string | null;
  created_at: string;
}

export interface PlayerView {
  id: string;
  nome: string;
}

export interface Enrollment {
  inscricao_id: string;
  time_id: string;
  time_nome: string;
  jogadores: PlayerView[];
  status: "PENDENTE" | "CONFIRMADA";
  confirmed_at: string | null;
}

async function postJson<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as { error?: string };
    throw new Error(payload.error ?? `${url} respondeu ${response.status}`);
  }
  return (await response.json()) as T;
}

export function fetchChampionships(): Promise<Championship[]> {
  return getJson<Championship[]>("/api/inscricoes/campeonatos");
}

export function createChampionship(nome: string, formato: ChampionshipFormat): Promise<Championship> {
  return postJson<Championship>("/api/inscricoes/campeonatos", { nome, formato });
}

// ---- ciclo de vida do torneio (sorteio -> início -> campeão) ----

export function sortearCampeonato(championshipId: string): Promise<Championship> {
  return postJson<Championship>(`/api/inscricoes/campeonatos/${championshipId}/sortear`, {});
}

export function reabrirCampeonato(championshipId: string): Promise<Championship> {
  return postJson<Championship>(`/api/inscricoes/campeonatos/${championshipId}/reabrir`, {});
}

export function iniciarCampeonato(championshipId: string): Promise<Championship> {
  return postJson<Championship>(`/api/inscricoes/campeonatos/${championshipId}/iniciar`, {});
}

/** Sorteia os times e gera todos os confrontos no partidas-service. */
export function gerarConfrontos(
  championshipId: string,
  formato: ChampionshipFormat,
  teams: { team_id: string; name: string }[],
): Promise<Match[]> {
  return postJson<Match[]>("/api/partidas/matches/generate", {
    championship_id: championshipId,
    formato,
    teams,
  });
}

/** Descarta o sorteio no partidas-service (reabrir inscrições). */
export async function descartarConfrontos(championshipId: string): Promise<void> {
  const response = await fetch(`/api/partidas/matches/draw/${championshipId}`, { method: "DELETE" });
  if (!response.ok && response.status !== 404) {
    const payload = (await response.json().catch(() => ({}))) as { error?: string };
    throw new Error(payload.error ?? `descartar sorteio falhou (${response.status})`);
  }
}

export function fetchEnrollments(championshipId: string): Promise<Enrollment[]> {
  return getJson<Enrollment[]>(`/api/inscricoes/campeonatos/${championshipId}/times`);
}

export function enrollTeam(championshipId: string, nome: string, jogadores: string[]): Promise<unknown> {
  return postJson(`/api/inscricoes/campeonatos/${championshipId}/times`, { nome, jogadores });
}

export function startMatch(matchId: string): Promise<Match> {
  return postJson<Match>(`/api/partidas/matches/${matchId}/start`, {});
}

export async function registerResult(matchId: string, homeScore: number, awayScore: number): Promise<Match> {
  const response = await fetch(`/api/partidas/matches/${matchId}/result`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ home_score: homeScore, away_score: awayScore }),
  });
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as { error?: string };
    throw new Error(body.error ?? `registro de resultado falhou (${response.status})`);
  }
  return (await response.json()) as Match;
}
