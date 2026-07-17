import { Link } from "react-router-dom";
import { BracketSlot, ChampionshipFormat, Match } from "../api";

// Regras espelhadas do backend (Chaveamento.java) só para desenhar os slots
// ainda não criados: nº de grupos por faixa e tamanho do bracket.
function proximaPotenciaDe2(n: number): number {
  let potencia = 1;
  while (potencia < n) potencia *= 2;
  return potencia;
}

function numeroDeGrupos(times: number): number {
  for (const grupos of [8, 4, 2]) {
    if (Math.floor(times / grupos) >= 3) return grupos;
  }
  return 2;
}

export function totalRoundsDoFormato(formato: ChampionshipFormat, times: number): number {
  if (formato === "PLAYOFFS") return Math.log2(proximaPotenciaDe2(Math.max(times, 2)));
  return Math.log2(2 * numeroDeGrupos(Math.max(times, 6)));
}

export function nomeDaRodada(round: number, totalRounds: number): string {
  const distanciaDaFinal = totalRounds - round;
  if (distanciaDaFinal === 0) return "Final";
  if (distanciaDaFinal === 1) return "Semifinais";
  if (distanciaDaFinal === 2) return "Quartas de final";
  if (distanciaDaFinal === 3) return "Oitavas de final";
  return `Rodada ${round}`;
}

function BracketMatch({ match, myTeamId }: { match: Match; myTeamId?: string | null }) {
  const finished = match.status === "FINALIZADA";
  const homeWins = finished && (match.home_team.score ?? 0) > (match.away_team.score ?? 0);
  const awayWins = finished && (match.away_team.score ?? 0) > (match.home_team.score ?? 0);

  return (
    <Link to={`/partidas/${match.match_id}`} className="bracket-match">
      <span
        className={`bracket-team ${homeWins ? "winner" : ""} ${myTeamId === match.home_team.team_id ? "my-team" : ""}`}
      >
        {match.home_team.name}
        <span className="bracket-score">{match.home_team.score ?? "–"}</span>
      </span>
      <span
        className={`bracket-team ${awayWins ? "winner" : ""} ${myTeamId === match.away_team.team_id ? "my-team" : ""}`}
      >
        {match.away_team.name}
        <span className="bracket-score">{match.away_team.score ?? "–"}</span>
      </span>
      {match.status === "EM_ANDAMENTO" && <span className="bracket-live">● ao vivo</span>}
    </Link>
  );
}

/**
 * Bracket do mata-mata: uma coluna por rodada; posições ainda não definidas
 * aparecem como placeholder (bye na 1ª rodada dos playoffs diretos; "aguardando"
 * nas demais). A coluna final mostra o campeão quando a final termina.
 */
export function Bracket({
  matches,
  formato,
  teamCount,
  slots = [],
  myTeamId,
}: {
  matches: Match[];
  formato: ChampionshipFormat;
  teamCount: number;
  slots?: BracketSlot[];
  myTeamId?: string | null;
}) {
  const playoffMatches = matches.filter((match) => match.stage === "PLAYOFF");
  const totalRounds = totalRoundsDoFormato(formato, teamCount);

  const byRoundAndPos = new Map<string, Match>();
  for (const match of playoffMatches) {
    if (match.round !== null && match.bracket_pos !== null) {
      byRoundAndPos.set(`${match.round}-${match.bracket_pos}`, match);
    }
  }

  // times que já ocupam um slot (byes e vencedores aguardando adversário)
  const slotTeams = new Map<string, string>();
  for (const slot of slots) {
    slotTeams.set(`${slot.round}-${slot.slot}`, slot.team_name);
  }

  const finalMatch = byRoundAndPos.get(`${totalRounds}-0`);
  const champion =
    finalMatch && finalMatch.status === "FINALIZADA"
      ? (finalMatch.home_team.score ?? 0) > (finalMatch.away_team.score ?? 0)
        ? finalMatch.home_team.name
        : finalMatch.away_team.name
      : null;

  // placeholder informativo: quem já está no confronto (aguardando adversário),
  // quem avançou direto por bye, ou de onde a vaga virá
  const placeholderDoConfronto = (round: number, pos: number): string => {
    const jaNoConfronto = slotTeams.get(`${round}-${2 * pos}`) ?? slotTeams.get(`${round}-${2 * pos + 1}`);
    if (jaNoConfronto) return `${jaNoConfronto} — aguarda adversário`;
    const bye = slotTeams.get(`${round + 1}-${pos}`);
    if (bye && round === 1) return `${bye} — bye, avança direto`;
    if (formato === "GRUPOS_PLAYOFFS" && round === 1) return "definido pela fase de grupos";
    return "aguardando definição";
  };

  const rounds = Array.from({ length: totalRounds }, (_, index) => index + 1);

  return (
    <div className="bracket-scroll">
      <div className="bracket">
        {rounds.map((round) => {
          const expected = Math.pow(2, totalRounds - round);
          return (
            <div key={round} className="bracket-round">
              <h3>{nomeDaRodada(round, totalRounds)}</h3>
              {Array.from({ length: expected }, (_, pos) => {
                const match = byRoundAndPos.get(`${round}-${pos}`);
                return match ? (
                  <BracketMatch key={pos} match={match} myTeamId={myTeamId} />
                ) : (
                  <div key={pos} className="bracket-match placeholder">
                    <span className="meta">{placeholderDoConfronto(round, pos)}</span>
                  </div>
                );
              })}
            </div>
          );
        })}
        <div className="bracket-round">
          <h3>Campeão</h3>
          <div className={`bracket-match champion ${champion ? "" : "placeholder"}`}>
            {champion ? <span>🏆 {champion}</span> : <span className="meta">a definir</span>}
          </div>
        </div>
      </div>
    </div>
  );
}
