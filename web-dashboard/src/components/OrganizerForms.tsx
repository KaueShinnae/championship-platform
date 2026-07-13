import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { createChampionship, enrollTeam, scheduleMatch } from "../api";
import { useToast } from "../ui/toast";

export function CreateChampionshipForm() {
  const [nome, setNome] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();

  const mutation = useMutation({
    mutationFn: () => createChampionship(nome.trim()),
    onSuccess: (championship) => {
      toast("success", `Torneio "${championship.nome}" criado`);
      setNome("");
      queryClient.invalidateQueries({ queryKey: ["championships"] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  return (
    <form
      className="org-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (nome.trim()) mutation.mutate();
      }}
    >
      <h3>Novo torneio</h3>
      <div className="row">
        <input
          placeholder="Nome do torneio (ex.: Copa da Firma 2026)"
          value={nome}
          maxLength={100}
          onChange={(event) => setNome(event.target.value)}
        />
        <button type="submit" disabled={!nome.trim() || mutation.isPending}>
          {mutation.isPending ? "Criando…" : "Criar"}
        </button>
      </div>
    </form>
  );
}

export function EnrollTeamForm({ championshipId }: { championshipId: string }) {
  const [nome, setNome] = useState("");
  const [jogadores, setJogadores] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();

  const parsedPlayers = jogadores.split(",").map((jogador) => jogador.trim()).filter(Boolean);

  const mutation = useMutation({
    mutationFn: () => enrollTeam(championshipId, nome.trim(), parsedPlayers),
    onSuccess: () => {
      // a confirmacao e assincrona (saga): o status PENDENTE -> CONFIRMADA
      // aparece sozinho na lista quando os eventos forem processados
      toast("success", `Inscrição de "${nome.trim()}" enviada — aguardando confirmação`);
      setNome("");
      setJogadores("");
      queryClient.invalidateQueries({ queryKey: ["enrollments", championshipId] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const valid = nome.trim() !== "" && parsedPlayers.length > 0;

  return (
    <form
      className="org-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <h3>Inscrever time</h3>
      <div className="row">
        <input
          placeholder="Nome do time"
          value={nome}
          maxLength={100}
          onChange={(event) => setNome(event.target.value)}
        />
      </div>
      <div className="row">
        <input
          placeholder="Jogadores separados por vírgula"
          value={jogadores}
          onChange={(event) => setJogadores(event.target.value)}
        />
        <button type="submit" disabled={!valid || mutation.isPending}>
          {mutation.isPending ? "Enviando…" : "Inscrever"}
        </button>
      </div>
      {parsedPlayers.length > 0 && (
        <span className="hint">
          {parsedPlayers.length} jogador{parsedPlayers.length > 1 ? "es" : ""}: {parsedPlayers.join(" · ")}
        </span>
      )}
    </form>
  );
}

export function ScheduleMatchForm({
  championshipId,
  groupId,
  teams,
}: {
  championshipId: string;
  groupId: string;
  teams: { team_id: string; name: string }[];
}) {
  const [homeId, setHomeId] = useState("");
  const [awayId, setAwayId] = useState("");
  const [kickoff, setKickoff] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();

  const mutation = useMutation({
    mutationFn: () => {
      const home = teams.find((team) => team.team_id === homeId)!;
      const away = teams.find((team) => team.team_id === awayId)!;
      // datetime-local vem sem fuso; Date interpreta como hora local e
      // toISOString converte para UTC, que e o que a API espera
      const scheduledAt = kickoff !== "" ? new Date(kickoff).toISOString() : null;
      return scheduleMatch({ championshipId, groupId, home, away, scheduledAt });
    },
    onSuccess: (match) => {
      toast("success", `Partida ${match.home_team.name} x ${match.away_team.name} agendada`);
      setHomeId("");
      setAwayId("");
      setKickoff("");
      queryClient.invalidateQueries({ queryKey: ["matches"] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const valid = homeId !== "" && awayId !== "" && homeId !== awayId;

  return (
    <form
      className="org-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <h3>Agendar partida</h3>
      <div className="row">
        <select value={homeId} onChange={(event) => setHomeId(event.target.value)}>
          <option value="">Mandante…</option>
          {teams.map((team) => (
            <option key={team.team_id} value={team.team_id}>
              {team.name}
            </option>
          ))}
        </select>
        <select value={awayId} onChange={(event) => setAwayId(event.target.value)}>
          <option value="">Visitante…</option>
          {teams.map((team) => (
            <option key={team.team_id} value={team.team_id}>
              {team.name}
            </option>
          ))}
        </select>
      </div>
      <div className="row">
        <input
          type="datetime-local"
          value={kickoff}
          onChange={(event) => setKickoff(event.target.value)}
          aria-label="horário de início"
        />
        <button type="submit" disabled={!valid || mutation.isPending}>
          {mutation.isPending ? "Agendando…" : "Agendar"}
        </button>
      </div>
      {homeId !== "" && homeId === awayId && <span className="error">um time não joga contra si mesmo</span>}
      {teams.length < 2 && <span className="hint">inscreva (e aguarde confirmar) pelo menos 2 times</span>}
    </form>
  );
}
