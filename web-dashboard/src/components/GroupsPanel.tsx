import { Match } from "../api";
import { toggleMyTeam, useMyTeam } from "../ui/myteam";

/**
 * Composição dos grupos sorteados, derivada das partidas geradas — a tela do
 * momento "contra quem meu time caiu". A estrela marca o "meu time" do
 * visitante (preferência local, sem conta), destacado em todo o torneio.
 */
export function GroupsPanel({
  championshipId,
  matches,
  groupLabels,
}: {
  championshipId: string;
  matches: Match[];
  groupLabels: Map<string, string>;
}) {
  const myTeam = useMyTeam(championshipId);

  const teamsByGroup = new Map<string, Map<string, string>>();
  for (const match of matches) {
    if (match.stage !== "GRUPOS" || !match.group_id) continue;
    const teams = teamsByGroup.get(match.group_id) ?? new Map<string, string>();
    teams.set(match.home_team.team_id, match.home_team.name);
    teams.set(match.away_team.team_id, match.away_team.name);
    teamsByGroup.set(match.group_id, teams);
  }

  if (teamsByGroup.size === 0) return null;

  const ordered = [...teamsByGroup.keys()].sort((a, b) =>
    (groupLabels.get(a) ?? "").localeCompare(groupLabels.get(b) ?? "", "pt-BR", { numeric: true }),
  );

  return (
    <section className="panel">
      <h2>Grupos sorteados</h2>
      <div className="groups-grid">
        {ordered.map((groupId) => (
          <div key={groupId} className="group-card">
            <h3>{groupLabels.get(groupId) ?? "Grupo"}</h3>
            <ul className="roster">
              {[...teamsByGroup.get(groupId)!.entries()].map(([teamId, name]) => (
                <li key={teamId} className={myTeam?.teamId === teamId ? "my-team" : ""}>
                  <button
                    type="button"
                    className="star-button"
                    title={myTeam?.teamId === teamId ? "Deixar de seguir este time" : "Marcar como meu time"}
                    onClick={() => toggleMyTeam(championshipId, { teamId, name })}
                  >
                    {myTeam?.teamId === teamId ? "★" : "☆"}
                  </button>
                  {name}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <p className="meta">Os 2 melhores de cada grupo avançam ao mata-mata (1º A × 2º B). ☆ marca o seu time.</p>
    </section>
  );
}
