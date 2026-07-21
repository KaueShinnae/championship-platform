import { Match, ScheduleConflict } from "../api";

export function AttentionPanel({
  status,
  aprovacoesPendentes,
  aguardandoApuracao,
  aoVivo,
  semHorario,
  conflitos,
  onIr,
}: {
  status: string;
  aprovacoesPendentes: number;
  aguardandoApuracao: Match[];
  aoVivo: Match[];
  semHorario: number;
  conflitos: ScheduleConflict[];
  onIr: (tab: string, extra?: Record<string, string>) => void;
}) {
  const itens: { chave: string; texto: string; acao: () => void; urgente?: boolean }[] = [];

  if (status === "SORTEADO") {
    itens.push({
      chave: "sorteado",
      texto: "Sorteio pronto — revise os confrontos e inicie o torneio",
      acao: () => onIr("visao"),
    });
  }
  if (aprovacoesPendentes > 0) {
    itens.push({
      chave: "aprovar",
      texto: `${aprovacoesPendentes} inscrição${aprovacoesPendentes > 1 ? "ões" : ""} aguardando sua aprovação`,
      acao: () => onIr("times"),
      urgente: true,
    });
  }
  if (aoVivo.length > 0) {
    itens.push({
      chave: "aovivo",
      texto: `${aoVivo.length} partida${aoVivo.length > 1 ? "s" : ""} ao vivo — aponte o placar`,
      acao: () => onIr("partidas"),
      urgente: true,
    });
  }
  if (aguardandoApuracao.length > 0) {
    itens.push({
      chave: "apurar",
      texto: `${aguardandoApuracao.length} partida${aguardandoApuracao.length > 1 ? "s" : ""} aguardando resultado`,
      acao: () => onIr("partidas", { filtro: "pendentes" }),
    });
  }
  if (conflitos.length > 0) {
    itens.push({
      chave: "conflito",
      texto: `${conflitos.length} conflito${conflitos.length > 1 ? "s" : ""} de agenda (mesmo time ou mesmo local em dois jogos)`,
      acao: () => onIr("partidas"),
      urgente: true,
    });
  }
  if (semHorario > 0 && status !== "ENCERRADO") {
    itens.push({
      chave: "horario",
      texto: `${semHorario} partida${semHorario > 1 ? "s" : ""} sem horário definido`,
      acao: () => onIr("partidas"),
    });
  }

  if (itens.length === 0) {
    return null;
  }

  return (
    <div className="panel attention-panel">
      <h2>🔔 Precisa da sua atenção</h2>
      <ul className="attention-list">
        {itens.map((item) => (
          <li key={item.chave} className={item.urgente ? "urgent" : ""}>
            <span>{item.texto}</span>
            <button type="button" className="link-button" onClick={item.acao}>
              resolver →
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
