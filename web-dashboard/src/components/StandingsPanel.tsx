import { useStandings } from "../data";
import { formatTime } from "../format";
import { Skeleton } from "../ui/Skeleton";

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

  const comDesempate = data?.standings.filter((entry) => entry.desempate) ?? [];

  return (
    <div className="panel">
      <h2>{title}</h2>
      {!groupId && <p className="empty">A classificação aparece após o sorteio dos confrontos.</p>}
      {groupId && isLoading && <Skeleton lines={4} />}
      {groupId && !isLoading && !data && (
        <p className="empty">A classificação aparece após o sorteio dos confrontos.</p>
      )}
      {data && (
        <>
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th className="left">Equipe</th>
                <th title="Pontos de classificação (vitória 3, empate 1)">P</th>
                <th title="Vitórias" className="hide-narrow">V</th>
                <th title="Empates" className="hide-narrow">E</th>
                <th title="Derrotas" className="hide-narrow">D</th>
                <th title="Pró (marcados a favor)" className="hide-narrow">Pró</th>
                <th title="Contra (sofridos)" className="hide-narrow">Con</th>
                <th title="Saldo (Pró − Contra)">S</th>
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
                    <td className="left">
                      {entry.team_name}
                      {entry.desempate && (
                        <span className="tiebreak-tag" title={`desempate por ${entry.desempate}`}>
                          {" "}
                          ⚖
                        </span>
                      )}
                    </td>
                    <td className="points">{entry.pontos}</td>
                    <td className="hide-narrow">{entry.vitorias}</td>
                    <td className="hide-narrow">{entry.empates}</td>
                    <td className="hide-narrow">{entry.derrotas}</td>
                    <td className="hide-narrow">{entry.pro}</td>
                    <td className="hide-narrow">{entry.contra}</td>
                    <td>{entry.saldo}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {comDesempate.length > 0 && (
            <p className="meta">
              ⚖ desempate:{" "}
              {comDesempate.map((e, i) => (
                <span key={e.team_id}>
                  {i > 0 && "; "}
                  {e.team_name} por {e.desempate}
                </span>
              ))}
            </p>
          )}
          <p className="meta">
            Atualizada às {formatTime(new Date(dataUpdatedAt).toISOString())}
            {qualifyCount !== undefined && qualifyCount > 1 && (
              <> · os {qualifyCount} primeiros avançam ao mata-mata</>
            )}
            {qualifyCount === 1 && <> · o líder ao fim da última rodada é o campeão</>}
            {" · "}desempate: pontos → confronto direto → saldo → pró → vitórias → sorteio
          </p>
        </>
      )}
    </div>
  );
}
