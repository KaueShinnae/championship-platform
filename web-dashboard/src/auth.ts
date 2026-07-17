import { useSyncExternalStore } from "react";
import { loginAccount, registerAccount, SessionUser } from "./api";

// Sessão do usuário (token HMAC emitido pelo inscricoes-service). O api.ts lê
// a mesma chave do localStorage para anexar o Authorization em toda chamada.
const STORAGE_KEY = "championship.session";

interface Session {
  token: string;
  user: SessionUser;
}

const listeners = new Set<() => void>();

// snapshot cacheado: useSyncExternalStore exige referência estável entre
// leituras sem mudança, senão entra em loop de render
let cachedRaw: string | null = null;
let cachedSession: Session | null = null;
let cacheInitialized = false;

function readSession(): Session | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!cacheInitialized || raw !== cachedRaw) {
    cachedRaw = raw;
    cacheInitialized = true;
    try {
      cachedSession = raw ? (JSON.parse(raw) as Session) : null;
    } catch {
      cachedSession = null;
    }
  }
  return cachedSession;
}

function notify() {
  listeners.forEach((listener) => listener());
}

function persist(session: Session) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  notify();
}

export async function login(email: string, senha: string): Promise<SessionUser> {
  const response = await loginAccount(email, senha);
  persist({ token: response.token, user: response.usuario });
  return response.usuario;
}

export async function register(nome: string, email: string, senha: string): Promise<SessionUser> {
  const response = await registerAccount(nome, email, senha);
  persist({ token: response.token, user: response.usuario });
  return response.usuario;
}

export function logout(): void {
  localStorage.removeItem(STORAGE_KEY);
  notify();
}

/** Usuário logado (ou null). */
export function useAuth(): SessionUser | null {
  const session = useSyncExternalStore((listener) => {
    listeners.add(listener);
    return () => listeners.delete(listener);
  }, readSession);
  return session?.user ?? null;
}
