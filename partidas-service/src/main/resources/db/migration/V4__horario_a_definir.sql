-- Partidas geradas pelo sorteio nascem sem horario ("a definir") ate o
-- organizador marcar pelo botao Remarcar/Definir horario. Nada de exibir
-- data inventada para o visitante.
ALTER TABLE partida ALTER COLUMN scheduled_at DROP NOT NULL;
