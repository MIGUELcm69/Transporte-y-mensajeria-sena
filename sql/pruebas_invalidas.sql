-- ============================================================
-- pruebas_invalidas.sql  —  Casos que la BD debe rechazar
-- MySQL 8.3+
-- Ejecutar DESPUÉS de seed.sql
-- Cada sentencia debe fallar. Si alguna tiene éxito → revisar la restricción.
-- ============================================================

USE transportes_db;

-- ============================================================
-- Prueba 1: intentos_fallidos > 0 con destinatario EMPRESA
-- Esperado: trg_envio_bi → SQLSTATE '45000'
--           "Un envío a una empresa no puede registrar intentos de entrega fallidos"
-- CLI-002 (id=2) es EMPRESA; MER-002 (id=2) no tiene envío asignado aún.
-- ============================================================
INSERT INTO envio
    (cliente_origen_id, destinatario_id, mercancia_id, fecha_envio, intentos_fallidos, precio_total)
VALUES
    (1, 2, 2, '2026-03-01', 1, 15.00);

-- ============================================================
-- Prueba 2: empresa sin teléfono
-- Esperado: chk_cliente_subtipo → CHECK constraint failed
-- ============================================================
INSERT INTO cliente
    (codigo_cliente, fecha_alta, total_facturado, tipo, nif, cif, nombre, direccion, telefono)
VALUES
    ('CLI-ERR', '2026-01-01', 0.00, 'EMPRESA', NULL, 'B99999999', 'Sin Teléfono S.L.', 'Calle Prueba 1, Madrid', NULL);

-- ============================================================
-- Prueba 3: cambiar el tipo de un cliente existente
-- Esperado: trg_cliente_tipo_bu → SQLSTATE '45000'
--           "El tipo de cliente no puede modificarse"
-- CLI-001 (id=1) es PARTICULAR; se intenta cambiar a EMPRESA.
-- ============================================================
UPDATE cliente
   SET tipo = 'EMPRESA',
       nif  = NULL,
       cif  = 'B11111111',
       telefono = '900000000'
 WHERE id = 1;

-- ============================================================
-- Prueba 4: sobre asignado a furgoneta
-- Viola: RF-21 → trg_mercancia_bi → sp_validar_mercancia
-- Esperado: SQLSTATE '45000' "Un sobre debe asignarse a una moto"
-- vehiculo_id=2 es FURGONETA
-- ============================================================
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,  largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-ERR1', 'Sobre asignado a furgoneta', 5.00, 0, 0, 'SOBRE',
     0.030, NULL, NULL, NULL, 2, NULL, NULL);

-- ============================================================
-- Prueba 5: paquete asignado a moto
-- Viola: RF-21 → trg_mercancia_bi → sp_validar_mercancia
-- Esperado: SQLSTATE '45000' "Un paquete debe asignarse a una furgoneta"
-- vehiculo_id=1 es MOTO
-- ============================================================
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,  largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-ERR2', 'Paquete asignado a moto', 20.00, 0, 0, 'PAQUETE',
     NULL, 25.00, 15.00, 10.00, 1, NULL, NULL);

-- ============================================================
-- Prueba 6: avión asignado a mercancía sin largo recorrido
-- Viola: chk_mercancia_largo_recorrido
--   (largo_recorrido=0 AND avion_id IS NULL) falla porque avion_id IS NOT NULL
-- Esperado: CHECK constraint failed
-- ============================================================
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,  largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-ERR3', 'Sobre con avion pero sin largo recorrido', 8.00, 0, 0, 'SOBRE',
     0.040, NULL, NULL, NULL, 1, 3, NULL);

-- ============================================================
-- Prueba 7: largo recorrido sin avión
-- Viola: chk_mercancia_largo_recorrido
--   (largo_recorrido=1 AND avion_id IS NOT NULL) falla porque avion_id IS NULL
-- Esperado: CHECK constraint failed
-- ============================================================
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,  largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-ERR4', 'Sobre largo recorrido sin avion', 12.00, 0, 1, 'SOBRE',
     0.060, NULL, NULL, NULL, 1, NULL, 1);

-- ============================================================
-- Prueba 8: cliente_origen_id = destinatario_id
-- Viola: chk_envio_clientes CHECK (cliente_origen_id <> destinatario_id)
-- Esperado: CHECK constraint failed
-- MER-002 (id=2) no tiene envío; se usa como mercancía del intento.
-- ============================================================
INSERT INTO envio
    (cliente_origen_id, destinatario_id, mercancia_id, fecha_envio, intentos_fallidos, precio_total)
VALUES
    (1, 1, 2, '2026-03-10', 0, 15.00);

-- ============================================================
-- Prueba 9: cambiar el tipo de un vehículo existente
-- Viola: trg_vehiculo_tipo_bu → SQLSTATE '45000'
--        "El tipo de vehículo no puede modificarse"
-- VEH-001 (id=1) es MOTO; se intenta cambiar a FURGONETA.
-- ============================================================
UPDATE vehiculo
   SET tipo       = 'FURGONETA',
       cilindrada = NULL,
       capacidad  = 2500.00
 WHERE id = 1;

-- ============================================================
-- Prueba 10: moto sin cilindrada
-- Viola: chk_vehiculo_subtipo
--   (tipo='MOTO' AND cilindrada IS NOT NULL AND cilindrada > 0 AND capacidad IS NULL)
-- Esperado: CHECK constraint failed
-- ============================================================
INSERT INTO vehiculo
    (codigo, tipo, descripcion, fecha_compra, precio_compra, supervisor_id, cilindrada, capacidad)
VALUES
    ('VEH-ERR', 'MOTO', 'Moto sin cilindrada', '2024-01-01', 1500.00, 1, NULL, NULL);

-- ============================================================
-- Prueba 11: particular sin nif
-- Viola: chk_cliente_subtipo
--   (tipo='PARTICULAR' AND nif IS NOT NULL AND cif IS NULL)
-- Esperado: CHECK constraint failed
-- ============================================================
INSERT INTO cliente
    (codigo_cliente, fecha_alta, total_facturado, tipo, nif, cif, nombre, direccion, telefono)
VALUES
    ('CLI-ERR2', '2026-01-01', 0.00, 'PARTICULAR', NULL, NULL, 'Particular Sin NIF', 'Calle Prueba 2, Madrid', NULL);

-- ============================================================
-- Prueba 12: largo recorrido con avion_id apuntando a una moto (RF-25)
-- Viola: trg_mercancia_bi → sp_validar_mercancia
--   avion_id=1 es MOTO, pero debe ser AVION
-- Esperado: SQLSTATE '45000' "avion_id debe referenciar un avión"
-- (El CHECK chk_mercancia_largo_recorrido no falla porque avion_id IS NOT NULL
--  y vehiculo_destino_id IS NOT NULL; la validación cruzada recae en el trigger.)
-- ============================================================
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,  largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-ERR5', 'Sobre largo recorrido con avion_id incorrecto', 10.00, 0, 1, 'SOBRE',
     0.050, NULL, NULL, NULL, 1, 1, 1);

-- ============================================================
-- Prueba 13: sobre de largo recorrido con vehiculo_destino_id apuntando a furgoneta (RF-21 destino)
-- Viola: trg_mercancia_bi → sp_validar_mercancia
--   tipo='SOBRE' exige MOTO en vehiculo_destino_id; aquí apunta a FURGONETA (id=2)
-- Esperado: SQLSTATE '45000' "El vehículo de destino de un sobre debe ser una moto"
-- ============================================================
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,  largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-ERR6', 'Sobre largo recorrido con destino furgoneta', 10.00, 0, 1, 'SOBRE',
     0.050, NULL, NULL, NULL, 1, 3, 2);

-- ============================================================
-- Prueba 14: UPDATE de mercancía existente con vehiculo_id de tipo incorrecto (trg_mercancia_bu)
-- Viola: trg_mercancia_bu → sp_validar_mercancia
--   MER-001 (id=1) es SOBRE; se cambia vehiculo_id a 2 (FURGONETA), pero SOBRE exige MOTO
-- Esperado: SQLSTATE '45000' "Un sobre debe asignarse a una moto"
-- ============================================================
UPDATE mercancia
   SET vehiculo_id = 2
 WHERE id = 1;
