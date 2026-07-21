import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ChampionshipFormat, createChampionship } from "../api";
import { useAuth } from "../auth";
import { useToast } from "../ui/toast";

const FORMATS: {
  id: ChampionshipFormat;
  icon: string;
  title: string;
  description: string;
  rules: string;
  min: number;
}[] = [
  {
    id: "GRUPOS_PLAYOFFS",
    icon: "🎯",
    title: "Grupos + Playoffs",
    description: "Fase de grupos e depois mata-mata.",
    rules: "Grupos sorteados, todos contra todos dentro do grupo; os 2 melhores avançam ao mata-mata (1º A × 2º B).",
    min: 6,
  },
  {
    id: "PLAYOFFS",
    icon: "🏆",
    title: "Playoffs direto",
    description: "Mata-mata desde o início.",
    rules: "Confrontos sorteados; quem perde está fora. Com número ímpar de times, um deles avança de fase direto (bye).",
    min: 2,
  },
  {
    id: "PONTOS_CORRIDOS",
    icon: "📋",
    title: "Pontos corridos",
    description: "Todos contra todos, tabela única.",
    rules: "Turno único; vitória 3 pontos, empate 1. O líder ao fim da última rodada é o campeão.",
    min: 3,
  },
];

export function CreateTournamentPage() {
  const user = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [nome, setNome] = useState("");
  const [formato, setFormato] = useState<ChampionshipFormat | null>(null);
  const [aprovacao, setAprovacao] = useState(true);
  const [minIntegrantes, setMinIntegrantes] = useState("");
  const [maxIntegrantes, setMaxIntegrantes] = useState("");
  const [disputaTerceiro, setDisputaTerceiro] = useState(false);

  const temMataMata = formato === "PLAYOFFS" || formato === "GRUPOS_PLAYOFFS";

  const mutation = useMutation({
    mutationFn: () =>
      createChampionship(nome.trim(), formato!, {
        aprovacaoInscricoes: aprovacao,
        minIntegrantes: minIntegrantes === "" ? null : Number(minIntegrantes),
        maxIntegrantes: maxIntegrantes === "" ? null : Number(maxIntegrantes),
        disputaTerceiro: temMataMata && disputaTerceiro,
      }),
    onSuccess: (championship) => {
      queryClient.invalidateQueries({ queryKey: ["championships"] });
      toast("success", `Torneio "${championship.nome}" criado — agora inscreva os times`);
      navigate(`/torneios/${championship.id}?tab=times`);
    },
    onError: (error) => toast("error", (error as Error).message),
  });

  if (!user) {
    return (
      <>
        <div className="page-header">
          <h2 className="page-title">Criar torneio</h2>
        </div>
        <div className="panel">
          <p className="prose">
            Para criar um torneio, entre ou crie sua conta na página <Link to="/conta">Conta</Link> — o torneio
            fica registrado no seu nome.
          </p>
        </div>
      </>
    );
  }

  const selected = FORMATS.find((format) => format.id === formato);
  const valid = nome.trim() !== "" && formato !== null;

  return (
    <>
      <p className="breadcrumb">
        <Link to="/torneios">← Torneios</Link>
      </p>

      <div className="page-header">
        <h2 className="page-title">Criar torneio</h2>
        <p className="subtitle">Escolha o nome e o formato — o formato não pode ser trocado depois.</p>
      </div>

      <form
        className="panel org-form"
        onSubmit={(event) => {
          event.preventDefault();
          if (valid && !mutation.isPending) mutation.mutate();
        }}
      >
        <label className="field-label" htmlFor="tournament-name">
          Nome do torneio
        </label>
        <div className="row">
          <input
            id="tournament-name"
            placeholder="ex.: Copa da Firma 2026"
            value={nome}
            maxLength={100}
            onChange={(event) => setNome(event.target.value)}
          />
        </div>

        <label className="field-label">Formato</label>
        <div className="format-grid">
          {FORMATS.map((format) => (
            <button
              key={format.id}
              type="button"
              className={`format-card ${formato === format.id ? "selected" : ""}`}
              onClick={() => setFormato(format.id)}
              aria-pressed={formato === format.id}
            >
              <span className="format-icon">{format.icon}</span>
              <strong>{format.title}</strong>
              <span className="format-desc">{format.description}</span>
              <span className="meta">mín. {format.min} times</span>
            </button>
          ))}
        </div>

        {selected && <p className="hint">{selected.rules}</p>}

        <label className="field-label">Inscrições de capitães</label>
        <div className="format-grid approval-grid">
          <button
            type="button"
            className={`format-card ${aprovacao ? "selected" : ""}`}
            onClick={() => setAprovacao(true)}
            aria-pressed={aprovacao}
          >
            <span className="format-icon">✅</span>
            <strong>Com minha aprovação</strong>
            <span className="format-desc">Capitães se inscrevem e aguardam você aprovar cada time.</span>
          </button>
          <button
            type="button"
            className={`format-card ${!aprovacao ? "selected" : ""}`}
            onClick={() => setAprovacao(false)}
            aria-pressed={!aprovacao}
          >
            <span className="format-icon">⚡</span>
            <strong>Entrada direta</strong>
            <span className="format-desc">
              Times de capitães entram confirmados na hora — você pode remover indesejados enquanto as
              inscrições estiverem abertas.
            </span>
          </button>
        </div>

        <label className="field-label">Integrantes por equipe (opcional)</label>
        <div className="row team-size-row">
          <input
            type="number"
            min={1}
            placeholder="mínimo"
            value={minIntegrantes}
            onChange={(event) => setMinIntegrantes(event.target.value)}
            aria-label="mínimo de integrantes por equipe"
          />
          <span className="muted">a</span>
          <input
            type="number"
            min={1}
            placeholder="máximo"
            value={maxIntegrantes}
            onChange={(event) => setMaxIntegrantes(event.target.value)}
            aria-label="máximo de integrantes por equipe"
          />
          <span className="hint">
            deixe em branco para não limitar · torneio individual (xadrez, corrida): use 1 e 1
          </span>
        </div>

        {temMataMata && (
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={disputaTerceiro}
              onChange={(event) => setDisputaTerceiro(event.target.checked)}
            />
            <span>
              Disputa de <strong>3º lugar</strong> — os perdedores das semifinais se enfrentam pelo bronze
            </span>
          </label>
        )}

        <div className="match-actions">
          <Link to="/torneios" className="button-ghost">
            Cancelar
          </Link>
          <button type="submit" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "Criando…" : "Criar torneio"}
          </button>
        </div>
      </form>
    </>
  );
}
