import { Route, Routes } from "react-router-dom";
import NoEncontrada from "../../components/NoEncontrada";
import AgregarTarea from "./AgregarTarea";
import AtenderTarea from "./AtenderTarea";
import DetalleMantenimiento from "./DetalleMantenimiento";
import Mantenimientos from "./Mantenimientos";
import ReportarIncidencia from "./ReportarIncidencia";

// Rutas dentro de /mantenimientos.
export default function RutasMantenimientos() {
  return (
    <Routes>
      <Route index element={<Mantenimientos />} />
      <Route path=":id" element={<DetalleMantenimiento />} />
      <Route path=":id/agregar" element={<AgregarTarea />} />
      <Route path=":id/tareas/:tareaId" element={<AtenderTarea />} />
      <Route path=":id/tareas/:tareaId/incidencia" element={<ReportarIncidencia />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}