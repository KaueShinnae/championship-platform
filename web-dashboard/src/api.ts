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
  local: string | null; // sede/quadra/mesa/tabuleiro; null = a definir
  started_at: string | null;
  played_at: string | null;
  stage: "GRUPOS" | "PLAYOFF" | null;
  round: number | null;
  bracket_pos: number | null;
  wo: boolean;
  wo_motivo: string | null;
  terceiro_lugar: boolean;
}

// Termos neutros (torneios em geral, sem "gols"): pontos de classificação (P),
// pró/contra/saldo do placar agregado. desempate = critério que separou do
// time acima no empate.
export interface StandingEntry {
  team_id: string;
  team_name: string;
  pontos: number;
  vitorias: number;
  empates: number;
  derrotas: number;
  pro: number;
  contra: number;
  saldo: number;
  desempate: string | null;
}

export interface GroupStandings {
  group_id: string;
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

// Classificação vem do partidas-service: fonte ÚNICA (a mesma tabela que
// decide o avanço), com o desempate defensável (confronto direto etc.).
export async function fetchStandings(groupId: string): Promise<GroupStandings | null> {
  const response = await fetch(`/api/partidas/matches/standings/${groupId}`, { headers: authHeaders() });
  if (response.status === 404) return null; // grupo ainda sem partidas geradas
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
  status: "ABERTO" | "SORTEADO" | "EM_ANDAMENTO" | "ENCERRADO" | "CANCELADO";
  formato: ChampionshipFormat;
  campeao_nome: string | null;
  can_manage: boolean;
  is_dono: boolean;
  sem_dono: boolean; // torneio legado, criado antes das contas
  aprovacao_inscricoes: boolean; // capitães aguardam aprovação (true) ou entram direto
  min_integrantes: number | null; // limites de integrantes por equipe (null = sem limite)
  max_integrantes: number | null;
  disputa_terceiro: boolean; // disputa de 3º lugar no mata-mata
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

export interface CreateChampionshipOptions {
  aprovacaoInscricoes: boolean;
  minIntegrantes: number | null;
  maxIntegrantes: number | null;
  disputaTerceiro: boolean;
}

export function createChampionship(
  nome: string,
  formato: ChampionshipFormat,
  options: CreateChampionshipOptions,
): Promise<Championship> {
  return postJson<Championship>("/api/inscricoes/campeonatos", {
    nome,
    formato,
    aprovacao_inscricoes: options.aprovacaoInscricoes,
    min_integrantes: options.minIntegrantes,
    max_integrantes: options.maxIntegrantes,
    disputa_terceiro: options.disputaTerceiro,
  });
}

export function editarCampeonato(
  championshipId: string,
  nome: string,
  options: CreateChampionshipOptions,
): Promise<Championship> {
  return putJson<Championship>(`/api/inscricoes/campeonatos/${championshipId}`, {
    nome,
    aprovacao_inscricoes: options.aprovacaoInscricoes,
    min_integrantes: options.minIntegrantes,
    max_integrantes: options.maxIntegrantes,
    disputa_terceiro: options.disputaTerceiro,
  });
}

export function cancelarCampeonato(championshipId: string): Promise<Championship> {
  return postJson<Championship>(`/api/inscricoes/campeonatos/${championshipId}/cancelar`, {});
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

export function gerarConfrontos(
  championshipId: string,
  formato: ChampionshipFormat,
  teams: { team_id: string; name: string }[],
  disputaTerceiro = false,
): Promise<Match[]> {
  return postJson<Match[]>("/api/partidas/matches/generate", {
    championship_id: championshipId,
    formato,
    teams,
    disputa_terceiro: disputaTerceiro,
  });
}

export function desistirTime(championshipId: string, teamId: string): Promise<{ walkovers: number }> {
  return postJson<{ walkovers: number }>("/api/partidas/matches/withdraw", {
    championship_id: championshipId,
    team_id: teamId,
  });
}

export interface BracketSlot {
  round: number;
  slot: number;
  team_id: string;
  team_name: string;
}

export function fetchBracketSlots(championshipId: string): Promise<BracketSlot[]> {
  return getJson<BracketSlot[]>(`/api/partidas/matches/draw/${championshipId}/slots`);
}

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

export function reagendarPartida(matchId: string, scheduledAt: string, local?: string | null): Promise<Match> {
  return postJson<Match>(`/api/partidas/matches/${matchId}/schedule`, {
    scheduled_at: scheduledAt,
    local: local ?? null,
  });
}

export function startMatch(matchId: string): Promise<Match> {
  return postJson<Match>(`/api/partidas/matches/${matchId}/start`, {});
}

export function atualizarPlacarParcial(matchId: string, homeScore: number, awayScore: number): Promise<Match> {
  return postJson<Match>(`/api/partidas/matches/${matchId}/score`, {
    home_score: homeScore,
    away_score: awayScore,
  });
}

export async function registerResult(
  matchId: string,
  homeScore: number,
  awayScore: number,
  wo?: { motivo: string },
): Promise<Match> {
  const response = await fetch(`/api/partidas/matches/${matchId}/result`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify({
      home_score: homeScore,
      away_score: awayScore,
      wo: wo !== undefined,
      wo_motivo: wo?.motivo ?? null,
    }),
  });
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as { error?: string };
    throw new Error(body.error ?? `registro de resultado falhou (${response.status})`);
  }
  return (await response.json()) as Match;
}

export function corrigirResultado(matchId: string, homeScore: number, awayScore: number): Promise<Match> {
  return postJson<Match>(`/api/partidas/matches/${matchId}/correct`, {
    home_score: homeScore,
    away_score: awayScore,
  });
}

export function reagendarEmLote(championshipId: string, shiftMinutes: number): Promise<{ rescheduled: number }> {
  return postJson<{ rescheduled: number }>("/api/partidas/matches/reschedule-batch", {
    championship_id: championshipId,
    shift_minutes: shiftMinutes,
  });
}

export interface ScheduleConflict {
  partida_a: string;
  partida_b: string;
  tipo: "TIME" | "LOCAL";
  team_id: string | null; // preenchido no conflito de TIME
  team_name: string | null;
  local: string | null; // preenchido no conflito de LOCAL
  scheduled_at: string;
}

export function fetchConflitos(championshipId: string): Promise<ScheduleConflict[]> {
  return getJson<ScheduleConflict[]>(`/api/partidas/matches/conflicts/${championshipId}`);
}

// ---- log de gestão legível (merge de inscricoes + partidas) ----

export interface ManagementLogEntry {
  id: string;
  actor_id: string;
  actor_nome: string;
  acao: string;
  descricao: string;
  created_at: string;
}

export async function fetchManagementLog(championshipId: string): Promise<ManagementLogEntry[]> {
  const [inscricoes, partidas] = await Promise.all([
    getJson<ManagementLogEntry[]>(`/api/inscricoes/campeonatos/${championshipId}/gestao-log`),
    getJson<ManagementLogEntry[]>(`/api/partidas/matches/management-log/${championshipId}`),
  ]);
  return [...inscricoes, ...partidas].sort((a, b) => b.created_at.localeCompare(a.created_at));
}

export function editarTime(
  championshipId: string,
  inscricaoId: string,
  nome: string,
  jogadores: string[],
): Promise<unknown> {
  return putJson(`/api/inscricoes/campeonatos/${championshipId}/times/${inscricaoId}`, { nome, jogadores });
}

async function putJson<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as { error?: string };
    throw new Error(payload.error ?? `${url} respondeu ${response.status}`);
  }
  return (await response.json()) as T;
}
