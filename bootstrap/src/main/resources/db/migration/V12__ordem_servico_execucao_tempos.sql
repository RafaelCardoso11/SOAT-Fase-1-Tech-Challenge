ALTER TABLE ordemservico.ordem_servico
    ADD COLUMN execucao_iniciada_em TIMESTAMP NULL,
    ADD COLUMN execucao_finalizada_em TIMESTAMP NULL;
