# App móvil (mecánicos y operadores de traslado)

Usa el mismo backend que la app de oficina (`/api`, puerto 8080).

```
npm install
npm run dev        # http://localhost:5174
```

Para abrirla en el celular: misma red Wi-Fi que la PC y entrar a `http://IP-DE-LA-PC:5174`
(la IP sale en la consola de Vite como "Network").

Catálogo de componentes con datos de ejemplo (solo en desarrollo): `http://localhost:5174/componentes`

## Estructura

```
src/
  components/        Piezas comunes: Pantalla, BarraSuperior, Tarjeta, Chip, Filtros, Aviso, Boton...
    formulario/      Preguntas de formulario: CampoTexto, CampoSegmentado, CampoOpciones, CampoFotos, CampoNota
  features/          Una carpeta por sección, cada una con su rutas.tsx
    traslados/       Operador: Mis tareas
    revisiones/      Mecánico
    mantenimientos/  Mecánico
    historial/       Mecánico
  sesion/            Quién usa la app (temporal hasta HU-28), pestañas y protección de rutas
  services/api.ts    getApi, postApi, patchApi, deleteApi (mandan X-Personal-Id solos)
  services/fotosApi.ts  subirFoto, listarFotos, borrarFoto
  utils/fechas.ts    Formatos de fecha y hora (24 h): "Hoy · 8 oct", "15:00 – 17:00"
  utils/domicilio.ts Cómo se muestra un domicilio (texto o por partes con código postal)
  styles/            tokens.css (colores), base.css, componentes.css, formulario.css
```

## Cómo agregar una historia

1. Trabaja dentro de la carpeta de tu sección (`features/revisiones/...`). Agrega tus pantallas en
   el `rutas.tsx` de esa carpeta: `<Route path=":id" element={<RealizarRevision />} />`.
   No hace falta tocar `App.tsx`.
2. La pantalla de inicio de la sección usa `PantallaPrincipal`; las demás usan
   `Pantalla` con `BarraSuperior atras` y, si llevan botón abajo, `BarraAccion`.
3. Tu archivo de API va en tu carpeta (`features/revisiones/revisionesApi.ts`) y usa `getApi`/`postApi`.
4. Fechas: solo con `utils/fechas.ts`. Domicilios: con el componente `Domicilio`.
5. Fotos: `CampoFotos` con `onSubir={(etiqueta, archivo) => subirFoto("REVISION", id, etiqueta, archivo)}`.
   Al terminar, el backend valida con `fotoService.verificarCompletas(...)`.
6. Al terminar una acción, regresa con aviso: `navigate("/revisiones", conNotificacion("EQ-014 pasó al historial"))`.
7. Si tu sección necesita otra pestaña abajo, agrégala en `sesion/navegacion.ts`.

## Backend

- Para saber quién hace la petición, agrega el parámetro `PersonalActual personal` al método del
  controlador. Si la acción es de un solo puesto: `personal.exigir(TipoPersonal.MECANICO)`.
- Domicilios para la app: `DomicilioResponse.deTexto(...)` hoy; `deDireccion(...)` cuando se capture por partes.