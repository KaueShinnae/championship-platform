import { useQueries } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { fetchStandings } from "../api";
import { MatchList } from "../components/MatchCard";
import { TournamentGrid } from "../components/TournamentGrid";
import { buildGroupLabels, sortMatches, useAllEnrollments, useChampionships, useMatches } from "../data";
import { useOrganizer } from "../organizer";
import { Skeleton } from "../ui/Skeleton";

function StatTile({ label, value, accent = false }: { label: string; value: number; accent?: boolean }) {
  return (
    <div className={`stat-tile ${accent && value > 0 ? "accent" : ""}`}>
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
    </div>
  );
}

function GroupLeaders() {
  const { data: matches = [] } = useMatches();
  const { data: championships = [] } = useChampionships();

  const groupLabels = buildGroupLabels(matches);
  const groupIds = [...groupLabels.keys()].slice(0, 6);
  const championshipNames = new Map(championships.map((championship) => [championship.id, championship.nome]));
  const groupChampionship = new Map(
    matches.filter((match) => match.group_id).map((match) => [match.group_id!, match.championship_id]),
  );

  const results = useQueries({
    queries: groupIds.map((groupId) => ({
      queryKey: ["standings", groupId],
      queryFn: () => fetchStandings(groupId),
    })),
  });

  const leaders = groupIds.flatMap((groupId, index) => {
    const leader = results[index]?.data?.standings[0];
    if (!leader) return [];
    return [{ groupId, leader }];
  });

  if (leaders.length === 0) {
    return <p className="empty">Os líderes aparecem aqui quando os primeiros resultados forem processados.</p>;
  }

  return (
    <ul className="leaders">
      {leaders.map(({ groupId, leader }) => {
        const championshipId = groupChampionship.get(groupId);
        return (
          <li key={groupId}>
            <span className="leader-name">{leader.team_name}</span>
            <span className="meta">
              {championshipId && championshipNames.get(championshipId)} · {groupLabels.get(groupId)} · {leader.points} pts
            </span>
          </li>
        );
      })}
    </ul>
  );
}

export function HomePage() {
  const organizer = useOrganizer();
  const { data: matches = [], isLoading: loadingMatches, isError } = useMatches();
  const { data: championships = [] } = useChampionships();
  const { byChampionship } = useAllEnrollments();

  // Visitante não vê a visão geral agregada (isso é gestão): ele escolhe um
  // torneio e acompanha tudo dentro da página daquele torneio.
  if (!organizer) {
    return (
      <>
        <div className="page-header">
          <h2 className="page-title">Início</h2>
          <p className="subtitle">Escolha um torneio para acompanhar partidas, times e classificação.</p>
        </div>

        {isError && (
          <div className="banner-error">
            Não foi possível falar com os serviços — rode <code>npm run dev</code> na raiz do projeto.
          </div>
        )}

        <TournamentGrid emptyMessage="Nenhum torneio disponível ainda. Volte em breve!" />
      </>
    );
  }

  const championshipNames = new Map(championships.map((championship) => [championship.id, championship.nome]));
  const confirmedTeams = byChampionship.reduce(
    (total, { enrollments }) => total + enrollments.filter((enrollment) => enrollment.status === "CONFIRMADA").length,
    0,
  );

  const live = sortMatches(matches.filter((match) => match.status === "EM_ANDAMENTO"));
  const upcoming = sortMatches(matches.filter((match) => match.status === "AGENDADA")).slice(0, 5);
  const recent = sortMatches(matches.filter((match) => match.status === "FINALIZADA")).slice(0, 5);

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">Início</h2>
        <p className="subtitle">Visão geral de tudo que está acontecendo nos seus campeonatos.</p>
      </div>

      {isError && (
        <div className="banner-error">
          Não foi possível falar com os serviços — rode <code>npm run dev</code> na raiz do projeto.
        </div>
      )}

      <div className="stat-row">
        <StatTile label="Torneios" value={championships.length} />
        <StatTile label="Times confirmados" value={confirmedTeams} />
        <StatTile label="Partidas" value={matches.length} />
        <StatTile label="Ao vivo" value={live.length} accent />
      </div>

      {live.length > 0 && (
        <section className="panel">
          <h2>● Ao vivo agora</h2>
          <MatchList matches={live} championshipNames={championshipNames} />
        </section>
      )}

      <div className="two-col">
        <section className="panel">
          <div className="panel-header">
            <h2>Próximas partidas</h2>
            <Link className="panel-link" to="/torneios">
              ver torneios →
            </Link>
          </div>
          {loadingMatches ? (
            <Skeleton lines={4} />
          ) : (
            <MatchList
              matches={upcoming}
              championshipNames={championshipNames}
              emptyMessage="Nenhuma partida agendada."
            />
          )}
        </section>

        <section className="panel">
          <div className="panel-header">
            <h2>Últimos resultados</h2>
            <Link className="panel-link" to="/torneios">
              ver torneios →
            </Link>
          </div>
          {loadingMatches ? (
            <Skeleton lines={4} />
          ) : (
            <MatchList
              matches={recent}
              championshipNames={championshipNames}
              emptyMessage="Nenhum resultado ainda — os placares aparecem aqui."
            />
          )}
        </section>
      </div>

      <section className="panel">
        <h2>Líderes por grupo</h2>
        <GroupLeaders />
      </section>
    </>
  );
}
