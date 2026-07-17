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
  scheduled_at: string | null; // null = horário a definir
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

// Sessão gravada pelo auth.ts; toda chamada anexa o token quando existir
// (as leituras usam para calcular can_manage; as mutações exigem).
const SESSION_STORAGE_KEY = "championship.session";

function authHeaders(): Record<string, string> {
  try {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return {};
    const session = JSON.parse(raw) as { token?: string };
    return session.token ? { Authorization: `Bearer ${session.token}` } : {};
  } catch {
    return {};
  }
}

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url, { headers: authHeaders() });
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
  can_manage: boolean;
  is_dono: boolean;
  sem_dono: boolean; // torneio legado, criado antes das contas
  aprovacao_inscricoes: boolean; // capitães aguardam aprovação (true) ou entram direto
  created_at: string;
}

// ---- contas e sessão (inscricoes-service /auth) ----

export interface SessionUser {
  id: string;
  nome: string;
  email: string;
}

export interface SessionResponse {
  usuario: SessionUser;
  token: string;
}

export function registerAccount(nome: string, email: string, senha: string): Promise<SessionResponse> {
  return postJson<SessionResponse>("/api/inscricoes/auth/register", { nome, email, senha });
}

export function loginAccount(email: string, senha: string): Promise<SessionResponse> {
  return postJson<SessionResponse>("/api/inscricoes/auth/login", { email, senha });
}

export function fetchAdmins(championshipId: string): Promise<SessionUser[]> {
  return getJson<SessionUser[]>(`/api/inscricoes/campeonatos/${championshipId}/admins`);
}

export function addAdmin(championshipId: string, email: string): Promise<SessionUser> {
  return postJson<SessionUser>(`/api/inscricoes/campeonatos/${championshipId}/admins`, { email });
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
  status: "PENDENTE" | "CONFIRMADA" | "RECUSADA";
  capitao_usuario_id: string | null; // preenchido = auto-inscrição de capitão
  confirmed_at: string | null;
}

/** Resposta da inscrição recém-criada (organizador ou capitão). */
export interface EnrollmentCreated {
  inscricao_id: string;
  time_id: string;
  time_nome: string;
  campeonato_id: string;
  status: "PENDENTE" | "CONFIRMADA" | "RECUSADA";
  capitao_usuario_id: string | null;
  created_at: string;
}

async function postJson<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
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

export function createChampionship(
  nome: string,
  formato: ChampionshipFormat,
  aprovacaoInscricoes: boolean,
): Promise<Championship> {
  return postJson<Championship>("/api/inscricoes/campeonatos", {
    nome,
    formato,
    aprovacao_inscricoes: aprovacaoInscricoes,
  });
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

/** Slot ocupado do bracket (inclui byes — time que avança direto). */
export interface BracketSlot {
  round: number;
  slot: number;
  team_id: string;
  team_name: string;
}

export function fetchBracketSlots(championshipId: string): Promise<BracketSlot[]> {
  return getJson<BracketSlot[]>(`/api/partidas/matches/draw/${championshipId}/slots`);
}

/** Descarta o sorteio no partidas-service (reabrir inscrições). */
export async function descartarConfrontos(championshipId: string): Promise<void> {
  const response = await fetch(`/api/partidas/matches/draw/${championshipId}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!response.ok && response.status !== 404) {
    const payload = (await response.json().catch(() => ({}))) as { error?: string };
    throw new Error(payload.error ?? `descartar sorteio falhou (${response.status})`);
  }
}

export function fetchEnrollments(championshipId: string): Promise<Enrollment[]> {
  return getJson<Enrollment[]>(`/api/inscricoes/campeonatos/${championshipId}/times`);
}

export function enrollTeam(championshipId: string, nome: string, jogadores: string[]): Promise<EnrollmentCreated> {
  return postJson<EnrollmentCreated>(`/api/inscricoes/campeonatos/${championshipId}/times`, { nome, jogadores });
}

// ---- auto-inscrição pelo capitão (aprovação do organizador) ----

export function aprovarInscricao(championshipId: string, inscricaoId: string): Promise<EnrollmentCreated> {
  return postJson<EnrollmentCreated>(
    `/api/inscricoes/campeonatos/${championshipId}/times/${inscricaoId}/aprovar`, {});
}

export function recusarInscricao(championshipId: string, inscricaoId: string): Promise<EnrollmentCreated> {
  return postJson<EnrollmentCreated>(
    `/api/inscricoes/campeonatos/${championshipId}/times/${inscricaoId}/recusar`, {});
}

/** Gestor remove time (inscrições abertas) ou capitão cancela a própria pendente. */
export function removerInscricao(championshipId: string, inscricaoId: string): Promise<void> {
  return deleteJson(`/api/inscricoes/campeonatos/${championshipId}/times/${inscricaoId}`);
}

// ---- reuso de elenco ("Meus times") — sempre snapshot/cópia ----

export interface ReusableTeam {
  nome: string;
  jogadores: string[];
}

export function fetchMeusTimes(): Promise<ReusableTeam[]> {
  return getJson<ReusableTeam[]>("/api/inscricoes/meus-times");
}

export function removeAdmin(championshipId: string, adminId: string): Promise<void> {
  return deleteJson(`/api/inscricoes/campeonatos/${championshipId}/admins/${adminId}`);
}

async function deleteJson(url: string): Promise<void> {
  const response = await fetch(url, { method: "DELETE", headers: authHeaders() });
  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as { error?: string };
    throw new Error(payload.error ?? `${url} respondeu ${response.status}`);
  }
}

/** Remarca data/horário de uma partida ainda não iniciada. */
export function reagendarPartida(matchId: string, scheduledAt: string): Promise<Match> {
  return postJson<Match>(`/api/partidas/matches/${matchId}/schedule`, { scheduled_at: scheduledAt });
}

export function startMatch(matchId: string): Promise<Match> {
  return postJson<Match>(`/api/partidas/matches/${matchId}/start`, {});
}

/** Placar parcial ao vivo — contagem do organizador durante a partida. */
export function atualizarPlacarParcial(matchId: string, homeScore: number, awayScore: number): Promise<Match> {
  return postJson<Match>(`/api/partidas/matches/${matchId}/score`, {
    home_score: homeScore,
    away_score: awayScore,
  });
}

export async function registerResult(matchId: string, homeScore: number, awayScore: number): Promise<Match> {
  const response = await fetch(`/api/partidas/matches/${matchId}/result`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({ home_score: homeScore, away_score: awayScore }),
  });
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as { error?: string };
    throw new Error(body.error ?? `registro de resultado falhou (${response.status})`);
  }
  return (await response.json()) as Match;
}
