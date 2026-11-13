import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Chip from "../../components/Chip";
import { Dato, ListaDatos } from "../../components/Datos";
import Domicilio from "../../components/Domicilio";
import Encabezado from "../../components/Encabezado";
import EncabezadoGrupo from "../../components/EncabezadoGrupo";
import Filtros from "../../components/Filtros";
import { conNotificacion } from "../../components/Notificacion";
import Pantalla from "../../components/Pantalla";
import Resultado from "../../components/Resultado";
import { LineaTarjeta, Tarjeta, TarjetaEnlace, TituloTarjeta } from "../../components/Tarjeta";
import CampoFotos from "../../components/formulario/CampoFotos";
import CampoNota from "../../components/formulario/CampoNota";
import CampoOpciones from "../../components/formulario/CampoOpciones";
import CampoSegmentado from "../../components/formulario/CampoSegmentado";
import CampoTexto from "../../components/formulario/CampoTexto";
import type { Foto } from "../../services/fotosApi";
import { claveDia, etiquetaDia, fechaHoraCorta, horario } from "../../utils/fechas";

// Catálogo de componentes con datos de ejemplo. Solo existe en desarrollo: http://localhost:5174/componentes
// No usa el backend; las fotos se quedan en el navegador.

// Hoy a cierta hora, como lo manda el backend: "2026-10-08T15:00:00"
const enHoras = (h: number) => `${claveDia(new Date())}T${String(h).padStart(2, "0")}:00:00`;
const FOTOS = ["Frente", "Lateral", "Tablero y horómetro", "Llantas"];

export default function Componentes() {
  const navigate = useNavigate();
  const [filtro, setFiltro] = useState<"TODOS" | "SALIDA" | "REGRESO">("TODOS");
  const [horometro, setHorometro] = useState("");
  const [combustible, setCombustible] = useState<string | null>("1/2");
  const [llantas, setLlantas] = useState<string | null>("ATENCION");
  const [detalle, setDetalle] = useState("Desgaste en llanta delantera");
  const [nota, setNota] = useState("");
  const [fotos, setFotos] = useState<Record<string, Foto>>({});
  const [resultado, setResultado] = useState(false);

  if (resultado) {
    return (
      <Resultado
        tipo="exito"
        titulo="Traslado iniciado"
        texto="El auxiliar ya puede ver que vas en camino."
        accion={{ texto: "Volver a mis tareas", onClick: () => setResultado(false) }}
      />
    );
  }

  // En una historia real: await subirFoto("REVISION", id, etiqueta, archivo)
  async function subir(etiqueta: string, archivo: File) {
    await new Promise((listo) => setTimeout(listo, 600));
    setFotos((f) => ({ ...f, [etiqueta]: { id: 0, etiqueta, url: URL.createObjectURL(archivo), subidaEn: "" } }));
  }

  const faltan = FOTOS.filter((f) => !fotos[f]).length + (horometro ? 0 : 1);

  return (
    <Pantalla
      barraSuperior={<BarraSuperior atras="/" titulo="Componentes" chip={<Chip tono="verde">Salida</Chip>} />}
      barraAccion={
        <BarraAccion
          progreso={{
            texto: faltan ? `Faltan ${faltan} campos y fotos` : "Todo listo",
            valor: (FOTOS.length + 1 - faltan) / (FOTOS.length + 1),
            completo: faltan === 0,
          }}
        >
          <Boton bloqueado={faltan > 0} onClick={() => setResultado(true)}>
            Terminar revisión
          </Boton>
        </BarraAccion>
      }
    >
      <Encabezado titulo="Mis tareas">
        <span>Rafael Amaya · Operador de traslado</span>
      </Encabezado>

      <Filtros
        etiqueta="Tipo de traslado"
        opciones={[
          { valor: "TODOS", etiqueta: "Todos" },
          { valor: "SALIDA", etiqueta: "Salida" },
          { valor: "REGRESO", etiqueta: "Regreso" },
        ]}
        seleccionado={filtro}
        onCambio={setFiltro}
      />

      <EncabezadoGrupo titulo={etiquetaDia(enHoras(15))} detalle="1 pendiente" />
      <TarjetaEnlace to="/componentes">
        <Chip tono="verde" icono="north_east">
          Entrega
        </Chip>
        <TituloTarjeta>Constructora del Yaqui</TituloTarjeta>
        <LineaTarjeta icono="schedule">{horario(enHoras(15), enHoras(17))}</LineaTarjeta>
        <LineaTarjeta icono="location_on">Calle Náinari 450, Col. Centro</LineaTarjeta>
        <LineaTarjeta icono="forklift">2 equipos</LineaTarjeta>
        <Chip tono="naranja" icono="draw">
          Firma de carta en sitio
        </Chip>
      </TarjetaEnlace>
      <TarjetaEnlace to="/componentes" borde="atencion">
        <Chip tono="amarillo" icono="south_west">
          Regreso
        </Chip>
        <TituloTarjeta>EQ-031</TituloTarjeta>
        <LineaTarjeta icono="schedule">{fechaHoraCorta(enHoras(10))}</LineaTarjeta>
      </TarjetaEnlace>

      <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
        <Chip tono="verde">Listo para entrega</Chip>
        <Chip tono="lila">En camino</Chip>
        <Chip tono="azul">En curso</Chip>
        <Chip tono="amarillo">Pendiente</Chip>
        <Chip tono="gris">Programada</Chip>
        <Chip tono="rojo">Falla</Chip>
      </div>

      <Tarjeta>
        <h3>Datos del traslado</h3>
        <ListaDatos>
          <Dato etiqueta="Traslado">TR-0089</Dato>
          <Dato etiqueta="Horario">{horario(enHoras(15), enHoras(17))}</Dato>
          <Dato etiqueta="Trabajo realizado" apilado>
            Cambio de aceite y filtro de aire.
          </Dato>
        </ListaDatos>
      </Tarjeta>

      <Tarjeta>
        <h3>Domicilio en un solo texto (como hoy)</h3>
        <Domicilio domicilio={{ texto: "Calle Náinari 450, Col. Centro, Cd. Obregón", calle: null, numero: null, colonia: null, codigoPostal: null, ciudad: null, estado: null }} mapa />
        <h3>Domicilio por partes (con código postal)</h3>
        <Domicilio domicilio={{ texto: "", calle: "Calle Náinari", numero: "450", colonia: "Centro", codigoPostal: "85000", ciudad: "Cd. Obregón", estado: "Sonora" }} />
      </Tarjeta>

      <Aviso tipo="error" titulo="Aún no puedes iniciar el traslado">
        El equipo EQ-014 no tiene revisión de salida.
      </Aviso>
      <Aviso tipo="advertencia" titulo="Revisa la falla antes de salir" />
      <Aviso tipo="info" titulo="Recuerda tomar las fotos con buena luz" />
      <Aviso tipo="exito" titulo="Equipo listo para entrega" />

      <CampoTexto etiqueta="Horómetro (h)" obligatorio inputMode="decimal" placeholder="Ej. 1250.5" valor={horometro} onCambio={setHorometro} error={horometro ? undefined : "Escribe el horómetro"} />
      <CampoSegmentado
        etiqueta="Combustible"
        obligatorio
        opciones={["1/4", "1/2", "3/4", "Lleno"].map((v) => ({ valor: v, etiqueta: v }))}
        valor={combustible}
        onCambio={setCombustible}
      />
      <CampoOpciones
        etiqueta="Llantas"
        obligatorio
        opciones={[
          { valor: "BIEN", etiqueta: "Bien" },
          { valor: "ATENCION", etiqueta: "Atención", tono: "atencion", pideDetalle: true },
          { valor: "FALLA", etiqueta: "Falla", tono: "falla", pideDetalle: true },
        ]}
        valor={llantas}
        onCambio={setLlantas}
        detalle={detalle}
        onDetalle={setDetalle}
      />
      <CampoFotos etiqueta="Fotos" obligatorio requeridas={FOTOS} subidas={fotos} onSubir={subir} />
      <CampoNota etiqueta="Notas (opcional)" valor={nota} onCambio={setNota} />

      <Boton variante="secundario" icono="notifications" onClick={() => navigate("/componentes", conNotificacion("EQ-014 pasó al historial"))}>
        Probar notificación
      </Boton>
      <Boton variante="peligro" icono="report">
        Reportar falla
      </Boton>
    </Pantalla>
  );
}