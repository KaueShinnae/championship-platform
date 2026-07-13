import { useState } from "react";
import { Link } from "react-router-dom";
import { loginOrganizer, logoutOrganizer, useOrganizer } from "../organizer";
import { useToast } from "../ui/toast";

function OrganizerLoginForm() {
  const [key, setKey] = useState("");
  const [failed, setFailed] = useState(false);
  const toast = useToast();

  return (
    <form
      className="org-form"
      onSubmit={(event) => {
        event.preventDefault();
        const success = loginOrganizer(key);
        setFailed(!success);
        if (success) toast("success", "Você entrou como organizador");
      }}
    >
      <div className="row">
        <input
          type="password"
          placeholder="Chave de acesso"
          value={key}
          onChange={(event) => setKey(event.target.value)}
        />
        <button type="submit" disabled={key === ""}>
          Entrar
        </button>
      </div>
      {failed && <span className="error">chave incorreta</span>}
    </form>
  );
}

export function AccountPage() {
  const organizer = useOrganizer();
  const toast = useToast();

  return (
    <>
      <div className="page-header">
        <h2 className="page-title">Conta</h2>
        <p className="subtitle">Sua sessão e permissões.</p>
      </div>

      <div className="panel">
        <h2>Sessão</h2>
        {organizer ? (
          <>
            <p className="prose">
              Você está no modo <strong>Organizador</strong>. Pode criar torneios, inscrever times, agendar,
              iniciar e apurar partidas — os botões aparecem direto nas páginas de cada torneio e partida. Você
              também tem acesso ao <Link to="/monitoramento">Monitoramento</Link>, com a rastreabilidade dos
              eventos da plataforma.
            </p>
            <div className="match-actions">
              <button
                type="button"
                className="ghost"
                onClick={() => {
                  logoutOrganizer();
                  toast("success", "Você saiu do modo organizador");
                }}
              >
                Sair do modo organizador
              </button>
            </div>
          </>
        ) : (
          <>
            <p className="prose">
              Você está como <strong>visitante</strong>: pode acompanhar torneios, partidas e classificações.
              Para criar e gerenciar campeonatos, entre com a chave de organizador.
            </p>
            <OrganizerLoginForm />
          </>
        )}
      </div>
    </>
  );
}
