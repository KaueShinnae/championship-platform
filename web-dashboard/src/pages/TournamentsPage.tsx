import { Link } from "react-router-dom";
import { CreateChampionshipForm } from "../components/OrganizerForms";
import { TournamentGrid } from "../components/TournamentGrid";
import { useOrganizer } from "../organizer";

export function TournamentsPage() {
  const organizer = useOrganizer();

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

      <TournamentGrid
        emptyMessage={
          organizer ? (
            "Nenhum torneio ainda. Crie o primeiro no formulário acima."
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
