-- ============================================================
-- schema.sql  —  Sistema de Transportes y Mensajería
-- MySQL 8.3+  |  Script reproducible (RNF-17)
-- Orden de ejecución: 1º schema.sql, 2º usuario.sql, 3º seed.sql
-- ADVERTENCIA: este script elimina todas las tablas y sus datos.
-- Usar solo en entorno de desarrollo. No ejecutar en producción.
-- ============================================================

CREATE DATABASE IF NOT EXISTS transportes_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE transportes_db;

-- ---- Drop en orden inverso de dependencia ------------------
DROP TABLE IF EXISTS envio;
DROP TABLE IF EXISTS mercancia;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS vehiculo;
DROP TABLE IF EXISTS supervisor;

-- ---- Triggers y procedimientos pendientes de recrear -------
DROP TRIGGER IF EXISTS trg_envio_bu;
DROP TRIGGER IF EXISTS trg_envio_bi;
DROP TRIGGER IF EXISTS trg_mercancia_bu;
DROP TRIGGER IF EXISTS trg_mercancia_bi;
DROP TRIGGER IF EXISTS trg_cliente_tipo_bu;
DROP TRIGGER IF EXISTS trg_vehiculo_tipo_bu;
DROP PROCEDURE IF EXISTS sp_validar_mercancia;

-- ============================================================
-- TABLAS
-- ============================================================

CREATE TABLE supervisor (
    id        INT          NOT NULL AUTO_INCREMENT,
    nif       VARCHAR(20)  NOT NULL,
    nombre    VARCHAR(100) NOT NULL,
    direccion VARCHAR(200) NOT NULL,
    telefono  VARCHAR(15)  NOT NULL,
    CONSTRAINT pk_supervisor     PRIMARY KEY (id),
    CONSTRAINT uq_supervisor_nif UNIQUE (nif)
) ENGINE = InnoDB;

CREATE TABLE vehiculo (
    id            INT           NOT NULL AUTO_INCREMENT,
    codigo        VARCHAR(20)   NOT NULL,
    tipo          ENUM('MOTO','FURGONETA','AVION') NOT NULL,
    descripcion   VARCHAR(200)  NOT NULL,
    fecha_compra  DATE          NOT NULL,
    precio_compra DECIMAL(12,2) NOT NULL,
    supervisor_id INT           NOT NULL,
    cilindrada    DECIMAL(8,2)  NULL,
    capacidad     DECIMAL(10,2) NULL,
    CONSTRAINT pk_vehiculo         PRIMARY KEY (id),
    CONSTRAINT uq_vehiculo_codigo  UNIQUE (codigo),
    CONSTRAINT fk_vehiculo_supervisor
        FOREIGN KEY (supervisor_id) REFERENCES supervisor (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_vehiculo_precio   CHECK (precio_compra >= 0),
    CONSTRAINT chk_vehiculo_subtipo  CHECK (
        (tipo = 'MOTO'      AND cilindrada IS NOT NULL AND cilindrada > 0 AND capacidad IS NULL)
     OR (tipo = 'FURGONETA' AND capacidad  IS NOT NULL AND capacidad  > 0 AND cilindrada IS NULL)
     OR (tipo = 'AVION'     AND cilindrada IS NULL AND capacidad IS NULL)
    )
) ENGINE = InnoDB;

CREATE TABLE cliente (
    id              INT           NOT NULL AUTO_INCREMENT,
    codigo_cliente  VARCHAR(20)   NOT NULL,
    fecha_alta      DATE          NOT NULL,
    total_facturado DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    tipo            ENUM('PARTICULAR','EMPRESA') NOT NULL,
    nif             VARCHAR(20)   NULL,
    cif             VARCHAR(20)   NULL,
    nombre          VARCHAR(100)  NOT NULL,
    direccion       VARCHAR(200)  NOT NULL,
    telefono        VARCHAR(15)   NULL,
    CONSTRAINT pk_cliente          PRIMARY KEY (id),
    CONSTRAINT uq_cliente_codigo   UNIQUE (codigo_cliente),
    CONSTRAINT uq_cliente_nif      UNIQUE (nif),
    CONSTRAINT uq_cliente_cif      UNIQUE (cif),
    CONSTRAINT chk_cliente_facturado CHECK (total_facturado >= 0),
    CONSTRAINT chk_cliente_subtipo   CHECK (
        (tipo = 'PARTICULAR' AND nif IS NOT NULL AND cif IS NULL)
     OR (tipo = 'EMPRESA'    AND cif IS NOT NULL AND nif IS NULL AND telefono IS NOT NULL)
    )
) ENGINE = InnoDB;

CREATE TABLE mercancia (
    id                  INT           NOT NULL AUTO_INCREMENT,
    codigo              VARCHAR(20)   NOT NULL,
    descripcion         VARCHAR(200)  NOT NULL,
    precio_envio        DECIMAL(12,2) NOT NULL,
    asegurada           TINYINT(1)    NOT NULL DEFAULT 0,
    largo_recorrido     TINYINT(1)    NOT NULL DEFAULT 0,
    tipo                ENUM('SOBRE','PAQUETE') NOT NULL,
    peso                DECIMAL(10,3) NULL,
    largo               DECIMAL(10,2) NULL,
    ancho               DECIMAL(10,2) NULL,
    alto                DECIMAL(10,2) NULL,
    vehiculo_id         INT           NOT NULL,
    avion_id            INT           NULL,
    vehiculo_destino_id INT           NULL,
    CONSTRAINT pk_mercancia        PRIMARY KEY (id),
    CONSTRAINT uq_mercancia_codigo UNIQUE (codigo),
    CONSTRAINT fk_mercancia_vehiculo
        FOREIGN KEY (vehiculo_id)         REFERENCES vehiculo (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_mercancia_avion
        FOREIGN KEY (avion_id)            REFERENCES vehiculo (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_mercancia_destino
        FOREIGN KEY (vehiculo_destino_id) REFERENCES vehiculo (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_mercancia_precio    CHECK (precio_envio >= 0),
    CONSTRAINT chk_mercancia_asegurada CHECK (asegurada IN (0, 1)),
    CONSTRAINT chk_mercancia_subtipo   CHECK (
        (tipo = 'SOBRE'   AND peso  IS NOT NULL AND peso  > 0
                          AND largo IS NULL AND ancho IS NULL AND alto IS NULL)
     OR (tipo = 'PAQUETE' AND peso  IS NULL
                          AND largo IS NOT NULL AND largo > 0
                          AND ancho IS NOT NULL AND ancho > 0
                          AND alto  IS NOT NULL AND alto  > 0)
    ),
    -- Solo referencia columnas de la misma fila; si MySQL lo rechazara, mover a trg_mercancia_bi/bu
    CONSTRAINT chk_mercancia_largo_recorrido CHECK (
        (largo_recorrido = 1 AND avion_id IS NOT NULL AND vehiculo_destino_id IS NOT NULL)
     OR (largo_recorrido = 0 AND avion_id IS NULL     AND vehiculo_destino_id IS NULL)
    )
) ENGINE = InnoDB;

CREATE TABLE envio (
    id                INT           NOT NULL AUTO_INCREMENT,
    cliente_origen_id INT           NOT NULL,
    destinatario_id   INT           NOT NULL,
    mercancia_id      INT           NOT NULL,
    fecha_envio       DATE          NOT NULL,
    intentos_fallidos INT           NOT NULL DEFAULT 0,
    precio_total      DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_envio             PRIMARY KEY (id),
    CONSTRAINT uq_envio_mercancia   UNIQUE (mercancia_id),
    CONSTRAINT fk_envio_origen
        FOREIGN KEY (cliente_origen_id) REFERENCES cliente (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_envio_destinatario
        FOREIGN KEY (destinatario_id)   REFERENCES cliente (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_envio_mercancia
        FOREIGN KEY (mercancia_id)      REFERENCES mercancia (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_envio_intentos  CHECK (intentos_fallidos >= 0),
    CONSTRAINT chk_envio_precio    CHECK (precio_total >= 0),
    CONSTRAINT chk_envio_clientes  CHECK (cliente_origen_id <> destinatario_id)
) ENGINE = InnoDB;

-- ============================================================
-- PROCEDIMIENTO AUXILIAR
-- ============================================================

DELIMITER $$

-- Lógica de validación de tipos de vehículo compartida por trg_mercancia_bi/bu
CREATE PROCEDURE sp_validar_mercancia(
    IN p_tipo               VARCHAR(10),
    IN p_vehiculo_id        INT,
    IN p_avion_id           INT,
    IN p_vehiculo_destino_id INT
)
BEGIN
    DECLARE v_tipo VARCHAR(20);

    SELECT tipo INTO v_tipo FROM vehiculo WHERE id = p_vehiculo_id;
    IF p_tipo = 'SOBRE' AND v_tipo <> 'MOTO' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un sobre debe asignarse a una moto';
    END IF;
    IF p_tipo = 'PAQUETE' AND v_tipo <> 'FURGONETA' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un paquete debe asignarse a una furgoneta';
    END IF;

    IF p_avion_id IS NOT NULL THEN
        SELECT tipo INTO v_tipo FROM vehiculo WHERE id = p_avion_id;
        IF v_tipo <> 'AVION' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La mercancía de largo recorrido debe asignarse a un avión';
        END IF;
    END IF;

    IF p_vehiculo_destino_id IS NOT NULL THEN
        SELECT tipo INTO v_tipo FROM vehiculo WHERE id = p_vehiculo_destino_id;
        IF p_tipo = 'SOBRE' AND v_tipo <> 'MOTO' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'El vehículo de destino de un sobre debe ser una moto';
        END IF;
        IF p_tipo = 'PAQUETE' AND v_tipo <> 'FURGONETA' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'El vehículo de destino de un paquete debe ser una furgoneta';
        END IF;
    END IF;
END$$

-- ============================================================
-- TRIGGERS
-- ============================================================

-- Impide modificar el tipo de un cliente
CREATE TRIGGER trg_cliente_tipo_bu
BEFORE UPDATE ON cliente
FOR EACH ROW
BEGIN
    IF NEW.tipo <> OLD.tipo THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El tipo de cliente no puede modificarse';
    END IF;
END$$

-- Impide modificar el tipo de un vehículo (RF-02, inmutabilidad de tipo)
CREATE TRIGGER trg_vehiculo_tipo_bu
BEFORE UPDATE ON vehiculo
FOR EACH ROW
BEGIN
    IF NEW.tipo <> OLD.tipo THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El tipo de vehículo no puede modificarse';
    END IF;
END$$

-- Valida tipos de vehículos asignados a una mercancía (RF-21, RF-25)
CREATE TRIGGER trg_mercancia_bi
BEFORE INSERT ON mercancia
FOR EACH ROW
BEGIN
    CALL sp_validar_mercancia(NEW.tipo, NEW.vehiculo_id, NEW.avion_id, NEW.vehiculo_destino_id);
END$$

CREATE TRIGGER trg_mercancia_bu
BEFORE UPDATE ON mercancia
FOR EACH ROW
BEGIN
    CALL sp_validar_mercancia(NEW.tipo, NEW.vehiculo_id, NEW.avion_id, NEW.vehiculo_destino_id);
END$$

-- Impide registrar intentos fallidos en envíos a empresa (RF-30)
CREATE TRIGGER trg_envio_bi
BEFORE INSERT ON envio
FOR EACH ROW
BEGIN
    DECLARE v_tipo_dest VARCHAR(20);
    SELECT tipo INTO v_tipo_dest FROM cliente WHERE id = NEW.destinatario_id;
    IF v_tipo_dest = 'EMPRESA' AND NEW.intentos_fallidos > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un envío a una empresa no puede registrar intentos de entrega fallidos';
    END IF;
END$$

CREATE TRIGGER trg_envio_bu
BEFORE UPDATE ON envio
FOR EACH ROW
BEGIN
    DECLARE v_tipo_dest VARCHAR(20);
    SELECT tipo INTO v_tipo_dest FROM cliente WHERE id = NEW.destinatario_id;
    IF v_tipo_dest = 'EMPRESA' AND NEW.intentos_fallidos > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un envío a una empresa no puede registrar intentos de entrega fallidos';
    END IF;
END$$

DELIMITER ;
