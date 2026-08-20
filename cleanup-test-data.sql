-- =============================================================================
-- Limpeza de dados de teste/desenvolvimento — banco de produção (Supabase)
--
-- Preserva SOMENTE: users.id = 2058  (Mateus / mateus259mt@gmail.com, MANAGER)
-- Remove: todo o restante (15 usuários de teste e tudo que os referencia).
--
-- Levantamento em 2026-08-20. Verificado antes de gerar este script:
--   - o usuário 2058 não é referenciado por nenhuma outra tabela;
--   - as 15 obras, 18 OS e 7 pedidos existentes pertencem ao usuário de teste 638;
--   - responsible_user_id / requester_id são NOT NULL, então nada disso pode
--     sobreviver à remoção do usuário 638.
--
-- EXECUTADO em 2026-08-20 contra o banco de produção. Mantido no repositório
-- como registro do que foi removido antes da entrega ao cliente.
--
-- Resultado: 127 linhas apagadas, restando somente o usuário 2058. As duas
-- sequências abaixo foram reiniciadas, então a primeira OS real é "OS Nº 1".
-- Backup pg_dump feito imediatamente antes (não versionado — ver .gitignore).
--
-- Este script é de uso único: rodá-lo de novo apagaria dados reais do cliente.
-- =============================================================================

BEGIN;

-- --- 1. Conferência antes (opcional, só leitura) ------------------------------
-- SELECT 'users' t, count(*) FROM users
-- UNION ALL SELECT 'projects', count(*) FROM projects
-- UNION ALL SELECT 'work_orders', count(*) FROM work_orders
-- UNION ALL SELECT 'daily_reports', count(*) FROM daily_reports
-- UNION ALL SELECT 'material_requests', count(*) FROM material_requests;

-- --- 2. Tabelas filhas (nível mais profundo primeiro) -------------------------
DELETE FROM daily_report_items;
DELETE FROM daily_report_photos;
DELETE FROM daily_report_team_present;
DELETE FROM material_request_items;
DELETE FROM work_order_workers;
DELETE FROM work_order_photos;
DELETE FROM project_photos;

-- --- 3. Entidades que dependem de work_orders / projects / workers / users ----
DELETE FROM daily_reports;
DELETE FROM material_requests;
DELETE FROM work_orders;
DELETE FROM workers;
DELETE FROM projects;

-- --- 4. Usuários, exceto o real ----------------------------------------------
DELETE FROM users WHERE id <> 2058;

-- --- 5. Conferência depois ----------------------------------------------------
-- Deve retornar exatamente 1 linha (Mateus) e zero em todo o resto.
-- SELECT id, name, email, role FROM users;

COMMIT;
-- Em caso de qualquer resultado inesperado nas conferências acima: ROLLBACK;


-- =============================================================================
-- Reinício das sequências (aplicado)
--
-- Motivo: work_order_number e material_request_number são visíveis ao usuário
-- final ("OS Nº 537", "Pedido Nº 94"). Sem reiniciar, a primeira OS real do
-- cliente nasceria como "OS Nº 538", o que parece estranho num sistema novo.
--
-- ATENÇÃO: `./mvnw test` consome essas sequências (nextOrderNumber()) e o
-- incremento não volta atrás com o rollback dos testes. Rodar a suíte contra
-- este banco desfaz o reinício abaixo.
-- =============================================================================

ALTER SEQUENCE work_order_number_seq        RESTART WITH 1;
ALTER SEQUENCE material_request_number_seq  RESTART WITH 1;

-- Sequências de chave primária (cosmético — ids não aparecem para o usuário).
-- Mantidas comentadas: não foram pedidas e não têm efeito visível ao usuário.
-- ALTER SEQUENCE projects_id_seq              RESTART WITH 1;
-- ALTER SEQUENCE work_orders_id_seq           RESTART WITH 1;
-- ALTER SEQUENCE workers_id_seq               RESTART WITH 1;
-- ALTER SEQUENCE daily_reports_id_seq         RESTART WITH 1;
-- ALTER SEQUENCE daily_report_items_id_seq    RESTART WITH 1;
-- ALTER SEQUENCE daily_report_photos_id_seq   RESTART WITH 1;
-- ALTER SEQUENCE material_requests_id_seq     RESTART WITH 1;
-- ALTER SEQUENCE material_request_items_id_seq RESTART WITH 1;
-- ALTER SEQUENCE project_photos_id_seq        RESTART WITH 1;
-- ALTER SEQUENCE work_order_photos_id_seq     RESTART WITH 1;

-- NÃO reinicie users_id_seq: o usuário 2058 continua existindo e um RESTART
-- WITH 1 faria os próximos cadastros colidirem com ids já usados.
