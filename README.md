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
