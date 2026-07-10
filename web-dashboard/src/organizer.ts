import { useSyncExternalStore } from "react";

// Gate do organizador (SPEC.md §2, "fora do escopo"): chave simples no
// cliente, suficiente para demo local. NAO e seguranca real — JWT nos
// servicos e a expansao planejada. A chave pode ser trocada via
// VITE_ORGANIZER_KEY no build.
const ORGANIZER_KEY = (import.meta.env.VITE_ORGANIZER_KEY as string | undefined) ?? "organizador";
const STORAGE_KEY = "championship.organizer";

const listeners = new Set<() => void>();

function notify() {
  listeners.forEach((listener) => listener());
}

export function isOrganizer(): boolean {
  return localStorage.getItem(STORAGE_KEY) === "true";
}

export function loginOrganizer(key: string): boolean {
  if (key === ORGANIZER_KEY) {
    localStorage.setItem(STORAGE_KEY, "true");
    notify();
    return true;
  }
  return false;
}

export function logoutOrganizer(): void {
  localStorage.removeItem(STORAGE_KEY);
  notify();
}

export function useOrganizer(): boolean {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    isOrganizer,
  );
}
