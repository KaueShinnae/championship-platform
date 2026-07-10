# web-dashboard

Aplicação web (SPA React + Vite + TS + React Router). É **camada de
apresentação**, não bounded context — nenhuma regra de domínio vive aqui.

## Páginas
| Rota | Perfil | Conteúdo |
|---|---|---|
| `/` | Público | Torneio: partidas, classificação ao vivo, feed de eventos (leitura) |
| `/partidas/:id` | Público | Detalhe da partida: status, linha do tempo, elencos dos dois times, campanha no grupo |
| `/organizador` | Restrito | Criar campeonato, inscrever time, agendar/iniciar/apurar partida |

A área do organizador pede uma **chave de acesso** (padrão: `organizador`;
troque com `VITE_ORGANIZER_KEY` no build). É um gate de demo no cliente —
não é segurança real; JWT nos serviços é a expansão planejada (SPEC.md §2).

## O que mostra
| Painel | Fonte | Atualização |
|---|---|---|
| Organizador (criar campeonato, inscrever time com jogadores, agendar partida) | `inscricoes-service` + `partidas-service` | polling 2s |
| Partidas + registrar resultado | `partidas-service` (`GET/POST /matches`) | polling 2s |
| Classificação do grupo | `ranking-service` (`GET /groups/{id}/standings`) | polling 2s |
| Feed de eventos Kafka | `ranking-service` (`GET /events/recent`) | polling 2s |

No painel do organizador, o time inscrito aparece como `⏳ pendente` e vira
`✓ confirmada` sozinho quando a saga (`team.registered.v1` →
`enrollment.confirmed.v1`) processa — segunda demonstração visível de fluxo
assíncrono, além do feed.

O feed é o ponto central da demo: ao registrar um resultado, você vê
`match.finished.v1` (consumido) e `ranking.updated.v1` (publicado)
aparecerem em sequência — o fluxo assíncrono fica visível, provando que a
classificação não atualizou por uma chamada síncrona.

## Rodando
Pré-requisito: stack no ar (`powershell -ExecutionPolicy Bypass -File ..\scripts\dev-up.ps1`).

```bash
npm install
npm run dev     # http://localhost:5173
```

Sem CORS: o proxy do Vite mapeia `/api/partidas` → :8082 e `/api/ranking` → :8083
(ver `vite.config.ts`).

## Fluxo de demo (tudo pela UI, sem curl)
1. **Organizador**: crie um campeonato, inscreva 2+ times com jogadores
   (aguarde os badges virarem `✓ confirmada`) e agende uma partida
2. **Partidas**: registre o placar
3. Observe a **Classificação** e o **feed de eventos** atualizarem sozinhos
   em ~2s — sem chamada síncrona entre os serviços

## Build de produção
```bash
npm run build   # tsc + vite build -> dist/
```
