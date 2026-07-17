import { Link } from "react-router-dom";
import { useAuth } from "../auth";
import { TournamentGrid } from "../components/TournamentGrid";

export function TournamentsPage() {
  const organizer = useAuth() !== null;

  return (
    <>
      <div className="page-header">
        <div className="page-title-row">
          <h2 className="page-title">Torneios</h2>
          {organizer && (
            <Link to="/torneios/novo" className="button-link">
              + Criar torneio
            </Link>
          )}
        </div>
        <p className="subtitle">Todos os campeonatos da plataforma — clique num torneio para gerenciar e acompanhar.</p>
      </div>

      <TournamentGrid
        emptyMessage={
          organizer ? (
            <>
              Nenhum torneio ainda. <Link to="/torneios/novo">Crie o primeiro</Link>.
            </>
          ) : (
            <>
              Nenhum torneio ainda. Entre como organizador na página <Link to="/conta">Conta</Link> para criar o
              primeiro.
            </>
          )
        }
      />
    </>
  );
}
