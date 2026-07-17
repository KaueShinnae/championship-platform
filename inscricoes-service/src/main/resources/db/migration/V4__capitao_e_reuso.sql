-- Auto-inscrição pelo capitão (aprovação do organizador) e reuso de elencos:
-- capitao_usuario_id marca inscrições feitas pelo próprio time (nulo = fluxo
-- direto do organizador); criado_por permite sugerir times já cadastrados
-- pelo usuário em torneios anteriores (reuso é sempre snapshot/cópia).
ALTER TABLE inscricao ADD COLUMN capitao_usuario_id UUID NULL REFERENCES usuario (id);
ALTER TABLE time ADD COLUMN criado_por UUID NULL REFERENCES usuario (id);
