import { Link, NavLink, Outlet } from "react-router-dom";
import { useMatches } from "./data";
import { useOrganizer } from "./organizer";
import { ToastProvider } from "./ui/toast";

// Navegação enxuta: o app é de gestão de torneios, então times, jogadores e
// partidas vivem dentro de cada torneio — nada de listas globais misturadas.
const NAV_ITEMS = [
  { to: "/", label: "Início", end: true },
  { to: "/torneios", label: "Torneios" },
];

export default function App() {
  const organizer = useOrganizer();
  const { data: matches = [] } = useMatches();
  const liveCount = matches.filter((match) => match.status === "EM_ANDAMENTO").length;

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
              {organizer ? "Organizador" : "Visitante"}
            </Link>
          </div>
        </header>

        <div className="layout">
          <aside className="sidebar">
            <nav aria-label="Seções">
              {NAV_ITEMS.map((item) => (
                <NavLink key={item.to} to={item.to} end={item.end}>
                  {item.label}
                </NavLink>
              ))}
              <div className="nav-separator" />
              {organizer && <NavLink to="/monitoramento">Monitoramento</NavLink>}
              <NavLink to="/conta">Conta</NavLink>
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
