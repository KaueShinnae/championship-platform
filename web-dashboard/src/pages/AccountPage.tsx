import { useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { login, logout, register, useAuth } from "../auth";
import { useToast } from "../ui/toast";

/**
 * Login/criação de conta com copy contextual: quem chega de uma ação
 * (inscrever meu time, reivindicar torneio) vê o motivo no título e volta
 * para onde estava após entrar (?voltar=<rota>). Fluxo de inscrição abre na
 * aba "Criar conta" (quem vem do link público raramente tem conta).
 */
export function AccountPage() {
  const user = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const voltar = searchParams.get("voltar");
  const motivo = searchParams.get("motivo");
  const torneio = searchParams.get("torneio");

  const [tab, setTab] = useState<"entrar" | "criar">(motivo === "inscrever" ? "criar" : "entrar");
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [pending, setPending] = useState(false);
  const senhaRef = useRef<HTMLInputElement>(null);

  const titulo =
    motivo === "inscrever"
      ? torneio
        ? `Inscreva seu time em ${torneio}`
        : "Inscreva seu time"
      : "Conta";
  const subtitulo =
    motivo === "inscrever"
      ? "Crie uma conta grátis para acompanhar e gerenciar sua inscrição."
      : "Sua sessão e permissões.";

  const ctaEntrar = motivo === "inscrever" ? "Entrar e inscrever meu time" : "Entrar";
  const ctaCriar = motivo === "inscrever" ? "Criar conta e inscrever meu time" : "Criar conta";

  const aposEntrar = (nomeUsuario: string, novaConta: boolean) => {
    toast("success", novaConta ? `Conta criada — bem-vindo, ${nomeUsuario}!` : `Bem-vindo de volta, ${nomeUsuario}!`);
    if (voltar && voltar.startsWith("/")) navigate(voltar);
  };

  const entrar = async () => {
    setPending(true);
    try {
      const usuario = await login(email.trim(), senha);
      aposEntrar(usuario.nome, false);
    } catch (error) {
      toast("error", (error as Error).message);
    } finally {
      setPending(false);
    }
  };

  const criar = async () => {
    setPending(true);
    try {
      const usuario = await register(nome.trim(), email.trim(), senha);
      aposEntrar(usuario.nome, true);
    } catch (error) {
      const mensagem = (error as Error).message;
      // erro mais comum vira o caminho mais curto: email já tem conta ->
      // troca para Entrar com o email preenchido e foco na senha
      if (/ja (cadastrado|existe|em uso|usado)/i.test(mensagem)) {
        setTab("entrar");
        setSenha("");
        toast("error", "Este email já tem conta — entre com a sua senha.");
        setTimeout(() => senhaRef.current?.focus(), 0);
      } else {
        toast("error", mensagem);
      }
    } finally {
      setPending(false);
    }
  };

  if (user) {
    return (
      <>
        <div className="page-header">
          <h2 className="page-title">Conta</h2>
          <p className="subtitle">Sua sessão e permissões.</p>
        </div>
        <div className="panel">
          <h2>Sessão</h2>
          <p className="prose">
            Você está como <strong>{user.nome}</strong> ({user.email}). Torneios que você criar são seus — você
            pode delegar administradores pelo email deles na página de cada torneio. O{" "}
            <Link to="/monitoramento">Monitoramento</Link> mostra a rastreabilidade dos eventos dos torneios que
            você gerencia.
          </p>
          {voltar && voltar.startsWith("/") && (
            <p className="prose">
              <Link to={voltar}>← Voltar para onde você estava</Link>
            </p>
          )}
          <div className="match-actions">
            <button
              type="button"
              className="ghost"
              onClick={() => {
                logout();
                toast("success", "Você saiu da sua conta");
              }}
            >
              Sair da conta
            </button>
          </div>
        </div>
      </>
    );
  }

  const validEntrar = email.trim() !== "" && senha !== "";
  const validCriar = nome.trim() !== "" && email.trim() !== "" && senha.length >= 6;

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">{titulo}</h2>
        <p className="subtitle">{subtitulo}</p>
      </div>

      <div className="auth-card panel">
        <div className="tabs auth-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={tab === "entrar"}
            className={`tab ${tab === "entrar" ? "active" : ""}`}
            onClick={() => setTab("entrar")}
          >
            Entrar
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === "criar"}
            className={`tab ${tab === "criar" ? "active" : ""}`}
            onClick={() => setTab("criar")}
          >
            Criar conta
          </button>
        </div>

        <form
          className="org-form auth-form"
          onSubmit={(event) => {
            event.preventDefault();
            if (tab === "entrar" && validEntrar && !pending) entrar();
            if (tab === "criar" && validCriar && !pending) criar();
          }}
        >
          {tab === "criar" && (
            <div className="row">
              <input
                placeholder="Seu nome"
                value={nome}
                maxLength={100}
                onChange={(event) => setNome(event.target.value)}
                autoComplete="name"
              />
            </div>
          )}
          <div className="row">
            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
            />
          </div>
          <div className="row password-row">
            <input
              ref={senhaRef}
              type={mostrarSenha ? "text" : "password"}
              placeholder={tab === "criar" ? "Senha (mín. 6 caracteres)" : "Senha"}
              value={senha}
              onChange={(event) => setSenha(event.target.value)}
              autoComplete={tab === "criar" ? "new-password" : "current-password"}
            />
            <button
              type="button"
              className="ghost password-toggle"
              aria-label={mostrarSenha ? "ocultar senha" : "mostrar senha"}
              onClick={() => setMostrarSenha((valor) => !valor)}
            >
              {mostrarSenha ? "🙈" : "👁"}
            </button>
          </div>
          <div className="row">
            <button
              type="submit"
              className="auth-cta"
              disabled={pending || (tab === "entrar" ? !validEntrar : !validCriar)}
            >
              {pending ? (tab === "entrar" ? "Entrando…" : "Criando…") : tab === "entrar" ? ctaEntrar : ctaCriar}
            </button>
          </div>
        </form>

        <p className="meta">
          Sem conta você continua acompanhando qualquer torneio como visitante — conta é só para criar, gerenciar
          ou inscrever seu time.
        </p>
      </div>
    </>
  );
}
