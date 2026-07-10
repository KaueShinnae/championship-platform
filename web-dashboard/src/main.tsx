import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import App from "./App";
import { MatchDetailPage } from "./pages/MatchDetailPage";
import { OrganizerPage } from "./pages/OrganizerPage";
import { TournamentPage } from "./pages/TournamentPage";
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
      { index: true, element: <TournamentPage /> },
      { path: "partidas/:matchId", element: <MatchDetailPage /> },
      { path: "organizador", element: <OrganizerPage /> },
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
