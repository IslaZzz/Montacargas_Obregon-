import { Route, Routes } from "react-router-dom";
import NoEncontrada from "../../components/NoEncontrada";
import Historial from "./Historial";

// Rutas dentro de /historial. Cada historia agrega aquí sus pantallas, por ejemplo:
// <Route path=":id" element={<DetalleHistorial />} />
export default function RutasHistorial() {
  return (
    <Routes>
      <Route index element={<Historial />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}