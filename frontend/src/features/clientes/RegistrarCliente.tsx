import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import Campo from "../../components/CampoForm";
import { registrarCliente } from "./clientesApi";
import { ErrorApi } from "../../services/apiClient";
import { useTitulo } from "../../hooks/useTitulo";
import { Button } from "../../components/buttons";

// Razón social en blanco
const razonVacia = () => ({
  nombre: "",
  rfc: "",
  calle: "",
  numero: "",
  colonia: "",
  ciudad: "",
  codigoPostal: "",
});

// Deja solo dígitos y corta al máximo
const soloDigitos = (valor: string, max: number) =>
  valor.replace(/\D/g, "").slice(0, max);

// Deja solo caracteres válidos de un RFC, en mayúsculas
const soloRfc = (valor: string) =>
  valor
    .toUpperCase()
    .replace(/[^A-ZÑ&0-9]/g, "")
    .slice(0, 13);

// Campo del backend a llave del formulario:
// "telefono" → "telefono"; "razonesSociales[0].direccionFiscal.codigoPostal" → "0.codigoPostal"
const llaveDelCampo = (campo: string) => {
  const razon = campo.match(/^razonesSociales\[(\d+)\]\.(?:direccionFiscal\.)?(\w+)$/);
  return razon ? `${razon[1]}.${razon[2]}` : campo;
};

export default function RegistrarCliente() {
  useTitulo("Registrar cliente");
  const navigate = useNavigate();

  // Datos de contacto
  const [contacto, setContacto] = useState({
    nombre: "",
    apellidoPaterno: "",
    apellidoMaterno: "",
    empresa: "",
    telefono: "",
    correo: "",
  });

  // Razones sociales
  const [razones, setRazones] = useState([razonVacia()]);

  // True después de pulsar Guardar
  const [enviado, setEnviado] = useState(false);

  // Errores del backend: los de cada campo van debajo de él; los demás, en el aviso
  const [erroresServidor, setErroresServidor] = useState<Record<string, string>>({});
  const [mensajeServidor, setMensajeServidor] = useState("");

  // Revisa los campos obligatorios vacíos
  // Llaves: "telefono" o "posición.campo" como "0.rfc"
  function validar() {
    const e: Record<string, string> = {};
    const vacio = (valor: string) => !valor.trim();

    // Contacto
    if (vacio(contacto.nombre)) e.nombre = "Escribe el nombre del contacto.";
    if (vacio(contacto.apellidoPaterno))
      e.apellidoPaterno = "Escribe el apellido paterno.";
    if (vacio(contacto.telefono))
      e.telefono = "Escribe el teléfono del contacto.";
    if (vacio(contacto.correo)) e.correo = "Escribe el correo electrónico.";

    // Razones sociales
    razones.forEach((r, i) => {
      if (vacio(r.nombre))
        e[`${i}.nombre`] = "Escribe el nombre o razón social.";
      if (vacio(r.rfc)) e[`${i}.rfc`] = "Escribe el RFC.";
      if (vacio(r.calle)) e[`${i}.calle`] = "Escribe la calle.";
      if (vacio(r.numero)) e[`${i}.numero`] = "Escribe el número.";
      if (vacio(r.colonia)) e[`${i}.colonia`] = "Escribe la colonia.";
      if (vacio(r.ciudad)) e[`${i}.ciudad`] = "Escribe la ciudad.";
      if (vacio(r.codigoPostal))
        e[`${i}.codigoPostal`] = "Escribe el código postal.";
    });

    return e;
  }

  // Los errores se muestran solo después de intentar guardar
  const errores = enviado ? { ...erroresServidor, ...validar() } : {};
  const total = Object.keys(errores).length;

  // Al corregir un campo se quita el error que el backend marcó en él
  const quitarErrorServidor = (llave: string) =>
    setErroresServidor((actuales) => {
      if (!(llave in actuales)) return actuales;
      const resto = { ...actuales };
      delete resto[llave];
      return resto;
    });

  // Cambia un dato de contacto
  const cambiarContacto = (campo: keyof typeof contacto, valor: string) => {
    setContacto({ ...contacto, [campo]: valor });
    quitarErrorServidor(campo);
  };

  // Cambia un campo de una razón social
  const cambiarRazon = (i: number, campo: string, valor: string) => {
    setRazones(razones.map((r, j) => (j === i ? { ...r, [campo]: valor } : r)));
    quitarErrorServidor(`${i}.${campo}`);
  };

  // Al pulsar Guardar cliente
  async function guardar(e: FormEvent) {
    e.preventDefault();
    setEnviado(true);
    setErroresServidor({});
    setMensajeServidor("");

    // Si hay campos vacíos no se envía
    if (Object.keys(validar()).length > 0) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    try {
      // Envía los datos al backend
      const clienteRegistrado = await registrarCliente({
        nombre: contacto.nombre.trim(),
        apellidoPaterno: contacto.apellidoPaterno.trim(),
        apellidoMaterno: contacto.apellidoMaterno?.trim() || null,
        empresa: contacto.empresa?.trim() || null,
        telefono: contacto.telefono.replace(/\D/g, ""),
        correo: contacto.correo.trim(),
        razonesSociales: razones.map((r) => ({
          nombre: r.nombre.trim(),
          rfc: r.rfc.trim().toUpperCase(),
          direccionFiscal: {
            calle: r.calle.trim(),
            numero: r.numero.trim(),
            colonia: r.colonia.trim(),
            ciudad: r.ciudad.trim(),
            codigoPostal: r.codigoPostal.trim(),
          },
        })),
      });

      // Va al detalle del cliente con el aviso de éxito
      navigate(`/clientes/${clienteRegistrado.id}`, { state: { recienGuardado: true } });
    } catch (err) {
      // Cada error del backend se marca en su campo; si no es de un campo, va al aviso
      const porCampo: Record<string, string> = {};
      const sinCampo: string[] = [];
      if (err instanceof ErrorApi) {
        for (const [campo, texto] of Object.entries(err.errores)) {
          const llave = llaveDelCampo(campo);
          if (llave in contacto || /^\d+\.\w+$/.test(llave)) porCampo[llave] = texto;
          else sinCampo.push(texto);
        }
      }
      setErroresServidor(porCampo);
      if (Object.keys(porCampo).length === 0) {
        setMensajeServidor(
          sinCampo[0] ??
            (err instanceof Error ? err.message : "No fue posible guardar el cliente."),
        );
      }
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  }

  return (
    <form className="pagina-form" onSubmit={guardar} noValidate>
      <div className="pagina">
        {/* Título */}
        <h1>Registrar cliente</h1>
        <p>
          Llena los datos y presiona "Guardar cliente". Los campos marcados con
          (obligatorio) no pueden quedar vacíos.
        </p>

        {/* Aviso de error */}
        {(total > 0 || mensajeServidor) && (
          <div className="aviso-error" role="alert">
            <span className="material-symbols-rounded">error</span>
            <div>
              <strong>
                {total > 0
                  ? `No se pudo guardar: hay ${total} ${total === 1 ? "dato" : "datos"} por corregir`
                  : "No se pudo guardar"}
              </strong>
              {total > 0 ? (
                <p>
                  Revisa los campos marcados en rojo y vuelve a presionar
                  "Guardar cliente".
                </p>
              ) : (
                <p>{mensajeServidor}</p>
              )}
            </div>
          </div>
        )}

        <div className="secciones">
          {/* Tarjeta de contacto */}
          <section className="tarjeta">
            <h2>
              <span className="paso">1</span> Datos de contacto
            </h2>

            {/* Nombre y apellidos */}
            <Campo
              etiqueta="Nombre(s)"
              obligatorio
              value={contacto.nombre}
              error={errores.nombre}
              onChange={(v) => cambiarContacto("nombre", v)}
            />
            <Campo
              etiqueta="Apellido paterno"
              obligatorio
              value={contacto.apellidoPaterno}
              error={errores.apellidoPaterno}
              onChange={(v) => cambiarContacto("apellidoPaterno", v)}
            />
            <Campo
              etiqueta="Apellido materno"
              value={contacto.apellidoMaterno}
              error={errores.apellidoMaterno}
              onChange={(v) => cambiarContacto("apellidoMaterno", v)}
            />

            {/* Empresa opcional */}
            <Campo
              etiqueta="Empresa"
              value={contacto.empresa}
              error={errores.empresa}
              onChange={(v) => cambiarContacto("empresa", v)}
            />

            {/* Teléfono de 10 dígitos */}
            <Campo
              etiqueta="Teléfono"
              obligatorio
              type="tel"
              inputMode="numeric"
              autoComplete="tel"
              maxLength={10}
              placeholder="Ej. 6871234567"
              value={contacto.telefono}
              error={errores.telefono}
              onChange={(v) =>
                cambiarContacto("telefono", soloDigitos(v, 10))
              }
            />

            {/* Correo */}
            <Campo
              etiqueta="Correo electrónico"
              obligatorio
              type="email"
              value={contacto.correo}
              error={errores.correo}
              onChange={(v) => cambiarContacto("correo", v)}
            />

            {/* Nota informativa */}
            <p className="nota">
              <span className="material-symbols-rounded">info</span>
              La dirección de la obra no se captura aquí: se pide en cada
              cotización.
            </p>
          </section>

          {/* Tarjeta de razones sociales */}
          <section className="tarjeta">
            <h2>
              <span className="paso">2</span> Razones sociales para facturar
            </h2>
            <p className="ayuda">Puede agregar una o varias.</p>

            {/* Una por cada razón social */}
            {razones.map((r, i) => (
              <div className="razon" key={i}>
                <h3>Razón social {i + 1}</h3>

                <Campo
                  etiqueta="Nombre o razón social"
                  obligatorio
                  value={r.nombre}
                  error={errores[`${i}.nombre`]}
                  onChange={(v) => cambiarRazon(i, "nombre", v)}
                />

                {/* RFC en mayúsculas */}
                <Campo
                  etiqueta="RFC"
                  obligatorio
                  maxLength={13}
                  autoCapitalize="characters"
                  value={r.rfc}
                  error={errores[`${i}.rfc`]}
                  onChange={(v) => cambiarRazon(i, "rfc", soloRfc(v))}
                />

                {/* Dirección fiscal */}
                <p className="campo-etiqueta">
                  Dirección fiscal <small>(obligatorio)</small>
                </p>
                <div className="direccion">
                  <Campo
                    etiqueta="Calle"
                    value={r.calle}
                    error={errores[`${i}.calle`]}
                    onChange={(v) => cambiarRazon(i, "calle", v)}
                  />
                  <Campo
                    etiqueta="Número"
                    value={r.numero}
                    error={errores[`${i}.numero`]}
                    onChange={(v) => cambiarRazon(i, "numero", v)}
                  />
                  <Campo
                    etiqueta="Colonia"
                    value={r.colonia}
                    error={errores[`${i}.colonia`]}
                    onChange={(v) => cambiarRazon(i, "colonia", v)}
                  />
                  <Campo
                    etiqueta="Ciudad"
                    value={r.ciudad}
                    error={errores[`${i}.ciudad`]}
                    onChange={(v) => cambiarRazon(i, "ciudad", v)}
                  />
                  <Campo
                    etiqueta="Código postal"
                    inputMode="numeric"
                    maxLength={5}
                    value={r.codigoPostal}
                    error={errores[`${i}.codigoPostal`]}
                    onChange={(v) =>
                      cambiarRazon(i, "codigoPostal", soloDigitos(v, 5))
                    }
                  />
                </div>

                {/* Quitar, solo si hay más de una */}
                {razones.length > 1 && (
                  <Button
                    type="button"
                    variant="tertiary"
                    onClick={() => {
                      setRazones(razones.filter((_, j) => j !== i));
                      // Las razones se renumeran: los errores del backend ya no aplican
                      setErroresServidor({});
                    }}
                  >
                    Quitar
                  </Button>
                )}
              </div>
            ))}

            {/* Agregar otra razón social */}
            <Button
              type="button"
              variant="secondary"
              className="btn-ancho"
              onClick={() => setRazones([...razones, razonVacia()])}
            >
              + Agregar otra razón social
            </Button>
          </section>
        </div>
      </div>

      {/* Barra de acciones */}
      <div className="barra-acciones">
        <p>Los datos se guardan solo al presionar "Guardar cliente".</p>
        <Button
          type="button"
          variant="secondary"
          onClick={() => navigate("/clientes")}
        >
          Cancelar
        </Button>
        <Button type="submit" variant="primary">
          Guardar cliente
        </Button>
      </div>
    </form>
  );
}
