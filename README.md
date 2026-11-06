# Montacargas_Obregon-
Proyecto web para la empresa Sonorense Montacargas Obregon 

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
