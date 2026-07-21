import { useQuery } from "@tanstack/react-query";
import { fetchManagementLog } from "../api";
import { formatTimestamp } from "../format";
import { Skeleton } from "../ui/Skeleton";

const ACAO_ICONE: Record<string, string> = {
  APROVACAO: "✓",
  RECUSA: "✗",
  REMOCAO: "🗑",
  EDICAO_TIME: "✎",
  CORRECAO: "✎",
  WO: "⚖",
  REMARCACAO_LOTE: "🕒",
};

export function ManagementLog({ championshipId }: { championshipId: string }) {
  const { data: entradas = [], isLoading } = useQuery({
    queryKey: ["management-log", championshipId],
    queryFn: () => fetchManagementLog(championshipId),
  });

  return (
    <section className="panel">
      <h2>Histórico de gestão</h2>
      <p className="meta">Ações administrativas deste torneio (aprovações, remoções, correções, W.O., remarcações).</p>
      {isLoading && <Skeleton lines={4} />}
      {!isLoading && entradas.length === 0 && (
        <p className="empty">Nenhuma ação de gestão registrada ainda.</p>
      )}
      {entradas.length > 0 && (
        <ul className="management-log">
          {entradas.map((entrada) => (
            <li key={entrada.id}>
              <span className="log-icon" aria-hidden>
                {ACAO_ICONE[entrada.acao] ?? "•"}
              </span>
              <span className="log-text">
                <strong>{entrada.actor_nome}</strong> {entrada.descricao}
              </span>
              <span className="meta">{formatTimestamp(entrada.created_at)}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
