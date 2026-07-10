import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

// Config deterministica nos testes: independe do ambiente da maquina
// (ex: ANTHROPIC_API_KEY setada mudaria o comportamento de generateMatchRecap).
vi.mock("../src/config.js", () => ({
  config: {
    rankingServiceUrl: "http://localhost:8083",
    partidasServiceUrl: "http://localhost:8082",
    anthropicApiKey: "",
    langfuse: { publicKey: "", secretKey: "", host: "" },
    otelExporterEndpoint: "",
  },
}));

import { getGroupStandings } from "../src/tools/getGroupStandings.js";
import { getLastMatchResult } from "../src/tools/getLastMatchResult.js";
import { generateMatchRecap } from "../src/tools/generateMatchRecap.js";
import { runTool } from "../src/tools/types.js";

const VALID_UUID = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";

describe("runTool", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("converte backend 500 em erro estruturado, nunca excecao crua", async () => {
    vi.mocked(fetch).mockResolvedValue({ status: 500, ok: false } as Response);

    const result = await runTool(() => getGroupStandings({ group_id: VALID_UUID }));

    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toContain("falha ao consultar backend");
  });

  it("converte backend fora do ar (rede) em erro estruturado", async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError("fetch failed"));

    const result = await runTool(() => getLastMatchResult({ match_id: VALID_UUID }));

    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toContain("falha ao consultar backend");
  });
});

describe("getGroupStandings", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("rejeita group_id que nao e UUID antes de tocar o backend (guardrail de input)", async () => {
    const result = await getGroupStandings({ group_id: "'; DROP TABLE ranking; --" });

    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toContain("group_id invalido");
    expect(fetch).not.toHaveBeenCalled();
  });

  it("retorna erro estruturado quando o grupo nao existe (404), sem lancar excecao", async () => {
    vi.mocked(fetch).mockResolvedValue({ status: 404, ok: false } as Response);

    const result = await getGroupStandings({ group_id: VALID_UUID });

    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toContain("grupo nao encontrado");
  });

  it("retorna a classificacao quando o backend responde", async () => {
    const standings = {
      group_id: VALID_UUID,
      updated_at: "2026-07-09T17:23:19Z",
      standings: [
        { team_id: "t1", team_name: "Timaco FC", points: 3, wins: 1, draws: 0, losses: 0 },
      ],
    };
    vi.mocked(fetch).mockResolvedValue({
      status: 200,
      ok: true,
      json: async () => standings,
    } as Response);

    const result = await getGroupStandings({ group_id: VALID_UUID });

    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.data.standings[0]?.team_name).toBe("Timaco FC");
      // escopo: a URL consultada e restrita ao grupo pedido
      expect(vi.mocked(fetch).mock.calls[0]?.[0]).toContain(`/groups/${VALID_UUID}/standings`);
    }
  });
});

describe("getLastMatchResult", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("rejeita match_id invalido antes de tocar o backend", async () => {
    const result = await getLastMatchResult({ match_id: "abc" });

    expect(result.ok).toBe(false);
    expect(fetch).not.toHaveBeenCalled();
  });

  it("retorna erro estruturado para partida inexistente", async () => {
    vi.mocked(fetch).mockResolvedValue({ status: 404, ok: false } as Response);

    const result = await getLastMatchResult({ match_id: VALID_UUID });

    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toContain("partida nao encontrada");
  });
});

describe("generateMatchRecap", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("rejeita match_id invalido antes de qualquer chamada", async () => {
    const result = await generateMatchRecap({ match_id: "not-a-uuid" });

    expect(result.ok).toBe(false);
    expect(fetch).not.toHaveBeenCalled();
  });

  it("retorna erro estruturado quando ANTHROPIC_API_KEY nao esta configurada", async () => {
    const result = await generateMatchRecap({ match_id: VALID_UUID });

    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.error).toContain("ANTHROPIC_API_KEY");
  });
});
