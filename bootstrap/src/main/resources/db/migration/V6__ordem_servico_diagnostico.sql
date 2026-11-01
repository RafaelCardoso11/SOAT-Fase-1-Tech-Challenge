ALTER TABLE ordemservico.ordem_servico
    ADD COLUMN observacoes VARCHAR(2000),
    ADD COLUMN liberado_para_orcamento BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
