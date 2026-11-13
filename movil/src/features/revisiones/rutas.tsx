import { Route, Routes } from "react-router-dom";
import NoEncontrada from "../../components/NoEncontrada";
import Revisiones from "./Revisiones";

// Rutas dentro de /revisiones. Cada historia agrega aquí sus pantallas, por ejemplo:
// <Route path=":id" element={<RealizarRevision />} />
export default function RutasRevisiones() {
  return (
    <Routes>
      <Route index element={<Revisiones />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}