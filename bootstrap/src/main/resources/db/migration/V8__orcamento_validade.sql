ALTER TABLE ordemservico.orcamento
    ADD COLUMN valido_ate TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN aprovado_em TIMESTAMP NULL;

ALTER TABLE ordemservico.orcamento ALTER COLUMN valido_ate DROP DEFAULT;
