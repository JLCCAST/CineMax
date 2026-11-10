-- Datos semilla de usuarios_db: roles, permisos, tipos de documento y los
-- permisos de cada rol. Son los mismos que cargaba el init-script.
--
-- El usuario administrador no está aquí: lo crea usuarios-service al arrancar
-- (DataSeeder), porque su contraseña se cifra en la aplicación.

INSERT INTO role (role_name) VALUES
    ('ADMIN'),
    ('GERENTE_GENERAL'),
    ('GERENTE_MARKETING'),
    ('GERENTE_OPERACIONES'),
    ('CLIENTE');

INSERT INTO permission (permission_name) VALUES
    ('VIEW_DASHBOARD'),
    ('VIEW_VENUES'),
    ('MANAGE_VENUES'),
    ('MANAGE_ROOMS'),
    ('MANAGE_SEATS'),
    ('MANAGE_MOVIES'),
    ('MANAGE_SHOWTIMES'),
    ('MANAGE_CONFITERIA'),
    ('MANAGE_USERS');

INSERT INTO document_type (doc_name) VALUES
    ('DNI'),
    ('CE'),
    ('PASAPORTE');

-- ADMIN: gestiona usuarios y ve las sedes.
INSERT INTO role_permission (id_role, id_permission)
SELECT r.id_role, p.id_permission
FROM role r, permission p
WHERE r.role_name = 'ADMIN'
  AND p.permission_name IN ('MANAGE_USERS', 'VIEW_VENUES');

-- GERENTE_GENERAL: todo menos gestionar usuarios.
INSERT INTO role_permission (id_role, id_permission)
SELECT r.id_role, p.id_permission
FROM role r, permission p
WHERE r.role_name = 'GERENTE_GENERAL'
  AND p.permission_name <> 'MANAGE_USERS';

INSERT INTO role_permission (id_role, id_permission)
SELECT r.id_role, p.id_permission
FROM role r, permission p
WHERE r.role_name = 'GERENTE_MARKETING'
  AND p.permission_name IN ('VIEW_DASHBOARD', 'MANAGE_CONFITERIA');

INSERT INTO role_permission (id_role, id_permission)
SELECT r.id_role, p.id_permission
FROM role r, permission p
WHERE r.role_name = 'GERENTE_OPERACIONES'
  AND p.permission_name IN ('VIEW_DASHBOARD', 'MANAGE_ROOMS', 'MANAGE_SEATS', 'MANAGE_MOVIES', 'MANAGE_SHOWTIMES');
