-- ============================================================================
-- Xtrim — the staff users
--
-- The two accounts of the evaluation (HU-13). The passwords are hashed with
-- BCrypt; the hash below is the only thing that is ever stored.
--
--   admin@xtrim.ec  /  admin123     → ADMIN    (the catalogue and the CRM)
--   auditor@xtrim.ec / auditor123   → AUDITOR  (the audit trail, read only)
--
-- The seed exists so that a fresh database is demonstrable. On a real
-- deployment these accounts are replaced before the first login.
-- ============================================================================

insert into users (id, email, password_hash, display_name, role, created_at) values
  (1, 'admin@xtrim.ec',
   '$2a$10$0/CackPj8O69AgW2g.B8CeCiyamZqrKA80eRt9QMeoUy5lANg7u5e',
   'Administración', 'ADMIN', now()),
  (2, 'auditor@xtrim.ec',
   '$2a$10$5W7Z4HIR3wPcmEn6t6M/DuDSXV8CJdvSn/izfAk9jLmam6dVj9vhq',
   'Auditoría', 'AUDITOR', now());

select setval('users_id_seq', (select max(id) from users));
