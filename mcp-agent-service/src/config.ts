function optionalEnv(name: string, fallback = ""): string {
  return process.env[name] ?? fallback;
}

export const config = {
  rankingServiceUrl: optionalEnv("RANKING_SERVICE_URL", "http://localhost:8083"),
  partidasServiceUrl: optionalEnv("PARTIDAS_SERVICE_URL", "http://localhost:8082"),
  anthropicApiKey: optionalEnv("ANTHROPIC_API_KEY"),
  langfuse: {
    publicKey: optionalEnv("LANGFUSE_PUBLIC_KEY"),
    secretKey: optionalEnv("LANGFUSE_SECRET_KEY"),
    host: optionalEnv("LANGFUSE_HOST", "https://cloud.langfuse.com"),
  },
  otelExporterEndpoint: optionalEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317"),
};
