-- ============================================================
-- seed.sql  —  Datos de prueba
-- MySQL 8.3+
-- Ejecutar DESPUÉS de schema.sql (los IDs asumen AUTO_INCREMENT desde 1)
-- Re-ejecutar desde schema.sql si se necesita limpiar y volver a cargar
-- ============================================================

USE transportes_db;

-- ============================================================
-- SUPERVISOR  (id=1)
-- ============================================================
INSERT INTO supervisor (nif, nombre, direccion, telefono) VALUES
    ('12345678A', 'Carlos García López', 'Calle Mayor 10, Madrid', '912345678');

-- ============================================================
-- VEHÍCULOS  (id: 1=MOTO, 2=FURGONETA, 3=AVION)
-- ============================================================
INSERT INTO vehiculo (codigo, tipo, descripcion, fecha_compra, precio_compra, supervisor_id, cilindrada, capacidad) VALUES
    ('VEH-001', 'MOTO',      'Honda CB125F',           '2023-03-15',  3200.00, 1, 125.00,  NULL),
    ('VEH-002', 'FURGONETA', 'Mercedes Sprinter 314',  '2022-06-01', 28000.00, 1,   NULL, 2000.00),
    ('VEH-003', 'AVION',     'Cessna 208 Caravan',     '2021-09-10', 950000.00, 1,  NULL,  NULL);

-- ============================================================
-- CLIENTES  (id: 1=PARTICULAR, 2=EMPRESA)
-- total_facturado se precalcula con los envíos que siguen:
--   CLI-001: precio_total ENV-001 = 10.00
--   CLI-002: precio_total ENV-002 = 26.50
-- ============================================================
INSERT INTO cliente (codigo_cliente, fecha_alta, total_facturado, tipo, nif, cif, nombre, direccion, telefono) VALUES
    ('CLI-001', '2025-01-10',  10.00, 'PARTICULAR', '87654321B', NULL,       'Juan Martínez Ruiz',    'Av. Libertad 5, Valencia',            NULL),
    ('CLI-002', '2025-02-20',  26.50, 'EMPRESA',    NULL,        'B12345678','Logística Rápida S.L.', 'Pol. Industrial Norte 12, Barcelona', '934567890');

-- ============================================================
-- MERCANCÍAS
-- Los triggers trg_mercancia_bi validan que vehiculo_id, avion_id
-- y vehiculo_destino_id sean del tipo correcto según la mercancía.
-- ============================================================

-- SOBRE sin largo recorrido → vehiculo_id debe ser MOTO (VEH-001=id 1)
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,   largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-001', 'Sobre documentos contrato', 10.00, 0, 0, 'SOBRE',
     0.050, NULL, NULL, NULL, 1, NULL, NULL);

-- SOBRE con largo recorrido → vehiculo_id=MOTO, avion_id=AVION, vehiculo_destino_id=MOTO
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,   largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-002', 'Sobre documentos internacionales', 15.00, 1, 1, 'SOBRE',
     0.080, NULL, NULL, NULL, 1, 3, 1);

-- PAQUETE sin largo recorrido → vehiculo_id debe ser FURGONETA (VEH-002=id 2)
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,   largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-003', 'Paquete electrónica', 25.00, 1, 0, 'PAQUETE',
     NULL, 30.00, 20.00, 15.00, 2, NULL, NULL);

-- PAQUETE con largo recorrido → vehiculo_id=FURGONETA, avion_id=AVION, vehiculo_destino_id=FURGONETA
INSERT INTO mercancia
    (codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo,
     peso,   largo, ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)
VALUES
    ('MER-004', 'Paquete maquinaria industrial', 40.00, 1, 1, 'PAQUETE',
     NULL, 80.00, 60.00, 50.00, 2, 3, 2);

-- ============================================================
-- ENVÍOS
-- ============================================================

-- ENV-001: CLI-001 (particular) envía a CLI-002 (empresa)
--   destinatario = EMPRESA → plus = 0 → precio_total = 10.00
INSERT INTO envio (cliente_origen_id, destinatario_id, mercancia_id, fecha_envio, intentos_fallidos, precio_total)
VALUES (1, 2, 1, '2026-01-15', 0, 10.00);

-- ENV-002: CLI-002 (empresa) envía a CLI-001 (particular), 2 intentos fallidos
--   destinatario = PARTICULAR → plus = 25.00 × (0.04 + 0.01×2) = 25.00 × 0.06 = 1.50
--   precio_total = 25.00 + 1.50 = 26.50
INSERT INTO envio (cliente_origen_id, destinatario_id, mercancia_id, fecha_envio, intentos_fallidos, precio_total)
VALUES (2, 1, 3, '2026-02-10', 2, 26.50);
