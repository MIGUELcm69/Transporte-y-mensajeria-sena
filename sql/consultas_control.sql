-- ============================================================
-- consultas_control.sql  —  Consultas de control de integridad
-- MySQL 8.3+  |  Solo lectura (únicamente SELECT)
-- Ejecutar después de schema.sql (y seed.sql, si se quieren datos).
--
-- Cada consulta busca filas que incumplen una regla de negocio:
-- con una base sana, las TRES deben devolver 0 filas.
-- Si alguna devuelve filas, cada fila es una incoherencia que revisar.
-- Los resultados solo muestran ids, códigos e importes; nunca nombres,
-- NIF, CIF, direcciones ni teléfonos.
-- ============================================================

USE transportes_db;

-- ------------------------------------------------------------
-- 1) Supervisores sin ningún vehículo a cargo (RF-13).
--    Todo supervisor se registra con su primer vehículo y no se puede
--    eliminar ni reasignar su último vehículo: debe devolver 0 filas.
-- ------------------------------------------------------------
SELECT s.id AS supervisor_id
FROM supervisor s
WHERE NOT EXISTS (SELECT 1 FROM vehiculo v WHERE v.supervisor_id = s.id)
ORDER BY s.id;

-- ------------------------------------------------------------
-- 2) Clientes cuyo total_facturado no coincide con la suma de
--    precio_total de los envíos que encargaron como cliente origen (RF-17).
--    Un cliente sin envíos debe tener total_facturado = 0.
--    Debe devolver 0 filas.
-- ------------------------------------------------------------
SELECT c.codigo_cliente,
       c.total_facturado,
       COALESCE(e.suma_envios, 0.00) AS suma_envios
FROM cliente c
LEFT JOIN (SELECT cliente_origen_id, SUM(precio_total) AS suma_envios
           FROM envio
           GROUP BY cliente_origen_id) e
       ON e.cliente_origen_id = c.id
WHERE c.total_facturado <> COALESCE(e.suma_envios, 0.00)
ORDER BY c.codigo_cliente;

-- ------------------------------------------------------------
-- 3) Envíos cuyo precio_total no coincide con la fórmula del plus
--    (RF-31/RF-32), la misma de Envio.calcularPlus:
--      destinatario PARTICULAR: plus = ROUND(precio_envio × (0.04 + 0.01 × intentos_fallidos), 2)
--      destinatario EMPRESA:    plus = 0
--      precio_total = precio_envio + plus
--    ROUND de MySQL sobre DECIMAL redondea la mitad hacia arriba, igual que
--    RoundingMode.HALF_UP para importes positivos. Debe devolver 0 filas.
-- ------------------------------------------------------------
SELECT e.id AS envio_id,
       m.codigo AS mercancia_codigo,
       d.tipo AS tipo_destinatario,
       e.intentos_fallidos,
       m.precio_envio,
       e.precio_total,
       calculo.precio_esperado
FROM envio e
JOIN mercancia m ON m.id = e.mercancia_id
JOIN cliente d   ON d.id = e.destinatario_id
CROSS JOIN LATERAL (
    SELECT m.precio_envio
         + CASE d.tipo
               WHEN 'PARTICULAR'
                   THEN ROUND(m.precio_envio * (0.04 + 0.01 * e.intentos_fallidos), 2)
               ELSE 0.00
           END AS precio_esperado
) calculo
WHERE e.precio_total <> calculo.precio_esperado
ORDER BY e.id;
