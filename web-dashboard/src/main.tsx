import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, Navigate, RouterProvider } from "react-router-dom";
import App from "./App";
import { AccountPage } from "./pages/AccountPage";
import { CreateTournamentPage } from "./pages/CreateTournamentPage";
import { HomePage } from "./pages/HomePage";
import { MatchDetailPage } from "./pages/MatchDetailPage";
import { MonitoringPage } from "./pages/MonitoringPage";
import { TournamentDetailPage } from "./pages/TournamentDetailPage";
import { TournamentsPage } from "./pages/TournamentsPage";
import "./styles.css";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchInterval: 2000, // polling: mantem classificacao e feed "ao vivo"
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "torneios", element: <TournamentsPage /> },
      { path: "torneios/novo", element: <CreateTournamentPage /> },
      { path: "torneios/:championshipId", element: <TournamentDetailPage /> },
      { path: "partidas/:matchId", element: <MatchDetailPage /> },
      { path: "monitoramento", element: <MonitoringPage /> },
      { path: "conta", element: <AccountPage /> },
      // rotas antigas: listas globais foram absorvidas pelos torneios,
      // e a "atividade" virou o Monitoramento restrito ao organizador
      { path: "partidas", element: <Navigate to="/torneios" replace /> },
      { path: "times", element: <Navigate to="/torneios" replace /> },
      { path: "jogadores", element: <Navigate to="/torneios" replace /> },
      { path: "atividade", element: <Navigate to="/monitoramento" replace /> },
      { path: "organizador", element: <Navigate to="/conta" replace /> },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </React.StrictMode>,
);
