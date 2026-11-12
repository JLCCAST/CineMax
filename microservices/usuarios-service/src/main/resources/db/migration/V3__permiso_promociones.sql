-- Permiso para gestionar promociones: gerencia general y marketing.

INSERT INTO permission (permission_name) VALUES ('MANAGE_PROMOTIONS');

INSERT INTO role_permission (id_role, id_permission)
SELECT r.id_role, p.id_permission
FROM role r, permission p
WHERE r.role_name IN ('GERENTE_GENERAL', 'GERENTE_MARKETING')
  AND p.permission_name = 'MANAGE_PROMOTIONS';
