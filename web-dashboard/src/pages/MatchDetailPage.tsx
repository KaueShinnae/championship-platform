import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { fetchMatch } from "../api";
import { MATCH_STATUS_LABEL } from "../components/MatchCard";
import { useChampionships, useEnrollments, useStandings } from "../data";
import { formatDateTime } from "../format";
import { Skeleton } from "../ui/Skeleton";

function TeamRoster({ teamId, teamName, championshipId }: { teamId: string; teamName: string; championshipId: string }) {
  const { data: enrollments = [], isLoading } = useEnrollments(championshipId);
  const enrollment = enrollments.find((entry) => entry.time_id === teamId);

  return (
    <div className="panel">
      <h2>{teamName}</h2>
      {isLoading && <Skeleton lines={4} />}
      {!isLoading && !enrollment && <p className="empty">Elenco não encontrado na inscrição.</p>}
      {enrollment && (
        <>
          <ul className="roster">
            {enrollment.jogadores.map((jogador) => (
              <li key={jogador.id}>{jogador.nome}</li>
            ))}
          </ul>
          <p className="meta">
            inscrição {enrollment.status === "CONFIRMADA" ? "confirmada" : "pendente"}
            {enrollment.confirmed_at && ` em ${formatDateTime(enrollment.confirmed_at)}`}
          </p>
        </>
      )}
    </div>
  );
}

function TeamCampaign({ groupId, teamId }: { groupId: string; teamId: string }) {
  const { data } = useStandings(groupId);
  const entry = data?.standings.find((standing) => standing.team_id === teamId);
  if (!entry) return null;

  return (
    <p className="meta">
      {entry.team_name}: {entry.pontos} pts · {entry.vitorias}V {entry.empates}E {entry.derrotas}D · saldo{" "}
      {entry.saldo >= 0 ? `+${entry.saldo}` : entry.saldo}
    </p>
  );
}

export function MatchDetailPage() {
  const { matchId } = useParams<{ matchId: string }>();
  const { data: match, isError } = useQuery({
    queryKey: ["match", matchId],
    queryFn: () => fetchMatch(matchId!),
    enabled: matchId !== undefined,
  });
  const { data: championships = [] } = useChampionships();

  if (isError) {
    return (
      <>
        <p className="breadcrumb">
          <Link to="/torneios">← Torneios</Link>
        </p>
        <div className="banner-error">Partida não encontrada.</div>
      </>
    );
  }

  if (!match) {
    return (
      <div className="panel">
        <Skeleton lines={4} />
      </div>
    );
  }

  const championship = championships.find((entry) => entry.id === match.championship_id);

  return (
    <>
      <p className="breadcrumb">
        <Link to={`/torneios/${match.championship_id}?tab=partidas`}>← Partidas</Link>
        {championship && (
          <>
            {" · "}
            <Link to={`/torneios/${championship.id}`}>{championship.nome}</Link>
          </>
        )}
      </p>

      <div className="panel match-hero">
        <span className={`badge status-${match.status.toLowerCase()}`}>
          {match.status === "EM_ANDAMENTO" && "● "}
          {MATCH_STATUS_LABEL[match.status]}
        </span>
        <div className="teams hero">
          <span>{match.home_team.name}</span>
          <span className="score">
            {match.status === "FINALIZADA" ? `${match.home_team.score} x ${match.away_team.score}` : "vs"}
          </span>
          <span>{match.away_team.name}</span>
        </div>
        <ul className="timeline">
          <li>
            <strong>Agendada para</strong>{" "}
            {match.scheduled_at ? formatDateTime(match.scheduled_at) : "horário a definir"}
          </li>
          {match.local && (
            <li>
              <strong>Local</strong> 📍 {match.local}
            </li>
          )}
          {match.started_at && (
            <li>
              <strong>Começou</strong> {formatDateTime(match.started_at)}
            </li>
          )}
          {match.played_at && (
            <li>
              <strong>Encerrada</strong> {formatDateTime(match.played_at)}
            </li>
          )}
        </ul>
        {match.group_id && (
          <>
            <TeamCampaign groupId={match.group_id} teamId={match.home_team.team_id} />
            <TeamCampaign groupId={match.group_id} teamId={match.away_team.team_id} />
          </>
        )}
      </div>

      <div className="two-col">
        <TeamRoster
          teamId={match.home_team.team_id}
          teamName={match.home_team.name}
          championshipId={match.championship_id}
        />
        <TeamRoster
          teamId={match.away_team.team_id}
          teamName={match.away_team.name}
          championshipId={match.championship_id}
        />
      </div>
    </>
  );
}
