/**
 * Formato de resposta padrao das tools (skill mcp-tool-builder): sempre
 * estruturado, erro nunca vira excecao crua pro cliente MCP.
 */
export type ToolResult<T> = { ok: true; data: T } | { ok: false; error: string };

export function ok<T>(data: T): ToolResult<T> {
  return { ok: true, data };
}

export function fail(error: string): ToolResult<never> {
  return { ok: false, error };
}

export function toMcpContent(result: ToolResult<unknown>) {
  return {
    content: [{ type: "text" as const, text: JSON.stringify(result.ok ? result.data : { error: result.error }) }],
    isError: !result.ok,
  };
}

/**
 * Executa uma tool garantindo que falha inesperada (backend fora, 500, rede)
 * vira erro estruturado — nunca excecao crua pro cliente MCP
 * (skill mcp-tool-builder, "Formato de resposta").
 */
export async function runTool<T>(fn: () => Promise<ToolResult<T>>): Promise<ToolResult<T>> {
  try {
    return await fn();
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return fail(`falha ao consultar backend: ${message}`);
  }
}
