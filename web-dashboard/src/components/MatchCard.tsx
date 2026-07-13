import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Match, registerResult, startMatch } from "../api";
import { formatShortDateTime } from "../format";
import { useOrganizer } from "../organizer";
import { useToast } from "../ui/toast";

export const MATCH_STATUS_LABEL: Record<Match["status"], string> = {
  AGENDADA: "Agendada",
  EM_ANDAMENTO: "Ao vivo",
  FINALIZADA: "Encerrada",
};

function StartButton({ match }: { match: Match }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const mutation = useMutation({
    mutationFn: () => startMatch(match.match_id),
    onSuccess: () => {
      toast("success", `Partida ${match.home_team.name} x ${match.away_team.name} iniciada`);
      queryClient.invalidateQueries({ queryKey: ["matches"] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  return (
    <div className="match-actions">
      <button type="button" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
        {mutation.isPending ? "Iniciando…" : "▶ Iniciar partida"}
      </button>
    </div>
  );
}

function ResultForm({ match }: { match: Match }) {
  const [homeScore, setHomeScore] = useState("");
  const [awayScore, setAwayScore] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();

  const mutation = useMutation({
    mutationFn: () => registerResult(match.match_id, Number(homeScore), Number(awayScore)),
    onSuccess: () => {
      // partidas invalidam na hora; classificacao e atividade chegam pelo
      // polling quando os eventos forem processados (fluxo assincrono)
      toast("success", "Resultado registrado — a classificação atualiza em instantes");
      queryClient.invalidateQueries({ queryKey: ["matches"] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const valid = homeScore !== "" && awayScore !== "" && Number(homeScore) >= 0 && Number(awayScore) >= 0;

  return (
    <form
      className="match-actions"
      onSubmit={(event) => {
        event.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <input
        type="number"
        min={0}
        value={homeScore}
        onChange={(event) => setHomeScore(event.target.value)}
        aria-label={`gols ${match.home_team.name}`}
      />
      <span className="muted">x</span>
      <input
        type="number"
        min={0}
        value={awayScore}
        onChange={(event) => setAwayScore(event.target.value)}
        aria-label={`gols ${match.away_team.name}`}
      />
      <button type="submit" disabled={!valid || mutation.isPending}>
        {mutation.isPending ? "Enviando…" : "Encerrar com placar"}
      </button>
    </form>
  );
}

export function MatchCard({ match, championshipName }: { match: Match; championshipName?: string }) {
  const organizer = useOrganizer();

  return (
    <li className={`match-card ${match.status.toLowerCase()}`}>
      <div className="match-header">
        <span className={`badge status-${match.status.toLowerCase()}`}>
          {match.status === "EM_ANDAMENTO" && "● "}
          {MATCH_STATUS_LABEL[match.status]}
        </span>
        <span className="meta">
          {championshipName && <>{championshipName} · </>}
          {match.status === "AGENDADA" && `início ${formatShortDateTime(match.scheduled_at)}`}
          {match.status === "EM_ANDAMENTO" && match.started_at && `começou ${formatShortDateTime(match.started_at)}`}
          {match.status === "FINALIZADA" && match.played_at && `encerrada ${formatShortDateTime(match.played_at)}`}
        </span>
      </div>
      <Link to={`/partidas/${match.match_id}`} className="teams-link">
        <div className="teams">
          <span>{match.home_team.name}</span>
          <span className="score">
            {match.status === "FINALIZADA" ? `${match.home_team.score} x ${match.away_team.score}` : "vs"}
          </span>
          <span>{match.away_team.name}</span>
        </div>
      </Link>
      {organizer && match.status === "AGENDADA" && <StartButton match={match} />}
      {organizer && match.status === "EM_ANDAMENTO" && <ResultForm match={match} />}
    </li>
  );
}

export function MatchList({
  matches,
  championshipNames,
  emptyMessage = "Nenhuma partida ainda.",
}: {
  matches: Match[];
  championshipNames?: Map<string, string>;
  emptyMessage?: string;
}) {
  if (matches.length === 0) {
    return <p className="empty">{emptyMessage}</p>;
  }
  return (
    <ul className="matches">
      {matches.map((match) => (
        <MatchCard key={match.match_id} match={match} championshipName={championshipNames?.get(match.championship_id)} />
      ))}
    </ul>
  );
}
