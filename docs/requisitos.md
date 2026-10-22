# Sistema de Gestión de Transportes y Mensajería

## 1. Descripción

Aplicación de **consola** en Java para gestionar los envíos de una empresa de transportes y mensajería.
Arquitectura **MVC** (la "vista" es un menú de consola, sin interfaz gráfica).
Persistencia en **MySQL**, diseñada y administrada con **MySQL Workbench**.
IDE: **IntelliJ IDEA**. Gestión de dependencias: **Maven**. JDK 17+.

> Nota: el borrador original mencionaba Visual Studio Code; se unifica a IntelliJ IDEA según RNF-01.

---

## 2. Requerimientos funcionales

### 2.1 Gestión de vehículos
- **RF-01**: Registrar vehículos con código, tipo, descripción, fecha de compra y precio de compra.
- **RF-02**: Clasificar cada vehículo en uno de tres tipos: moto, furgoneta o avión.
- **RF-03**: Registrar la cilindrada para las motos.
- **RF-04**: Registrar la capacidad para las furgonetas.
- **RF-05**: Modificar los datos de un vehículo.
- **RF-06**: Eliminar un vehículo (solo si no tiene envíos o mercancías asignadas).
- **RF-07**: Listar y consultar vehículos por código y por tipo.
- **RF-08**: Asignar a cada vehículo un único supervisor responsable de su mantenimiento.

### 2.2 Gestión de supervisores (empleados)
- **RF-09**: Registrar supervisores con NIF, nombre, dirección y teléfono.
- **RF-10**: Calcular automáticamente el número de aviones a cargo de cada supervisor.
- **RF-11**: Modificar y eliminar supervisores (no se puede eliminar uno que tenga vehículos a cargo).
- **RF-12**: Listar y consultar supervisores, junto con los vehículos que tienen asignados.
- **RF-13**: Validar que cada supervisor tenga al menos un vehículo a cargo.

### 2.3 Gestión de clientes
- **RF-14**: Registrar clientes con código de cliente, fecha y total facturado.
- **RF-15**: Distinguir dos tipos de cliente: particular (NIF, nombre, dirección) y empresa (CIF, nombre, dirección, teléfono).
- **RF-16**: Modificar, eliminar, listar y consultar clientes.
- **RF-17**: Actualizar automáticamente el total facturado de un cliente cada vez que se registra un envío.
- **RF-18**: Consultar el historial de envíos encargados por cada cliente.

### 2.4 Gestión de mercancías
- **RF-19**: Registrar mercancías con código, descripción, precio del envío, indicador de si está asegurada e indicador de si es de largo recorrido.
- **RF-20**: Clasificar la mercancía por tamaño: sobre (peso) o paquete (dimensiones).
- **RF-21**: Asignar los sobres obligatoriamente a una moto y los paquetes obligatoriamente a una furgoneta.
- **RF-22**: Permitir que una moto o furgoneta tenga varias mercancías asignadas o ninguna.
- **RF-23**: Para mercancías de largo recorrido, asignar obligatoriamente un avión.
- **RF-24**: Para mercancías de largo recorrido, asignar además una moto/furgoneta para el trayecto origen→aeropuerto y otra (distinta o la misma, ver P-02) para aeropuerto destino→destinatario.
- **RF-25**: Impedir que un avión tenga asignadas mercancías que no sean de largo recorrido.
- **RF-26**: Modificar, eliminar, listar y consultar mercancías.

### 2.5 Gestión de envíos
- **RF-27**: Registrar envíos con cliente origen, destinatario, mercancía enviada y fecha de envío.
- **RF-28**: Permitir que el destinatario sea una empresa o un particular.
- **RF-29**: Garantizar que cada cliente que envía a una empresa haya enviado al menos una mercancía, y registrar todos sus envíos (ver P-04).
- **RF-30**: Para envíos a particulares, registrar el número de intentos de entrega fallidos.
- **RF-31**: Calcular el plus de almacenaje en envíos a particulares: `plus = precio × (0.04 + 0.01 × intentosFallidos)`.
- **RF-32**: Calcular el precio total del envío (precio base + plus, si aplica).
- **RF-33**: Registrar un nuevo intento fallido de entrega y recalcular el plus.
- **RF-34**: Modificar, eliminar, listar y consultar envíos.

### 2.6 Reportes y consultas
- **RF-35**: Listar las mercancías asignadas a cada vehículo.
- **RF-36**: Listar los envíos de un cliente y su total facturado.
- **RF-37**: Listar las mercancías de largo recorrido con su avión asignado.

### 2.7 Interfaz y control
- **RF-38**: Ofrecer un menú de consola con opciones para cada módulo (vehículos, supervisores, clientes, mercancías, envíos).
- **RF-39**: Mostrar mensajes claros de éxito o error en cada operación.

---

## 3. Requerimientos no funcionales

| Código | Categoría | Descripción |
|---|---|---|
| RNF-01 | Plataforma | Java (JDK 17+), IntelliJ IDEA, Maven. |
| RNF-02 | Arquitectura | Patrón MVC: paquetes `model`, `view` y `controller` claramente separados. |
| RNF-03 | Persistencia | Base de datos relacional MySQL, diseñada y administrada con MySQL Workbench. |
| RNF-04 | Interfaz | Consola, sin parte gráfica. |
| RNF-05 | Usabilidad | Menús claros, mensajes comprensibles, navegación sencilla. |
| RNF-06 | Integridad de datos | Validar reglas de negocio (restricciones de asignación, campos obligatorios, códigos únicos). |
| RNF-07 | Validación de entradas | Controlar entradas inválidas (tipos, fechas, negativos, vacíos) sin que la aplicación se caiga. |
| RNF-08 | Mantenibilidad | Código modular, comentado y con nombres descriptivos; herencia y polimorfismo en las jerarquías. |
| RNF-09 | Extensibilidad | Permitir agregar nuevos tipos de vehículos o mercancías sin reescribir el sistema. |
| RNF-10 | Rendimiento | Registro, búsqueda y listado inmediatos para volúmenes pequeños/medios. |
| RNF-11 | Fiabilidad | Manejo de excepciones para evitar cierres inesperados. |
| RNF-12 | Portabilidad | Ejecutable en cualquier SO con JVM. |
| RNF-13 | Documentación | Diagrama de clases, descripción de requerimientos y manual básico de uso. |
| RNF-14 | Conectividad | Conexión a la BD mediante JDBC (MySQL Connector/J). |
| RNF-15 | Integridad referencial | La BD garantiza reglas con PK, FK, UNIQUE, NOT NULL y CHECK. |
| RNF-16 | Seguridad | `PreparedStatement` para evitar inyección SQL; credenciales fuera del código fuente. |
| RNF-17 | Respaldo | Entregar el script `.sql` de creación de la BD para reproducirla en otro equipo. |

---

## 4. Reglas de negocio clave

1. Cada vehículo tiene exactamente un supervisor; cada supervisor tiene al menos un vehículo.
2. Sobre → moto. Paquete → furgoneta (obligatorio).
3. Mercancía de largo recorrido → avión obligatorio, más un vehículo terrestre de origen y otro de destino.
4. Un avión solo transporta mercancías de largo recorrido.
5. Envío a particular → plus = 4 % + 1 % por intento fallido.
6. Todo envío queda registrado y asociado al cliente que lo encargó.

---

## 5. Puntos por resolver (antes de diseñar la BD)

- **P-01 · Dependencia circular RF-08 / RF-13.** El vehículo exige supervisor y el supervisor exige al menos un vehículo: ninguno puede crearse primero.
  *Propuesta:* `vehiculo.supervisor_id` NOT NULL con FK (lo garantiza la BD); RF-13 se aplica en la aplicación, registrando el supervisor junto con su primer vehículo en una **transacción**, y bloqueando eliminar o reasignar su último vehículo.
- **P-02 · RF-24.** ¿El vehículo terrestre de origen y el de destino pueden ser el mismo? *Propuesta:* sí, sin restricción.
- **P-03 · Aeropuertos y direcciones.** El enunciado habla de trayecto origen→aeropuerto y aeropuerto→destinatario, pero no modela aeropuertos ni direcciones de origen/destino. ¿Se guardan o es solo conceptual?
- **P-04 · RF-29 ambigua.** ¿Significa "no se puede registrar un cliente-empresa sin envíos"? ¿O "todo envío debe quedar asociado a su cliente"? Si es lo primero, tiene el mismo problema circular de P-01.
- **P-05 · Destinatario.** ¿El destinatario es siempre una fila de `cliente` (particular/empresa) o es una entidad aparte, sin código de cliente ni total facturado?
- **P-06 · Envío ↔ mercancía.** ¿Un envío lleva una sola mercancía? *Propuesta:* relación 1:1 (`UNIQUE` sobre `mercancia_id` en `envio`).
- **P-07 · Campo "fecha" del cliente (RF-14).** ¿Es fecha de alta? *Propuesta:* sí.
- **P-08 · Mercancía asegurada (RF-19).** No afecta ninguna fórmula. ¿Cambia el precio o es solo informativa?
- **P-09 · Reglas entre tablas.** RF-21, RF-23 y RF-25 (el tipo de vehículo debe coincidir con la mercancía) no se pueden expresar solo con `CHECK`; requieren triggers o validación en la capa de servicio/DAO.
