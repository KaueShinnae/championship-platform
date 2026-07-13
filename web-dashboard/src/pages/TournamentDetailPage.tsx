import { useMemo } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { MatchList } from "../components/MatchCard";
import { EnrollTeamForm, ScheduleMatchForm } from "../components/OrganizerForms";
import { StandingsPanel } from "../components/StandingsPanel";
import { buildGroupLabels, sortMatches, useChampionships, useEnrollments, useMatches } from "../data";
import { formatDate, formatDateTime } from "../format";
import { useOrganizer } from "../organizer";
import { Skeleton } from "../ui/Skeleton";
import { CHAMPIONSHIP_STATUS_LABEL } from "./TournamentsPage";

const TABS = [
  { id: "visao", label: "Visão geral" },
  { id: "partidas", label: "Partidas" },
  { id: "classificacao", label: "Classificação" },
  { id: "times", label: "Times" },
  { id: "jogadores", label: "Jogadores" },
] as const;

type TabId = (typeof TABS)[number]["id"];

export function TournamentDetailPage() {
  const { championshipId } = useParams<{ championshipId: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const organizer = useOrganizer();

  const { data: championships = [], isLoading: loadingChampionships } = useChampionships();
  const { data: enrollments = [] } = useEnrollments(championshipId ?? null);
  const { data: allMatches = [] } = useMatches();

  const championship = championships.find((entry) => entry.id === championshipId);
  const matches = useMemo(
    () => sortMatches(allMatches.filter((match) => match.championship_id === championshipId)),
    [allMatches, championshipId],
  );

  const groupLabels = buildGroupLabels(allMatches);
  const groups = [...new Set(matches.map((match) => match.group_id).filter((id): id is string => id !== null))];

  const requestedGroup = searchParams.get("grupo");
  const selectedGroup = requestedGroup && groups.includes(requestedGroup) ? requestedGroup : groups[0] ?? null;

  // grupo unico por campeonato no MVP: reusa o grupo das partidas
  // existentes ou deriva um novo id estavel para o primeiro agendamento
  const scheduleGroupId = useMemo(() => groups[0] ?? crypto.randomUUID(), [groups[0]]);

  const activeTab: TabId = (TABS.find((tab) => tab.id === searchParams.get("tab"))?.id ?? "visao") as TabId;

  const setTab = (tab: TabId) => {
    setSearchParams(tab === "visao" ? {} : { tab }, { replace: true });
  };

  if (loadingChampionships) {
    return (
      <div className="panel">
        <Skeleton lines={4} />
      </div>
    );
  }

  if (!championship) {
    return (
      <>
        <p className="breadcrumb">
          <Link to="/torneios">← Torneios</Link>
        </p>
        <div className="banner-error">Torneio não encontrado.</div>
      </>
    );
  }

  const confirmedTeams = enrollments
    .filter((enrollment) => enrollment.status === "CONFIRMADA")
    .map((enrollment) => ({ team_id: enrollment.time_id, name: enrollment.time_nome }));
  const live = matches.filter((match) => match.status === "EM_ANDAMENTO");
  const upcoming = matches.filter((match) => match.status === "AGENDADA");
  const finished = matches.filter((match) => match.status === "FINALIZADA");

  return (
    <>
      <p className="breadcrumb">
        <Link to="/torneios">← Torneios</Link>
      </p>

      <div className="page-header">
        <div className="page-title-row">
          <h2 className="page-title">{championship.nome}</h2>
          <span className={`badge championship-${championship.status.toLowerCase()}`}>
            {CHAMPIONSHIP_STATUS_LABEL[championship.status]}
          </span>
        </div>
        <p className="subtitle">
          criado em {formatDate(championship.created_at)} · {confirmedTeams.length} time
          {confirmedTeams.length === 1 ? "" : "s"} confirmado{confirmedTeams.length === 1 ? "" : "s"} ·{" "}
          {matches.length} partida{matches.length === 1 ? "" : "s"}
        </p>
      </div>

      <div className="tabs" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            className={`tab ${activeTab === tab.id ? "active" : ""}`}
            onClick={() => setTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "visao" && (
        <>
          {live.length > 0 && (
            <section className="panel">
              <h2>● Ao vivo agora</h2>
              <MatchList matches={live} />
            </section>
          )}
          <div className="two-col">
            <section className="panel">
              <h2>Próxima partida</h2>
              {upcoming.length === 0 && (
                <p className="empty">
                  Nenhuma partida agendada.
                  {organizer && " Use a aba Partidas para agendar."}
                </p>
              )}
              {upcoming[0] && (
                <>
                  <MatchList matches={[upcoming[0]]} />
                  <p className="meta">início {formatDateTime(upcoming[0].scheduled_at)}</p>
                </>
              )}
            </section>
            <StandingsPanel groupId={selectedGroup} title="Classificação" />
          </div>
          {finished.length > 0 && (
            <section className="panel">
              <h2>Últimos resultados</h2>
              <MatchList matches={finished.slice(0, 5)} />
            </section>
          )}
        </>
      )}

      {activeTab === "partidas" && (
        <>
          {organizer && (
            <div className="panel">
              <ScheduleMatchForm championshipId={championship.id} groupId={scheduleGroupId} teams={confirmedTeams} />
            </div>
          )}
          <section className="panel">
            <h2>Todas as partidas</h2>
            <MatchList
              matches={matches}
              emptyMessage={
                organizer
                  ? "Nenhuma partida ainda — agende a primeira no formulário acima."
                  : "Nenhuma partida agendada ainda."
              }
            />
          </section>
        </>
      )}

      {activeTab === "classificacao" && (
        <>
          {groups.length > 1 && (
            <div className="tabs sub" role="tablist">
              {groups.map((groupId) => (
                <button
                  key={groupId}
                  type="button"
                  role="tab"
                  aria-selected={selectedGroup === groupId}
                  className={`tab ${selectedGroup === groupId ? "active" : ""}`}
                  onClick={() => setSearchParams({ tab: "classificacao", grupo: groupId }, { replace: true })}
                >
                  {groupLabels.get(groupId) ?? "Grupo"}
                </button>
              ))}
            </div>
          )}
          <StandingsPanel
            groupId={selectedGroup}
            title={groups.length > 1 ? groupLabels.get(selectedGroup ?? "") ?? "Classificação" : "Classificação"}
          />
        </>
      )}

      {activeTab === "jogadores" && (
        <section className="panel">
          <h2>Jogadores do torneio</h2>
          {enrollments.length === 0 && <p className="empty">Nenhum jogador ainda — inscreva um time primeiro.</p>}
          {enrollments.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th className="left">Jogador</th>
                  <th className="left">Time</th>
                </tr>
              </thead>
              <tbody>
                {enrollments.flatMap((enrollment) =>
                  enrollment.jogadores.map((jogador) => (
                    <tr key={`${enrollment.inscricao_id}-${jogador.id}`}>
                      <td className="left">{jogador.nome}</td>
                      <td className="left">{enrollment.time_nome}</td>
                    </tr>
                  )),
                )}
              </tbody>
            </table>
          )}
        </section>
      )}

      {activeTab === "times" && (
        <>
          {organizer && (
            <div className="panel">
              <EnrollTeamForm championshipId={championship.id} />
            </div>
          )}
          <section className="panel">
            <h2>Times inscritos</h2>
            {enrollments.length === 0 && (
              <p className="empty">
                Nenhum time inscrito ainda.
                {organizer ? " Inscreva o primeiro no formulário acima." : ""}
              </p>
            )}
            <ul className="enrollments">
              {enrollments.map((enrollment) => (
                <li key={enrollment.inscricao_id}>
                  <div className="enrollment-header">
                    <strong>{enrollment.time_nome}</strong>
                    <span className={`badge ${enrollment.status.toLowerCase()}`}>
                      {enrollment.status === "CONFIRMADA" ? "✓ confirmada" : "⏳ aguardando confirmação"}
                    </span>
                  </div>
                  <span className="meta">{enrollment.jogadores.map((jogador) => jogador.nome).join(" · ")}</span>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}
    </>
  );
}
