import { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Championship } from "../api";
import { useAllEnrollments, useMatches } from "../data";
import { formatDate } from "../format";
import { Skeleton } from "../ui/Skeleton";

export const CHAMPIONSHIP_STATUS_LABEL: Record<Championship["status"], string> = {
  ABERTO: "Inscrições abertas",
  SORTEADO: "Sorteio realizado",
  EM_ANDAMENTO: "Em andamento",
  ENCERRADO: "Encerrado",
};

/** Grade de torneios clicáveis — usada no Início do visitante e na página Torneios do organizador. */
export function TournamentGrid({ emptyMessage }: { emptyMessage: ReactNode }) {
  const { byChampionship, isLoading } = useAllEnrollments();
  const { data: matches = [] } = useMatches();

  if (isLoading) {
    return (
      <div className="panel">
        <Skeleton lines={3} />
      </div>
    );
  }

  if (byChampionship.length === 0) {
    return (
      <div className="panel">
        <p className="empty">{emptyMessage}</p>
      </div>
    );
  }

  // torneios que o usuário gerencia vêm primeiro
  const ordered = [...byChampionship].sort(
    (a, b) => Number(b.championship.can_manage) - Number(a.championship.can_manage),
  );

  return (
    <ul className="card-grid">
      {ordered.map(({ championship, enrollments }) => {
        const confirmed = enrollments.filter((enrollment) => enrollment.status === "CONFIRMADA").length;
        const championshipMatches = matches.filter((match) => match.championship_id === championship.id);
        const liveCount = championshipMatches.filter((match) => match.status === "EM_ANDAMENTO").length;
        return (
          <li key={championship.id}>
            <Link to={`/torneios/${championship.id}`} className="tournament-card">
              <div className="tournament-card-header">
                <h3>{championship.nome}</h3>
                <span className={`badge championship-${championship.status.toLowerCase()}`}>
                  {CHAMPIONSHIP_STATUS_LABEL[championship.status]}
                </span>
                {championship.can_manage && <span className="badge manage-badge">sua gestão</span>}
              </div>
              <p className="meta">
                {confirmed} de {enrollments.length} time{enrollments.length === 1 ? "" : "s"} confirmado
                {confirmed === 1 ? "" : "s"} · {championshipMatches.length} partida
                {championshipMatches.length === 1 ? "" : "s"}
                {liveCount > 0 && <span className="live-inline"> · ● {liveCount} ao vivo</span>}
              </p>
              {championship.status === "ENCERRADO" && championship.campeao_nome ? (
                <p className="meta champion-inline">🏆 {championship.campeao_nome}</p>
              ) : (
                <p className="meta">criado em {formatDate(championship.created_at)}</p>
              )}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}
