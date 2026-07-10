import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { fetchMatches } from "../api";
import { MatchesPanel } from "../components/MatchesPanel";
import { OrganizerPanel } from "../components/OrganizerPanel";
import { StandingsPanel } from "../components/StandingsPanel";
import { loginOrganizer, useOrganizer } from "../organizer";

function OrganizerLogin() {
  const [key, setKey] = useState("");
  const [failed, setFailed] = useState(false);

  return (
    <main>
      <div className="panel login-panel">
        <h2>Área do organizador</h2>
        <p className="meta">
          Somente o organizador pode criar campeonatos, inscrever times, agendar e apurar partidas.
        </p>
        <form
          className="org-form"
          onSubmit={(event) => {
            event.preventDefault();
            setFailed(!loginOrganizer(key));
          }}
        >
          <div className="row">
            <input
              type="password"
              placeholder="Chave de acesso"
              value={key}
              onChange={(event) => setKey(event.target.value)}
            />
            <button type="submit" disabled={key === ""}>
              Entrar
            </button>
          </div>
          {failed && <span className="error">chave incorreta</span>}
        </form>
      </div>
    </main>
  );
}

/**
 * Area restrita: unico lugar com acoes de escrita (criar, inscrever,
 * agendar, iniciar, apurar). Gate simples de demo — ver SPEC.md §2.
 */
export function OrganizerPage() {
  const organizer = useOrganizer();
  const { data: matches = [] } = useQuery({
    queryKey: ["matches"],
    queryFn: fetchMatches,
    enabled: organizer,
  });

  const groupIds = useMemo(
    () => [...new Set(matches.map((match) => match.group_id).filter((id): id is string => id !== null))],
    [matches],
  );

  if (!organizer) return <OrganizerLogin />;

  return (
    <main>
      <section className="col">
        <OrganizerPanel defaultGroupId={groupIds[0] ?? null} />
      </section>
      <section className="col">
        <MatchesPanel matches={matches} />
      </section>
      <section className="col">
        <StandingsPanel groupId={groupIds[0] ?? null} />
      </section>
    </main>
  );
}
