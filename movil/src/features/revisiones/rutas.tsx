import { Route, Routes } from "react-router-dom";
import NoEncontrada from "../../components/NoEncontrada";
import RealizarRevision from "./RealizarRevision";
import Revisiones from "./Revisiones";

// Rutas dentro de /revisiones.
export default function RutasRevisiones() {
  return (
    <Routes>
      <Route index element={<Revisiones />} />
      <Route path=":id" element={<RealizarRevision />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}