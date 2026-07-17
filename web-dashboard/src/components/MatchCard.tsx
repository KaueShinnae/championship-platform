import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { atualizarPlacarParcial, Match, reagendarPartida, registerResult, startMatch } from "../api";
import { useCanManage } from "../data";
import { formatShortDateTime } from "../format";
import { useMyTeam } from "../ui/myteam";
import { useToast } from "../ui/toast";

export const MATCH_STATUS_LABEL: Record<Match["status"], string> = {
  AGENDADA: "Agendada",
  EM_ANDAMENTO: "Ao vivo",
  FINALIZADA: "Encerrada",
};

function useRefreshMatch(match: Match) {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["matches"] });
    queryClient.invalidateQueries({ queryKey: ["match", match.match_id] });
  };
}

/**
 * Formulário de placar manual. Em partida eliminatória, empate é bloqueado
 * aqui mesmo — não deixamos o erro estourar só no backend.
 */
function ScoreForm({
  match,
  submitLabel,
  pendingLabel,
  onSubmit,
  isPending,
  onCancel,
}: {
  match: Match;
  submitLabel: string;
  pendingLabel: string;
  onSubmit: (homeScore: number, awayScore: number) => void;
  isPending: boolean;
  onCancel?: () => void;
}) {
  const [homeScore, setHomeScore] = useState(match.home_team.score?.toString() ?? "");
  const [awayScore, setAwayScore] = useState(match.away_team.score?.toString() ?? "");

  const filled = homeScore !== "" && awayScore !== "" && Number(homeScore) >= 0 && Number(awayScore) >= 0;
  const empateEmPlayoff = filled && match.stage === "PLAYOFF" && Number(homeScore) === Number(awayScore);
  const valid = filled && !empateEmPlayoff;

  return (
    <form
      className="match-actions"
      onSubmit={(event) => {
        event.preventDefault();
        if (valid) onSubmit(Number(homeScore), Number(awayScore));
      }}
    >
      <input
        type="number"
        min={0}
        value={homeScore}
        onChange={(event) => setHomeScore(event.target.value)}
        aria-label={`pontos ${match.home_team.name}`}
      />
      <span className="muted">x</span>
      <input
        type="number"
        min={0}
        value={awayScore}
        onChange={(event) => setAwayScore(event.target.value)}
        aria-label={`pontos ${match.away_team.name}`}
      />
      <button type="submit" disabled={!valid || isPending}>
        {isPending ? pendingLabel : submitLabel}
      </button>
      {onCancel && (
        <button type="button" className="ghost" onClick={onCancel}>
          Cancelar
        </button>
      )}
      {empateEmPlayoff && (
        <span className="error">
          eliminatória precisa de vencedor — registre o placar já com a decisão (prorrogação/pênaltis)
        </span>
      )}
    </form>
  );
}

function ScheduledActions({ match }: { match: Match }) {
  const [mode, setMode] = useState<"none" | "schedule" | "result">("none");
  const [kickoff, setKickoff] = useState("");
  const refresh = useRefreshMatch(match);
  const toast = useToast();

  const start = useMutation({
    mutationFn: () => startMatch(match.match_id),
    onSuccess: () => {
      toast("success", `Partida ${match.home_team.name} x ${match.away_team.name} iniciada`);
      refresh();
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const reschedule = useMutation({
    // datetime-local vem sem fuso; Date interpreta como hora local e
    // toISOString converte para UTC, que e o que a API espera
    mutationFn: () => reagendarPartida(match.match_id, new Date(kickoff).toISOString()),
    onSuccess: () => {
      toast("success", "Horário definido");
      setMode("none");
      setKickoff("");
      refresh();
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  // atalho de apuração: inicia e encerra na mesma ação (o domínio continua
  // passando por AGENDADA -> EM_ANDAMENTO -> FINALIZADA, com os dois eventos)
  const finalize = useMutation({
    mutationFn: async ({ homeScore, awayScore }: { homeScore: number; awayScore: number }) => {
      await startMatch(match.match_id);
      return registerResult(match.match_id, homeScore, awayScore);
    },
    onSuccess: () => {
      toast("success", "Placar final registrado — a classificação atualiza em instantes");
      setMode("none");
      refresh();
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  if (mode === "schedule") {
    return (
      <form
        className="match-actions"
        onSubmit={(event) => {
          event.preventDefault();
          if (kickoff !== "") reschedule.mutate();
        }}
      >
        <input
          type="datetime-local"
          value={kickoff}
          onChange={(event) => setKickoff(event.target.value)}
          aria-label="data e horário da partida"
        />
        <button type="submit" disabled={kickoff === "" || reschedule.isPending}>
          {reschedule.isPending ? "Salvando…" : "Salvar"}
        </button>
        <button type="button" className="ghost" onClick={() => setMode("none")}>
          Cancelar
        </button>
      </form>
    );
  }

  if (mode === "result") {
    return (
      <ScoreForm
        match={match}
        submitLabel="Registrar placar final"
        pendingLabel="Registrando…"
        onSubmit={(homeScore, awayScore) => finalize.mutate({ homeScore, awayScore })}
        isPending={finalize.isPending}
        onCancel={() => setMode("none")}
      />
    );
  }

  return (
    <div className="match-actions">
      <button type="button" onClick={() => start.mutate()} disabled={start.isPending}>
        {start.isPending ? "Iniciando…" : "▶ Iniciar partida"}
      </button>
      <button type="button" className="ghost" onClick={() => setMode("result")}>
        🏁 Placar final
      </button>
      <button type="button" className="ghost" onClick={() => setMode("schedule")}>
        🕒 {match.scheduled_at ? "Remarcar" : "Definir horário"}
      </button>
    </div>
  );
}

/**
 * Contagem ao vivo: pontos genéricos (vale para qualquer esporte), ajustados
 * com +/− durante a partida. Encerrar usa o placar corrente; "outro placar"
 * abre a digitação manual.
 */
function LiveActions({ match }: { match: Match }) {
  const [manual, setManual] = useState(false);
  const refresh = useRefreshMatch(match);
  const toast = useToast();

  const home = match.home_team.score ?? 0;
  const away = match.away_team.score ?? 0;

  const updateScore = useMutation({
    mutationFn: ({ h, a }: { h: number; a: number }) => atualizarPlacarParcial(match.match_id, h, a),
    onSuccess: refresh,
    onError: (error) => toast("error", (error as Error).message),
  });

  const finish = useMutation({
    mutationFn: ({ h, a }: { h: number; a: number }) => registerResult(match.match_id, h, a),
    onSuccess: () => {
      toast("success", "Resultado registrado — a classificação atualiza em instantes");
      refresh();
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  if (manual) {
    return (
      <ScoreForm
        match={match}
        submitLabel="Encerrar com placar"
        pendingLabel="Enviando…"
        onSubmit={(homeScore, awayScore) => finish.mutate({ h: homeScore, a: awayScore })}
        isPending={finish.isPending}
        onCancel={() => setManual(false)}
      />
    );
  }

  const empateEmPlayoff = match.stage === "PLAYOFF" && home === away;

  return (
    <>
      <div className="match-actions live-counter">
        <span className="counter-side">
          <button
            type="button"
            className="ghost"
            aria-label={`tirar ponto de ${match.home_team.name}`}
            disabled={home === 0 || updateScore.isPending}
            onClick={() => updateScore.mutate({ h: home - 1, a: away })}
          >
            −
          </button>
          <button
            type="button"
            aria-label={`ponto para ${match.home_team.name}`}
            disabled={updateScore.isPending}
            onClick={() => updateScore.mutate({ h: home + 1, a: away })}
          >
            +1 {match.home_team.name}
          </button>
        </span>
        <span className="counter-side">
          <button
            type="button"
            aria-label={`ponto para ${match.away_team.name}`}
            disabled={updateScore.isPending}
            onClick={() => updateScore.mutate({ h: home, a: away + 1 })}
          >
            +1 {match.away_team.name}
          </button>
          <button
            type="button"
            className="ghost"
            aria-label={`tirar ponto de ${match.away_team.name}`}
            disabled={away === 0 || updateScore.isPending}
            onClick={() => updateScore.mutate({ h: home, a: away - 1 })}
          >
            −
          </button>
        </span>
      </div>
      <div className="match-actions">
        <button
          type="button"
          disabled={finish.isPending || empateEmPlayoff}
          onClick={() => finish.mutate({ h: home, a: away })}
        >
          {finish.isPending ? "Encerrando…" : `Encerrar partida (${home} x ${away})`}
        </button>
        <button type="button" className="ghost" onClick={() => setManual(true)}>
          ✎ outro placar
        </button>
        {empateEmPlayoff && <span className="hint">eliminatória não termina empatada — desempate antes de encerrar</span>}
      </div>
    </>
  );
}

export function MatchCard({
  match,
  championshipName,
  phaseLabel,
}: {
  match: Match;
  championshipName?: string;
  phaseLabel?: string;
}) {
  const canManage = useCanManage(match.championship_id);
  const myTeam = useMyTeam(match.championship_id);
  const isMyTeamMatch =
    myTeam !== null && (match.home_team.team_id === myTeam.teamId || match.away_team.team_id === myTeam.teamId);

  const showScore = match.status !== "AGENDADA" && match.home_team.score !== null && match.away_team.score !== null;

  return (
    <li className={`match-card ${match.status.toLowerCase()} ${isMyTeamMatch ? "my-team-match" : ""}`}>
      <div className="match-header">
        <span className={`badge status-${match.status.toLowerCase()}`}>
          {match.status === "EM_ANDAMENTO" && "● "}
          {MATCH_STATUS_LABEL[match.status]}
        </span>
        <span className="meta">
          {phaseLabel && <>{phaseLabel} · </>}
          {championshipName && <>{championshipName} · </>}
          {match.status === "AGENDADA" &&
            (match.scheduled_at ? `início ${formatShortDateTime(match.scheduled_at)}` : "horário a definir")}
          {match.status === "EM_ANDAMENTO" && match.started_at && `começou ${formatShortDateTime(match.started_at)}`}
          {match.status === "FINALIZADA" && match.played_at && `encerrada ${formatShortDateTime(match.played_at)}`}
        </span>
      </div>
      <Link to={`/partidas/${match.match_id}`} className="teams-link">
        <div className="teams">
          <span className={myTeam?.teamId === match.home_team.team_id ? "my-team" : ""}>{match.home_team.name}</span>
          <span className="score">
            {showScore ? `${match.home_team.score} x ${match.away_team.score}` : "vs"}
          </span>
          <span className={myTeam?.teamId === match.away_team.team_id ? "my-team" : ""}>{match.away_team.name}</span>
        </div>
      </Link>
      {canManage && match.status === "AGENDADA" && <ScheduledActions match={match} />}
      {canManage && match.status === "EM_ANDAMENTO" && <LiveActions match={match} />}
    </li>
  );
}

export function MatchList({
  matches,
  championshipNames,
  phaseLabels,
  emptyMessage = "Nenhuma partida ainda.",
}: {
  matches: Match[];
  championshipNames?: Map<string, string>;
  phaseLabels?: Map<string, string>;
  emptyMessage?: string;
}) {
  if (matches.length === 0) {
    return <p className="empty">{emptyMessage}</p>;
  }
  return (
    <ul className="matches">
      {matches.map((match) => (
        <MatchCard
          key={match.match_id}
          match={match}
          championshipName={championshipNames?.get(match.championship_id)}
          phaseLabel={phaseLabels?.get(match.match_id)}
        />
      ))}
    </ul>
  );
}
