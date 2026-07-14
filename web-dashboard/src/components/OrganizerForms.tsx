import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { enrollTeam } from "../api";
import { useToast } from "../ui/toast";

// A criação de torneio vive em /torneios/novo (CreateTournamentPage) e o
// agendamento manual de partidas foi substituído pelo sorteio automático.

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

