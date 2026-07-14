import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useMemo } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import {
  Championship,
  ChampionshipFormat,
  descartarConfrontos,
  gerarConfrontos,
  iniciarCampeonato,
  reabrirCampeonato,
  sortearCampeonato,
} from "../api";
import { Bracket } from "../components/Bracket";
import { MatchList } from "../components/MatchCard";
import { EnrollTeamForm } from "../components/OrganizerForms";
import { StandingsPanel } from "../components/StandingsPanel";
import { CHAMPIONSHIP_STATUS_LABEL } from "../components/TournamentGrid";
import { buildGroupLabels, sortMatches, useChampionships, useEnrollments, useMatches } from "../data";
import { formatDate, formatDateTime } from "../format";
import { useOrganizer } from "../organizer";
import { Skeleton } from "../ui/Skeleton";
import { useToast } from "../ui/toast";

const FORMAT_LABEL: Record<ChampionshipFormat, string> = {
  GRUPOS_PLAYOFFS: "Grupos + Playoffs",
  PLAYOFFS: "Playoffs direto",
  PONTOS_CORRIDOS: "Pontos corridos",
};

const MIN_TEAMS: Record<ChampionshipFormat, number> = {
  GRUPOS_PLAYOFFS: 6,
  PLAYOFFS: 2,
  PONTOS_CORRIDOS: 3,
};

const STEPS = ["Inscrições", "Sorteio", "Competição", "Campeão"];
const STEP_BY_STATUS: Record<Championship["status"], number> = {
  ABERTO: 0,
  SORTEADO: 1,
  EM_ANDAMENTO: 2,
  ENCERRADO: 3,
};

function Stepper({ status }: { status: Championship["status"] }) {
  const current = STEP_BY_STATUS[status];
  return (
    <ol className="stepper" aria-label="Etapa do torneio">
      {STEPS.map((label, index) => (
        <li key={label} className={index < current ? "done" : index === current ? "current" : ""}>
          {label}
        </li>
      ))}
    </ol>
  );
}

/** Painel de operação do organizador: sortear, re-sortear, reabrir e iniciar. */
function SetupPanel({
  championship,
  confirmedTeams,
}: {
  championship: Championship;
  confirmedTeams: { team_id: string; name: string }[];
}) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["championships"] });
    queryClient.invalidateQueries({ queryKey: ["matches"] });
  };

  const sortear = useMutation({
    mutationFn: async () => {
      await gerarConfrontos(championship.id, championship.formato, confirmedTeams);
      return sortearCampeonato(championship.id);
    },
    onSuccess: () => {
      refresh();
      toast("success", "Confrontos sorteados — revise e inicie o torneio");
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const iniciar = useMutation({
    mutationFn: () => iniciarCampeonato(championship.id),
    onSuccess: () => {
      refresh();
      toast("success", "Torneio iniciado — boa competição!");
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const reabrir = useMutation({
    mutationFn: async () => {
      await descartarConfrontos(championship.id);
      return reabrirCampeonato(championship.id);
    },
    onSuccess: () => {
      refresh();
      toast("success", "Inscrições reabertas — o sorteio foi descartado");
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const minimo = MIN_TEAMS[championship.formato];
  const faltam = Math.max(0, minimo - confirmedTeams.length);
  const busy = sortear.isPending || iniciar.isPending || reabrir.isPending;

  if (championship.status === "ABERTO") {
    return (
      <div className="panel setup-panel">
        <h2>Inscrições abertas</h2>
        <p className="prose">
          {confirmedTeams.length} de {minimo} time{minimo === 1 ? "" : "s"} mínimo{minimo === 1 ? "" : "s"}{" "}
          confirmado{confirmedTeams.length === 1 ? "" : "s"} para o formato{" "}
          <strong>{FORMAT_LABEL[championship.formato]}</strong>.
        </p>
        <div className="match-actions">
          <button type="button" disabled={faltam > 0 || busy} onClick={() => sortear.mutate()}>
            {sortear.isPending ? "Sorteando…" : "🔀 Sortear confrontos"}
          </button>
          {faltam > 0 && (
            <span className="hint">
              faltam {faltam} time{faltam === 1 ? "" : "s"} confirmado{faltam === 1 ? "" : "s"}
            </span>
          )}
        </div>
      </div>
    );
  }

  if (championship.status === "SORTEADO") {
    return (
      <div className="panel setup-panel">
        <h2>Sorteio realizado</h2>
        <p className="prose">
          Revise os confrontos nas abas abaixo. Ao iniciar, inscrições e chaveamento ficam travados.
        </p>
        <div className="match-actions">
          <button type="button" className="ghost" disabled={busy} onClick={() => sortear.mutate()}>
            {sortear.isPending ? "Sorteando…" : "🔀 Sortear novamente"}
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={() => {
              if (window.confirm("Inscrições e chaveamento ficam travados. Iniciar o torneio?")) {
                iniciar.mutate();
              }
            }}
          >
            {iniciar.isPending ? "Iniciando…" : "▶ Iniciar torneio"}
          </button>
        </div>
        <button type="button" className="link-button" disabled={busy} onClick={() => reabrir.mutate()}>
          reabrir inscrições (descarta o sorteio)
        </button>
      </div>
    );
  }

  return null;
}

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

  const temChaveamento = championship?.formato !== "PONTOS_CORRIDOS";
  const temClassificacao = championship?.formato !== "PLAYOFFS";

  const tabs = [
    { id: "visao", label: "Visão geral" },
    { id: "partidas", label: "Partidas" },
    ...(temClassificacao ? [{ id: "classificacao", label: "Classificação" }] : []),
    ...(temChaveamento ? [{ id: "chaveamento", label: "Chaveamento" }] : []),
    { id: "times", label: "Times" },
    { id: "jogadores", label: "Jogadores" },
  ];

  const activeTab = tabs.find((tab) => tab.id === searchParams.get("tab"))?.id ?? "visao";

  const setTab = (tab: string) => {
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
          {FORMAT_LABEL[championship.formato]} · criado em {formatDate(championship.created_at)} ·{" "}
          {confirmedTeams.length} time{confirmedTeams.length === 1 ? "" : "s"} confirmado
          {confirmedTeams.length === 1 ? "" : "s"} · {matches.length} partida{matches.length === 1 ? "" : "s"}
        </p>
      </div>

      <Stepper status={championship.status} />

      {championship.status === "ENCERRADO" && championship.campeao_nome && (
        <div className="champion-banner">
          🏆 Campeão: <strong>{championship.campeao_nome}</strong>
        </div>
      )}

      {organizer && <SetupPanel championship={championship} confirmedTeams={confirmedTeams} />}

      {!organizer && championship.status === "ABERTO" && (
        <div className="panel">
          <p className="prose">
            Inscrições em andamento — {confirmedTeams.length} time{confirmedTeams.length === 1 ? "" : "s"}{" "}
            confirmado{confirmedTeams.length === 1 ? "" : "s"}. Os confrontos aparecem aqui após o sorteio.
          </p>
        </div>
      )}

      <div className="tabs" role="tablist">
        {tabs.map((tab) => (
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
              {upcoming.length === 0 && <p className="empty">Nenhuma partida agendada.</p>}
              {upcoming[0] && (
                <>
                  <MatchList matches={[upcoming[0]]} />
                  <p className="meta">início {formatDateTime(upcoming[0].scheduled_at)}</p>
                </>
              )}
            </section>
            {temClassificacao ? (
              <StandingsPanel groupId={selectedGroup} title="Classificação" />
            ) : (
              <section className="panel">
                <h2>Chaveamento</h2>
                <p className="prose">
                  Mata-mata em andamento — acompanhe os confrontos na aba{" "}
                  <button type="button" className="link-button" onClick={() => setTab("chaveamento")}>
                    Chaveamento
                  </button>
                  .
                </p>
              </section>
            )}
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
        <section className="panel">
          <h2>Todas as partidas</h2>
          <MatchList
            matches={matches}
            emptyMessage={
              championship.status === "ABERTO"
                ? "As partidas são geradas pelo sorteio, quando as inscrições fecharem."
                : "Nenhuma partida ainda."
            }
          />
        </section>
      )}

      {activeTab === "classificacao" && temClassificacao && (
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

      {activeTab === "chaveamento" && temChaveamento && (
        <section className="panel">
          <h2>Chaveamento</h2>
          {championship.status === "ABERTO" ? (
            <p className="empty">O chaveamento é montado no sorteio, quando as inscrições fecharem.</p>
          ) : (
            <Bracket matches={matches} formato={championship.formato} teamCount={confirmedTeams.length} />
          )}
        </section>
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
          {organizer && championship.status === "ABERTO" && (
            <div className="panel">
              <EnrollTeamForm championshipId={championship.id} />
            </div>
          )}
          <section className="panel">
            <h2>Times inscritos</h2>
            {enrollments.length === 0 && (
              <p className="empty">
                Nenhum time inscrito ainda.
                {organizer && championship.status === "ABERTO" ? " Inscreva o primeiro no formulário acima." : ""}
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
