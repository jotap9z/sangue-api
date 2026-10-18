BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS
    uk_api_equipe_administrador_principal
ON public.api_vinculos_equipe (hemocentro_id)
WHERE administrador_principal = TRUE;

COMMIT;