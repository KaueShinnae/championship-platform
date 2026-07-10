import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Match, registerResult, startMatch } from "../api";

const STATUS_LABEL: Record<Match["status"], string> = {
  AGENDADA: "agendada",
  EM_ANDAMENTO: "em andamento",
  FINALIZADA: "finalizada",
};

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function StartButton({ match }: { match: Match }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => startMatch(match.match_id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["matches"] }),
  });

  return (
    <div className="result-form">
      <button type="button" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
        {mutation.isPending ? "..." : "▶ Iniciar partida"}
      </button>
      {mutation.isError && <span className="error">{(mutation.error as Error).message}</span>}
    </div>
  );
}

function ResultForm({ match }: { match: Match }) {
  const [homeScore, setHomeScore] = useState("");
  const [awayScore, setAwayScore] = useState("");
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => registerResult(match.match_id, Number(homeScore), Number(awayScore)),
    onSuccess: () => {
      // invalida partidas na hora; classificacao e feed atualizam sozinhos
      // pelo polling quando os eventos forem processados (fluxo assincrono)
      queryClient.invalidateQueries({ queryKey: ["matches"] });
    },
  });

  const valid = homeScore !== "" && awayScore !== "" && Number(homeScore) >= 0 && Number(awayScore) >= 0;

  return (
    <form
      className="result-form"
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
      <span>x</span>
      <input
        type="number"
        min={0}
        value={awayScore}
        onChange={(event) => setAwayScore(event.target.value)}
        aria-label={`gols ${match.away_team.name}`}
      />
      <button type="submit" disabled={!valid || mutation.isPending}>
        {mutation.isPending ? "..." : "Encerrar c/ placar"}
      </button>
      {mutation.isError && <span className="error">{(mutation.error as Error).message}</span>}
    </form>
  );
}

export function MatchesPanel({ matches, readOnly = false }: { matches: Match[]; readOnly?: boolean }) {
  return (
    <div className="panel">
      <h2>Partidas</h2>
      {matches.length === 0 && (
        <p className="empty">
          Nenhuma partida ainda{readOnly ? "" : " — use o painel Organizador para agendar"}.
        </p>
      )}
      <ul className="matches">
        {matches.map((match) => (
          <li key={match.match_id} className={match.status.toLowerCase()}>
            <div className="match-header">
              <span className={`badge status-${match.status.toLowerCase()}`}>
                {match.status === "EM_ANDAMENTO" && "● "}
                {STATUS_LABEL[match.status]}
              </span>
              <span className="meta">
                {match.status === "AGENDADA" && `início ${formatDateTime(match.scheduled_at)}`}
                {match.status === "EM_ANDAMENTO" && match.started_at && `começou ${formatDateTime(match.started_at)}`}
                {match.status === "FINALIZADA" && match.played_at && `encerrada ${formatDateTime(match.played_at)}`}
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
            {!readOnly && match.status === "AGENDADA" && <StartButton match={match} />}
            {!readOnly && match.status === "EM_ANDAMENTO" && <ResultForm match={match} />}
          </li>
        ))}
      </ul>
      {readOnly && <p className="meta">Clique numa partida para ver detalhes e elencos.</p>}
    </div>
  );
}
