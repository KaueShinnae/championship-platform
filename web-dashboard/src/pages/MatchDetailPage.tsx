import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { fetchEnrollments, fetchMatch, fetchStandings, Match } from "../api";

const STATUS_LABEL: Record<Match["status"], string> = {
  AGENDADA: "agendada",
  EM_ANDAMENTO: "em andamento",
  FINALIZADA: "finalizada",
};

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function TeamRoster({ teamId, teamName, championshipId }: { teamId: string; teamName: string; championshipId: string }) {
  const { data: enrollments = [] } = useQuery({
    queryKey: ["enrollments", championshipId],
    queryFn: () => fetchEnrollments(championshipId),
  });

  const enrollment = enrollments.find((entry) => entry.time_id === teamId);

  return (
    <div className="panel">
      <h2>{teamName}</h2>
      {!enrollment && <p className="empty">Elenco não encontrado na inscrição.</p>}
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
  const { data } = useQuery({
    queryKey: ["standings", groupId],
    queryFn: () => fetchStandings(groupId),
  });

  const entry = data?.standings.find((standing) => standing.team_id === teamId);
  if (!entry) return null;

  return (
    <p className="meta">
      campanha no grupo: {entry.points} pts · {entry.wins}V {entry.draws}E {entry.losses}D · gols {entry.goals_for}:
      {entry.goals_against}
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

  if (isError) {
    return (
      <main>
        <div className="banner-error">Partida não encontrada.</div>
      </main>
    );
  }
  if (!match) return <main className="meta">carregando…</main>;

  return (
    <>
      <p className="breadcrumb">
        <Link to="/">← voltar ao torneio</Link>
      </p>

      <div className="panel match-hero">
        <span className={`badge status-${match.status.toLowerCase()}`}>
          {match.status === "EM_ANDAMENTO" && "● "}
          {STATUS_LABEL[match.status]}
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
            <strong>Agendada para</strong> {formatDateTime(match.scheduled_at)}
          </li>
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

      <main>
        <section className="col">
          <TeamRoster
            teamId={match.home_team.team_id}
            teamName={match.home_team.name}
            championshipId={match.championship_id}
          />
        </section>
        <section className="col">
          <TeamRoster
            teamId={match.away_team.team_id}
            teamName={match.away_team.name}
            championshipId={match.championship_id}
          />
        </section>
      </main>
    </>
  );
}
