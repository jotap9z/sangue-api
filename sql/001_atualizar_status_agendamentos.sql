-- Atualiza os estados permitidos para o fluxo de atendimento.
-- Executar no banco sangue após a criação de api_agendamentos.
-- Não exclui registros existentes.

BEGIN;

DO $$
DECLARE
    restricao RECORD;
    coluna_status SMALLINT;
BEGIN
    SELECT attnum
    INTO STRICT coluna_status
    FROM pg_attribute
    WHERE attrelid = 'public.api_agendamentos'::regclass
      AND attname = 'status'
      AND NOT attisdropped;

    FOR restricao IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'public.api_agendamentos'::regclass
          AND contype = 'c'
          AND conkey = ARRAY[coluna_status]::SMALLINT[]
    LOOP
        EXECUTE format(
            'ALTER TABLE public.api_agendamentos DROP CONSTRAINT %I',
            restricao.conname
        );
    END LOOP;

    ALTER TABLE public.api_agendamentos
        ADD CONSTRAINT ck_api_agendamentos_status
        CHECK (
            status IN (
                'AGENDADO',
                'EM_ATENDIMENTO',
                'APROVADO_TRIAGEM',
                'NAO_APTO_TRIAGEM',
                'DOACAO_REALIZADA',
                'CANCELADO_DOADOR',
                'CANCELADO_HEMOCENTRO'
            )
        );
END;
$$;

COMMIT;