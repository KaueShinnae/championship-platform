import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { FeedEntry, fetchEventFeed } from "../api";
import { useAuth } from "../auth";
import { buildGroupLabels, useChampionships, useMatches } from "../data";
import { formatTimestamp } from "../format";
import { Skeleton } from "../ui/Skeleton";

// Traducao dos eventos de dominio para leitura rapida; o nome tecnico
// (match.finished.v1 etc.) aparece sempre ao lado, pois esta tela e de
// rastreabilidade para o dono do torneio / suporte.
const EVENT_LABELS: Record<string, string> = {
  "championship.created.v1": "Campeonato criado",
  "team.registered.v1": "Time registrado",
  "enrollment.confirmed.v1": "Inscrição confirmada",
  "match.scheduled.v1": "Partida agendada",
  "match.started.v1": "Partida iniciada",
  "match.finished.v1": "Partida encerrada",
  "ranking.updated.v1": "Classificação atualizada",
};

function prettyJson(payload: string | null): string | null {
  if (!payload) return null;
  try {
    return JSON.stringify(JSON.parse(payload), null, 2);
  } catch {
    return payload;
  }
}

export function MonitoringPage() {
  const user = useAuth();
  const [championshipFilter, setChampionshipFilter] = useState("todos");

  const { data: matches = [] } = useMatches();
  const { data: championships = [], isLoading: loadingChampionships } = useChampionships();

  // tela de operação/suporte: restrita a quem gerencia pelo menos um torneio
  // (dono ou admin delegado) — para um capitão isso é só ruído e exposição
  const gerenciaAlgum = user !== null && championships.some((championship) => championship.can_manage);

  const { data: entries, isLoading } = useQuery({
    queryKey: ["event-feed"],
    queryFn: fetchEventFeed,
    enabled: gerenciaAlgum,
  });

  if (!user || (!loadingChampionships && !gerenciaAlgum)) {
    return (
      <>
        <div className="page-header">
          <h2 className="page-title">Monitoramento</h2>
          <p className="subtitle">Rastreabilidade dos eventos da plataforma.</p>
        </div>
        <div className="panel">
          <h2>Acesso restrito</h2>
          <p className="prose">
            {user ? (
              <>
                Esta área é para quem gerencia torneios (dono ou administrador delegado).{" "}
                <Link to="/torneios/novo">Crie um torneio</Link> para acessá-la.
              </>
            ) : (
              <>
                Esta área é para organizadores e suporte. Entre na sua conta na página{" "}
                <Link to="/conta">Conta</Link> para acessar.
              </>
            )}
          </p>
        </div>
      </>
    );
  }

  const matchById = new Map(matches.map((match) => [match.match_id, match]));
  const championshipByGroup = new Map(
    matches.filter((match) => match.group_id).map((match) => [match.group_id!, match.championship_id]),
  );
  const championshipNames = new Map(championships.map((championship) => [championship.id, championship.nome]));
  const groupLabels = buildGroupLabels(matches);

  // O aggregate_id de cada evento aponta para partida, grupo ou torneio —
  // resolve para nomes legiveis e permite filtrar o feed por torneio.
  const resolveContext = (entry: FeedEntry): { championshipId: string | null; label: string | null } => {
    const match = matchById.get(entry.aggregate_id);
    if (match) {
      return {
        championshipId: match.championship_id,
        label: `${match.home_team.name} x ${match.away_team.name}`,
      };
    }
    const championshipFromGroup = championshipByGroup.get(entry.aggregate_id);
    if (championshipFromGroup) {
      return { championshipId: championshipFromGroup, label: groupLabels.get(entry.aggregate_id) ?? "Grupo" };
    }
    if (championshipNames.has(entry.aggregate_id)) {
      return { championshipId: entry.aggregate_id, label: null };
    }
    return { championshipId: null, label: null };
  };

  const visible = (entries ?? []).filter(
    (entry) => championshipFilter === "todos" || resolveContext(entry).championshipId === championshipFilter,
  );

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">Monitoramento</h2>
        <p className="subtitle">
          Rastreabilidade dos eventos da plataforma: data, identificadores, trace e payload de cada mensagem
          consumida ou publicada via Kafka.
        </p>
      </div>

      <div className="panel">
        <h2>Funil de torneios</h2>
        <div className="stat-row">
          <div className="stat-tile">
            <span className="stat-label">Criados</span>
            <span className="stat-value">{championships.length}</span>
          </div>
          <div className="stat-tile">
            <span className="stat-label">Sorteados</span>
            <span className="stat-value">
              {championships.filter((championship) => championship.status === "SORTEADO").length}
            </span>
          </div>
          <div className="stat-tile">
            <span className="stat-label">Em andamento</span>
            <span className="stat-value">
              {championships.filter((championship) => championship.status === "EM_ANDAMENTO").length}
            </span>
          </div>
          <div className="stat-tile accent">
            <span className="stat-label">Encerrados c/ campeão</span>
            <span className="stat-value">
              {championships.filter((championship) => championship.status === "ENCERRADO").length}
            </span>
          </div>
        </div>
        <p className="meta">
          Taxa de conclusão:{" "}
          {championships.length > 0
            ? `${Math.round(
                (championships.filter((championship) => championship.status === "ENCERRADO").length /
                  championships.length) *
                  100,
              )}% dos torneios criados chegaram ao campeão`
            : "sem torneios ainda"}
          . Situação atual por status — a régua de ativação e conclusão do produto.
        </p>
      </div>

      <div className="filters">
        <select
          value={championshipFilter}
          onChange={(event) => setChampionshipFilter(event.target.value)}
          aria-label="Filtrar por torneio"
        >
          <option value="todos">Todos os torneios</option>
          {championships.map((championship) => (
            <option key={championship.id} value={championship.id}>
              {championship.nome}
            </option>
          ))}
        </select>
      </div>

      <div className="panel">
        <h2>Eventos recentes</h2>
        {isLoading && <Skeleton lines={6} />}
        {entries && entries.length === 0 && <p className="empty">Nenhum evento registrado ainda.</p>}
        {entries && entries.length > 0 && visible.length === 0 && (
          <p className="empty">Nenhum evento para o torneio selecionado.</p>
        )}
        {visible.length > 0 && (
          <ul className="monitor">
            {visible.map((entry) => {
              const context = resolveContext(entry);
              const json = prettyJson(entry.payload);
              return (
                <li key={`${entry.kind}-${entry.event_id}`} className={entry.kind.toLowerCase()}>
                  <div className="monitor-row">
                    <span className="badge">{entry.kind === "CONSUMED" ? "⬇ consumido" : "⬆ publicado"}</span>
                    <strong>{EVENT_LABELS[entry.type] ?? entry.type}</strong>
                    <code>{entry.type}</code>
                    <span className="meta">{formatTimestamp(entry.at)}</span>
                  </div>
                  {(context.championshipId || context.label) && (
                    <p className="monitor-context">
                      {context.championshipId && (
                        <Link to={`/torneios/${context.championshipId}`}>
                          {championshipNames.get(context.championshipId) ?? "torneio"}
                        </Link>
                      )}
                      {context.label && <> · {context.label}</>}
                    </p>
                  )}
                  <p className="monitor-ids">
                    evento <code>{entry.event_id}</code> · agregado <code>{entry.aggregate_id}</code> · trace{" "}
                    {entry.trace_id ? <code>{entry.trace_id}</code> : <span>—</span>}
                  </p>
                  {json && (
                    <details className="payload">
                      <summary>payload JSON</summary>
                      <pre>{json}</pre>
                    </details>
                  )}
                </li>
              );
            })}
          </ul>
        )}
        <p className="meta">
          Fonte: tabelas <code>processed_events</code> e <code>outbox_event</code> do ranking-service.
        </p>
      </div>

      <div className="panel">
        <h2>Como ler este feed</h2>
        <p className="prose">
          Cada resultado registrado aparece duas vezes: primeiro como <code>match.finished.v1</code>{" "}
          (<strong>⬇ consumido</strong> pelo ranking-service) e depois como <code>ranking.updated.v1</code>{" "}
          (<strong>⬆ publicado</strong> via outbox transacional), com a latência real do fluxo assíncrono entre
          eles. O <em>trace</em> é o identificador do OpenTelemetry que liga o evento ao rastreamento distribuído
          entre os serviços; o <em>evento</em> é a chave de deduplicação usada na idempotência do consumer.
        </p>
      </div>
    </>
  );
}
