# Montacargas_Obregon-
Proyecto web para la empresa Sonorense Montacargas Obregon 

## Configuración

El backend toma la conexión a MySQL (y el correo de la carta responsiva) de `backend/.env`, que no se sube al repositorio.

1. Copia `backend/.env.example` como `backend/.env` (cópialo, no lo renombres).
2. Pon tu usuario y tu contraseña de MySQL en `DB_USER` y `DB_PASSWORD`.

No cambies `application.properties` para poner tus datos.

## Datos iniciales

Usuarios de Comercial, categorías, modelos con su tarifa por día y equipos de ejemplo (T-02).

1. Arranca el backend una vez para que se creen las tablas.
2. Desde la raíz del repositorio corre, en CMD o Git Bash:

   ```
   mysql -u root -p --default-character-set=utf8mb4 < backend/datos-prueba/datos-iniciales.sql
   ```

   En PowerShell el `<` no funciona; usa:

   ```
   cmd /c "mysql -u root -p --default-character-set=utf8mb4 < backend/datos-prueba/datos-iniciales.sql"
   ```

   Si no se reconoce `mysql`, está en `C:\Program Files\MySQL\MySQL Server <versión>\bin`. También puedes abrir el archivo en MySQL Workbench y ejecutarlo.

Se puede correr varias veces: solo agrega lo que falte. Los demás scripts de `backend/datos-prueba` son datos de prueba de cada historia.

## Inicio de sesión (HU-28)

Todas las pantallas piden iniciar sesión. Los usuarios de prueba son los de `datos-iniciales.sql`:

| Correo | Rol |
|---|---|
| patricia.romero@montacargasobregon.mx | Gerencia |
| daniela.felix@montacargasobregon.mx | Auxiliar |
| luis.ochoa@montacargasobregon.mx | Auxiliar |

Los tres tienen la misma contraseña de prueba. Está en la hoja «Datos de prueba» de los casos de prueba; no se escribe en el repositorio.

Si tu base ya tenía estos usuarios, haz esto una sola vez:

1. Arranca el backend. Agrega la columna `contrasena` a `usuarios`.
2. Vuelve a correr `backend/datos-prueba/datos-iniciales.sql`. Les pone la contraseña de prueba a los tres.

## Acceso por rol (HU-29)

Hay dos roles: Gerencia (`GERENTE`) y Auxiliar (`AUXILIAR`). Gerencia ve todo lo del auxiliar y además lo suyo.

- **API:** las rutas de `SOLO_GERENCIA` en `SeguridadConfig` (`/api/autorizaciones/**`, `/api/ingresos/**`) responden 403 a un auxiliar. Si agregas otra ruta de gerencia, ponla ahí.
- **Pantallas:** las rutas dentro de `<RutaGerencia />` en `AppRoutes.tsx` le muestran al auxiliar «No tienes permiso para ver esta pantalla».
- **Botones:** lo que solo hace gerencia, como «Autorizar» o «Ampliar apartado», se muestra con `esGerencia(sesion.rol)` de `sesionUtils.ts`.
- **Menú:** las opciones con `soloGerencia` no se le muestran al auxiliar, y `contadores` pone un número junto a una opción.

## Rentas que salen de la cotización (HU-37)

Toda renta nace al aceptar una cotización. Si tu base ya tenía rentas, haz esto una sola vez:

1. Arranca el backend. Avisará que no pudo crear la llave de `rentas.cotizacion_id`; es normal.
2. Corre `backend/datos-prueba/quitar-rentas-sin-cotizacion.sql`. Borra las rentas sin cotización y lo que cuelga de ellas.
3. Vuelve a correr los scripts que crean rentas: `revisiones.sql`, `traslados.sql`, `apartados-proximos.sql` y `rentas.sql`. Cada renta de prueba trae su cotización aceptada (COT-R2061 es la de R-2061).
4. Reinicia el backend.

## Modelos de equipo (HU-36)

Cada categoría puede pedir campos de especificación (`DefinicionCampo`) y cada modelo guarda su valor (`ValorEspecificacion`). Por ahora solo Montacargas los pide: capacidad de carga, altura del mástil y tipo de energía. Se cargan con `datos-iniciales.sql`.

Un modelo nuevo se registra sin tarifa; se le pone en «Tarifas» (HU-04). Mientras no tenga tarifa no se puede cotizar.

Si tu base ya existía, haz esto una sola vez:

1. Arranca el backend. Crea las tablas de los campos y de los valores.
2. Vuelve a correr `backend/datos-prueba/datos-iniciales.sql`. Deja la tarifa como opcional y agrega los campos de Montacargas.

- **API:** `GET/POST /api/modelos`, `GET/PUT /api/modelos/{id}` (cada modelo trae sus `especificaciones`) y `GET /api/categorias/{id}/campos`.

## Dirección por código postal (SCRUM-110)

Al escribir el código postal en «Registrar cliente» y en el paso «Fechas y obra» de «Nueva cotización», se llenan la colonia, la ciudad y el estado con el catálogo de SEPOMEX. Si el código tiene varias colonias, se elige de la lista o se escribe otra. Si no se encuentra, se escriben a mano. Todo lo que se llena se puede corregir antes de guardar.

Para cargar el catálogo, una sola vez:

1. Arranca el backend. Crea la tabla `codigos_postales`.
2. Corre `backend/datos-prueba/codigos-postales.sql` igual que `datos-iniciales.sql`. Tarda unos segundos. Si lo vuelves a correr, borra el catálogo y lo carga de nuevo.

El archivo sale del catálogo de Correos de México (correosdemexico.gob.mx, «Descarga de la base de datos de códigos postales», en formato TXT, versión del 12 de noviembre de 2026).

- **API:** `GET /api/codigos-postales/{cp}` responde las `colonias`, la `ciudad` (o el municipio, si el código no tiene ciudad) y el `estado`. Si no existe, responde 404.
