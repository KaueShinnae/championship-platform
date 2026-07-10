import { useQuery } from "@tanstack/react-query";
import { fetchEventFeed } from "../api";

// Feed que torna o fluxo assincrono VISIVEL: o mesmo resultado registrado
// aparece primeiro como match.finished.v1 (consumido) e depois como
// ranking.updated.v1 (publicado), com a latencia real entre eles.
export function EventFeed() {
  const { data: entries = [] } = useQuery({ queryKey: ["event-feed"], queryFn: fetchEventFeed });

  return (
    <div className="panel">
      <h2>Eventos (Kafka)</h2>
      {entries.length === 0 && <p className="empty">Nenhum evento processado ainda.</p>}
      <ul className="feed">
        {entries.map((entry) => (
          <li key={`${entry.kind}-${entry.type}-${entry.at}-${entry.aggregate_id}`} className={entry.kind.toLowerCase()}>
            <span className="badge">{entry.kind === "CONSUMED" ? "⬇ consumido" : "⬆ publicado"}</span>
            <code>{entry.type}</code>
            <span className="meta">{new Date(entry.at).toLocaleTimeString()}</span>
          </li>
        ))}
      </ul>
      <p className="meta">
        Fonte: tabelas <code>processed_events</code> e <code>outbox_event</code> do ranking-service
      </p>
    </div>
  );
}
