import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { enrollTeam, EnrollmentCreated, fetchMeusTimes } from "../api";
import { useAuth } from "../auth";
import { useToast } from "../ui/toast";

// A criação de torneio vive em /torneios/novo (CreateTournamentPage) e o
// agendamento manual de partidas foi substituído pelo sorteio automático.

/**
 * Inscrição de time — usado pelo organizador (auto-confirma via saga) e pelo
 * capitão (fica aguardando aprovação). "Meus times" sugere elencos de
 * torneios anteriores do usuário; reusar copia um snapshot (editar depois
 * não altera o torneio antigo).
 */
export function EnrollTeamForm({
  championshipId,
  title = "Inscrever time",
  cta = "Inscrever",
  successMessage,
  onEnrolled,
}: {
  championshipId: string;
  title?: string;
  cta?: string;
  successMessage?: (nome: string) => string;
  onEnrolled?: (enrollment: EnrollmentCreated) => void;
}) {
  const [nome, setNome] = useState("");
  const [jogadores, setJogadores] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();
  const user = useAuth();

  const { data: meusTimes = [] } = useQuery({
    queryKey: ["meus-times", user?.id],
    queryFn: fetchMeusTimes,
    enabled: user !== null,
    refetchInterval: false,
  });

  const parsedPlayers = jogadores.split(",").map((jogador) => jogador.trim()).filter(Boolean);

  const mutation = useMutation({
    mutationFn: () => enrollTeam(championshipId, nome.trim(), parsedPlayers),
    onSuccess: (enrollment) => {
      toast(
        "success",
        successMessage
          ? successMessage(nome.trim())
          : `Inscrição de "${nome.trim()}" enviada — aguardando confirmação`,
      );
      setNome("");
      setJogadores("");
      queryClient.invalidateQueries({ queryKey: ["enrollments", championshipId] });
      queryClient.invalidateQueries({ queryKey: ["meus-times"] });
      onEnrolled?.(enrollment);
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
      <h3>{title}</h3>
      {meusTimes.length > 0 && (
        <div className="suggestion-chips">
          <span className="meta">Usar time já cadastrado:</span>
          {meusTimes.slice(0, 6).map((time) => (
            <button
              key={time.nome}
              type="button"
              className="ghost chip"
              title={time.jogadores.join(" · ")}
              onClick={() => {
                setNome(time.nome);
                setJogadores(time.jogadores.join(", "));
              }}
            >
              {time.nome}
            </button>
          ))}
        </div>
      )}
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
          {mutation.isPending ? "Enviando…" : cta}
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
