import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ScheduleConflict, reagendarEmLote } from "../api";
import { formatShortDateTime } from "../format";
import { useToast } from "../ui/toast";

export function ScheduleTools({
  championshipId,
  conflitos,
}: {
  championshipId: string;
  conflitos: ScheduleConflict[];
}) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const shift = useMutation({
    mutationFn: (minutos: number) => reagendarEmLote(championshipId, minutos),
    onSuccess: (resultado) => {
      queryClient.invalidateQueries({ queryKey: ["matches"] });
      toast(
        "success",
        resultado.rescheduled > 0
          ? `${resultado.rescheduled} partida(s) remarcada(s)`
          : "nenhuma partida agendada com horário para remarcar",
      );
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const opcoes: { label: string; minutos: number }[] = [
    { label: "−30 min", minutos: -30 },
    { label: "+30 min", minutos: 30 },
    { label: "+1 h", minutos: 60 },
    { label: "+2 h", minutos: 120 },
  ];

  return (
    <div className="panel schedule-tools">
      {conflitos.length > 0 && (
        <div className="conflict-warning">
          <strong>⚠ {conflitos.length} conflito(s) de agenda</strong>
          <ul>
            {conflitos.map((c) => (
              <li key={`${c.tipo}-${c.partida_a}-${c.partida_b}`}>
                {c.tipo === "LOCAL" ? (
                  <>
                    <strong>📍 {c.local}</strong> tem dois jogos às {formatShortDateTime(c.scheduled_at)}
                  </>
                ) : (
                  <>
                    <strong>{c.team_name}</strong> está em dois jogos às {formatShortDateTime(c.scheduled_at)}
                  </>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
      <div className="batch-reschedule">
        <span className="meta">Imprevisto na rodada? Empurre todos os jogos agendados de uma vez:</span>
        <div className="match-actions">
          {opcoes.map((opcao) => (
            <button
              key={opcao.minutos}
              type="button"
              className="ghost"
              disabled={shift.isPending}
              onClick={() => {
                if (window.confirm(`Deslocar todas as partidas agendadas em ${opcao.label}?`)) {
                  shift.mutate(opcao.minutos);
                }
              }}
            >
              {opcao.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
