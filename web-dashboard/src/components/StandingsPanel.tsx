import { useStandings } from "../data";
import { formatTime } from "../format";
import { Skeleton } from "../ui/Skeleton";

export function StandingsPanel({ groupId, title = "Classificação" }: { groupId: string | null; title?: string }) {
  const { data, isLoading, dataUpdatedAt } = useStandings(groupId);

  return (
    <div className="panel">
      <h2>{title}</h2>
      {!groupId && <p className="empty">A classificação aparece quando a primeira partida for agendada.</p>}
      {groupId && isLoading && <Skeleton lines={4} />}
      {groupId && !isLoading && !data && (
        <p className="empty">Ainda sem resultados processados — encerre uma partida para gerar a tabela.</p>
      )}
      {data && (
        <>
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th className="left">Time</th>
                <th title="Pontos">P</th>
                <th title="Vitórias">V</th>
                <th title="Empates">E</th>
                <th title="Derrotas">D</th>
                <th title="Saldo de gols">SG</th>
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
          <p className="meta">Atualizada às {formatTime(new Date(dataUpdatedAt).toISOString())}</p>
        </>
      )}
    </div>
  );
}
