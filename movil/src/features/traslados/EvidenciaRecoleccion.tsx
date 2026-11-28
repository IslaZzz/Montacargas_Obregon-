import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import Encabezado from "../../components/Encabezado";
import Pantalla from "../../components/Pantalla";
import { Tarjeta } from "../../components/Tarjeta";
import CampoFotos from "../../components/formulario/CampoFotos";
import { mensajeDeError } from "../../services/api";
import type { Foto } from "../../services/fotosApi";
import { obtenerEvidencia, subirEvidencia } from "./trasladosApi";
import type { EquipoEvidencia, Evidencia } from "./trasladosTypes";

/**
 * OHU13: las cuatro fotos de cada equipo en el domicilio del cliente, antes de confirmar
 * la recolección. Cada foto se guarda al tomarla; tocarla otra vez la cambia.
 */
export default function EvidenciaRecoleccion() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const [evidencia, setEvidencia] = useState<Evidencia | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    obtenerEvidencia(id)
      .then(setEvidencia)
      .catch((e) => setError(mensajeDeError(e, "No se pudo cargar la evidencia.")));
  }, [id]);

  const atras = `/traslados/${id}`;

  if (error || !evidencia) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras={atras} titulo="Evidencia fotográfica" />}>
        {error ? <Aviso tipo="error" titulo={error} /> : <Cargando />}
      </Pantalla>
    );
  }

  const faltan = evidencia.requeridas - evidencia.tomadas;

  async function subir(equipoId: number, vista: string, archivo: File) {
    setEvidencia(await subirEvidencia(id, equipoId, vista, archivo)); // Si falla, CampoFotos muestra el error
  }

  return (
    <Pantalla
      barraSuperior={<BarraSuperior atras={atras} titulo={`Renta ${evidencia.rentaFolio}`} />}
      barraAccion={
        <BarraAccion
          progreso={{
            texto: `${evidencia.tomadas} de ${evidencia.requeridas} fotografías`,
            valor: evidencia.requeridas === 0 ? 0 : evidencia.tomadas / evidencia.requeridas,
            completo: faltan === 0,
          }}
        >
          {faltan > 0 && evidencia.editable && (
            <Aviso tipo="advertencia" titulo={`Faltan ${faltan} fotografías para poder confirmar la recolección`} />
          )}
          <Boton
            icono={evidencia.editable ? "check" : "arrow_back"}
            variante={evidencia.editable ? undefined : "secundario"}
            bloqueado={faltan > 0 && evidencia.editable}
            onClick={() => navigate(atras)}
          >
            {evidencia.editable ? "Finalizar evidencia" : "Volver a la recolección"}
          </Boton>
        </BarraAccion>
      }
    >
      <Encabezado titulo="Evidencia fotográfica">
        <span>Recolección {evidencia.folio}</span>
      </Encabezado>

      {!evidencia.editable && (
        <Aviso tipo="info" titulo="La recolección ya se confirmó">
          Las fotografías ya no se pueden cambiar.
        </Aviso>
      )}

      <h3 className="seccion-titulo">Equipos a documentar ({evidencia.equipos.length})</h3>
      {evidencia.equipos.map((equipo) => (
        <TarjetaEvidencia
          key={equipo.id}
          equipo={equipo}
          editable={evidencia.editable}
          onSubir={(vista, archivo) => subir(equipo.id, vista, archivo)}
        />
      ))}
    </Pantalla>
  );
}

interface PropsTarjeta {
  equipo: EquipoEvidencia;
  editable: boolean;
  onSubir: (vista: string, archivo: File) => Promise<void>;
}

function TarjetaEvidencia({ equipo, editable, onSubir }: PropsTarjeta) {
  const requeridas = equipo.vistas.length;
  const faltan = requeridas - equipo.tomadas;
  const subidas: Record<string, Foto | undefined> = Object.fromEntries(
    equipo.vistas.map((v) => [v.vista, v.foto ?? undefined]),
  );

  return (
    <Tarjeta borde={faltan === 0 ? "normal" : "atencion"}>
      <div className="equipo">
        <span className="equipo-unidad">{equipo.numeroUnidad}</span>
        {faltan === 0 ? (
          <Chip tono="verde">Completa</Chip>
        ) : equipo.tomadas > 0 ? (
          <Chip tono="azul">En progreso</Chip>
        ) : (
          <Chip tono="gris">Sin fotografías</Chip>
        )}
      </div>
      <p className="tarjeta-linea">
        {equipo.tomadas} de {requeridas} fotografías
      </p>

      {faltan > 0 && editable && (
        <Aviso tipo="error" titulo={`Obligatorio: faltan ${faltan} fotografías de este equipo`} />
      )}

      {editable ? (
        <CampoFotos
          etiqueta="Fotografías"
          requeridas={equipo.vistas.map((v) => v.vista)}
          subidas={subidas}
          onSubir={onSubir}
          obligatorio
        />
      ) : (
        <div className="fotos">
          {equipo.vistas.map((v) => (
            <span key={v.vista} className={`foto${v.foto ? " foto-lista" : ""}`}>
              {v.foto && <img src={v.foto.url} alt="" />}
              <span className="foto-nombre">{v.vista}</span>
            </span>
          ))}
        </div>
      )}
    </Tarjeta>
  );
}