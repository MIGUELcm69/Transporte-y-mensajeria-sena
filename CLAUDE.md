# CLAUDE.md

## Proyecto
Sistema de gestión de transportes y mensajería. Aplicación de **consola** en Java.
Requerimientos completos en `docs/requisitos.md` (RF-xx / RNF-xx). Léelos antes de planificar.

## Stack
- Java 17+ (JDK), Maven, IntelliJ IDEA.
- MySQL 8.3+ administrado con MySQL Workbench.
- JDBC con MySQL Connector/J. Sin frameworks (sin Spring, sin JPA).
- Pruebas: JUnit 5.

## Arquitectura (MVC)
Paquete base: `com.transportes`
- `model` → entidades y jerarquías (herencia y polimorfismo: Vehiculo, Mercancia, Cliente).
  - `model.dao` → acceso a datos por JDBC (una interfaz + una implementación por entidad).
- `controller` → reglas de negocio, orquestación y transacciones.
- `view` → menús de consola y lectura/validación de entradas. No contiene lógica de negocio.
- `config` → carga de configuración de conexión.

Reglas de diseño:
- Programar contra interfaces; agregar un nuevo tipo de vehículo o mercancía no debe obligar a reescribir el sistema (RNF-09).
- Tipado fuerte: enums para tipos y estados, `BigDecimal` para dinero, `LocalDate` para fechas.
- Excepciones propias de dominio; la vista las traduce a mensajes claros. La aplicación nunca debe caerse por una entrada inválida.

## Seguridad (obligatorio)
- **Solo `PreparedStatement`**. Prohibido concatenar valores en SQL.
- **Credenciales fuera del código**: leer URL, usuario y contraseña de variables de entorno o de un archivo local (`config.properties` / `.env`) incluido en `.gitignore`. Entregar un `config.properties.example` sin secretos.
- Usar `try-with-resources` para cerrar `Connection`, `Statement` y `ResultSet`.
- Operaciones que afecten varias tablas dentro de una **transacción** (commit/rollback).
- Mensajes de error al usuario sin detalles internos (ni SQL ni stack traces); el detalle va a un log.
- Validar toda entrada en la vista (tipos, fechas, negativos, vacíos) y volver a validar reglas de negocio en el controlador.
- La BD garantiza integridad: PK, FK, UNIQUE, NOT NULL y CHECK. Las reglas entre tablas se refuerzan con trigger o en el controlador.

## Forma de trabajo
- Plan primero, código después. Una etapa por sesión, con alcance explícito.
- Si algo no está en `docs/requisitos.md` o es ambiguo, **pregunta**; no inventes.
- No añadas dependencias sin indicar versión y motivo.
- Al terminar cada etapa: compila (`mvn -q verify`), ejecuta los tests y lista qué asumiste.
- Comentarios solo donde la complejidad lo exija; nombres descriptivos en español de dominio.
- Entregables: script `sql/schema.sql` reproducible (RNF-17), diagrama de clases y manual de uso (RNF-13).

Verificación: la hago yo desde IntelliJ (Maven → verify). No ejecutes mvn en la terminal.
No explores .idea/, .git/ ni target/. Lee solo los archivos que se te indiquen con @.