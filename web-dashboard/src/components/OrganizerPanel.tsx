import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import {
  createChampionship,
  enrollTeam,
  fetchChampionships,
  fetchEnrollments,
  scheduleMatch,
} from "../api";

function CreateChampionshipForm() {
  const [nome, setNome] = useState("");
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => createChampionship(nome.trim()),
    onSuccess: () => {
      setNome("");
      queryClient.invalidateQueries({ queryKey: ["championships"] });
    },
  });

  return (
    <form
      className="org-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (nome.trim()) mutation.mutate();
      }}
    >
      <h3>1 · Criar campeonato</h3>
      <div className="row">
        <input
          placeholder="Nome do campeonato"
          value={nome}
          maxLength={100}
          onChange={(event) => setNome(event.target.value)}
        />
        <button type="submit" disabled={!nome.trim() || mutation.isPending}>
          Criar
        </button>
      </div>
      {mutation.isError && <span className="error">{(mutation.error as Error).message}</span>}
    </form>
  );
}

function EnrollTeamForm({ championshipId }: { championshipId: string }) {
  const [nome, setNome] = useState("");
  const [jogadores, setJogadores] = useState("");
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () =>
      enrollTeam(
        championshipId,
        nome.trim(),
        jogadores.split(",").map((jogador) => jogador.trim()).filter(Boolean),
      ),
    onSuccess: () => {
      setNome("");
      setJogadores("");
      // a lista atualiza via polling; o status PENDENTE -> CONFIRMADA
      // aparece sozinho quando a saga processa os eventos
      queryClient.invalidateQueries({ queryKey: ["enrollments", championshipId] });
    },
  });

  const valid = nome.trim() !== "" && jogadores.split(",").some((jogador) => jogador.trim() !== "");

  return (
    <form
      className="org-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <h3>2 · Inscrever time</h3>
      <div className="row">
        <input placeholder="Nome do time" value={nome} maxLength={100} onChange={(event) => setNome(event.target.value)} />
      </div>
      <div className="row">
        <input
          placeholder="Jogadores separados por vírgula"
          value={jogadores}
          onChange={(event) => setJogadores(event.target.value)}
        />
        <button type="submit" disabled={!valid || mutation.isPending}>
          Inscrever
        </button>
      </div>
      {mutation.isError && <span className="error">{(mutation.error as Error).message}</span>}
    </form>
  );
}

function ScheduleMatchForm({
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

  const mutation = useMutation({
    mutationFn: () => {
      const home = teams.find((team) => team.team_id === homeId)!;
      const away = teams.find((team) => team.team_id === awayId)!;
      // datetime-local vem sem fuso; Date interpreta como hora local e
      // toISOString converte para UTC, que e o que a API espera
      const scheduledAt = kickoff !== "" ? new Date(kickoff).toISOString() : null;
      return scheduleMatch({ championshipId, groupId, home, away, scheduledAt });
    },
    onSuccess: () => {
      setHomeId("");
      setAwayId("");
      setKickoff("");
      queryClient.invalidateQueries({ queryKey: ["matches"] });
    },
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
      <h3>3 · Agendar partida</h3>
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
          Agendar
        </button>
      </div>
      {homeId !== "" && homeId === awayId && <span className="error">um time não joga contra si mesmo</span>}
      {mutation.isError && <span className="error">{(mutation.error as Error).message}</span>}
      {teams.length < 2 && <span className="meta">inscreva (e aguarde confirmar) pelo menos 2 times</span>}
    </form>
  );
}

export function OrganizerPanel({ defaultGroupId }: { defaultGroupId: string | null }) {
  const { data: championships = [] } = useQuery({ queryKey: ["championships"], queryFn: fetchChampionships });

  const [selectedChampionship, setSelectedChampionship] = useState<string | null>(null);
  const championshipId = selectedChampionship ?? championships[0]?.id ?? null;

  const { data: enrollments = [] } = useQuery({
    queryKey: ["enrollments", championshipId],
    queryFn: () => fetchEnrollments(championshipId!),
    enabled: championshipId !== null,
  });

  // grupo unico por campeonato no MVP (sorteio de grupos fica fora do escopo):
  // reusa o grupo das partidas existentes ou deriva um novo id estavel
  const groupId = useMemo(() => defaultGroupId ?? crypto.randomUUID(), [defaultGroupId]);

  const confirmedTeams = enrollments
    .filter((enrollment) => enrollment.status === "CONFIRMADA")
    .map((enrollment) => ({ team_id: enrollment.time_id, name: enrollment.time_nome }));

  return (
    <div className="panel organizer">
      <h2>Organizador</h2>

      <CreateChampionshipForm />

      {championships.length > 0 && (
        <>
          <label className="group-select">
            Campeonato:{" "}
            <select value={championshipId ?? ""} onChange={(event) => setSelectedChampionship(event.target.value)}>
              {championships.map((championship) => (
                <option key={championship.id} value={championship.id}>
                  {championship.nome}
                </option>
              ))}
            </select>
          </label>

          {championshipId && (
            <>
              <EnrollTeamForm championshipId={championshipId} />

              {enrollments.length > 0 && (
                <div className="org-form">
                  <h3>Times inscritos</h3>
                  <ul className="enrollments">
                    {enrollments.map((enrollment) => (
                      <li key={enrollment.inscricao_id}>
                        <span className={`badge ${enrollment.status.toLowerCase()}`}>
                          {enrollment.status === "CONFIRMADA" ? "✓ confirmada" : "⏳ pendente"}
                        </span>
                        <strong>{enrollment.time_nome}</strong>
                        <span className="meta">
                          {enrollment.jogadores.map((jogador) => jogador.nome).join(", ")}
                        </span>
                      </li>
                    ))}
                  </ul>
                  <p className="meta">
                    Status via saga: <code>team.registered.v1 → enrollment.confirmed.v1</code>
                  </p>
                </div>
              )}

              <ScheduleMatchForm championshipId={championshipId} groupId={groupId} teams={confirmedTeams} />
            </>
          )}
        </>
      )}
    </div>
  );
}
