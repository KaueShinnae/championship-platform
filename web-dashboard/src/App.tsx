import { Link, NavLink, Outlet } from "react-router-dom";
import { logoutOrganizer, useOrganizer } from "./organizer";

export default function App() {
  const organizer = useOrganizer();

  return (
    <div className="app">
      <header className="topbar">
        <Link to="/" className="brand">
          <h1>Championship Platform</h1>
        </Link>
        <nav>
          <NavLink to="/" end>
            Torneio
          </NavLink>
          <NavLink to="/organizador">Organizador</NavLink>
          {organizer && (
            <button type="button" className="link-button" onClick={logoutOrganizer}>
              Sair do modo organizador
            </button>
          )}
        </nav>
      </header>

      <Outlet />

      <footer className="meta">
        Arquitetura orientada a eventos — as telas atualizam via eventos Kafka
        (<code>match.finished.v1 → ranking.updated.v1</code>), sem chamadas síncronas entre serviços.
      </footer>
    </div>
  );
}
