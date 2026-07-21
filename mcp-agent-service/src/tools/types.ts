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

export async function runTool<T>(fn: () => Promise<ToolResult<T>>): Promise<ToolResult<T>> {
  try {
    return await fn();
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return fail(`falha ao consultar backend: ${message}`);
  }
}
