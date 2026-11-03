# Sistema de gestión de transportes y mensajería

Aplicación de consola en Java para gestionar los vehículos, supervisores, clientes, mercancías y
envíos de una empresa de transportes y mensajería. Sigue el patrón MVC, guarda los datos en MySQL
mediante JDBC y aplica las reglas de negocio tanto en la aplicación como en la base de datos
(restricciones CHECK, claves foráneas y triggers).

Funciones principales:
- Altas, modificaciones, bajas, listados y consultas de los cinco módulos.
- Supervisores registrados siempre junto con su primer vehículo, en una sola transacción.
- Asignación de mercancías a vehículos según su tipo (sobre → moto, paquete → furgoneta, avión
  para largo recorrido).
- Cálculo del plus de almacenaje y del precio total de cada envío, intentos de entrega fallidos y
  actualización automática del total facturado de cada cliente.
- Reportes: mercancías por vehículo, envíos y total facturado de un cliente, y mercancías de largo
  recorrido con su avión.

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 o superior |
| Maven | 3.x (integrado en IntelliJ IDEA) |
| MySQL | 8.0.16 o superior (desde esa versión MySQL aplica las restricciones `CHECK`) |
| MySQL Workbench o cliente `mysql` | para ejecutar los scripts de `sql/` |
| IntelliJ IDEA | recomendado |

Dependencias declaradas en `pom.xml`: `com.mysql:mysql-connector-j` 9.7.0 (driver JDBC) y
`org.junit.jupiter:junit-jupiter` 6.0.3 (solo para tests). No se usa ningún framework.

## Preparar la base de datos

Ejecute los scripts de `sql/` **en este orden**, desde MySQL Workbench o el cliente `mysql`
(usan `DELIMITER`, así que no se pueden lanzar por JDBC):

| Orden | Script | Con qué usuario | Qué hace |
|---|---|---|---|
| 1 | `sql/schema.sql` | `root` u otro con permisos de DDL | Crea la base `transportes_db` con sus tablas, restricciones, triggers y procedimiento. **Borra las tablas y sus datos si ya existían**; se puede repetir. |
| 2 | `sql/usuario.sql` | `root` o uno con `GRANT OPTION` | Crea el usuario `transportes_app` con permisos solo de `SELECT`, `INSERT`, `UPDATE` y `DELETE` sobre `transportes_db`. |
| 3 | `sql/seed.sql` | cualquiera con permisos de escritura | Carga datos de prueba (opcional). |

Antes de ejecutar `usuario.sql`, cambie `CAMBIAR_ESTA_CLAVE` por una contraseña propia. Para no
dejarla en un archivo versionado, puede copiarlo como `sql/usuario.local.sql` y cambiarla allí:
los archivos `sql/*.local.sql` están en `.gitignore`.

Al repetir `schema.sql` no hace falta volver a ejecutar `usuario.sql` (los permisos se conservan),
pero sí `seed.sql` si quiere los datos de prueba.

Scripts opcionales de comprobación:
- `sql/consultas_control.sql`: tres consultas de solo lectura que buscan incoherencias
  (supervisores sin vehículos, total facturado que no cuadra con los envíos, precio total que no
  cuadra con el plus). Con una base sana, las tres devuelven 0 filas.
- `sql/pruebas_invalidas.sql`: sentencias que la base de datos debe rechazar, una por regla. Se
  ejecuta después de `seed.sql` y cada sentencia debe fallar con el mensaje indicado en el script.

## Configurar la conexión

La aplicación lee tres valores. Las variables de entorno tienen prioridad sobre el archivo:

| Variable de entorno | Clave en `config.properties` | Ejemplo |
|---|---|---|
| `DB_URL` | `db.url` | `jdbc:mysql://127.0.0.1:3306/transportes_db` |
| `DB_USUARIO` | `db.usuario` | `transportes_app` |
| `DB_PASSWORD` | `db.password` | la contraseña elegida en `usuario.sql` |

Con archivo:

1. Copie `src/main/resources/config.properties.example` como
   `src/main/resources/config.properties`.
2. Sustituya `CAMBIAR_ESTA_CLAVE` por la contraseña real.

`config.properties` está en `.gitignore`: **nunca lo suba al repositorio**. El `.example` no
contiene credenciales y es el único que se versiona.

Si falta alguno de los tres valores, la aplicación arranca igualmente, pero cada operación que
consulte la base de datos muestra "No se pudo cargar la configuración de la base de datos…".

## Arrancar

1. Abra la carpeta del proyecto en IntelliJ IDEA como proyecto Maven.
2. Ejecute la clase `com.transportes.Main` (botón ▶ junto a `main`).
3. La aplicación se maneja desde la consola de IntelliJ. El uso está explicado en el
   [manual de uso](docs/manual-de-uso.md).

Para compilar y ejecutar los tests: panel Maven → `Lifecycle` → `verify` (equivale a `mvn verify`).

Los errores técnicos se registran en `logs/transportes.log`, dentro de la carpeta de trabajo, sin
datos personales (NIF, CIF, nombres, direcciones ni teléfonos). La carpeta `logs/` está en
`.gitignore`.

## Tests

Los tests unitarios (modelo, controladores con DAO en memoria y vista) no necesitan base de datos.

El test de integración opcional `TransaccionesJdbcIntegracionTest` solo se ejecuta si la variable
de entorno `DB_URL` termina en `/transportes_test`; si no, se omite.

Para prepararlo: ejecute `sql/schema.sql` cambiando `transportes_db` por `transportes_test` en
`CREATE DATABASE` y `USE`, conceda al usuario `GRANT SELECT, INSERT, UPDATE, DELETE ON
transportes_test.*` y defina en la configuración de ejecución de IntelliJ las variables `DB_URL`,
`DB_USUARIO` y `DB_PASSWORD` apuntando a ese esquema. El test borra todas las filas de las cinco
tablas antes de cada caso, así que nunca debe apuntar a `transportes_db`.

## Estructura del proyecto

```
├── pom.xml
├── sql/
│   ├── schema.sql               # creación de la BD (reproducible)
│   ├── usuario.sql              # usuario de aplicación con permisos mínimos
│   ├── seed.sql                 # datos de prueba
│   ├── consultas_control.sql    # comprobaciones de integridad (0 filas = correcto)
│   └── pruebas_invalidas.sql    # casos que la BD debe rechazar
├── docs/
│   ├── requisitos.md
│   ├── plan.md
│   ├── diagrama-clases.md
│   └── manual-de-uso.md
└── src/
    ├── main/
    │   ├── java/com/transportes/
    │   │   ├── Main.java        # raíz de composición y arranque
    │   │   ├── config/          # conexión (ConexionBD) y log (RegistroLog)
    │   │   ├── model/           # entidades, jerarquías y enums
    │   │   │   └── dao/         # interfaces DAO e implementaciones JDBC
    │   │   ├── controller/      # reglas de negocio y transacciones
    │   │   ├── view/            # menús de consola y lectura de datos
    │   │   └── exception/       # excepciones de dominio e infraestructura
    │   └── resources/
    │       └── config.properties.example
    └── test/java/com/transportes/   # tests JUnit 5
```

## Documentación

- [Requisitos](docs/requisitos.md): requerimientos funcionales (RF) y no funcionales (RNF).
- [Plan de implementación](docs/plan.md): modelo relacional, decisiones, etapas y dónde se cumple
  cada requisito.
- [Diagrama de clases](docs/diagrama-clases.md): un diagrama por capa.
- [Manual de uso](docs/manual-de-uso.md): guía para el usuario final.
