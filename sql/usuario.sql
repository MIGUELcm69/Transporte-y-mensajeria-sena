-- ============================================================
-- usuario.sql  —  Usuario de aplicación con permisos mínimos
-- MySQL 8.3+
-- Ejecutar con root o con GRANT OPTION sobre transportes_db
-- Cambiar 'CAMBIAR_ESTA_CLAVE' por la contraseña real antes de ejecutar
-- (ver src/main/resources/config.properties.example)
-- ============================================================

CREATE USER IF NOT EXISTS 'transportes_app'@'localhost'
    IDENTIFIED BY 'CAMBIAR_ESTA_CLAVE';

-- Solo DML; sin DDL, sin SUPER, sin GRANT OPTION
GRANT SELECT, INSERT, UPDATE, DELETE
    ON transportes_db.*
    TO 'transportes_app'@'localhost';

FLUSH PRIVILEGES;
