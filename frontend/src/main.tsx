import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './styles/global.css'
import './styles/buttons-chips.css'
import './styles/table.css'
import './styles/filters.css'
import "./styles/menu-lateral.css";
import "./styles/layout.css";
import "./styles/modal.css";
import "./styles/avisos.css";
import './styles/clientes/registrar-cliente.css';
import "./styles/clientes/detalle-cliente.css";
import "./styles/clientes/buscar-clientes.css";
import './styles/equipos/disponibilidad.css'
import App from './App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)