# Manual de uso

Sistema de gestión de transportes y mensajería. Este manual explica cómo usar la aplicación desde
la consola, sin conocimientos técnicos.

> La base de datos y la conexión las prepara una persona técnica siguiendo el [README](../README.md).
> Si al usar la aplicación aparece un mensaje sobre la configuración o la base de datos, avise a esa
> persona (ver [Mensajes del sistema](#mensajes-del-sistema)).

## Índice

1. [Arrancar y salir](#1-arrancar-y-salir)
2. [Cómo moverse y escribir datos](#2-cómo-moverse-y-escribir-datos)
3. [Cancelar una operación](#3-cancelar-una-operación)
4. [Estructura del menú](#4-estructura-del-menú)
5. [Registrar cada tipo de dato](#5-registrar-cada-tipo-de-dato)
6. [Modificar, eliminar, listar y consultar](#6-modificar-eliminar-listar-y-consultar)
7. [Reglas que aplica la aplicación](#7-reglas-que-aplica-la-aplicación)
8. [Mensajes de error y qué significan](#8-mensajes-de-error-y-qué-significan)

## 1. Arrancar y salir

**Arrancar.** Abra el proyecto en IntelliJ IDEA, abra la clase `Main`
(`src/main/java/com/transportes/Main.java`) y pulse el botón verde de ejecutar (▶). La aplicación se
abre en la consola de IntelliJ, en la parte inferior de la ventana, y muestra:

```
Sistema de gestión de transportes y mensajería
En cualquier campo, escriba 'cancelar' para abandonar la operación.

=== Menú principal ===
  1. Vehículos
  2. Supervisores
  3. Clientes
  4. Mercancías
  5. Envíos
  6. Reportes
  0. Salir
Opción:
```

Haga clic dentro de la consola antes de escribir.

**Salir.** En el menú principal escriba `0` y pulse Enter. La aplicación muestra "Hasta pronto." y
termina. Todo lo que se ha registrado ya está guardado: no hay que guardar nada antes de salir.

## 2. Cómo moverse y escribir datos

- **Menús.** Escriba el número de la opción y pulse Enter. `0` vuelve al menú anterior (o sale, en
  el menú principal). Si escribe otra cosa, aparece `[ERROR] Opción no válida…` y se vuelve a
  preguntar.
- **Resultado de cada operación.** Las líneas que empiezan por `[OK]` indican que la operación se
  ha hecho. Las que empiezan por `[ERROR]` indican que no se ha hecho nada; tras un error en un
  formulario se vuelve al menú del módulo.
- **Campos obligatorios.** No se pueden dejar vacíos: si pulsa Enter sin escribir nada, aparece
  "Este campo es obligatorio." y se vuelve a preguntar.
- **Fechas.** Formato `dd/mm/aaaa`, por ejemplo `15/03/2026`. No se aceptan fechas que no existen,
  como `31/02/2026`.
- **Números con decimales** (precios, medidas). Puede usar coma o punto: `12,50` o `12.50`. No
  escriba separador de miles ni símbolo de moneda: `1500` y no `1.500` ni `1500 €`.
- **Preguntas de sí o no.** Responda `s` o `n` (también se aceptan `si`, `sí` y `no`).
- **Al modificar**, cada pregunta muestra el valor actual entre corchetes, por ejemplo
  `Nombre [Ana Pérez, Enter = conservar]:`. Pulse Enter para dejarlo como está o escriba el valor nuevo.
- **Antes de eliminar** se muestran los datos y se pide confirmación: con `n` no se borra nada.
- **Cómo se identifica cada cosa.** Los supervisores, por su NIF; los vehículos, las mercancías y los
  clientes, por su código; los envíos, por su número (id), que la aplicación asigna al registrarlos
  y muestra en el mensaje de éxito y en los listados.

## 3. Cancelar una operación

En **cualquier campo** de un formulario, escriba `cancelar` (da igual en mayúsculas o minúsculas) y
pulse Enter. La aplicación muestra "Operación cancelada.", no guarda nada de esa operación y vuelve
al menú del módulo.

- En las listas de elección de tipo ("Tipo de vehículo", "Tipo de mercancía"), la opción `0. Cancelar`
  hace lo mismo.
- En los menús (donde se elige una opción con un número), `cancelar` no sirve: use `0` para volver.
- Al registrar un supervisor, cancelar mientras se piden los datos de su primer vehículo tampoco
  guarda el supervisor.

## 4. Estructura del menú

```
Menú principal
├── 1. Vehículos
│   ├── 1. Registrar vehículo para un supervisor existente
│   ├── 2. Modificar vehículo
│   ├── 3. Eliminar vehículo
│   ├── 4. Listar todos
│   ├── 5. Listar por tipo
│   └── 6. Consultar por código
├── 2. Supervisores
│   ├── 1. Registrar supervisor con su primer vehículo
│   ├── 2. Modificar supervisor
│   ├── 3. Eliminar supervisor sin vehículos
│   ├── 4. Eliminar supervisor traspasando sus vehículos a otro
│   ├── 5. Listar con sus vehículos y número de aviones
│   └── 6. Consultar por NIF
├── 3. Clientes
│   ├── 1. Registrar cliente particular
│   ├── 2. Registrar cliente empresa
│   ├── 3. Modificar cliente
│   ├── 4. Eliminar cliente
│   ├── 5. Listar todos
│   ├── 6. Consultar por código
│   └── 7. Historial de envíos y total facturado
├── 4. Mercancías
│   ├── 1. Registrar mercancía
│   ├── 2. Modificar mercancía
│   ├── 3. Eliminar mercancía
│   ├── 4. Listar todas
│   └── 5. Consultar por código
├── 5. Envíos
│   ├── 1. Registrar envío
│   ├── 2. Modificar envío
│   ├── 3. Eliminar envío
│   ├── 4. Registrar intento de entrega fallido
│   ├── 5. Listar todos
│   └── 6. Consultar por id
├── 6. Reportes
│   ├── 1. Mercancías asignadas a cada vehículo
│   ├── 2. Envíos de un cliente y su total facturado
│   └── 3. Mercancías de largo recorrido con su avión
└── 0. Salir
```

En cada submenú, `0. Volver` regresa al menú principal.

## 5. Registrar cada tipo de dato

Como unos datos dependen de otros, conviene registrarlos en este orden:
**supervisor (con su primer vehículo) → más vehículos → clientes → mercancías → envíos.**

Los ejemplos usan datos inventados. Los códigos `VEH-001`, `VEH-002`, `VEH-003` y `CLI-001`, y el NIF
`12345678A`, son de los datos de prueba que trae la aplicación; si su base no los tiene, use los suyos.

### 5.1 Supervisor con su primer vehículo

Menú **2. Supervisores → 1. Registrar supervisor con su primer vehículo**. Un supervisor siempre se
registra junto con un vehículo a su cargo.

| Pregunta | Ejemplo |
|---|---|
| NIF | `11223344C` |
| Nombre | `Ana Pérez Gómez` |
| Dirección | `Calle Sol 3, Sevilla` |
| Teléfono | `955112233` |
| Tipo de vehículo | `1` (MOTO) |
| Código | `VEH-010` |
| Descripción | `Yamaha NMAX 125` |
| Fecha de compra | `10/04/2024` |
| Precio de compra | `3500` |
| Cilindrada | `125` |

Resultado: `[OK] Supervisor 11223344C registrado con su primer vehículo.` y la ficha del
supervisor con su vehículo y su número de aviones. Si algo falla (por ejemplo, el código del vehículo
ya existe), no se guarda ni el supervisor ni el vehículo.

### 5.2 Vehículo para un supervisor que ya existe

Menú **1. Vehículos → 1. Registrar vehículo para un supervisor existente**. Primero se pide el NIF del
supervisor y después el tipo y los datos del vehículo. Cada tipo pide un dato propio:

- **MOTO:** cilindrada.
- **FURGONETA:** capacidad.
- **AVION:** ningún dato más.

Ejemplo de furgoneta:

| Pregunta | Ejemplo |
|---|---|
| NIF del supervisor | `12345678A` |
| Tipo de vehículo | `2` (FURGONETA) |
| Código | `VEH-011` |
| Descripción | `Ford Transit` |
| Fecha de compra | `01/09/2025` |
| Precio de compra | `31000,50` |
| Capacidad | `1500` |

Resultado: `[OK] Vehículo VEH-011 registrado a cargo de <nombre del supervisor>.`

### 5.3 Cliente particular

Menú **3. Clientes → 1. Registrar cliente particular**.

| Pregunta | Ejemplo |
|---|---|
| Código de cliente | `CLI-010` |
| NIF | `44556677D` |
| Nombre | `Luis Romero Sanz` |
| Dirección | `Av. del Puerto 22, Valencia` |

No se pide la fecha de alta ni el total facturado: la fecha es la de hoy y el total empieza en 0.
Resultado: `[OK] Cliente CLI-010 registrado con fecha de alta dd/mm/aaaa.`

### 5.4 Cliente empresa

Menú **3. Clientes → 2. Registrar cliente empresa**. Igual que el particular, pero con CIF en lugar
de NIF y con teléfono obligatorio.

| Pregunta | Ejemplo |
|---|---|
| Código de cliente | `CLI-011` |
| CIF | `B87654321` |
| Nombre | `Distribuciones Norte S.L.` |
| Dirección | `Calle Industria 8, Bilbao` |
| Teléfono | `944556677` |

### 5.5 Mercancía

Menú **4. Mercancías → 1. Registrar mercancía**. Primero se elige el tipo:

- **SOBRE:** pide el peso y se transporta en **moto**.
- **PAQUETE:** pide largo, ancho y alto y se transporta en **furgoneta**.

Después se pregunta si está asegurada y si es de largo recorrido. Antes de pedir el código de cada
vehículo, la aplicación lista los vehículos del tipo adecuado para que elija uno:

- **Sin largo recorrido:** solo se pide el vehículo (moto o furgoneta).
- **Con largo recorrido:** se piden el vehículo de origen, el **avión** y el vehículo de destino
  (este puede ser el mismo que el de origen).

Ejemplo de sobre sin largo recorrido:

| Pregunta | Ejemplo |
|---|---|
| Tipo de mercancía | `1` (SOBRE) |
| Código | `MER-010` |
| Descripción | `Carta certificada` |
| Precio de envío | `8,50` |
| ¿Está asegurada? | `n` |
| ¿Es de largo recorrido? | `n` |
| Peso | `0,2` |
| Código del vehículo | `VEH-001` (una moto de la lista) |

Ejemplo de paquete de largo recorrido:

| Pregunta | Ejemplo |
|---|---|
| Tipo de mercancía | `2` (PAQUETE) |
| Código | `MER-011` |
| Descripción | `Repuestos de maquinaria` |
| Precio de envío | `60` |
| ¿Está asegurada? | `s` |
| ¿Es de largo recorrido? | `s` |
| Largo / Ancho / Alto | `40` / `30` / `20` |
| Código del vehículo | `VEH-002` (una furgoneta) |
| Código del avión | `VEH-003` (un avión) |
| Código del vehículo de destino | `VEH-002` (una furgoneta) |

Resultado: `[OK] Mercancía MER-011 registrada.`

### 5.6 Envío

Menú **5. Envíos → 1. Registrar envío**. El cliente origen (quien encarga y paga el envío) y el
destinatario deben estar registrados como clientes y ser distintos. Antes de pedir la mercancía se
listan las que todavía no tienen envío.

| Pregunta | Ejemplo |
|---|---|
| Código del cliente origen | `CLI-010` |
| Código del destinatario | `CLI-001` (un particular) |
| Código de la mercancía | `MER-010` |
| Fecha de envío | Enter (usa la fecha de hoy y la muestra) o, por ejemplo, `15/03/2026` |

Resultado:

```
[OK] Envío 3 registrado.
  Precio base: 8.50
  Plus de almacenaje: 0.34
  Precio total: 8.84
```

El número del envío (aquí `3`) es el que se usa después para modificarlo, eliminarlo o registrar
intentos fallidos. Como el destinatario es un particular, se suma un plus del 4 % del precio base;
si fuera una empresa, el plus sería `0.00`.

### 5.7 Intento de entrega fallido

Menú **5. Envíos → 4. Registrar intento de entrega fallido**, e indique el id del envío. Solo es
posible en envíos a particulares. Cada intento sube el plus un 1 % más. Siguiendo el ejemplo
anterior, tras el primer intento el plus pasa a ser el 5 % (`0.43`) y el precio total `8.93`; la
diferencia se suma al total facturado del cliente origen.

## 6. Modificar, eliminar, listar y consultar

Todos los módulos funcionan igual:

- **Modificar:** se busca el registro (por NIF, código o id), se muestran sus datos y se pregunta
  cada campo con Enter para conservarlo. El código y el tipo de vehículos, mercancías y clientes no
  se piden porque no se pueden cambiar. El NIF de un supervisor sí se puede cambiar.
- **Eliminar:** se busca el registro, se muestran sus datos y se pide confirmación (`s`/`n`).
- **Listar:** muestra una línea por registro y el número total al final.
- **Consultar:** muestra los datos de un único registro.

Particularidades:

- **Vehículos → Modificar** permite cambiar el supervisor escribiendo otro NIF.
- **Supervisores → Eliminar supervisor traspasando sus vehículos a otro:** pide el NIF del supervisor
  que se elimina y el del que recibirá sus vehículos, indica cuántos se traspasan y pide confirmación.
- **Mercancías → Modificar** una mercancía que ya tiene envío: no pregunta el precio de envío, avisa
  de que se conserva y deja cambiar el resto de datos.
- **Envíos → Modificar:** no se pide el precio; la aplicación lo recalcula y ajusta el total
  facturado de los clientes afectados.
- **Envíos → Consultar por id** muestra además el precio base, el plus y el precio total.
- **Clientes → Historial** y **Reportes → Envíos de un cliente** muestran lo mismo: los envíos que
  el cliente ha encargado como origen, con su total facturado.

## 7. Reglas que aplica la aplicación

**Supervisores y vehículos**
- Un supervisor se registra **siempre con su primer vehículo**: si no se puede guardar uno de los dos,
  no se guarda ninguno.
- Todo supervisor debe tener al menos un vehículo. Por eso no se puede eliminar ni pasar a otro
  supervisor el **único** vehículo de un supervisor.
- Un supervisor con vehículos no se puede eliminar con "Eliminar supervisor sin vehículos"; use
  "Eliminar supervisor traspasando sus vehículos a otro", con un supervisor de destino distinto.
- El número de aviones de cada supervisor se calcula solo.
- No se puede eliminar un vehículo que tenga mercancías asignadas (como vehículo, avión o vehículo
  de destino).

**Mercancías**
- Un sobre va siempre en moto y un paquete siempre en furgoneta, también en el vehículo de destino.
- Una mercancía de largo recorrido necesita un avión y un vehículo de destino; solo las de largo
  recorrido pueden tener avión.
- Si no hay ningún vehículo del tipo necesario, la aplicación lo avisa y cancela la operación:
  regístrelo antes en Vehículos.
- Si la mercancía ya tiene un envío, no se puede cambiar su precio de envío ni eliminarla.
- Cada mercancía solo puede ir en un envío.

**Clientes**
- La fecha de alta es la del día del registro y el total facturado empieza en 0.
- El total facturado cambia solo: sube al registrar un envío o un intento fallido, y se ajusta al
  modificar o eliminar un envío. Solo cuenta para el cliente origen, no para el destinatario.
- No se puede cambiar el tipo de un cliente (particular o empresa): hay que registrar uno nuevo.
- No se puede eliminar un cliente que figure en algún envío, como origen o como destinatario.

**Envíos**
- El cliente origen y el destinatario deben ser clientes registrados y distintos.
- Solo se puede registrar un envío si hay alguna mercancía sin envío.
- Precio total = precio de envío de la mercancía + plus de almacenaje.
- El plus solo se cobra si el destinatario es un **particular**: 4 % del precio de envío más un 1 %
  por cada intento de entrega fallido. Si el destinatario es una empresa, el plus es 0. Los importes
  se redondean a dos decimales.
- Los intentos fallidos solo se registran en envíos a particulares. Si un envío ya tiene intentos,
  su destinatario no se puede cambiar por una empresa.

## 8. Mensajes de error y qué significan

### Al escribir un dato

Estos mensajes **no** cancelan la operación: la misma pregunta se repite hasta que el dato es válido.

| Mensaje | Significado |
|---|---|
| Opción no válida. Introduzca un número entero entre 0 y N. | En un menú se escribió algo que no es una de las opciones. |
| Introduzca un número entero entre 1 y … | Se pedía un número entero (por ejemplo, el id de un envío). |
| Introduzca un número, por ejemplo 12,50 o 12.50 (sin separador de miles). | Se pedía un número y se escribió texto, un separador de miles u otro símbolo. |
| El valor no puede ser negativo. | Precios: se admite 0, pero no números negativos. |
| El valor debe ser mayor que cero. | Medidas (cilindrada, capacidad, peso, largo, ancho, alto): deben ser mayores que 0. |
| Fecha no válida. Use el formato dd/mm/aaaa, por ejemplo 15/03/2026. | La fecha no tiene ese formato o no existe. |
| Este campo es obligatorio. | Se pulsó Enter sin escribir nada. |
| Responda s (sí) o n (no). | En una pregunta de sí o no se escribió otra cosa. |

### Al guardar o buscar

Estos mensajes empiezan por `[ERROR]`: la operación no se hace y se vuelve al menú del módulo.

| Mensaje | Significado y qué hacer |
|---|---|
| No existe un vehículo / un supervisor / un cliente / una mercancía / un envío con identificador '…'. | El código, NIF o id escrito no está registrado. Compruébelo con la opción de listar. |
| Ya existe un supervisor con ese NIF. | El NIF ya pertenece a otro supervisor. |
| Ya existe un vehículo con ese código. / Ya existe una mercancía con ese código. / Ya existe un cliente con ese código. | El código ya está en uso; elija otro. |
| Ya existe un cliente con ese NIF. / Ya existe un cliente con ese CIF. | El documento ya pertenece a otro cliente. |
| El campo '…' admite como máximo N caracteres. | El texto es demasiado largo. |
| El campo '…' no puede superar … | El número es demasiado grande. |
| No se puede reasignar el vehículo: es el único a cargo de su supervisor. | El supervisor se quedaría sin vehículos. Registre antes otro vehículo para él. |
| No se puede eliminar el vehículo: es el único a cargo de su supervisor. | Igual que el anterior. |
| No se puede eliminar el vehículo porque tiene mercancías asignadas. | Cambie antes el vehículo de esas mercancías o elimínelas. |
| No se puede eliminar el supervisor porque tiene vehículos a cargo; traspáselos antes a otro supervisor. | Use "Eliminar supervisor traspasando sus vehículos a otro". |
| El supervisor de destino debe ser distinto del que se elimina. | En el traspaso se escribió dos veces el mismo NIF. |
| No se puede eliminar el cliente porque figura en envíos como origen o como destinatario. | Un cliente con envíos no se puede borrar. |
| Una mercancía de tipo SOBRE debe asignarse a un vehículo de tipo MOTO. (o PAQUETE / FURGONETA) | Se eligió un vehículo de otro tipo. Elija uno de la lista mostrada. |
| La mercancía de largo recorrido debe asignarse a un avión. | El código escrito como avión no es de un avión. |
| El vehículo de destino de una mercancía de tipo SOBRE debe ser de tipo MOTO. (o PAQUETE / FURGONETA) | Igual, para el vehículo de destino. |
| No se puede eliminar la mercancía porque tiene un envío asociado. | Elimine antes el envío si de verdad quiere borrarla. |
| La mercancía ya tiene un envío registrado. | Esa mercancía ya va en otro envío; elija una de la lista de mercancías sin envío. |
| El cliente origen y el destinatario deben ser distintos. | Se escribió el mismo código en los dos. |
| Solo se registran intentos fallidos en envíos a particulares. | El destinatario del envío es una empresa. |
| Este envío ya registró N intento(s) de entrega fallido(s), así que su destinatario no puede ser una empresa. | Al modificar un envío con intentos se eligió una empresa como destinatario. |

Avisos que cancelan la operación sin ser un error:

| Aviso | Significado |
|---|---|
| No hay vehículos de tipo … registrados; regístrelo antes en Vehículos. | Para la mercancía hace falta un vehículo de ese tipo. |
| No hay mercancías sin envío; registre antes una en Mercancías. | Todas las mercancías tienen ya su envío. |
| No se puede modificar el precio de envío porque la mercancía ya tiene un envío asociado. Se conserva … | Informativo: el precio no se pregunta, el resto de datos sí. |

Excepcionalmente puede aparecer otro mensaje que viene de las comprobaciones de la base de datos
(por ejemplo, "Los datos introducidos no son válidos." o "Ya existe un registro con esos datos.").
Significa lo mismo: la operación no se ha hecho y los datos no se han modificado.

### Mensajes del sistema

Estos mensajes no dependen de los datos escritos. La aplicación sigue funcionando, pero si se
repiten hay que avisar a la persona técnica.

| Mensaje | Significado |
|---|---|
| No se pudo cargar la configuración de la base de datos. Revise config.properties o las variables de entorno. | Falta la configuración de conexión. |
| No se pudo completar la operación en la base de datos. Compruebe que el servidor MySQL esté activo e inténtelo de nuevo. | El servidor de base de datos no responde o hubo un fallo al guardar. No se ha guardado nada de esa operación. |
| Ha ocurrido un error inesperado. La operación no se ha realizado. | Fallo no previsto. El detalle queda registrado para la persona técnica. |
| No se pudo abrir el archivo de log logs/transportes.log; la aplicación funcionará sin registrar errores. | Aparece al arrancar. La aplicación funciona, pero no guarda el registro de errores. |
