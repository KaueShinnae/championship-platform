import { Link, NavLink, Outlet } from "react-router-dom";
import { useMatches } from "./data";
import { useOrganizer } from "./organizer";
import { ToastProvider } from "./ui/toast";

export default function App() {
  const organizer = useOrganizer();
  const { data: matches = [] } = useMatches();
  const liveCount = matches.filter((match) => match.status === "EM_ANDAMENTO").length;

  // Navegação por papel: o visitante escolhe um torneio no Início e acompanha
  // só aquele torneio; a visão geral (Torneios, Monitoramento) é do organizador.
  const navItems = organizer
    ? [
        { to: "/", label: "Início", end: true },
        { to: "/torneios", label: "Torneios" },
      ]
    : [{ to: "/", label: "Início", end: true }];

  return (
    <ToastProvider>
      <div className="shell">
        <header className="topbar">
          <Link to="/" className="brand">
            <span className="brand-mark">🏆</span>
            <h1>Championship</h1>
          </Link>
          <div className="topbar-right">
            {liveCount > 0 && (
              <Link to="/" className="live-chip">
                ● {liveCount} ao vivo
              </Link>
            )}
            <Link to="/conta" className={`session-chip ${organizer ? "organizer" : ""}`}>
              {organizer ? "Organizador" : "Conta"}
            </Link>
          </div>
        </header>

        <div className="layout">
          <aside className="sidebar">
            <nav aria-label="Seções">
              {navItems.map((item) => (
                <NavLink key={item.to} to={item.to} end={item.end}>
                  {item.label}
                </NavLink>
              ))}
              {organizer && (
                <>
                  <div className="nav-separator" />
                  <NavLink to="/monitoramento">Monitoramento</NavLink>
                </>
              )}
            </nav>
          </aside>

          <main className="content">
            <Outlet />
          </main>
        </div>
      </div>
    </ToastProvider>
  );
}
