import { useEffect, useState } from "react";
import { Button } from "../../../components/buttons";
import Campo from "../../../components/CampoForm";
import DataTable from "../../../components/table";
import type { TableColumn } from "../../../components/table";
import { formatoTelefono } from "../../../utils/formato";
import { buscarClientes } from "../../clientes/clientesApi";
import type { Cliente } from "../../clientes/clientesTypes";
import { nombreCompleto } from "../cotizacionesUtils";
import type { BorradorCotizacion } from "./nuevaCotizacionTypes";

interface Props {
  borrador: BorradorCotizacion;
  cambiar: (cambios: Partial<BorradorCotizacion>) => void;
}

const ESPERA_BUSQUEDA_MS = 300;

function useBusquedaClientes(texto: string) {
  const [resultados, setResultados] = useState<Cliente[]>([]);
  const [error, setError] = useState(false);

  useEffect(() => {
    let vigente = true;
    const temporizador = setTimeout(() => {
      buscarClientes(texto.trim())
        .then((lista) => {
          if (!vigente) return;
          setResultados(lista);
          setError(false);
        })
        .catch(() => vigente && setError(true));
    }, ESPERA_BUSQUEDA_MS);
    return () => {
      vigente = false;
      clearTimeout(temporizador);
    };
  }, [texto]);

  return { resultados, error };
}

const direccionFiscal = (d: Cliente["razonesSociales"][number]["direccionFiscal"]) =>
  `${d.calle} ${d.numero}, ${d.colonia}, ${d.ciudad}, C.P. ${d.codigoPostal}`;

interface ClienteElegidoProps {
  cliente: Cliente;
  razonSocialId: number | null;
  cambiar: Props["cambiar"];
}

function ClienteElegido({ cliente, razonSocialId, cambiar }: ClienteElegidoProps) {
  return (
    <section className="tarjeta">
      <div className="nc-tarjeta-encabezado">
        <div>
          <h2>{nombreCompleto(cliente)}</h2>
          <p>{[cliente.empresa, formatoTelefono(cliente.telefono), cliente.correo].filter(Boolean).join(" · ")}</p>
        </div>
        <Button type="button" variant="tertiary" onClick={() => cambiar({ cliente: null, razonSocialId: null })}>
          Cambiar cliente
        </Button>
      </div>

      <fieldset className="nc-opciones">
        <legend className="campo-etiqueta">Razón social para facturar</legend>
        {cliente.razonesSociales.map((razon) => {
          const elegida = razonSocialId === razon.id;
          return (
            <label key={razon.id} className={elegida ? "razon nc-opcion nc-opcion-elegida" : "razon nc-opcion"}>
              <input type="radio" name="razonSocial" value={razon.id} checked={elegida}
                onChange={() => cambiar({ razonSocialId: razon.id })} />
              <span>
                <strong>{razon.nombre}</strong>
                <span>RFC: {razon.rfc}</span>
                <span className="nc-secundario">{direccionFiscal(razon.direccionFiscal)}</span>
              </span>
            </label>
          );
        })}
      </fieldset>
    </section>
  );
}

function BuscadorClientes({ cambiar }: Pick<Props, "cambiar">) {
  const [texto, setTexto] = useState("");
  const { resultados, error } = useBusquedaClientes(texto);

  const elegir = (cliente: Cliente) =>
    cambiar({
      cliente,
      razonSocialId: cliente.razonesSociales.length === 1 ? cliente.razonesSociales[0].id : null,
    });

  const columnas: TableColumn<Cliente>[] = [
    { header: "Nombre", accessor: (c) => nombreCompleto(c), className: "cell-primary" },
    { header: "Empresa", accessor: (c) => c.empresa ?? "—" },
    { header: "Teléfono", accessor: (c) => formatoTelefono(c.telefono) },
    {
      header: "",
      accessor: (c) => (
        <Button type="button" variant="tertiary" onClick={() => elegir(c)} aria-label={`Elegir a ${nombreCompleto(c)}`}>
          Elegir
        </Button>
      ),
    },
  ];

  return (
    <section className="tarjeta">
      <h2>¿Para qué cliente es?</h2>
      <Campo etiqueta="Buscar cliente" value={texto} onChange={setTexto}
        placeholder="Nombre, empresa, teléfono o RFC" autoFocus />
      {error && <p className="campo-error" role="alert">No se pudo buscar. Revisa que el servidor esté encendido.</p>}
      <DataTable columns={columnas} data={resultados} getRowKey={(c) => c.id}
        emptyMessage="No hay clientes con ese dato. Regístralo primero en Clientes." />
    </section>
  );
}

export default function PasoCliente({ borrador, cambiar }: Props) {
  return borrador.cliente
    ? <ClienteElegido cliente={borrador.cliente} razonSocialId={borrador.razonSocialId} cambiar={cambiar} />
    : <BuscadorClientes cambiar={cambiar} />;
}
