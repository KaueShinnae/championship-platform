import { useQueries } from "@tanstack/react-query";
import { useEffect, useMemo, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { ChampionshipFormat, fetchStandings, GroupStandings, Match } from "../api";
import { nomeDaRodada } from "../components/Bracket";
import { buildGroupLabels, useChampionships, useMatches } from "../data";

// Telão do torneio: painel público de parede (kiosk) para projetar numa TV do
// ginásio. Read-only, sem menus, tema escuro, tipografia grande. O telão
// ROTACIONA sozinho entre telas: quadras (orientação física) → agenda geral
// (todos os jogos) → classificação (com linha de corte) → chaveamento. Reusa
// GET /matches + GET /matches/standings (polling global de 2s) — sem endpoint novo.

function hora(iso: string | null): string {
  if (!iso) return "A DEFINIR";
  return new Date(iso).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

function proximoPrimeiro(a: Match, b: Match): number {
  const ha = a.scheduled_at ?? null;
  const hb = b.scheduled_at ?? null;
  if (ha && hb) return ha.localeCompare(hb);
  if (ha) return -1;
  if (hb) return 1;
  return (a.round ?? 99) - (b.round ?? 99);
}

function Confronto({ match, ao_vivo }: { match: Match; ao_vivo: boolean }) {
  const placar = ao_vivo || match.status === "FINALIZADA";
  return (
    <div className={`telao-confronto ${ao_vivo ? "vivo" : ""}`}>
      <span className="telao-time casa">{match.home_team.name}</span>
      <span className="telao-placar">
        {placar ? `${match.home_team.score ?? 0} — ${match.away_team.score ?? 0}` : "×"}
      </span>
      <span className="telao-time fora">{match.away_team.name}</span>
    </div>
  );
}

function LocalCard({ local, matches }: { local: string; matches: Match[] }) {
  const vivo = matches.find((m) => m.status === "EM_ANDAMENTO");
  const proximos = matches.filter((m) => m.status === "AGENDADA").sort(proximoPrimeiro);
  const heroi = vivo ?? proximos[0];
  const aSeguir = vivo ? proximos[0] : proximos[1];

  return (
    <section className={`telao-local ${vivo ? "tem-vivo" : "ocioso"}`}>
      <header className="telao-local-head">
        <span className="telao-local-nome">📍 {local}</span>
        {vivo ? <span className="telao-badge-vivo">● AO VIVO</span> : <span className="telao-badge-ocioso">livre</span>}
      </header>
      {heroi ? (
        <>
          <Confronto match={heroi} ao_vivo={!!vivo} />
          <div className="telao-quando">{vivo ? "jogando agora" : hora(heroi.scheduled_at)}</div>
          {aSeguir && (
            <div className="telao-aseguir">
              ▸ A seguir: <strong>{aSeguir.home_team.name}</strong> × <strong>{aSeguir.away_team.name}</strong>
              {aSeguir.scheduled_at && <> · {hora(aSeguir.scheduled_at)}</>}
            </div>
          )}
        </>
      ) : (
        <div className="telao-quando ocioso">sem jogos pendentes</div>
      )}
    </section>
  );
}

function ProximosCard({ matches }: { matches: Match[] }) {
  const ordenados = [...matches].sort(proximoPrimeiro).slice(0, 5);
  return (
    <section className="telao-local proximos-lista">
      <header className="telao-local-head">
        <span className="telao-local-nome">PRÓXIMOS JOGOS</span>
      </header>
      <ul className="telao-proximos">
        {ordenados.map((m) => (
          <li key={m.match_id}>
            <span className="telao-quando-mini">{hora(m.scheduled_at)}</span>
            <span>
              <strong>{m.home_team.name}</strong> × <strong>{m.away_team.name}</strong>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function AgendaScreen({ matches }: { matches: Match[] }) {
  const vivos = matches
    .filter((m) => m.status === "EM_ANDAMENTO")
    .sort((a, b) => (a.local ?? "~").localeCompare(b.local ?? "~", "pt-BR", { numeric: true }));
  const proximosTodos = matches.filter((m) => m.status === "AGENDADA").sort(proximoPrimeiro);
  const proximos = proximosTodos.slice(0, 8);
  const restantes = proximosTodos.length - proximos.length;

  return (
    <div className="telao-agenda">
      {vivos.length > 0 && (
        <section className="telao-agenda-sec">
          <h2 className="telao-agenda-titulo vivo">● AO VIVO AGORA</h2>
          <ul className="telao-agenda-lista">
            {vivos.map((m) => (
              <li key={m.match_id} className="vivo">
                <span className="telao-ag-times">
                  <strong>{m.home_team.name}</strong>
                  <span className="telao-ag-placar">
                    {m.home_team.score ?? 0} — {m.away_team.score ?? 0}
                  </span>
                  <strong>{m.away_team.name}</strong>
                </span>
                {m.local && <span className="telao-ag-local">📍 {m.local}</span>}
              </li>
            ))}
          </ul>
        </section>
      )}
      <section className="telao-agenda-sec">
        <h2 className="telao-agenda-titulo">PRÓXIMOS JOGOS</h2>
        {proximos.length === 0 ? (
          <p className="telao-quando ocioso">sem jogos pendentes</p>
        ) : (
          <ul className="telao-agenda-lista">
            {proximos.map((m) => (
              <li key={m.match_id}>
                <span className="telao-ag-hora">{hora(m.scheduled_at)}</span>
                <span className="telao-ag-times">
                  <strong>{m.home_team.name}</strong> <span className="telao-ag-vs">×</span>{" "}
                  <strong>{m.away_team.name}</strong>
                </span>
                {m.local && <span className="telao-ag-local">📍 {m.local}</span>}
              </li>
            ))}
          </ul>
        )}
        {restantes > 0 && <p className="telao-agenda-mais">… e mais {restantes} jogo(s)</p>}
      </section>
    </div>
  );
}

function ClassificacaoScreen({
  titulo,
  standings,
  qualifyCount,
}: {
  titulo: string;
  standings: GroupStandings | null;
  qualifyCount: number;
}) {
  const linhas = standings?.standings ?? [];
  return (
    <div className="telao-tabela-wrap">
      <h2 className="telao-agenda-titulo">CLASSIFICAÇÃO · {titulo}</h2>
      {linhas.length === 0 ? (
        <p className="telao-quando ocioso">classificação aparece com os primeiros resultados</p>
      ) : (
        <table className="telao-tabela">
          <thead>
            <tr>
              <th>#</th>
              <th className="esq">Time</th>
              <th>P</th>
              <th>V</th>
              <th>Pró</th>
              <th>Con</th>
              <th>Saldo</th>
            </tr>
          </thead>
          <tbody>
            {linhas.map((s, i) => {
              const pos = i + 1;
              const classifica = qualifyCount > 0 && pos <= qualifyCount;
              const cut = qualifyCount > 0 && qualifyCount < linhas.length && pos === qualifyCount;
              return (
                <tr key={s.team_id} className={`${classifica ? "classifica" : ""} ${cut ? "corte" : ""}`}>
                  <td>{pos}</td>
                  <td className="esq">{s.team_name}</td>
                  <td className="forte">{s.pontos}</td>
                  <td>{s.vitorias}</td>
                  <td>{s.pro}</td>
                  <td>{s.contra}</td>
                  <td>{s.saldo > 0 ? `+${s.saldo}` : s.saldo}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
      {qualifyCount > 0 && qualifyCount < linhas.length && (
        <p className="telao-legenda-corte">
          <span className="telao-corte-marca" /> zona de classificação (passam {qualifyCount})
        </p>
      )}
    </div>
  );
}

function ChaveamentoScreen({ matches }: { matches: Match[] }) {
  const playoff = matches.filter((m) => m.stage === "PLAYOFF" && !m.terceiro_lugar);
  const rounds = [...new Set(playoff.map((m) => m.round).filter((r): r is number => r !== null))].sort((a, b) => a - b);
  const totalRounds = rounds[rounds.length - 1] ?? 1;
  const finalMatch = playoff.find((m) => m.round === totalRounds);
  const campeao =
    finalMatch && finalMatch.status === "FINALIZADA"
      ? (finalMatch.home_team.score ?? 0) > (finalMatch.away_team.score ?? 0)
        ? finalMatch.home_team.name
        : finalMatch.away_team.name
      : null;

  return (
    <div className="telao-bracket">
      {rounds.map((round) => (
        <div key={round} className="telao-bracket-col">
          <h3>{nomeDaRodada(round, totalRounds)}</h3>
          {playoff
            .filter((m) => m.round === round)
            .sort((a, b) => (a.bracket_pos ?? 0) - (b.bracket_pos ?? 0))
            .map((m) => {
              const vivo = m.status === "EM_ANDAMENTO";
              const fim = m.status === "FINALIZADA";
              const casaVence = fim && (m.home_team.score ?? 0) > (m.away_team.score ?? 0);
              const foraVence = fim && (m.away_team.score ?? 0) > (m.home_team.score ?? 0);
              return (
                <div key={m.match_id} className={`telao-bracket-jogo ${vivo ? "vivo" : ""}`}>
                  <span className={`telao-bk-time ${casaVence ? "vence" : ""}`}>
                    {m.home_team.name} <b>{fim || vivo ? m.home_team.score ?? 0 : ""}</b>
                  </span>
                  <span className={`telao-bk-time ${foraVence ? "vence" : ""}`}>
                    {m.away_team.name} <b>{fim || vivo ? m.away_team.score ?? 0 : ""}</b>
                  </span>
                  {vivo && <span className="telao-bk-vivo">● jogando</span>}
                </div>
              );
            })}
        </div>
      ))}
      <div className="telao-bracket-col">
        <h3>Campeão</h3>
        <div className={`telao-bracket-campeao ${campeao ? "" : "aguardando"}`}>
          {campeao ? <>🏆 {campeao}</> : "a definir"}
        </div>
      </div>
    </div>
  );
}

function Ticker({ resultados }: { resultados: Match[] }) {
  if (resultados.length === 0) return <span className="telao-ultimo">Bem-vindo ao torneio</span>;
  const item = (m: Match, k: string) => (
    <span key={k} className="telao-ticker-item">
      <strong>{m.home_team.name}</strong> {m.home_team.score} × {m.away_team.score} <strong>{m.away_team.name}</strong>
      {m.local && <> · {m.local}</>}
    </span>
  );
  return (
    <div className="telao-ticker">
      <div className="telao-ticker-track">
        {resultados.map((m) => item(m, m.match_id))}
        {resultados.map((m) => item(m, m.match_id + "-2"))}
      </div>
    </div>
  );
}

type Slide =
  | { kind: "quadras"; page: number }
  | { kind: "agenda" }
  | { kind: "classificacao"; groupId: string }
  | { kind: "chaveamento" };

const QUALIFY_POR_FORMATO: Record<ChampionshipFormat, number> = {
  GRUPOS_PLAYOFFS: 2,
  PONTOS_CORRIDOS: 1,
  PLAYOFFS: 0,
};

const MAX_QUADRAS = 4; // 2×2 por tela; mais que isso pagina

export function TelaoPage() {
  const { championshipId } = useParams<{ championshipId: string }>();
  const { data: championships = [] } = useChampionships();
  const { data: allMatches = [] } = useMatches();
  const championship = championships.find((c) => c.id === championshipId);

  const matches = useMemo(
    () => allMatches.filter((m) => m.championship_id === championshipId),
    [allMatches, championshipId],
  );

  const groupLabels = useMemo(() => buildGroupLabels(allMatches), [allMatches]);
  const groups = useMemo(
    () => [...new Set(matches.map((m) => m.group_id).filter((g): g is string => g !== null))],
    [matches],
  );

  // classificação de cada grupo (read model público; polling global)
  const standingsResults = useQueries({
    queries: groups.map((g) => ({ queryKey: ["standings", g], queryFn: () => fetchStandings(g) })),
  });
  const standingsByGroup = new Map(groups.map((g, i) => [g, standingsResults[i]?.data ?? null]));

  // relógio client-side: prova que o telão está "vivo" mesmo se o polling falhar
  const [agora, setAgora] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setAgora(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const rootRef = useRef<HTMLDivElement>(null);
  const [fs, setFs] = useState(false);
  const toggleFullscreen = () => {
    if (!document.fullscreenElement) rootRef.current?.requestFullscreen?.().then(() => setFs(true)).catch(() => {});
    else document.exitFullscreen?.().then(() => setFs(false)).catch(() => {});
  };

  const ativos = matches.filter((m) => m.status !== "FINALIZADA");
  const locais = [...new Set(ativos.map((m) => m.local).filter((l): l is string => l !== null))].sort((a, b) =>
    a.localeCompare(b, "pt-BR", { numeric: true }),
  );
  const semLocal = ativos.filter((m) => m.local === null);
  const finalizadas = matches
    .filter((m) => m.status === "FINALIZADA")
    .sort((a, b) => (b.played_at ?? "").localeCompare(a.played_at ?? ""));

  // cartões da grade de quadras (locais + eventual coluna "sem local"), paginados
  const quadraCards = [
    ...locais.map((local) => ({
      key: local,
      node: <LocalCard key={local} local={local} matches={ativos.filter((m) => m.local === local)} />,
    })),
    ...(semLocal.length > 0 ? [{ key: "__sem__", node: <ProximosCard key="__sem__" matches={semLocal} /> }] : []),
  ];
  const paginasQuadras = Math.ceil(quadraCards.length / MAX_QUADRAS);

  const formato = championship?.formato;
  const temClassificacao = formato !== "PLAYOFFS";
  const temChaveamento = matches.some((m) => m.stage === "PLAYOFF");

  // ---- estados de borda (tela única, sem rotação) ----
  let estadoBorda: null | "nao-encontrado" | "cancelado" | "campeao" | "sem-jogos" | "rodada-encerrada" = null;
  if (!championship) estadoBorda = "nao-encontrado";
  else if (championship.status === "CANCELADO") estadoBorda = "cancelado";
  else if (championship.status === "ENCERRADO" && championship.campeao_nome) estadoBorda = "campeao";
  else if (matches.length === 0) estadoBorda = "sem-jogos";
  else if (ativos.length === 0) estadoBorda = "rodada-encerrada";

  // ---- slides da rotação ----
  const slides: Slide[] = [];
  if (!estadoBorda) {
    if (locais.length > 0) {
      for (let p = 0; p < paginasQuadras; p++) slides.push({ kind: "quadras", page: p });
    }
    slides.push({ kind: "agenda" });
    if (temClassificacao) {
      if (groups.length > 0) groups.forEach((g) => slides.push({ kind: "classificacao", groupId: g }));
    }
    if (temChaveamento) slides.push({ kind: "chaveamento" });
  }

  const [slideIdx, setSlideIdx] = useState(0);
  useEffect(() => {
    setSlideIdx(0);
  }, [slides.length]);
  useEffect(() => {
    if (slides.length <= 1) return;
    const id = setInterval(() => setSlideIdx((i) => (i + 1) % slides.length), 13000);
    return () => clearInterval(id);
  }, [slides.length]);
  const slide = slides[Math.min(slideIdx, Math.max(slides.length - 1, 0))];

  // ---- cabeçalho ----
  const rodadaAtual = ativos
    .map((m) => m.round)
    .filter((r): r is number => r !== null)
    .sort((a, b) => a - b)[0];
  const temPlayoffAtivo = ativos.some((m) => m.stage === "PLAYOFF");
  const faseLabel = ativos.length === 0
    ? ""
    : `${temPlayoffAtivo ? "Mata-mata" : "Fase de liga"}${rodadaAtual ? ` · Rodada ${rodadaAtual}` : ""}`;
  const relogio = agora.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  const temVivo = ativos.some((m) => m.status === "EM_ANDAMENTO");

  // ---- corpo ----
  let corpo: JSX.Element;
  if (estadoBorda === "nao-encontrado") {
    corpo = <div className="telao-vazio">Torneio não encontrado.</div>;
  } else if (estadoBorda === "cancelado") {
    corpo = <div className="telao-vazio">Torneio cancelado.</div>;
  } else if (estadoBorda === "campeao") {
    corpo = (
      <div className="telao-campeao">
        <div className="telao-trofeu">🏆</div>
        <div className="telao-campeao-rotulo">CAMPEÃO</div>
        <div className="telao-campeao-nome">{championship!.campeao_nome}</div>
      </div>
    );
  } else if (estadoBorda === "sem-jogos") {
    corpo = (
      <div className="telao-boasvindas">
        <div className="telao-boasvindas-status">INSCRIÇÕES ABERTAS</div>
        <div className="telao-boasvindas-msg">Os jogos serão divulgados em breve.</div>
      </div>
    );
  } else if (estadoBorda === "rodada-encerrada") {
    corpo = (
      <div className="telao-boasvindas">
        <div className="telao-boasvindas-status">RODADA ENCERRADA</div>
        <div className="telao-boasvindas-msg">Aguardando os próximos confrontos.</div>
      </div>
    );
  } else {
    const s = slide as Slide; // sem estado de borda, sempre há slide
    if (s.kind === "agenda") {
      corpo = <AgendaScreen matches={ativos} />;
    } else if (s.kind === "classificacao") {
      corpo = (
        <ClassificacaoScreen
          titulo={groups.length > 1 ? groupLabels.get(s.groupId) ?? "Grupo" : "Geral"}
          standings={standingsByGroup.get(s.groupId) ?? null}
          qualifyCount={formato ? QUALIFY_POR_FORMATO[formato] : 0}
        />
      );
    } else if (s.kind === "chaveamento") {
      corpo = <ChaveamentoScreen matches={matches} />;
    } else {
      const cards = quadraCards.slice(s.page * MAX_QUADRAS, s.page * MAX_QUADRAS + MAX_QUADRAS);
      corpo = <div className={`telao-grid ${cards.length === 1 ? "col-1" : ""}`}>{cards.map((c) => c.node)}</div>;
    }
  }

  const paginaQuadraLabel =
    slide?.kind === "quadras" && paginasQuadras > 1 ? ` · Quadras ${slide.page + 1}/${paginasQuadras}` : "";

  return (
    <div className="telao" ref={rootRef}>
      <header className="telao-header">
        <div className="telao-titulo">
          {championship?.nome ?? "Torneio"}
          {faseLabel && <span className="telao-fase"> · {faseLabel}{paginaQuadraLabel}</span>}
        </div>
        <div className="telao-relogio">
          {temVivo && <span className="telao-header-vivo">● AO VIVO</span>}
          <span className="telao-hora">{relogio}</span>
        </div>
      </header>

      {corpo}

      <footer className="telao-footer">
        <Ticker resultados={finalizadas.slice(0, 8)} />
        {slides.length > 1 && (
          <span className="telao-dots" aria-hidden="true">
            {slides.map((s, i) => (
              <span key={`${s.kind}-${i}`} className={i === slideIdx ? "on" : ""} />
            ))}
          </span>
        )}
        <button type="button" className="telao-fs" onClick={toggleFullscreen} aria-label="Tela cheia">
          {fs ? "⤢ sair" : "⛶ tela cheia"}
        </button>
      </footer>
    </div>
  );
}
