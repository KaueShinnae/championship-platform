-- Contas e papéis por torneio: usuário com senha (PBKDF2), dono do campeonato
-- e administradores delegados. Campeonatos antigos ficam com dono nulo
-- (qualquer usuário autenticado pode gerenciá-los — legado de demo).
CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_hash VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE campeonato ADD COLUMN dono_id UUID NULL REFERENCES usuario (id);

CREATE TABLE campeonato_admin (
    campeonato_id UUID NOT NULL REFERENCES campeonato (id),
    usuario_id UUID NOT NULL REFERENCES usuario (id),
    PRIMARY KEY (campeonato_id, usuario_id)
);
