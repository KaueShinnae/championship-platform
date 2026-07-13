import { Link } from "react-router-dom";
import { Championship } from "../api";
import { CreateChampionshipForm } from "../components/OrganizerForms";
import { useAllEnrollments, useMatches } from "../data";
import { formatDate } from "../format";
import { useOrganizer } from "../organizer";
import { Skeleton } from "../ui/Skeleton";

export const CHAMPIONSHIP_STATUS_LABEL: Record<Championship["status"], string> = {
  ABERTO: "Inscrições abertas",
  EM_ANDAMENTO: "Em andamento",
  ENCERRADO: "Encerrado",
};

export function TournamentsPage() {
  const organizer = useOrganizer();
  const { byChampionship, isLoading } = useAllEnrollments();
  const { data: matches = [] } = useMatches();

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">Torneios</h2>
        <p className="subtitle">Todos os campeonatos da plataforma — clique num torneio para gerenciar e acompanhar.</p>
      </div>

      {organizer && (
        <div className="panel">
          <CreateChampionshipForm />
        </div>
      )}

      {isLoading && (
        <div className="panel">
          <Skeleton lines={3} />
        </div>
      )}

      {!isLoading && byChampionship.length === 0 && (
        <div className="panel">
          <p className="empty">
            Nenhum torneio ainda.{" "}
            {organizer ? "Crie o primeiro no formulário acima." : (
              <>
                Entre como organizador na página <Link to="/conta">Conta</Link> para criar o primeiro.
              </>
            )}
          </p>
        </div>
      )}

      <ul className="card-grid">
        {byChampionship.map(({ championship, enrollments }) => {
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
                </div>
                <p className="meta">
                  {confirmed} de {enrollments.length} time{enrollments.length === 1 ? "" : "s"} confirmado
                  {confirmed === 1 ? "" : "s"} · {championshipMatches.length} partida
                  {championshipMatches.length === 1 ? "" : "s"}
                  {liveCount > 0 && <span className="live-inline"> · ● {liveCount} ao vivo</span>}
                </p>
                <p className="meta">criado em {formatDate(championship.created_at)}</p>
              </Link>
            </li>
          );
        })}
      </ul>
    </>
  );
}
