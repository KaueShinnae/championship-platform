import { Championship, Enrollment, Match } from "../api";
import { nomeDaRodada, totalRoundsDoFormato } from "./Bracket";

interface Linha {
  nome: string;
  p: number;
  v: number;
  e: number;
  d: number;
  gp: number;
  gc: number;
}

function classificar(partidas: Match[]): Linha[] {
  const por = new Map<string, Linha>();
  const linha = (id: string, nome: string) =>
    por.get(id) ?? por.set(id, { nome, p: 0, v: 0, e: 0, d: 0, gp: 0, gc: 0 }).get(id)!;
  for (const m of partidas) {
    if (m.status !== "FINALIZADA" || m.home_team.score === null || m.away_team.score === null) continue;
    const casa = linha(m.home_team.team_id, m.home_team.name);
    const fora = linha(m.away_team.team_id, m.away_team.name);
    const ph = m.home_team.score;
    const pa = m.away_team.score;
    if (!m.wo) {
      casa.gp += ph;
      casa.gc += pa;
      fora.gp += pa;
      fora.gc += ph;
    }
    if (ph > pa) {
      casa.v++;
      casa.p += 3;
      fora.d++;
    } else if (pa > ph) {
      fora.v++;
      fora.p += 3;
      casa.d++;
    } else {
      casa.e++;
      fora.e++;
      casa.p++;
      fora.p++;
    }
  }
  return [...por.values()].sort(
    (a, b) => b.p - a.p || b.v - a.v || b.gp - b.gc - (a.gp - a.gc) || b.gp - a.gp || a.nome.localeCompare(b.nome),
  );
}

const escape = (texto: string) =>
  texto.replace(/[&<>]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" })[c] ?? c);

function standingsTable(linhas: Linha[]): string {
  const linhasHtml = linhas
    .map(
      (l, i) =>
        `<tr><td>${i + 1}</td><td class="l">${escape(l.nome)}</td><td>${l.p}</td><td>${l.v}</td><td>${l.e}</td><td>${l.d}</td><td>${l.gp}</td><td>${l.gc}</td><td>${l.gp - l.gc}</td></tr>`,
    )
    .join("");
  return `<table><thead><tr><th>#</th><th class="l">Equipe</th><th>P</th><th>V</th><th>E</th><th>D</th><th>Pró</th><th>Con</th><th>S</th></tr></thead><tbody>${linhasHtml}</tbody></table>`;
}

function partidaLinha(m: Match): string {
  const placar =
    m.status === "FINALIZADA" ? `${m.home_team.score} x ${m.away_team.score}` : m.status === "EM_ANDAMENTO" ? "em andamento" : "a jogar";
  const wo = m.wo ? " (W.O.)" : "";
  return `<li>${escape(m.home_team.name)} <strong>${placar}</strong> ${escape(m.away_team.name)}${wo}</li>`;
}

export function ExportButton({
  championship,
  matches,
  enrollments,
  groupLabels,
}: {
  championship: Championship;
  matches: Match[];
  enrollments: Enrollment[];
  groupLabels: Map<string, string>;
}) {
  const exportar = () => {
    const grupos = [...new Set(matches.map((m) => m.group_id).filter((id): id is string => id !== null))];
    const totalRounds = totalRoundsDoFormato(championship.formato, Math.max(enrollments.length, 2));

    let corpo = "";

    for (const grupoId of grupos) {
      const doGrupo = matches.filter((m) => m.group_id === grupoId);
      corpo += `<h2>${escape(groupLabels.get(grupoId) ?? "Grupo")}</h2>`;
      corpo += standingsTable(classificar(doGrupo));
      corpo += `<ul class="jogos">${doGrupo.map(partidaLinha).join("")}</ul>`;
    }

    const playoffs = matches.filter((m) => m.stage === "PLAYOFF" && m.round !== null);
    if (playoffs.length > 0) {
      corpo += `<h2>Chaveamento</h2>`;
      const rodadas = [...new Set(playoffs.map((m) => m.round!))].sort((a, b) => a - b);
      for (const rodada of rodadas) {
        corpo += `<h3>${escape(nomeDaRodada(rodada, totalRounds))}</h3>`;
        corpo += `<ul class="jogos">${playoffs.filter((m) => m.round === rodada).map(partidaLinha).join("")}</ul>`;
      }
    }

    corpo += `<h2>Times inscritos</h2><ul class="times">`;
    for (const e of enrollments) {
      corpo += `<li><strong>${escape(e.time_nome)}</strong>: ${escape(e.jogadores.map((j) => j.nome).join(", "))}</li>`;
    }
    corpo += `</ul>`;

    if (championship.status === "ENCERRADO" && championship.campeao_nome) {
      corpo = `<p class="campeao">🏆 Campeão: <strong>${escape(championship.campeao_nome)}</strong></p>` + corpo;
    }

    const html = `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>${escape(championship.nome)}</title>
<style>
  body{font-family:system-ui,-apple-system,sans-serif;color:#111;margin:32px;max-width:800px}
  h1{margin:0 0 4px}h2{margin:24px 0 8px;border-bottom:2px solid #111;padding-bottom:4px}h3{margin:16px 0 4px}
  .sub{color:#555;margin:0 0 16px}
  table{border-collapse:collapse;width:100%;margin:8px 0}
  th,td{border:1px solid #999;padding:4px 8px;text-align:center;font-size:14px}
  td.l,th.l{text-align:left}
  ul.jogos{list-style:none;padding:0}ul.jogos li{padding:2px 0;border-bottom:1px dotted #ccc}
  ul.times li{margin:2px 0}
  .campeao{font-size:18px;background:#fef9c3;padding:8px 12px;border-radius:6px}
  @media print{body{margin:12px}}
</style></head><body>
  <h1>${escape(championship.nome)}</h1>
  <p class="sub">${escape(FORMAT_LABEL[championship.formato])} · gerado em ${new Date().toLocaleString("pt-BR")}</p>
  ${corpo}
</body></html>`;

    const janela = window.open("", "_blank");
    if (!janela) return;
    janela.document.write(html);
    janela.document.close();
    janela.focus();
    setTimeout(() => janela.print(), 300);
  };

  return (
    <button type="button" className="ghost" onClick={exportar}>
      🖨 Imprimir / exportar
    </button>
  );
}

const FORMAT_LABEL: Record<Championship["formato"], string> = {
  GRUPOS_PLAYOFFS: "Grupos + Playoffs",
  PLAYOFFS: "Playoffs direto",
  PONTOS_CORRIDOS: "Pontos corridos",
};
