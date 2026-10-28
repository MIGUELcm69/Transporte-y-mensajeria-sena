# Plan de implementación — Sistema de Transportes y Mensajería

## 1. Modelo Relacional (MySQL 8.3)

### Estrategia de herencia: Single Table Inheritance (STI)

| Jerarquía | Tabla | Columnas extra (nullable) |
|---|---|---|
| Vehiculo → Moto, Furgoneta, Avion | `vehiculo` | `cilindrada` (MOTO), `capacidad` (FURGONETA) |
| Mercancia → Sobre, Paquete | `mercancia` | `peso` (SOBRE), `largo/ancho/alto` (PAQUETE) |
| Cliente → Particular, Empresa | `cliente` | `nif` (PARTICULAR), `cif`/`telefono` (EMPRESA) |

**Justificación:** pocas columnas extra (máx. 4 nullable por tabla), consultas "listar todo" sin JOINs, CHECK de MySQL 8 cubre restricciones de subtipo en la misma fila. Tabla por subclase añadiría JOINs sin beneficio para este volumen.

---

### Tablas, PK, FK, UNIQUE, CHECK

#### `supervisor`
| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | INT | PK AUTO_INCREMENT |
| `nif` | VARCHAR(20) | UNIQUE NOT NULL |
| `nombre` | VARCHAR(100) | NOT NULL |
| `direccion` | VARCHAR(200) | NOT NULL |
| `telefono` | VARCHAR(15) | NOT NULL |

#### `vehiculo`
| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | INT | PK AUTO_INCREMENT |
| `codigo` | VARCHAR(20) | UNIQUE NOT NULL |
| `tipo` | ENUM('MOTO','FURGONETA','AVION') | NOT NULL |
| `descripcion` | VARCHAR(200) | NOT NULL |
| `fecha_compra` | DATE | NOT NULL |
| `precio_compra` | DECIMAL(12,2) | NOT NULL, CHECK (precio_compra >= 0) |
| `supervisor_id` | INT | NOT NULL, FK → supervisor(id) RESTRICT |
| `cilindrada` | DECIMAL(8,2) | NULL |
| `capacidad` | DECIMAL(10,2) | NULL |

```sql
CHECK (
  (tipo = 'MOTO'      AND cilindrada IS NOT NULL AND cilindrada > 0 AND capacidad IS NULL) OR
  (tipo = 'FURGONETA' AND capacidad  IS NOT NULL AND capacidad  > 0 AND cilindrada IS NULL) OR
  (tipo = 'AVION'     AND cilindrada IS NULL AND capacidad IS NULL)
)
```

**Trigger de inmutabilidad:** `trg_vehiculo_tipo_bu` (BEFORE UPDATE) — señala error si `NEW.tipo <> OLD.tipo`.

#### `cliente`
| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | INT | PK AUTO_INCREMENT |
| `codigo_cliente` | VARCHAR(20) | UNIQUE NOT NULL |
| `fecha_alta` | DATE | NOT NULL |
| `total_facturado` | DECIMAL(14,2) | NOT NULL DEFAULT 0.00, CHECK (total_facturado >= 0) |
| `tipo` | ENUM('PARTICULAR','EMPRESA') | NOT NULL |
| `nif` | VARCHAR(20) | NULL, UNIQUE |
| `cif` | VARCHAR(20) | NULL, UNIQUE |
| `nombre` | VARCHAR(100) | NOT NULL |
| `direccion` | VARCHAR(200) | NOT NULL |
| `telefono` | VARCHAR(15) | NULL (obligatorio para EMPRESA) |

*NIF/CIF admiten 20 caracteres (p. ej. un NIT colombiano `900123456-7` mide 11); `Validacion.LONGITUD_DOCUMENTO = 20`.*

**Trigger de inmutabilidad:** `trg_cliente_tipo_bu` (BEFORE UPDATE) — señala error si `NEW.tipo <> OLD.tipo`.

*UNIQUE(nif) y UNIQUE(cif): MySQL permite múltiples NULL en un índice UNIQUE, por lo que solo restringe valores no nulos.*

*`telefono` es obligatorio para EMPRESA: lo exigen el CHECK `chk_cliente_subtipo` y el modelo (`ClienteEmpresa`). La columna admite NULL porque PARTICULAR no lo tiene.*

*`fecha_alta` la pone el controlador con `LocalDate.now()` al registrar el cliente; el modelo la exige no nula (P-07).*

```sql
CHECK (
  (tipo = 'PARTICULAR' AND nif IS NOT NULL AND cif IS NULL) OR
  (tipo = 'EMPRESA'    AND cif IS NOT NULL AND nif IS NULL AND telefono IS NOT NULL)
)
```

#### `mercancia`
| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | INT | PK AUTO_INCREMENT |
| `codigo` | VARCHAR(20) | UNIQUE NOT NULL |
| `descripcion` | VARCHAR(200) | NOT NULL |
| `precio_envio` | DECIMAL(12,2) | NOT NULL, CHECK (precio_envio >= 0) |
| `asegurada` | TINYINT(1) | NOT NULL DEFAULT 0 |
| `largo_recorrido` | TINYINT(1) | NOT NULL DEFAULT 0 |
| `tipo` | ENUM('SOBRE','PAQUETE') | NOT NULL |
| `peso` | DECIMAL(10,3) | NULL |
| `largo` | DECIMAL(10,2) | NULL |
| `ancho` | DECIMAL(10,2) | NULL |
| `alto` | DECIMAL(10,2) | NULL |
| `vehiculo_id` | INT | NOT NULL, FK → vehiculo(id) RESTRICT |
| `avion_id` | INT | NULL, FK → vehiculo(id) RESTRICT |
| `vehiculo_destino_id` | INT | NULL, FK → vehiculo(id) RESTRICT |

```sql
-- Subtipo
CHECK (
  (tipo = 'SOBRE'   AND peso IS NOT NULL AND peso > 0
                    AND largo IS NULL AND ancho IS NULL AND alto IS NULL) OR
  (tipo = 'PAQUETE' AND peso IS NULL
                    AND largo IS NOT NULL AND largo > 0
                    AND ancho IS NOT NULL AND ancho > 0
                    AND alto  IS NOT NULL AND alto  > 0)
)

-- Largo recorrido (referencia columnas FK, sin subconsulta — válido en MySQL 8)
-- Si MySQL lo rechaza en prueba, estas dos condiciones pasan a trg_mercancia_bi/bu.
CHECK (
  (largo_recorrido = 1 AND avion_id IS NOT NULL AND vehiculo_destino_id IS NOT NULL) OR
  (largo_recorrido = 0 AND avion_id IS NULL     AND vehiculo_destino_id IS NULL)
)
```

**Trigger de tipos cruzados** `trg_mercancia_bi` / `trg_mercancia_bu` (BEFORE INSERT/UPDATE):
- `vehiculo_id` → MOTO si tipo='SOBRE', FURGONETA si tipo='PAQUETE' (RF-21)
- `avion_id` → tipo='AVION' cuando NOT NULL (RF-25)
- `vehiculo_destino_id` → MOTO si tipo='SOBRE', FURGONETA si tipo='PAQUETE' (RF-21 + RF-24)

#### `envio`
| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | INT | PK AUTO_INCREMENT |
| `cliente_origen_id` | INT | NOT NULL, FK → cliente(id) RESTRICT |
| `destinatario_id` | INT | NOT NULL, FK → cliente(id) RESTRICT |
| `mercancia_id` | INT | NOT NULL, UNIQUE, FK → mercancia(id) RESTRICT |
| `fecha_envio` | DATE | NOT NULL |
| `intentos_fallidos` | INT | NOT NULL DEFAULT 0, CHECK (intentos_fallidos >= 0) |
| `precio_total` | DECIMAL(12,2) | NOT NULL, CHECK (precio_total >= 0) |

```sql
CHECK (cliente_origen_id <> destinatario_id)
```

**Trigger RF-30** `trg_envio_bi` / `trg_envio_bu` (BEFORE INSERT/UPDATE): señala error si el destinatario es EMPRESA e `intentos_fallidos > 0`.

---

## 2. Triggers vs Controlador

### Triggers (inmutabilidad y reglas que requieren consultar otra tabla)

Seis triggers en `sql/schema.sql`:

| Trigger | Valida | RF |
|---|---|---|
| `trg_vehiculo_tipo_bu` BEFORE UPDATE | `NEW.tipo = OLD.tipo` siempre | RF-02, inmutabilidad |
| `trg_cliente_tipo_bu` BEFORE UPDATE | `NEW.tipo = OLD.tipo` siempre | inmutabilidad |
| `trg_mercancia_bi` / `trg_mercancia_bu` BEFORE INSERT/UPDATE | llaman a `sp_validar_mercancia` (ver abajo) | RF-21, RF-24, RF-25 |
| `trg_envio_bi` / `trg_envio_bu` BEFORE INSERT/UPDATE | destinatario EMPRESA ⇒ `intentos_fallidos = 0` | RF-30 |

**Procedimiento `sp_validar_mercancia(tipo, vehiculo_id, avion_id, vehiculo_destino_id)`**, compartido por `trg_mercancia_bi/bu`:
- `vehiculo_id` es MOTO si tipo='SOBRE', FURGONETA si tipo='PAQUETE' (RF-21)
- `avion_id` apunta a tipo='AVION' cuando NOT NULL (RF-25)
- `vehiculo_destino_id` es MOTO/FURGONETA según tipo cuando NOT NULL (RF-21 + RF-24)

Los mensajes de `SIGNAL` están pensados para el usuario final (sin nombres de columna ni términos técnicos); el DAO los pasa tal cual en `ReglaAsignacionException`.

Las demás restricciones (subtipos, largo_recorrido ↔ avion_id) van en CHECK de definición de tabla.

### Controlador (reglas de negocio multi-entidad)

| Regla | RF / Nota |
|---|---|
| Crear supervisor + primer vehículo en una sola transacción | RF-08/RF-13, P-01 |
| Bloquear eliminación de supervisor con vehículos a cargo | RF-11 |
| Bloquear eliminación de vehículo con mercancías o envíos asignados | RF-06 |
| **Bloquear modificación de `precio_envio` si la mercancía ya tiene un envío asociado** (mensaje claro) | RF-26 |
| Crear envío: `UPDATE cliente SET total_facturado = total_facturado + ? WHERE id = cliente_origen_id` | RF-17 |
| Eliminar envío: `UPDATE cliente SET total_facturado = total_facturado - ? WHERE id = cliente_origen_id` | RF-17 |
| Modificar `precio_total` de un envío (mismo cliente): `total_facturado + (nuevo - viejo)` atómico | RF-17 |
| Modificar `cliente_origen_id` de un envío: restar del cliente anterior y sumar al nuevo en la misma transacción | RF-17 |
| Registrar intento fallido: obtener el envío recalculado con `Envio.conIntentoFallido`, ajustar `total_facturado` con la diferencia | RF-33 |
| Invocar el cálculo de plus y `precio_total`, que vive en `Envio` (`calcularPlus`, `calcularPrecioTotal`, `nuevo`, `conIntentoFallido`); el controlador no calcula | RF-31/RF-32 |
| Poner `fecha_alta = LocalDate.now()` al registrar un cliente | P-07 |
| Validar tipos de vehículo (sobre → moto, paquete → furgoneta, avión) antes de guardar, para dar un mensaje claro | RF-21/RF-24/RF-25 |
| Contar aviones por supervisor (query derivada) | RF-10 |

**Actualización atómica de `total_facturado`:**
```sql
-- Siempre con operación incremental, nunca read-modify-write en la app
UPDATE cliente SET total_facturado = total_facturado + ? WHERE id = ?
-- Para restar: pasar valor negativo
UPDATE cliente SET total_facturado = total_facturado + ? WHERE id = ?  -- ? = -precio_total
```

**total_facturado incluye el plus:** `precio_total = precio_envio + plus`, donde `plus = precio_envio × (0.04 + 0.01 × intentos_fallidos)` para destinatario PARTICULAR; `plus = 0` para EMPRESA. `total_facturado` refleja lo facturado incluyendo sobrecargo.

---

## 3. Dinero y redondeo

Todas las operaciones monetarias en Java usan `BigDecimal` con escala 2 y `RoundingMode.HALF_UP`.
El cálculo del plus y del precio total está en `Envio` (`calcularPlus`, `calcularPrecioTotal`); `EnvioController` solo lo invoca:

```java
BigDecimal factor = new BigDecimal("0.04")
    .add(new BigDecimal("0.01").multiply(BigDecimal.valueOf(intentosFallidos)));
BigDecimal plus = precioEnvio.multiply(factor).setScale(2, RoundingMode.HALF_UP);
BigDecimal precioTotal = precioEnvio.add(plus).setScale(2, RoundingMode.HALF_UP);
```

Columnas `DECIMAL(x,2)` en BD: la escala Java siempre coincide con la BD.

---

## 4. Estructura de paquetes y clases principales

```
com.transportes
├── Main.java
├── config/
│   └── ConexionBD.java              # Utilidad estática, no Singleton: carga la configuración (entorno o config.properties) al llamar a getConnection()
├── model/
│   ├── Vehiculo.java                # Abstract
│   ├── Moto.java                    # + cilindrada: BigDecimal
│   ├── Furgoneta.java               # + capacidad: BigDecimal
│   ├── Avion.java
│   ├── TipoVehiculo.java            # enum MOTO, FURGONETA, AVION
│   ├── Mercancia.java               # Abstract
│   ├── Sobre.java                   # + peso: BigDecimal
│   ├── Paquete.java                 # + largo, ancho, alto: BigDecimal
│   ├── TipoMercancia.java           # enum SOBRE, PAQUETE
│   ├── Cliente.java                 # Abstract
│   ├── ClienteParticular.java       # + nif: String
│   ├── ClienteEmpresa.java          # + cif, telefono: String
│   ├── TipoCliente.java             # enum PARTICULAR, EMPRESA
│   ├── Supervisor.java
│   ├── Envio.java                   # clienteOrigenId, destinatarioId, mercanciaId, fechaEnvio, intentosFallidos, precioTotal;
│   │                                #   calcularPlus, calcularPrecioTotal, nuevo, conIntentoFallido
│   ├── Validacion.java              # interna del paquete: invariantes (textos, longitudes, importes, medidas, ids)
│   └── dao/
│       ├── SoporteJdbc.java         # interna: conexión propia/externa, PreparedStatement, try-with-resources
│       ├── TraductorErroresSql.java # interna: SQLException → excepción con mensaje para el usuario + log
│       ├── VehiculoDao.java         # Interface: save, update, delete, findById, findByCodigo, findByTipo, findBySupervisor, findAll, contarPorSupervisor, tieneMercancias
│       ├── VehiculoDaoImpl.java
│       ├── SupervisorDao.java       # Interface: save, update, delete, findById, findByNif, findAll, contarAviones
│       ├── SupervisorDaoImpl.java
│       ├── ClienteDao.java          # Interface: save, update, delete, findById, findByCodigo, findAll, sumarTotalFacturado
│       ├── ClienteDaoImpl.java
│       ├── MercanciaDao.java        # Interface: save, update, delete, findById, findByCodigo, findByVehiculo, findLargoRecorrido, findAll, tieneEnvio
│       ├── MercanciaDaoImpl.java
│       ├── EnvioDao.java            # Interface: save, update, delete, findById, findByCliente, findAll, actualizarIntentos
│       └── EnvioDaoImpl.java
├── controller/
│   ├── Transacciones.java           # Interface: ejecutar(TrabajoTransaccional) con commit/rollback
│   ├── TransaccionesJdbc.java       # Connection con autoCommit=false; SQLException de conexión/commit → AccesoDatosException
│   ├── TrabajoTransaccional.java    # Interface funcional: T ejecutar(Connection), sin SQLException (los DAO ya la traducen)
│   ├── FichaSupervisor.java         # record: supervisor + vehículos a cargo + numeroAviones() (RF-10, RF-12)
│   ├── VehiculoController.java      # guards RF-06 y RF-13 (último vehículo) al eliminar o reasignar
│   ├── SupervisorController.java    # transacción supervisor + primer vehículo; eliminar / eliminarTraspasandoVehiculos
│   ├── ClienteController.java
│   ├── MercanciaController.java     # incluye guard de precio_envio si hay envío
│   └── EnvioController.java        # invoca Envio.nuevo/conIntentoFallido; transacción total_facturado (crear/modificar/eliminar/intentos)
├── view/
│   ├── MenuPrincipal.java
│   ├── Consola.java                 # leerInt, leerDecimal, leerFecha, leerTextoNoVacio
│   ├── VehiculoView.java
│   ├── SupervisorView.java
│   ├── ClienteView.java
│   ├── MercanciaView.java
│   └── EnvioView.java
└── exception/
    ├── DomainException.java         # Base unchecked
    ├── DatoInvalidoException.java   # invariantes del modelo, CHECK (3819), fuera de rango (1264)
    ├── VehiculoEnUsoException.java
    ├── SupervisorConVehiculosException.java
    ├── MercanciaConEnvioException.java
    ├── ReglaAsignacionException.java
    ├── EntidadNoEncontradaException.java
    ├── RegistroDuplicadoException.java  # UNIQUE violado (1062), mensaje según la restricción
    ├── ClienteConEnviosException.java   # borrar cliente con envíos (1451 sobre fk_envio_origen/destinatario)
    ├── UltimoVehiculoException.java     # eliminar o reasignar el último vehículo de un supervisor (RF-13)
    ├── ConfiguracionException.java  # infraestructura; hereda de RuntimeException, no de DomainException
    └── AccesoDatosException.java    # infraestructura; hereda de RuntimeException; errores de BD no previstos, 1205, 1213
```

**Traducción de `SQLException` en el DAO** (detalle técnico al log con `java.util.logging`; nunca el mensaje crudo de MySQL):

| Código MySQL | Excepción |
|---|---|
| 1062 duplicado | `RegistroDuplicadoException` (mensaje según `uq_*`; genérico si no se reconoce) |
| 3819 CHECK | `DatoInvalidoException` |
| 1644 SIGNAL de trigger | `ReglaAsignacionException` con el mensaje del trigger |
| 1451 FK RESTRICT | según la FK: `fk_vehiculo_supervisor` → `SupervisorConVehiculosException`; `fk_mercancia_*` → `VehiculoEnUsoException`; `fk_envio_mercancia` → `MercanciaConEnvioException`; `fk_envio_origen/destinatario` → `ClienteConEnviosException` |
| 1452 FK inexistente | `DatoInvalidoException` |
| 1264 fuera de rango | `DatoInvalidoException` |
| 1205, 1213 y resto | `AccesoDatosException` (mensaje genérico) |

---

## 5. Etapas verificables

- [x] **Etapa 0 · Verificación de compilación**
  - [x] Ejecutar `mvn -q verify`; si falla por `junit-jupiter`, corregir a versión disponible y documentar
  - [x] Confirmar versión de `mysql-connector-j`

- [x] **Etapa 1 · Esquema de BD**
  - [x] `sql/schema.sql`: tablas en orden de dependencia (supervisor → vehiculo → cliente → mercancia → envio), con DROP TABLE IF EXISTS en orden inverso al inicio
  - [x] CHECK de subtipos en vehiculo, cliente, mercancia (subtipo + largo_recorrido)
  - [x] CHECK `cliente_origen_id <> destinatario_id` en envio
  - [x] FKs con ON DELETE RESTRICT en todas las relaciones
  - [x] UNIQUE: vehiculo.codigo, cliente.codigo_cliente, cliente.nif, cliente.cif, mercancia.codigo, envio.mercancia_id
  - [x] Triggers: `trg_vehiculo_tipo_bu`, `trg_cliente_tipo_bu`, `trg_mercancia_bi`, `trg_mercancia_bu`, `trg_envio_bi`, `trg_envio_bu`; procedimiento `sp_validar_mercancia`
  - [x] `sql/usuario.sql`: `CREATE USER 'transportes_app'@'localhost' IDENTIFIED BY 'CAMBIAR_ESTA_CLAVE'`; GRANT SELECT, INSERT, UPDATE, DELETE sobre el schema (sin DDL ni SUPER); contraseña real documentada en `config.properties.example`
  - [x] `sql/seed.sql`: ≥1 supervisor, ≥1 de cada tipo de vehículo, ≥1 de cada tipo de cliente, ≥1 mercancía de cada tipo con y sin largo_recorrido, ≥1 envío a particular (con intentos) y a empresa
  - [x] Verificación: cargar los tres scripts en MySQL Workbench con usuario `transportes_app`, confirmar 0 errores

- [x] **Etapa 2 · Config y conexión**
  - [x] `src/main/resources/config.properties.example` con claves `db.url`, `db.usuario`, `db.password` (sin valores reales; nota sobre `CAMBIAR_ESTA_CLAVE` de `usuario.sql`)
  - [x] Confirmar `config.properties` en `.gitignore`
  - [x] `ConexionBD.java`: carga desde `config.properties`, devuelve `Connection`
  - [x] Verificación: `mvn -q verify` compila

- [x] **Etapa 3 · Capa Model**
  - [x] Enums `TipoVehiculo`, `TipoMercancia`, `TipoCliente`
  - [x] Jerarquías Vehiculo, Mercancia, Cliente (abstract + concretas); todos los campos monetarios como `BigDecimal`
  - [x] `Supervisor`, `Envio`, excepciones de dominio
  - [x] Verificación: `mvn -q verify`

- [x] **Etapa 4 · Capa DAO**
  - [x] Interfaces + implementaciones JDBC para las 5 entidades
  - [x] `PreparedStatement` en todo; `try-with-resources` en todos los métodos
  - [x] Sobrecarga con `Connection` externa en cada método que escribe (sin commit/rollback/close)
  - [x] Mapeo ResultSet → objeto con factory por tipo (STI)
  - [x] `MercanciaDao.tieneEnvio(int mercanciaId)` para el guard de precio_envio
  - [x] `ClienteDao.sumarTotalFacturado(int clienteId, BigDecimal delta, Connection)` con SQL atómico
  - [x] Traducción de `SQLException` (1062, 3819, 1644, 1451, 1452, 1264, resto) con log `java.util.logging`
  - [x] Verificación: `mvn -q verify` + test de conexión manual

- [ ] **Etapa 5 · Capa Controller**
  - [ ] CRUD por módulo; guards de eliminación (RF-06, RF-11)
  - [ ] Guard en `MercanciaController`: rechaza modificar `precio_envio` si `tieneEnvio = true`
  - [ ] Transacción atómica supervisor + primer vehículo
  - [ ] `EnvioController.crear`: obtener el envío con `Envio.nuevo` (calcula `precioTotal`), `sumarTotalFacturado(+delta)`, todo en una transacción
  - [ ] `EnvioController.modificar`: manejar cambio de `cliente_origen_id` (restar viejo, sumar nuevo) y cambio de `precio_total` (sumar diferencia)
  - [ ] `EnvioController.eliminar`: `sumarTotalFacturado(-precioTotal)` en la misma transacción
  - [ ] `EnvioController.registrarIntentoFallido`: obtener el envío con `Envio.conIntentoFallido`, ajustar diferencia en `total_facturado`
  - [ ] `ClienteController.registrar`: `fecha_alta = LocalDate.now()`
  - [ ] Todos los `BigDecimal` con `setScale(2, RoundingMode.HALF_UP)`
  - [ ] Limitar dígitos de decimales en `Validacion` según `schema.sql` (error MySQL 1264)
  - [ ] Bloquear eliminar o reasignar a otro supervisor el último vehículo de un supervisor (P-01, RF-13)
  - [ ] `MercanciaController`: validar los tipos de vehículo (sobre → moto, paquete → furgoneta, avión → AVION) antes de guardar, para dar un mensaje claro (RF-21/RF-24/RF-25)
  - [ ] `SupervisorController`: exponer el conteo de aviones por supervisor (RF-10)
  - [ ] `SupervisorController.eliminarTraspasandoVehiculos(id, supervisorDestinoId)`: destino existente y distinto; mover vehículos y borrar en una transacción. `eliminar(id)` solo para supervisores sin vehículos (RF-11, RF-13)
  - [ ] Parte final: test de integración de `TransaccionesJdbc` y de las operaciones transaccionales contra el esquema `transportes_test`
  - [ ] Verificación: `mvn -q verify` + tests JUnit 5 de reglas de negocio críticas
    - Reglas de los controladores: con DAO falsos en memoria que implementan las interfaces `*Dao`
    - Transacciones: contra un esquema aparte `transportes_test`, nunca contra `transportes_db`

- [ ] **Etapa 6 · Capa View**
  - [ ] `Consola.java`: validación de tipos, fechas, rangos; nunca se propaga excepción al menú
  - [ ] Menú principal + submenús por módulo (RF-38/RF-39)
  - [ ] Mensajes de éxito/error sin stack traces; detalle a log
  - [ ] Configurar `java.util.logging` para escribir en archivo y no en consola (por defecto, `ConsoleHandler` imprime WARNING/SEVERE con stack trace en stderr)
  - [ ] Capturar por separado `ConfiguracionException` y `AccesoDatosException` (no heredan de `DomainException`)
  - [ ] Crear objetos nuevos en cada intento tras un rollback: la entidad conserva el id asignado por `save` aunque la transacción se revierta (`asignarId` solo admite una asignación)
  - [ ] `catch` final genérico para excepciones no de dominio (`IllegalArgumentException`, `IllegalStateException`, etc.): mensaje amable al usuario y detalle al log
  - [ ] Eliminar `PruebaLectura.java` (prueba manual de la Etapa 4) cuando la vista la sustituya
  - [ ] Verificación: prueba manual de flujo completo

- [ ] **Etapa 7 · Reportes**
  - [ ] RF-35: mercancías asignadas a cada vehículo
  - [ ] RF-36: envíos por cliente y total facturado
  - [ ] RF-37: mercancías largo recorrido con avión asignado
  - [ ] `sql/consultas_control.sql`: consulta de supervisores sin vehículos a cargo (control de RF-13)
  - [ ] Verificación: consultas con datos de `seed.sql`

- [ ] **Etapa 8 · Entregables finales**
  - [ ] Diagrama de clases UML (RNF-13): actualizar `docs/diagrama-clases.md` con los paquetes `dao` y `controller` y las excepciones añadidas desde la Etapa 3
  - [ ] Manual básico de uso (RNF-13)
  - [ ] `sql/schema.sql` final reproducible (RNF-17)

---

## 6. Puntos resueltos (P-01 a P-09)

| P | Decisión |
|---|---|
| P-01 | Transacción atómica supervisor + primer vehículo. Controller bloquea eliminar/reasignar el último vehículo de un supervisor. |
| P-02 | `vehiculo_id` y `vehiculo_destino_id` pueden apuntar a la misma fila. Sin restricción adicional. |
| P-03 | No se modelan aeropuertos ni direcciones de ruta. Los tres IDs de vehículo en `mercancia` representan los tres tramos implícitamente. |
| P-04 | RF-29 = FK obligatoria `cliente_origen_id NOT NULL`. Sin precondición adicional de creación. |
| P-05 | `envio.destinatario_id FK → cliente(id)`. El destinatario debe estar registrado como cliente. |
| P-06 | UNIQUE sobre `envio.mercancia_id` (relación 1:1). |
| P-07 | `fecha_alta DATE NOT NULL`. La pone el controlador con `LocalDate.now()` al registrar; el modelo no aplica valor por defecto. |
| P-08 | `asegurada` solo informativa; sin impacto en cálculos; se muestra en listados. |
| P-09 | RF-23/RF-24 (largo recorrido exige avión y vehículo de destino) → CHECK en tabla + modelo (`Mercancia.asignarVehiculos`). Tipos de vehículo RF-21/RF-24/RF-25 (sobre → moto, paquete → furgoneta, avión → AVION) → triggers en `mercancia` + controlador para mensajes claros. |

---

## Supuestos registrados

1. `pom.xml`: `junit-jupiter:6.0.3` se verifica con `mvn -q verify`; se corrige solo si falla.
2. Teléfono del supervisor: NOT NULL.
3. `precio_total` almacenado (no columna generada MySQL) para simplificar RF-36 y RF-33.
4. Para `Sobre` largo_recorrido, `vehiculo_id` actúa como vehículo terrestre origen→aeropuerto (cumple RF-21 y RF-24 sin columna adicional).
5. CHECK de largo_recorrido sobre columnas FK: si MySQL lo rechaza, se mueve a `trg_mercancia_bi/bu` y se indica en el script.
6. `total_facturado` siempre se actualiza con SQL atómico incremental (`total_facturado + delta`), nunca con read-modify-write.
7. En los controladores, comprobar una regla y después escribir (p. ej. contar los vehículos del supervisor y luego eliminar o reasignar) no es atómico. Se acepta porque es una consola de un solo usuario; la BD sigue protegiendo la integridad con FK, UNIQUE y CHECK.
