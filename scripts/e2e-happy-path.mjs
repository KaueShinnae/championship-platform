#!/usr/bin/env node
// E2E do caminho feliz nos 3 formatos, contra a stack local no ar:
//   conta -> criar torneio -> inscrever times -> sortear -> iniciar ->
//   placar ao vivo -> resultados (com avanço automático e bye) -> campeão.
// Uso: npm run e2e   (requer npm run dev antes; usa 8081/8082 direto)
//
// Sem dependências externas — Node 18+ (fetch nativo).

const INSCRICOES = process.env.E2E_INSCRICOES ?? "http://localhost:8081";
const PARTIDAS = process.env.E2E_PARTIDAS ?? "http://localhost:8082";

let token = null;
let falhas = 0;

function ok(nome) {
  console.log(`  ✓ ${nome}`);
}

function falha(nome, detalhe) {
  falhas++;
  console.error(`  ✗ ${nome}${detalhe ? ` — ${detalhe}` : ""}`);
}

async function req(base, method, path, body) {
  const response = await fetch(base + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    /* corpo não-JSON */
  }
  if (!response.ok) {
    throw new Error(`${method} ${path} -> ${response.status} ${json?.error ?? text}`);
  }
  return json;
}

const esperar = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/** Espera uma condição assíncrona (saga/eventos) com timeout. */
async function ate(descricao, fn, timeoutMs = 30000) {
  const inicio = Date.now();
  for (;;) {
    const valor = await fn();
    if (valor) return valor;
    if (Date.now() - inicio > timeoutMs) {
      throw new Error(`timeout esperando: ${descricao}`);
    }
    await esperar(1000);
  }
}

async function inscreverTimes(campeonatoId, quantidade, prefixo) {
  for (let i = 1; i <= quantidade; i++) {
    await req(INSCRICOES, "POST", `/campeonatos/${campeonatoId}/times`, {
      nome: `${prefixo} ${i}`,
      jogadores: [`${prefixo} ${i} Jogador A`, `${prefixo} ${i} Jogador B`],
    });
  }
  // saga: PENDENTE -> CONFIRMADA via Kafka
  return ate(`${quantidade} inscrições confirmadas`, async () => {
    const inscricoes = await req(INSCRICOES, "GET", `/campeonatos/${campeonatoId}/times`);
    const confirmadas = inscricoes.filter((inscricao) => inscricao.status === "CONFIRMADA");
    return confirmadas.length === quantidade ? confirmadas : null;
  });
}

async function partidasDoCampeonato(campeonatoId) {
  const todas = await req(PARTIDAS, "GET", "/matches");
  return todas.filter((partida) => partida.championship_id === campeonatoId);
}

/** Joga todas as partidas até o torneio encerrar com campeão. */
async function jogarAteOCampeao(campeonatoId, { usarPlacarAoVivo = false } = {}) {
  for (let rodadas = 0; rodadas < 40; rodadas++) {
    const agendadas = (await partidasDoCampeonato(campeonatoId)).filter(
      (partida) => partida.status === "AGENDADA",
    );
    if (agendadas.length === 0) {
      const encerrado = await ate("campeonato encerrado com campeão", async () => {
        const campeonatos = await req(INSCRICOES, "GET", "/campeonatos");
        const campeonato = campeonatos.find((item) => item.id === campeonatoId);
        return campeonato?.status === "ENCERRADO" && campeonato.campeao_nome ? campeonato : null;
      });
      return encerrado;
    }
    for (const partida of agendadas) {
      await req(PARTIDAS, "POST", `/matches/${partida.match_id}/start`, {});
      // placar sem empate: eliminatória exige vencedor
      const [h, a] = Math.random() < 0.5 ? [2, 1] : [1, 3];
      if (usarPlacarAoVivo) {
        await req(PARTIDAS, "POST", `/matches/${partida.match_id}/score`, { home_score: 1, away_score: 0 });
        await req(PARTIDAS, "POST", `/matches/${partida.match_id}/score`, { home_score: h, away_score: a });
      }
      await req(PARTIDAS, "POST", `/matches/${partida.match_id}/result`, { home_score: h, away_score: a });
    }
    await esperar(500); // avanço de fase acontece na transação, mas dá folga ao outbox
  }
  throw new Error("torneio não encerrou dentro do limite de rodadas");
}

async function cicloCompleto(formato, quantidadeDeTimes) {
  const nome = `E2E ${formato} ${Date.now()}`;
  console.log(`\n▶ ${formato} com ${quantidadeDeTimes} times`);

  const campeonato = await req(INSCRICOES, "POST", "/campeonatos", { nome, formato });
  ok(`torneio criado (${campeonato.id})`);

  const confirmadas = await inscreverTimes(campeonato.id, quantidadeDeTimes, `T${formato.slice(0, 2)}`);
  ok(`${confirmadas.length} inscrições confirmadas via saga`);

  const times = confirmadas.map((inscricao) => ({ team_id: inscricao.time_id, name: inscricao.time_nome }));
  const geradas = await req(PARTIDAS, "POST", "/matches/generate", {
    championship_id: campeonato.id,
    formato,
    teams: times,
  });
  await req(INSCRICOES, "POST", `/campeonatos/${campeonato.id}/sortear`, {});
  ok(`sorteio gerou ${geradas.length} partidas`);

  await req(INSCRICOES, "POST", `/campeonatos/${campeonato.id}/iniciar`, {});
  ok("torneio iniciado");

  const encerrado = await jogarAteOCampeao(campeonato.id, { usarPlacarAoVivo: formato === "PLAYOFFS" });
  ok(`campeão: ${encerrado.campeao_nome} (status ${encerrado.status})`);
  return true;
}

async function main() {
  console.log("E2E caminho feliz — 3 formatos (stack local)\n");

  // saúde básica antes de começar
  await req(INSCRICOES, "GET", "/campeonatos").catch(() => {
    throw new Error(`inscricoes-service não responde em ${INSCRICOES} — rode npm run dev antes`);
  });

  // conta nova por execução: também cobre register/token
  const email = `e2e-${Date.now()}@teste.dev`;
  const sessao = await req(INSCRICOES, "POST", "/auth/register", {
    nome: "Robô E2E",
    email,
    senha: "senha-e2e-123",
  });
  token = sessao.token;
  ok(`conta criada e autenticada (${email})`);

  // enforcement: sem token, mutação tem que falhar com 401
  const tokenSalvo = token;
  token = null;
  try {
    await req(INSCRICOES, "POST", "/campeonatos", { nome: "não deveria", formato: "PLAYOFFS" });
    falha("mutação sem token deveria retornar 401");
  } catch (error) {
    if (String(error.message).includes("401")) ok("mutação sem token bloqueada (401)");
    else falha("mutação sem token", error.message);
  }
  token = tokenSalvo;

  // PLAYOFFS com 5 times cobre o bye; GRUPOS_PLAYOFFS 6 = 2 grupos de 3;
  // PONTOS_CORRIDOS 4 = todos contra todos
  const casos = [
    ["PLAYOFFS", 5],
    ["GRUPOS_PLAYOFFS", 6],
    ["PONTOS_CORRIDOS", 4],
  ];
  for (const [formato, quantidade] of casos) {
    try {
      await cicloCompleto(formato, quantidade);
    } catch (error) {
      falha(`ciclo ${formato}`, error.message);
    }
  }

  console.log(falhas === 0 ? "\n✅ E2E: todos os formatos chegaram ao campeão" : `\n❌ E2E: ${falhas} falha(s)`);
  process.exit(falhas === 0 ? 0 : 1);
}

main().catch((error) => {
  console.error(`\n❌ E2E abortado: ${error.message}`);
  process.exit(1);
});
