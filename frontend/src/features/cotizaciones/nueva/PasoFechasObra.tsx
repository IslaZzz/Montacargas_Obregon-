import Campo from "../../../components/CampoForm";
import { formatoFechaHora } from "../../../utils/formato";
import { textoDias } from "../cotizacionesUtils";
import type { BorradorCotizacion, CampoTexto, Errores } from "./nuevaCotizacionTypes";
import { calcularDias, fechaHoraIso } from "./calculo";

interface Props {
  borrador: BorradorCotizacion;
  cambiar: (cambios: Partial<BorradorCotizacion>) => void;
  errores: Errores;
}

interface CampoConfig {
  campo: CampoTexto;
  etiqueta: string;
  maxLength: number;
  obligatorio?: boolean;
  soloDigitos?: boolean;
  placeholder?: string;
}

const MOMENTOS = [
  { titulo: "Entrega", fecha: "fechaEntrega", hora: "horaEntrega" },
  { titulo: "Recolección", fecha: "fechaRecoleccion", hora: "horaRecoleccion" },
] as const;

// Los mismos campos que la dirección fiscal de Registrar cliente, más el estado
const CAMPOS_DIRECCION: CampoConfig[] = [
  { campo: "calleObra", etiqueta: "Calle", maxLength: 100 },
  { campo: "numeroObra", etiqueta: "Número", maxLength: 10 },
  { campo: "coloniaObra", etiqueta: "Colonia", maxLength: 100 },
  { campo: "ciudadObra", etiqueta: "Ciudad", maxLength: 100 },
  { campo: "estadoObra", etiqueta: "Estado", maxLength: 50 },
  { campo: "codigoPostalObra", etiqueta: "Código postal", maxLength: 5, soloDigitos: true },
];

const CAMPO_REFERENCIAS: CampoConfig = {
  campo: "referenciasObra",
  etiqueta: "Referencias para llegar",
  maxLength: 200,
  placeholder: "Portón azul. Preguntar por el Ing. Soto.",
};

const CAMPOS_CONTACTO: CampoConfig[] = [
  { campo: "contactoObra", etiqueta: "Quién recibe en la obra", maxLength: 100, obligatorio: true },
  { campo: "telefonoContacto", etiqueta: "Teléfono (10 dígitos)", maxLength: 10, obligatorio: true, soloDigitos: true },
];

export default function PasoFechasObra({ borrador, cambiar, errores }: Props) {
  const dias = calcularDias(borrador.fechaEntrega, borrador.fechaRecoleccion);

  const campoTexto = ({ campo, etiqueta, maxLength, obligatorio, soloDigitos, placeholder }: CampoConfig) => (
    <Campo key={campo} etiqueta={etiqueta} obligatorio={obligatorio} value={borrador[campo]} maxLength={maxLength}
      placeholder={placeholder} inputMode={soloDigitos ? "numeric" : undefined} error={errores[campo]}
      onChange={(valor) => cambiar({ [campo]: soloDigitos ? valor.replace(/\D/g, "") : valor })} />
  );

  return (
    <>
      <div className="secciones">
        {MOMENTOS.map(({ titulo, fecha, hora }) => (
          <section key={titulo} className="tarjeta">
            <h2>{titulo}</h2>
            <div className="direccion">
              <Campo etiqueta="Fecha" obligatorio type="date" value={borrador[fecha]} error={errores[fecha]}
                min={fecha === "fechaRecoleccion" ? borrador.fechaEntrega || undefined : undefined}
                onChange={(valor) => cambiar({ [fecha]: valor })} />
              <Campo etiqueta="Hora" obligatorio type="time" value={borrador[hora]} error={errores[hora]}
                onChange={(valor) => cambiar({ [hora]: valor })} />
            </div>
          </section>
        ))}
      </div>

      {dias !== null && (
        <div className="aviso-info">
          <span className="material-symbols-rounded" aria-hidden="true">event</span>
          <p>
            <strong>La renta dura {textoDias(dias)}.</strong>{" "}
            Del {formatoFechaHora(fechaHoraIso(borrador.fechaEntrega, borrador.horaEntrega))} al{" "}
            {formatoFechaHora(fechaHoraIso(borrador.fechaRecoleccion, borrador.horaRecoleccion))}.
          </p>
        </div>
      )}

      <section className="tarjeta">
        <h2>Lugar de entrega</h2>
        <p className="campo-etiqueta">
          Dirección de la obra <small>(obligatorio)</small>
        </p>
        <div className="direccion">{CAMPOS_DIRECCION.map(campoTexto)}</div>
        {campoTexto(CAMPO_REFERENCIAS)}
        <div className="direccion">{CAMPOS_CONTACTO.map(campoTexto)}</div>
      </section>
    </>
  );
}
