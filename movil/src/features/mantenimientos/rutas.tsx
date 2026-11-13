import { Route, Routes } from "react-router-dom";
import NoEncontrada from "../../components/NoEncontrada";
import Mantenimientos from "./Mantenimientos";

// Rutas dentro de /mantenimientos. Cada historia agrega aquí sus pantallas, por ejemplo:
// <Route path=":id" element={<DetalleMantenimiento />} />
export default function RutasMantenimientos() {
  return (
    <Routes>
      <Route index element={<Mantenimientos />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}