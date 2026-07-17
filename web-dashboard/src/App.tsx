import { Link, NavLink, Outlet } from "react-router-dom";
import { logout, useAuth } from "./auth";
import { useChampionships, useMatches } from "./data";
import { ToastProvider } from "./ui/toast";

export default function App() {
  const user = useAuth();
  const { data: matches = [] } = useMatches();
  const { data: championships = [] } = useChampionships();
  const liveCount = matches.filter((match) => match.status === "EM_ANDAMENTO").length;

  // Navegação por papel: visitante escolhe um torneio no Início e acompanha
  // só aquele torneio; quem tem conta vê a visão de gestão (Torneios) — e o
  // Monitoramento só aparece para quem gerencia pelo menos um torneio
  // (tela de operação/suporte; ruído para capitães).
  const gerenciaAlgum = user !== null && championships.some((championship) => championship.can_manage);
  const navItems = user
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
            {user ? (
              <details className="session-menu">
                <summary className="session-chip organizer">{user.nome}</summary>
                <div className="session-menu-items">
                  <Link to="/conta">Minha conta</Link>
                  <button type="button" onClick={() => logout()}>
                    Sair
                  </button>
                </div>
              </details>
            ) : (
              <Link to="/conta" className="session-chip">
                Entrar
              </Link>
            )}
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
              {gerenciaAlgum && (
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
