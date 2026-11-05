import FilterFactory from '../../components/filters/filterFactory';
import type {
  FilterDefinition
} from '../../components/filters/filterTypes';

interface Renta {
  id: number;
  folio: string;
  cliente: string;
  equipo: string;
  estado: string;
  ubicacion: string;
  fechaInicio: string;
  horas: number;
}

const rentas: Renta[] = [
  {
    id: 1,
    folio: 'REN-001',
    cliente: 'Constructora del Norte',
    equipo: 'Toyota 8FGU25',
    estado: 'en-curso',
    ubicacion: 'almacen-a',
    fechaInicio: '2026-11-02',
    horas: 120,
  },
  {
    id: 2,
    folio: 'REN-002',
    cliente: 'Industrial del Pacífico',
    equipo: 'Hyster H50FT',
    estado: 'programado',
    ubicacion: 'almacen-b',
    fechaInicio: '2026-11-05',
    horas: 85,
  },
  {
    id: 3,
    folio: 'REN-003',
    cliente: 'Constructora del Norte',
    equipo: 'CAT GP25N',
    estado: 'finalizado',
    ubicacion: 'almacen-a',
    fechaInicio: '2026-10-20',
    horas: 200,
  },
  {
    id: 4,
    folio: 'REN-004',
    cliente: 'Logística Express',
    equipo: 'Toyota 8FGU30',
    estado: 'en-curso',
    ubicacion: 'almacen-b',
    fechaInicio: '2026-11-10',
    horas: 150,
  },
  {
    id: 5,
    folio: 'REN-005',
    cliente: 'Industrial del Pacífico',
    equipo: 'Hyster H50FT',
    estado: 'programado',
    ubicacion: 'almacen-a',
    fechaInicio: '2026-11-15',
    horas: 95,
  },
  {
    id: 6,
    folio: 'REN-006',
    cliente: 'Logística Express',
    equipo: 'CAT GP25N',
    estado: 'finalizado',
    ubicacion: 'almacen-b',
    fechaInicio: '2026-09-18',
    horas: 60,
  },
];

// FILTER CONFIGURATION

const filters: FilterDefinition<Renta>[] = [

  // 1. CHIPS
  {
    id: 'estado',
    label: 'Estado',
    type: 'row',
    options: [
      { label: 'En curso', value: 'en-curso' },
      { label: 'Programado', value: 'programado' },
      { label: 'Finalizado', value: 'finalizado' },
    ],
    getValue: renta => renta.estado,
  },

  // 2. DROPDOWN
  {
    id: 'ubicacion',
    label: 'Ubicación',
    type: 'dropdown',
    options: [
      { label: 'Almacén A', value: 'almacen-a' },
      { label: 'Almacén B', value: 'almacen-b' },
    ],
    getValue: renta => renta.ubicacion,
  },

  // 3. DATE
  {
    id: 'fecha-exacta',
    label: 'Fecha exacta',
    type: 'date',
    getValue: renta => renta.fechaInicio,
  },

  // 4. DATE RANGE
  {
    id: 'periodo',
    label: 'Periodo de renta',
    type: 'date-range',
    getValue: renta => renta.fechaInicio,
  },

  // 5. TEXT
  {
    id: 'cliente',
    label: 'Cliente',
    type: 'text',
    placeholder: 'Buscar cliente...',
    getValue: renta => renta.cliente,
  },

];

export default function FilterTest() {

  return (
    <main style={{ padding: '32px' }}>

      <h1>Filter Factory</h1>

      <p>
        Prueba de filtros dinámicos para el sistema de rentas.
      </p>

      <FilterFactory
        data={rentas}
        filters={filters}
      >

        {(filteredRentas) => (

          <div className="table-container">

            <table className="data-table">

              <thead>
                <tr>
                  <th>Folio</th>
                  <th>Cliente</th>
                  <th>Equipo</th>
                  <th>Estado</th>
                  <th>Ubicación</th>
                  <th>Fecha de inicio</th>
                  <th>Horas</th>
                </tr>
              </thead>

              <tbody>

                {filteredRentas.length > 0 ? (

                  filteredRentas.map(renta => (

                    <tr key={renta.id}>
                      <td className="cell-primary">
                        {renta.folio}
                      </td>

                      <td>{renta.cliente}</td>
                      <td>{renta.equipo}</td>
                      <td>{renta.estado}</td>
                      <td>{renta.ubicacion}</td>
                      <td>{renta.fechaInicio}</td>
                      <td>{renta.horas}</td>
                    </tr>

                  ))

                ) : (

                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center' }}>
                      No se encontraron resultados.
                    </td>
                  </tr>

                )}

              </tbody>

            </table>

          </div>

        )}

      </FilterFactory>

    </main>
  );
}
