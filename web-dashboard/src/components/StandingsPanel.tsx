import { useQuery } from "@tanstack/react-query";
import { fetchStandings } from "../api";

export function StandingsPanel({ groupId }: { groupId: string | null }) {
  const { data, dataUpdatedAt } = useQuery({
    queryKey: ["standings", groupId],
    queryFn: () => fetchStandings(groupId!),
    enabled: groupId !== null,
  });

  return (
    <div className="panel">
      <h2>Classificação</h2>
      {!groupId && <p className="empty">Nenhum grupo ainda — agende uma partida com group_id.</p>}
      {groupId && !data && <p className="empty">Sem resultados processados neste grupo ainda.</p>}
      {data && (
        <>
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th className="left">Time</th>
                <th>P</th>
                <th>V</th>
                <th>E</th>
                <th>D</th>
                <th>SG</th>
              </tr>
            </thead>
            <tbody>
              {data.standings.map((entry, index) => (
                <tr key={entry.team_id} className={index === 0 ? "leader" : ""}>
                  <td>{index + 1}</td>
                  <td className="left">{entry.team_name}</td>
                  <td className="points">{entry.points}</td>
                  <td>{entry.wins}</td>
                  <td>{entry.draws}</td>
                  <td>{entry.losses}</td>
                  <td>{entry.goals_for - entry.goals_against}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="meta">
            Projeção CQRS atualizada só por eventos · sync {new Date(dataUpdatedAt).toLocaleTimeString()}
          </p>
        </>
      )}
    </div>
  );
}
