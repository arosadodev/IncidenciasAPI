-- Preparación de los perfiles iniciales (paso manual del enunciado).
--
-- El registro público siempre crea ROLE_USER, así que el PRIMER administrador
-- se asigna directamente en la base de datos de desarrollo. A partir de ahí ya
-- se puede usar PATCH /usuarios/{id}/rol para dar ROLE_TECNICO o ROLE_ADMIN.
--
-- Uso con MySQL/MariaDB:
--   mysql -h127.0.0.1 -P3307 -uroot -p incidencias < db/roles.sql
--
-- Antes de ejecutarlo hay que tener registrado el usuario con POST /auth/register.

UPDATE usuarios SET rol = 'ROLE_ADMIN' WHERE email = 'admin@centro.es';

-- Comprobación
SELECT id, email, nombre, rol FROM usuarios ORDER BY id;
