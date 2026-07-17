import { useSyncExternalStore } from "react";

// "Meu time": preferência local do visitante/capitão, por torneio, sem conta.
// A UI destaca o time nos grupos, na classificação e nas partidas.
const STORAGE_KEY = "championship.myteam";

export interface MyTeam {
  teamId: string;
  name: string;
}

type Store = Record<string, MyTeam>;

const listeners = new Set<() => void>();

let cachedRaw: string | null = null;
let cachedStore: Store = {};
let cacheInitialized = false;

function readStore(): Store {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!cacheInitialized || raw !== cachedRaw) {
    cachedRaw = raw;
    cacheInitialized = true;
    try {
      cachedStore = raw ? (JSON.parse(raw) as Store) : {};
    } catch {
      cachedStore = {};
    }
  }
  return cachedStore;
}

function notify() {
  listeners.forEach((listener) => listener());
}

export function toggleMyTeam(championshipId: string, team: MyTeam): void {
  const store = { ...readStore() };
  if (store[championshipId]?.teamId === team.teamId) {
    delete store[championshipId];
  } else {
    store[championshipId] = team;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(store));
  notify();
}

/** Time marcado como "meu" neste torneio (ou null). */
export function useMyTeam(championshipId: string | undefined): MyTeam | null {
  const store = useSyncExternalStore((listener) => {
    listeners.add(listener);
    return () => listeners.delete(listener);
  }, readStore);
  return championshipId ? store[championshipId] ?? null : null;
}
