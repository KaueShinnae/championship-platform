import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { fetchMatches } from "../api";
import { EventFeed } from "../components/EventFeed";
import { MatchesPanel } from "../components/MatchesPanel";
import { StandingsPanel } from "../components/StandingsPanel";

/**
 * Visao publica: acompanhar o torneio. Nenhuma acao de escrita aqui —
 * gerenciamento fica na pagina do organizador.
 */
export function TournamentPage() {
  const { data: matches = [], isError } = useQuery({ queryKey: ["matches"], queryFn: fetchMatches });

  const groupIds = useMemo(
    () => [...new Set(matches.map((match) => match.group_id).filter((id): id is string => id !== null))],
    [matches],
  );

  const [selectedGroup, setSelectedGroup] = useState<string | null>(null);
  const groupId = selectedGroup ?? groupIds[0] ?? null;

  return (
    <>
      {isError && (
        <div className="banner-error">
          Não foi possível falar com os serviços — rode <code>npm run dev</code> na raiz do projeto.
        </div>
      )}
      <main>
        <section className="col">
          <MatchesPanel matches={matches} readOnly />
        </section>
        <section className="col">
          {groupIds.length > 1 && (
            <label className="group-select">
              Grupo:{" "}
              <select value={groupId ?? ""} onChange={(event) => setSelectedGroup(event.target.value)}>
                {groupIds.map((id) => (
                  <option key={id} value={id}>
                    {id.slice(0, 8)}…
                  </option>
                ))}
              </select>
            </label>
          )}
          <StandingsPanel groupId={groupId} />
        </section>
        <section className="col">
          <EventFeed />
        </section>
      </main>
    </>
  );
}
