import { Link } from "react-router-dom";
import Chip from "../../components/chips";
import { useTitulo } from "../../hooks/useTitulo";
import type { ApartadoProximo } from "./avisosTypes";
import {
  accionCarta,
  chipCarta,
  esUrgente,
  nombreCarta,
  rutaCarta,
  sinCarta,
  textoEquipos,
  textoInicio,
} from "./avisosUtils";
import SelectorAnticipacion from "./SelectorAnticipacion";
import { useApartadosProximos } from "./useApartadosProximos";
import "../../styles/apartados/apartados.css";

function TarjetaApartado({ apartado }: { apartado: ApartadoProximo }) {
  const accion = accionCarta(apartado);
  return (
    <li className={`apartado${sinCarta(apartado) ? " apartado-sin-carta" : ""}`}>
      <div className="apartado-datos">
        <h3>{apartado.cliente}</h3>
        <p>
          <span className="apartado-folio">Renta {apartado.folio}</span> · {textoEquipos(apartado)}
        </p>
        <div className="apartado-chips">
          <span className={`apartado-inicio${esUrgente(apartado) ? " apartado-inicio-urgente" : ""}`}>
            <span className="material-symbols-rounded" aria-hidden="true">
              calendar_today
            </span>
            {textoInicio(apartado)}
          </span>
          <Chip category="carta" status={chipCarta(apartado)}>
            Carta: {nombreCarta(apartado)}
          </Chip>
        </div>
      </div>
      <Link to={rutaCarta(apartado)} className="btn btn-filter apartado-accion">
        {accion.texto}
        <span className="material-symbols-rounded" aria-hidden="true">
          {accion.icono}
        </span>
      </Link>
    </li>
  );
}

function Grupo({
  titulo,
  icono,
  clase,
  apartados,
}: {
  titulo: string;
  icono: string;
  clase: string;
  apartados: ApartadoProximo[];
}) {
  if (apartados.length === 0) return null;
  return (
    <section className="apartados-grupo">
      <h2 className={`apartados-grupo-titulo ${clase}`}>
        <span className="material-symbols-rounded" aria-hidden="true">
          {icono}
        </span>
        {titulo} ({apartados.length})
      </h2>
      <ul className="apartados-lista">
        {apartados.map((a) => (
          <TarjetaApartado key={a.rentaId} apartado={a} />
        ))}
      </ul>
    </section>
  );
}

function Conteo({ numero, texto, punto }: { numero: number | null; texto: string; punto: string }) {
  return (
    <div className="apartados-conteo">
      <span className="apartados-conteo-numero">{numero ?? "–"}</span>
      <span className="apartados-conteo-texto">
        <span className={`apartados-punto ${punto}`} aria-hidden="true" />
        {texto}
      </span>
    </div>
  );
}

// Apartados próximos (OHU-24): rentas que empiezan pronto y aún no tienen la carta firmada
export default function ApartadosProximos() {
  useTitulo("Apartados próximos");
  const { dias, apartados, error } = useApartadosProximos();

  const lista = apartados ?? [];
  const sinCartaLista = lista.filter(sinCarta);
  const faltaFirma = lista.filter((a) => !sinCarta(a));
  const contar = (estado: ApartadoProximo["estadoCarta"]) =>
    apartados === null ? null : lista.filter((a) => a.estadoCarta === estado).length;

  return (
    <div className="apartados">
      <div className="apartados-principal">
        <h1 className="apartados-titulo">Apartados próximos.</h1>

        {error && apartados === null ? (
          <p className="apartados-mensaje" role="alert">
            No se pudieron cargar los apartados próximos. Revisa que el servidor esté encendido.
          </p>
        ) : apartados === null ? (
          <p className="apartados-mensaje">Cargando apartados…</p>
        ) : lista.length === 0 ? (
          <div className="apartados-vacio">
            <span className="apartados-vacio-icono material-symbols-rounded" aria-hidden="true">
              check_circle
            </span>
            <h2>Todo listo</h2>
            <p className="apartados-vacio-resumen">No hay apartados próximos sin firmar.</p>
            <p>
              Cuando una renta esté por comenzar, aparecerá aquí con {dias}{" "}
              {dias === 1 ? "día" : "días"} de anticipación.
            </p>
          </div>
        ) : (
          <>
            <Grupo
              titulo="Sin carta"
              icono="notifications_active"
              clase="apartados-grupo-sin-carta"
              apartados={sinCartaLista}
            />
            <Grupo
              titulo="Con carta, falta la firma"
              icono="mail"
              clase="apartados-grupo-firma"
              apartados={faltaFirma}
            />
            <p className="apartados-nota">
              <span className="material-symbols-rounded" aria-hidden="true">
                info
              </span>
              No se muestran los apartados con la carta ya firmada.
            </p>
          </>
        )}
      </div>

      <aside className="apartados-panel" aria-labelledby="titulo-resumen">
        <div className="apartados-panel-encabezado">
          <h2 id="titulo-resumen">Resumen</h2>
        </div>
        <SelectorAnticipacion />
        <Conteo numero={contar("SIN_GENERAR")} texto="Sin carta" punto="apartados-punto-sin-carta" />
        <Conteo numero={contar("GENERADA")} texto="Carta generada" punto="apartados-punto-generada" />
        <Conteo numero={contar("ENVIADA")} texto="Carta enviada" punto="apartados-punto-enviada" />
        <p className="apartados-gerencia">
          <span className="material-symbols-rounded" aria-hidden="true">
            groups
          </span>
          Gerencia también recibe este aviso.
        </p>
      </aside>
    </div>
  );
}
