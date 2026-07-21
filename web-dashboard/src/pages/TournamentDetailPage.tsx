import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  addAdmin,
  aprovarInscricao,
  cancelarCampeonato,
  Championship,
  ChampionshipFormat,
  descartarConfrontos,
  desistirTime,
  editarCampeonato,
  editarTime,
  Enrollment,
  fetchAdmins,
  fetchBracketSlots,
  fetchConflitos,
  gerarConfrontos,
  iniciarCampeonato,
  Match,
  reabrirCampeonato,
  recusarInscricao,
  removeAdmin,
  removerInscricao,
  sortearCampeonato,
} from "../api";
import { useAuth } from "../auth";
import { AttentionPanel } from "../components/AttentionPanel";
import { Bracket, nomeDaRodada, totalRoundsDoFormato } from "../components/Bracket";
import { ExportButton } from "../components/ExportButton";
import { GroupsPanel } from "../components/GroupsPanel";
import { ManagementLog } from "../components/ManagementLog";
import { MatchList } from "../components/MatchCard";
import { EnrollTeamForm } from "../components/OrganizerForms";
import { ScheduleTools } from "../components/ScheduleTools";
import { StandingsPanel } from "../components/StandingsPanel";
import { CHAMPIONSHIP_STATUS_LABEL } from "../components/TournamentGrid";
import { buildGroupLabels, sortMatches, useChampionships, useEnrollments, useMatches } from "../data";
import { formatDate, formatDateTime } from "../format";
import { useState } from "react";
import { Skeleton } from "../ui/Skeleton";
import { toggleMyTeam, useMyTeam } from "../ui/myteam";
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
  CANCELADO: 0, // não renderizado — torneio cancelado mostra banner próprio
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

function ShareButton({ championship }: { championship: Championship }) {
  const toast = useToast();

  const share = async () => {
    const url = window.location.origin + `/torneios/${championship.id}`;
    const title = `${championship.nome} — acompanhe o torneio`;
    try {
      if (navigator.share) {
        await navigator.share({ title, url });
      } else {
        await navigator.clipboard.writeText(url);
        toast("success", "Link copiado — cole no grupo do time!");
      }
    } catch {
      // compartilhamento cancelado pelo usuário: nada a fazer
    }
  };

  return (
    <button type="button" className="ghost share-button" onClick={share}>
      🔗 Compartilhar
    </button>
  );
}

function SetupPanel({
  championship,
  confirmedTeams,
  onSorteado,
}: {
  championship: Championship;
  confirmedTeams: { team_id: string; name: string }[];
  onSorteado: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["championships"] });
    queryClient.invalidateQueries({ queryKey: ["matches"] });
    queryClient.invalidateQueries({ queryKey: ["bracket-slots", championship.id] });
  };

  const sortear = useMutation({
    mutationFn: async () => {
      await gerarConfrontos(championship.id, championship.formato, confirmedTeams, championship.disputa_terceiro);
      return sortearCampeonato(championship.id);
    },
    onSuccess: () => {
      refresh();
      toast("success", "Confrontos sorteados — revise e inicie o torneio");
      onSorteado();
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
        <p className="prose">Revise os confrontos e inicie o torneio quando estiver tudo certo.</p>
        <div className="match-actions">
          <button type="button" disabled={busy} onClick={() => iniciar.mutate()}>
            {iniciar.isPending ? "Iniciando…" : "▶ Iniciar torneio"}
          </button>
          <button
            type="button"
            className="ghost"
            disabled={busy}
            onClick={() => {
              if (window.confirm("Descarta o sorteio atual — todos os confrontos serão sorteados de novo. Continuar?")) {
                sortear.mutate();
              }
            }}
          >
            {sortear.isPending ? "Sorteando…" : "🔀 Sortear novamente"}
          </button>
        </div>
        <button
          type="button"
          className="link-button"
          disabled={busy}
          onClick={() => {
            if (window.confirm("Descarta o sorteio e reabre as inscrições. Continuar?")) {
              reabrir.mutate();
            }
          }}
        >
          reabrir inscrições (descarta o sorteio)
        </button>
      </div>
    );
  }

  return null;
}

function EnrollmentItem({
  enrollment,
  championship,
  canManage,
  isMyTeam,
  statusTexto,
  onRemove,
  removing,
  onWithdraw,
  withdrawing,
}: {
  enrollment: Enrollment;
  championship: Championship;
  canManage: boolean;
  isMyTeam: boolean;
  statusTexto: string;
  onRemove: () => void;
  removing: boolean;
  onWithdraw?: () => void;
  withdrawing?: boolean;
}) {
  const [editando, setEditando] = useState(false);
  const [nome, setNome] = useState(enrollment.time_nome);
  const [jogadores, setJogadores] = useState(enrollment.jogadores.map((j) => j.nome).join(", "));
  const queryClient = useQueryClient();
  const toast = useToast();

  const parsed = jogadores.split(",").map((j) => j.trim()).filter(Boolean);
  const podeEditar = canManage && championship.status === "ABERTO";

  const salvar = useMutation({
    mutationFn: () => editarTime(championship.id, enrollment.inscricao_id, nome.trim(), parsed),
    onSuccess: () => {
      toast("success", "Time atualizado");
      setEditando(false);
      queryClient.invalidateQueries({ queryKey: ["enrollments", championship.id] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  if (editando) {
    return (
      <li>
        <form
          className="org-form"
          onSubmit={(event) => {
            event.preventDefault();
            if (nome.trim() !== "" && parsed.length > 0) salvar.mutate();
          }}
        >
          <div className="row">
            <input value={nome} maxLength={100} onChange={(event) => setNome(event.target.value)} aria-label="nome do time" />
          </div>
          <div className="row">
            <input
              value={jogadores}
              onChange={(event) => setJogadores(event.target.value)}
              aria-label="integrantes separados por vírgula"
            />
            <button type="submit" disabled={nome.trim() === "" || parsed.length === 0 || salvar.isPending}>
              {salvar.isPending ? "Salvando…" : "Salvar"}
            </button>
            <button type="button" className="ghost" onClick={() => setEditando(false)}>
              Cancelar
            </button>
          </div>
        </form>
      </li>
    );
  }

  return (
    <li className={isMyTeam ? "my-team" : ""}>
      <div className="enrollment-header">
        <strong>
          <button
            type="button"
            className="star-button"
            title={isMyTeam ? "Deixar de seguir este time" : "Marcar como meu time"}
            onClick={() => toggleMyTeam(championship.id, { teamId: enrollment.time_id, name: enrollment.time_nome })}
          >
            {isMyTeam ? "★" : "☆"}
          </button>
          {enrollment.time_nome}
        </strong>
        <span className={`badge ${enrollment.status.toLowerCase()}`}>{statusTexto}</span>
        {podeEditar && (
          <button type="button" className="link-button" onClick={() => setEditando(true)}>
            editar
          </button>
        )}
        {podeEditar && (
          <button
            type="button"
            className="link-button"
            disabled={removing}
            onClick={() => {
              if (window.confirm(`Remover "${enrollment.time_nome}" do torneio?`)) onRemove();
            }}
          >
            remover
          </button>
        )}
        {onWithdraw && (
          <button
            type="button"
            className="link-button danger"
            disabled={withdrawing}
            onClick={() => {
              if (
                window.confirm(
                  `Registrar a desistência de "${enrollment.time_nome}"? Todas as partidas restantes da equipe viram W.O. a favor dos adversários.`,
                )
              ) {
                onWithdraw();
              }
            }}
          >
            desistiu
          </button>
        )}
      </div>
      <span className="meta">{enrollment.jogadores.map((jogador) => jogador.nome).join(" · ")}</span>
    </li>
  );
}

function AdminsPanel({ championship }: { championship: Championship }) {
  const [email, setEmail] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();
  const { data: admins = [] } = useQuery({
    queryKey: ["admins", championship.id],
    queryFn: () => fetchAdmins(championship.id),
    refetchInterval: false,
  });

  const add = useMutation({
    mutationFn: () => addAdmin(championship.id, email.trim()),
    onSuccess: (admin) => {
      toast("success", `${admin.nome} agora administra este torneio`);
      setEmail("");
      queryClient.invalidateQueries({ queryKey: ["admins", championship.id] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const remove = useMutation({
    mutationFn: (adminId: string) => removeAdmin(championship.id, adminId),
    onSuccess: () => {
      toast("success", "Administrador removido");
      queryClient.invalidateQueries({ queryKey: ["admins", championship.id] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  return (
    <div className="panel">
      <h2>Administradores</h2>
      <p className="meta">
        Quem você adicionar aqui pode operar este torneio (inscrever times, sortear, apurar) — mas não delegar.
      </p>
      {admins.length > 0 && (
        <ul className="roster">
          {admins.map((admin) => (
            <li key={admin.id}>
              {admin.nome} <span className="meta">({admin.email})</span>{" "}
              <button
                type="button"
                className="link-button"
                disabled={remove.isPending}
                onClick={() => {
                  if (window.confirm(`${admin.nome} deixa de administrar este torneio. Continuar?`)) {
                    remove.mutate(admin.id);
                  }
                }}
              >
                remover
              </button>
            </li>
          ))}
        </ul>
      )}
      <form
        className="org-form"
        onSubmit={(event) => {
          event.preventDefault();
          if (email.trim() !== "") add.mutate();
        }}
      >
        <div className="row">
          <input
            type="email"
            placeholder="Email da conta a delegar"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <button type="submit" disabled={email.trim() === "" || add.isPending}>
            {add.isPending ? "Adicionando…" : "Adicionar"}
          </button>
        </div>
      </form>
    </div>
  );
}

function ManageTournamentPanel({ championship }: { championship: Championship }) {
  const [editando, setEditando] = useState(false);
  const [nome, setNome] = useState(championship.nome);
  const [aprovacao, setAprovacao] = useState(championship.aprovacao_inscricoes);
  const [minInt, setMinInt] = useState(championship.min_integrantes?.toString() ?? "");
  const [maxInt, setMaxInt] = useState(championship.max_integrantes?.toString() ?? "");
  const [terceiro, setTerceiro] = useState(championship.disputa_terceiro);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const toast = useToast();

  const temMataMata = championship.formato !== "PONTOS_CORRIDOS";
  const podeEditar = championship.status === "ABERTO";

  const salvar = useMutation({
    mutationFn: () =>
      editarCampeonato(championship.id, nome.trim(), {
        aprovacaoInscricoes: aprovacao,
        minIntegrantes: minInt === "" ? null : Number(minInt),
        maxIntegrantes: maxInt === "" ? null : Number(maxInt),
        disputaTerceiro: terceiro,
      }),
    onSuccess: () => {
      toast("success", "Torneio atualizado");
      setEditando(false);
      queryClient.invalidateQueries({ queryKey: ["championships"] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  const cancelar = useMutation({
    mutationFn: () => cancelarCampeonato(championship.id),
    onSuccess: () => {
      toast("success", "Torneio cancelado");
      queryClient.invalidateQueries({ queryKey: ["championships"] });
      navigate("/torneios");
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  return (
    <div className="panel manage-panel">
      <h2>Gerenciar torneio</h2>
      {podeEditar && !editando && (
        <p className="meta">Edite nome, aprovação e limites de equipe enquanto as inscrições estão abertas.</p>
      )}
      {!podeEditar && championship.status !== "ENCERRADO" && (
        <p className="meta">Os dados de criação só podem ser editados com as inscrições abertas.</p>
      )}

      {podeEditar && editando && (
        <form
          className="org-form"
          onSubmit={(event) => {
            event.preventDefault();
            if (nome.trim() !== "") salvar.mutate();
          }}
        >
          <div className="row">
            <input value={nome} maxLength={100} onChange={(e) => setNome(e.target.value)} aria-label="nome do torneio" />
          </div>
          <div className="team-size-row">
            <label>
              Mín. integrantes
              <input type="number" min={1} value={minInt} onChange={(e) => setMinInt(e.target.value)} placeholder="—" />
            </label>
            <label>
              Máx. integrantes
              <input type="number" min={1} value={maxInt} onChange={(e) => setMaxInt(e.target.value)} placeholder="—" />
            </label>
          </div>
          <label className="checkbox-row">
            <input type="checkbox" checked={aprovacao} onChange={(e) => setAprovacao(e.target.checked)} />
            Inscrições de capitães passam pela minha aprovação
          </label>
          {temMataMata && (
            <label className="checkbox-row">
              <input type="checkbox" checked={terceiro} onChange={(e) => setTerceiro(e.target.checked)} />
              Disputa de 3º lugar
            </label>
          )}
          <div className="match-actions">
            <button type="submit" disabled={nome.trim() === "" || salvar.isPending}>
              {salvar.isPending ? "Salvando…" : "Salvar alterações"}
            </button>
            <button type="button" className="ghost" onClick={() => setEditando(false)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {podeEditar && !editando && (
        <div className="match-actions">
          <button type="button" className="ghost" onClick={() => setEditando(true)}>
            ✎ Editar torneio
          </button>
        </div>
      )}

      {championship.is_dono && (
        <button
          type="button"
          className="link-button danger"
          disabled={cancelar.isPending}
          onClick={() => {
            if (
              window.confirm(
                `Cancelar o torneio "${championship.nome}"? Isso é definitivo e apaga as partidas e a classificação. Não dá para desfazer.`,
              )
            ) {
              cancelar.mutate();
            }
          }}
        >
          cancelar torneio (definitivo)
        </button>
      )}
    </div>
  );
}

export function TournamentDetailPage() {
  const { championshipId } = useParams<{ championshipId: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const user = useAuth();
  const queryClient = useQueryClient();
  const toast = useToast();

  const { data: championships = [], isLoading: loadingChampionships } = useChampionships();
  const { data: enrollments = [] } = useEnrollments(championshipId ?? null);
  const { data: allMatches = [] } = useMatches();
  const myTeam = useMyTeam(championshipId);

  const championship = championships.find((entry) => entry.id === championshipId);
  const canManage = championship?.can_manage ?? false;

  const refreshEnrollments = () => {
    queryClient.invalidateQueries({ queryKey: ["enrollments", championshipId] });
  };

  // aprovação/recusa (organizador) e cancelamento (capitão) de inscrições
  const aprovar = useMutation({
    mutationFn: (inscricaoId: string) => aprovarInscricao(championshipId!, inscricaoId),
    onSuccess: (enrollment) => {
      toast("success", `"${enrollment.time_nome}" aprovado — confirmando inscrição`);
      refreshEnrollments();
    },
    onError: (error) => toast("error", (error as Error).message),
  });
  const recusar = useMutation({
    mutationFn: (inscricaoId: string) => recusarInscricao(championshipId!, inscricaoId),
    onSuccess: (enrollment) => {
      toast("success", `Inscrição de "${enrollment.time_nome}" recusada`);
      refreshEnrollments();
    },
    onError: (error) => toast("error", (error as Error).message),
  });
  const remover = useMutation({
    mutationFn: (inscricaoId: string) => removerInscricao(championshipId!, inscricaoId),
    onSuccess: () => {
      toast("success", "Inscrição removida");
      refreshEnrollments();
    },
    onError: (error) => toast("error", (error as Error).message),
  });
  const desistir = useMutation({
    mutationFn: (teamId: string) => desistirTime(championshipId!, teamId),
    onSuccess: (resultado) => {
      toast("success", `Desistência registrada — W.O. em ${resultado.walkovers} partida(s)`);
      queryClient.invalidateQueries({ queryKey: ["matches"] });
      queryClient.invalidateQueries({ queryKey: ["championships"] });
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  // inscrições de capitão aguardando decisão do organizador
  const pendentesDeAprovacao = enrollments.filter(
    (enrollment) => enrollment.status === "PENDENTE" && enrollment.capitao_usuario_id !== null,
  );
  // a inscrição pendente do próprio usuário (como capitão) neste torneio
  const minhaInscricaoPendente = user
    ? enrollments.find(
        (enrollment) => enrollment.capitao_usuario_id === user.id && enrollment.status === "PENDENTE",
      )
    : undefined;

  const statusDaInscricao = (enrollment: Enrollment) => {
    if (enrollment.status === "CONFIRMADA") return "✓ confirmada";
    if (enrollment.status === "RECUSADA") return "✗ recusada";
    // no modo de entrada direta a pendência é só a latência da saga
    return enrollment.capitao_usuario_id !== null && championship?.aprovacao_inscricoes
      ? "⏳ aguardando aprovação"
      : "⏳ confirmando…";
  };

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
  const qualifyCount =
    championship?.formato === "GRUPOS_PLAYOFFS" ? 2 : championship?.formato === "PONTOS_CORRIDOS" ? 1 : undefined;

  // filtro persistido na URL: sobrevive a entrar na partida e voltar
  const somentePendentes = searchParams.get("filtro") === "pendentes";

  const { data: bracketSlots = [] } = useQuery({
    queryKey: ["bracket-slots", championshipId],
    queryFn: () => fetchBracketSlots(championshipId!),
    enabled: championshipId !== undefined && temChaveamento && championship !== undefined && championship.status !== "ABERTO",
  });

  // conflitos de horário (mesmo time em dois jogos) — só interessa a quem gerencia
  const { data: conflitos = [] } = useQuery({
    queryKey: ["conflitos", championshipId],
    queryFn: () => fetchConflitos(championshipId!),
    enabled: championshipId !== undefined && canManage,
  });

  const emOperacao = canManage && championship?.status === "EM_ANDAMENTO";
  const tabs = [
    { id: "visao", label: "Visão geral" },
    ...(emOperacao ? [{ id: "operacao", label: "● Operação do dia" }] : []),
    { id: "partidas", label: "Partidas" },
    ...(temClassificacao ? [{ id: "classificacao", label: "Classificação" }] : []),
    ...(temChaveamento ? [{ id: "chaveamento", label: "Chaveamento" }] : []),
    {
      id: "times",
      label:
        canManage && championship?.aprovacao_inscricoes && pendentesDeAprovacao.length > 0
          ? `Times (${pendentesDeAprovacao.length})`
          : "Times",
    },
    { id: "jogadores", label: "Participantes" },
    ...(canManage ? [{ id: "gestao", label: "Gestão" }] : []),
  ];

  const activeTab = tabs.find((tab) => tab.id === searchParams.get("tab"))?.id ?? "visao";

  const setTab = (tab: string, extra?: Record<string, string>) => {
    setSearchParams(tab === "visao" && !extra ? {} : { tab, ...extra }, { replace: true });
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

  if (championship.status === "CANCELADO") {
    return (
      <>
        <p className="breadcrumb">
          <Link to="/torneios">← Torneios</Link>
        </p>
        <div className="page-header">
          <div className="page-title-row">
            <h2 className="page-title">{championship.nome}</h2>
            <span className="badge championship-cancelado">Cancelado</span>
          </div>
        </div>
        <div className="panel">
          <p className="prose">
            Este torneio foi <strong>cancelado</strong> pelo organizador. As partidas e a classificação foram
            removidas. Se precisar, crie um novo torneio a partir de{" "}
            <Link to="/torneios/novo">Novo torneio</Link>.
          </p>
        </div>
      </>
    );
  }

  const confirmedTeams = enrollments
    .filter((enrollment) => enrollment.status === "CONFIRMADA")
    .map((enrollment) => ({ team_id: enrollment.time_id, name: enrollment.time_nome }));
  const live = matches.filter((match) => match.status === "EM_ANDAMENTO");
  const upcoming = matches.filter((match) => match.status === "AGENDADA");
  const finished = matches.filter((match) => match.status === "FINALIZADA");

  // contexto da fase em cada card ("Grupo B · Rodada 2", "Semifinais"…)
  const totalRounds = totalRoundsDoFormato(championship.formato, Math.max(confirmedTeams.length, 2));
  const phaseLabels = new Map<string, string>();
  for (const match of matches) {
    if (match.group_id) {
      const grupo = groups.length > 1 ? groupLabels.get(match.group_id) ?? "Grupo" : "";
      const rodada = match.round !== null ? `Rodada ${match.round}` : "";
      const label = [grupo, rodada].filter(Boolean).join(" · ");
      if (label) phaseLabels.set(match.match_id, label);
    } else if (match.stage === "PLAYOFF" && match.round !== null) {
      phaseLabels.set(match.match_id, nomeDaRodada(match.round, totalRounds));
    }
  }

  const proximoJogoDoMeuTime = myTeam
    ? matches.find(
        (match) =>
          match.status !== "FINALIZADA" &&
          (match.home_team.team_id === myTeam.teamId || match.away_team.team_id === myTeam.teamId),
      )
    : undefined;

  // após o sorteio, leva direto à visualização de revisão do formato
  const aoSortear = () => {
    if (championship.formato === "PLAYOFFS") setTab("chaveamento");
    else if (championship.formato === "PONTOS_CORRIDOS") setTab("partidas");
    else setTab("visao");
  };

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
          <ShareButton championship={championship} />
          {canManage && (
            <a
              className="ghost share-button"
              href={`/torneios/${championship.id}/telao`}
              target="_blank"
              rel="noopener noreferrer"
              title="Abrir o painel de parede para projetar numa TV do ginásio"
            >
              📺 Abrir telão
            </a>
          )}
          {championship.status !== "ABERTO" && (
            <ExportButton
              championship={championship}
              matches={matches}
              enrollments={enrollments}
              groupLabels={groupLabels}
            />
          )}
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

      {canManage && (
        <AttentionPanel
          status={championship.status}
          aprovacoesPendentes={championship.aprovacao_inscricoes ? pendentesDeAprovacao.length : 0}
          aguardandoApuracao={upcoming}
          aoVivo={live}
          semHorario={upcoming.filter((match) => match.scheduled_at === null).length}
          conflitos={conflitos}
          onIr={setTab}
        />
      )}

      {canManage && (
        <SetupPanel championship={championship} confirmedTeams={confirmedTeams} onSorteado={aoSortear} />
      )}

      {!canManage && championship.status === "ABERTO" && (
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
          {championship.status === "SORTEADO" && championship.formato === "GRUPOS_PLAYOFFS" && (
            <GroupsPanel championshipId={championship.id} matches={matches} groupLabels={groupLabels} />
          )}
          {proximoJogoDoMeuTime && (
            <section className="panel my-team-panel">
              <h2>★ Próximo jogo do seu time</h2>
              <MatchList matches={[proximoJogoDoMeuTime]} phaseLabels={phaseLabels} />
            </section>
          )}
          {live.length > 0 && (
            <section className="panel">
              <h2>● Ao vivo agora</h2>
              <MatchList matches={live} phaseLabels={phaseLabels} />
            </section>
          )}
          <div className="two-col">
            <section className="panel">
              <h2>Próxima partida</h2>
              {upcoming.length === 0 && <p className="empty">Nenhuma partida agendada.</p>}
              {upcoming[0] && (
                <>
                  <MatchList matches={[upcoming[0]]} phaseLabels={phaseLabels} />
                  <p className="meta">
                    {upcoming[0].scheduled_at
                      ? `início ${formatDateTime(upcoming[0].scheduled_at)}`
                      : "horário a definir"}
                  </p>
                </>
              )}
            </section>
            {temClassificacao ? (
              <StandingsPanel
                groupId={selectedGroup}
                title="Classificação"
                qualifyCount={qualifyCount}
                highlightTeamId={myTeam?.teamId}
              />
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
              <MatchList matches={finished.slice(0, 5)} phaseLabels={phaseLabels} />
            </section>
          )}
        </>
      )}

      {activeTab === "operacao" && emOperacao && (
        <>
          <ScheduleTools championshipId={championship.id} conflitos={conflitos} />
          <section className="panel">
            <h2>● Ao vivo agora</h2>
            <MatchList
              matches={live}
              phaseLabels={phaseLabels}
              emptyMessage="Nenhuma partida em andamento — inicie a próxima abaixo."
            />
          </section>
          <section className="panel">
            <h2>A seguir</h2>
            <p className="meta">
              Partidas agendadas em ordem de horário — inicie, aponte o placar e encerre sem sair desta tela.
            </p>
            <MatchList
              matches={upcoming}
              phaseLabels={phaseLabels}
              emptyMessage="Nenhuma partida pendente — tudo apurado! 🎉"
            />
          </section>
        </>
      )}

      {activeTab === "partidas" && (
        <>
          {canManage && championship.status !== "ABERTO" && championship.status !== "ENCERRADO" && (
            <ScheduleTools championshipId={championship.id} conflitos={conflitos} />
          )}
          {canManage && matches.length > 0 && (
            <div className="tabs sub" role="tablist">
              <button
                type="button"
                role="tab"
                aria-selected={!somentePendentes}
                className={`tab ${!somentePendentes ? "active" : ""}`}
                onClick={() => setSearchParams({ tab: "partidas" }, { replace: true })}
              >
                Todas
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={somentePendentes}
                className={`tab ${somentePendentes ? "active" : ""}`}
                onClick={() => setSearchParams({ tab: "partidas", filtro: "pendentes" }, { replace: true })}
              >
                Pendentes de resultado
              </button>
            </div>
          )}
          {(() => {
            const visiveis = somentePendentes
              ? matches.filter((match) => match.status !== "FINALIZADA")
              : matches;

            if (visiveis.length === 0) {
              return (
                <section className="panel">
                  <h2>Partidas</h2>
                  <p className="empty">
                    {somentePendentes
                      ? "Nenhuma partida pendente — tudo apurado! 🎉"
                      : championship.status === "ABERTO"
                        ? "As partidas são geradas pelo sorteio, quando as inscrições fecharem."
                        : "Nenhuma partida ainda."}
                  </p>
                </section>
              );
            }

            // seções por grupo e por fase do mata-mata (a lista plana não
            // escala com a geração automática)
            const secoes: { titulo: string; itens: Match[] }[] = [];

            for (const groupId of groups) {
              const itensGrupo = visiveis.filter((match) => match.group_id === groupId);
              if (itensGrupo.length === 0) continue;
              const prefixo = groups.length > 1 ? `${groupLabels.get(groupId) ?? "Grupo"} · ` : "";
              const rodadasGrupo = [
                ...new Set(itensGrupo.map((m) => m.round).filter((r): r is number => r !== null)),
              ].sort((a, b) => a - b);
              if (rodadasGrupo.length > 0) {
                for (const rodada of rodadasGrupo) {
                  secoes.push({
                    titulo: `${prefixo}Rodada ${rodada}`,
                    itens: itensGrupo.filter((m) => m.round === rodada),
                  });
                }
                const semRodada = itensGrupo.filter((m) => m.round === null);
                if (semRodada.length > 0) secoes.push({ titulo: `${prefixo}Demais jogos`, itens: semRodada });
              } else {
                secoes.push({ titulo: groupLabels.get(groupId) ?? "Grupo", itens: itensGrupo });
              }
            }
            const rodadas = [
              ...new Set(
                visiveis
                  .filter((match) => match.stage === "PLAYOFF" && match.round !== null)
                  .map((match) => match.round!),
              ),
            ].sort((a, b) => a - b);
            for (const rodada of rodadas) {
              secoes.push({
                titulo: nomeDaRodada(rodada, totalRounds),
                itens: visiveis.filter((match) => match.stage === "PLAYOFF" && match.round === rodada),
              });
            }
            const outras = visiveis.filter((match) => !match.group_id && match.stage !== "PLAYOFF");
            if (outras.length > 0) secoes.push({ titulo: "Outras partidas", itens: outras });

            const unicaSecao = secoes.length === 1 ? secoes[0] : undefined;
            if (unicaSecao) {
              return (
                <section className="panel">
                  <h2>Todas as partidas</h2>
                  <MatchList matches={unicaSecao.itens} />
                </section>
              );
            }
            return secoes.map((secao) => (
              <section key={secao.titulo} className="panel">
                <h2>{secao.titulo}</h2>
                <MatchList matches={secao.itens} />
              </section>
            ));
          })()}
        </>
      )}

      {activeTab === "classificacao" && temClassificacao && (
        <>
          {championship.status === "SORTEADO" && championship.formato === "GRUPOS_PLAYOFFS" && (
            <GroupsPanel championshipId={championship.id} matches={matches} groupLabels={groupLabels} />
          )}
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
            qualifyCount={qualifyCount}
            highlightTeamId={myTeam?.teamId}
          />
        </>
      )}

      {activeTab === "chaveamento" && temChaveamento && (
        <section className="panel">
          <h2>Chaveamento</h2>
          {championship.status === "ABERTO" ? (
            <p className="empty">O chaveamento é montado no sorteio, quando as inscrições fecharem.</p>
          ) : (
            <Bracket
              matches={matches}
              formato={championship.formato}
              teamCount={confirmedTeams.length}
              slots={bracketSlots}
              myTeamId={myTeam?.teamId}
            />
          )}
        </section>
      )}

      {activeTab === "jogadores" && (
        <section className="panel">
          <h2>Participantes do torneio</h2>
          {enrollments.length === 0 && (
            <p className="empty">Nenhum participante ainda — as equipes aparecem primeiro.</p>
          )}
          {enrollments.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th className="left">Participante</th>
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

      {activeTab === "gestao" && canManage && (
        <>
          <ManageTournamentPanel championship={championship} />
          <ManagementLog championshipId={championship.id} />
        </>
      )}

      {activeTab === "times" && (
        <>
          {canManage && championship.is_dono && !championship.sem_dono && (
            <AdminsPanel championship={championship} />
          )}

          {canManage && championship.aprovacao_inscricoes && pendentesDeAprovacao.length > 0 && (
            <section className="panel approvals-panel">
              <h2>⏳ Inscrições aguardando sua aprovação</h2>
              <ul className="enrollments">
                {pendentesDeAprovacao.map((enrollment) => (
                  <li key={enrollment.inscricao_id}>
                    <div className="enrollment-header">
                      <strong>{enrollment.time_nome}</strong>
                      <span className="match-actions">
                        <button
                          type="button"
                          disabled={aprovar.isPending || recusar.isPending}
                          onClick={() => aprovar.mutate(enrollment.inscricao_id)}
                        >
                          ✓ Aprovar
                        </button>
                        <button
                          type="button"
                          className="ghost"
                          disabled={aprovar.isPending || recusar.isPending}
                          onClick={() => {
                            if (window.confirm(`Recusar a inscrição de "${enrollment.time_nome}"? O capitão poderá tentar de novo.`)) {
                              recusar.mutate(enrollment.inscricao_id);
                            }
                          }}
                        >
                          Recusar
                        </button>
                      </span>
                    </div>
                    <span className="meta">{enrollment.jogadores.map((jogador) => jogador.nome).join(" · ")}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {canManage && championship.status === "ABERTO" && (
            <div className="panel">
              <EnrollTeamForm championshipId={championship.id} />
            </div>
          )}

          {!canManage && championship.status === "ABERTO" && user && !minhaInscricaoPendente && (
            <div className="panel">
              <EnrollTeamForm
                championshipId={championship.id}
                title="Inscrever meu time"
                cta="Inscrever meu time"
                successMessage={(nome) =>
                  championship.aprovacao_inscricoes
                    ? `"${nome}" inscrito — aguardando aprovação do organizador`
                    : `"${nome}" inscrito no torneio!`
                }
                onEnrolled={(enrollment) =>
                  toggleMyTeam(championship.id, { teamId: enrollment.time_id, name: enrollment.time_nome })
                }
              />
            </div>
          )}

          {!canManage &&
            championship.status === "ABERTO" &&
            user &&
            minhaInscricaoPendente &&
            championship.aprovacao_inscricoes && (
              <div className="panel my-team-panel">
                <h2>Sua inscrição</h2>
                <p className="prose">
                  <strong>{minhaInscricaoPendente.time_nome}</strong> está aguardando a aprovação do organizador —
                  você acompanha o status aqui mesmo.
                </p>
                <div className="match-actions">
                  <button
                    type="button"
                    className="ghost"
                    disabled={remover.isPending}
                    onClick={() => {
                      if (window.confirm("Cancelar sua inscrição pendente? Você poderá se inscrever de novo.")) {
                        remover.mutate(minhaInscricaoPendente.inscricao_id);
                      }
                    }}
                  >
                    Cancelar inscrição
                  </button>
                </div>
              </div>
            )}

          {championship.status === "ABERTO" && !user && (
            <div className="panel my-team-panel">
              <h2>Seu time joga aqui?</h2>
              <p className="prose">Inscreva seu time e acompanhe a aprovação do organizador.</p>
              <div className="match-actions">
                <Link
                  to={`/conta?motivo=inscrever&torneio=${encodeURIComponent(championship.nome)}&voltar=${encodeURIComponent(
                    `/torneios/${championship.id}?tab=times`,
                  )}`}
                  className="button-link"
                >
                  Inscrever meu time
                </Link>
              </div>
            </div>
          )}

          {canManage && championship.status === "SORTEADO" && (
            <div className="panel">
              <p className="prose">
                Inscrições travadas pelo sorteio — para adicionar times,{" "}
                <button type="button" className="link-button" onClick={() => setTab("visao")}>
                  reabra as inscrições na Visão geral
                </button>
                .
              </p>
            </div>
          )}

          <section className="panel">
            <h2>Times inscritos</h2>
            {canManage && (
              <p className="meta">
                {championship.aprovacao_inscricoes
                  ? "Inscrições de capitães passam pela sua aprovação."
                  : "Entrada direta: times de capitães entram confirmados — remova indesejados enquanto as inscrições estiverem abertas."}
              </p>
            )}
            {enrollments.length === 0 && (
              <p className="empty">
                Nenhum time inscrito ainda.
                {canManage && championship.status === "ABERTO" ? " Inscreva o primeiro no formulário acima." : ""}
              </p>
            )}
            <ul className="enrollments">
              {enrollments.map((enrollment) => (
                <EnrollmentItem
                  key={enrollment.inscricao_id}
                  enrollment={enrollment}
                  championship={championship}
                  canManage={canManage}
                  isMyTeam={myTeam?.teamId === enrollment.time_id}
                  statusTexto={statusDaInscricao(enrollment)}
                  onRemove={() => remover.mutate(enrollment.inscricao_id)}
                  removing={remover.isPending}
                  onWithdraw={
                    canManage && championship.status === "EM_ANDAMENTO" && enrollment.status === "CONFIRMADA"
                      ? () => desistir.mutate(enrollment.time_id)
                      : undefined
                  }
                  withdrawing={desistir.isPending}
                />
              ))}
            </ul>
          </section>
        </>
      )}
    </>
  );
}
