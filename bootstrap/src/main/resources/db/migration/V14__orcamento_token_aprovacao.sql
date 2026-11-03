ALTER TABLE ordemservico.orcamento ADD COLUMN token_aprovacao VARCHAR(36);
UPDATE ordemservico.orcamento SET token_aprovacao = gen_random_uuid()::text WHERE token_aprovacao IS NULL;
ALTER TABLE ordemservico.orcamento ALTER COLUMN token_aprovacao SET NOT NULL;
ALTER TABLE ordemservico.orcamento ADD CONSTRAINT uk_orcamento_token_aprovacao UNIQUE (token_aprovacao);
