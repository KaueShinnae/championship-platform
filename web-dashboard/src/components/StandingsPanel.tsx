import { useStandings } from "../data";
import { formatTime } from "../format";
import { Skeleton } from "../ui/Skeleton";

/**
 * Tabela de classificação. `qualifyCount` marca a zona que importa: quem
 * avança ao mata-mata (grupos) ou o campeão em potencial (pontos corridos).
 */
export function StandingsPanel({
  groupId,
  title = "Classificação",
  qualifyCount,
  highlightTeamId,
}: {
  groupId: string | null;
  title?: string;
  qualifyCount?: number;
  highlightTeamId?: string | null;
}) {
  const { data, isLoading, dataUpdatedAt } = useStandings(groupId);

  return (
    <div className="panel">
      <h2>{title}</h2>
      {!groupId && <p className="empty">A classificação aparece após o sorteio dos confrontos.</p>}
      {groupId && isLoading && <Skeleton lines={4} />}
      {groupId && !isLoading && !data && (
        <p className="empty">Ainda sem resultados — a tabela aparece quando o primeiro for registrado.</p>
      )}
      {data && (
        <>
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th className="left">Time</th>
                <th title="Pontos">P</th>
                <th title="Vitórias" className="hide-narrow">V</th>
                <th title="Empates" className="hide-narrow">E</th>
                <th title="Derrotas" className="hide-narrow">D</th>
                <th title="Saldo de gols">SG</th>
              </tr>
            </thead>
            <tbody>
              {data.standings.map((entry, index) => {
                const classes = [
                  qualifyCount !== undefined && index < qualifyCount ? "qualify" : "",
                  index === 0 ? "leader" : "",
                  highlightTeamId === entry.team_id ? "my-team-row" : "",
                ]
                  .filter(Boolean)
                  .join(" ");
                return (
                  <tr key={entry.team_id} className={classes}>
                    <td>{index + 1}</td>
                    <td className="left">{entry.team_name}</td>
                    <td className="points">{entry.points}</td>
                    <td className="hide-narrow">{entry.wins}</td>
                    <td className="hide-narrow">{entry.draws}</td>
                    <td className="hide-narrow">{entry.losses}</td>
                    <td>{entry.goals_for - entry.goals_against}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          <p className="meta">
            Atualizada às {formatTime(new Date(dataUpdatedAt).toISOString())}
            {qualifyCount !== undefined && qualifyCount > 1 && (
              <> · os {qualifyCount} primeiros avançam ao mata-mata</>
            )}
            {qualifyCount === 1 && <> · o líder ao fim da última rodada é o campeão</>}
          </p>
        </>
      )}
    </div>
  );
}
