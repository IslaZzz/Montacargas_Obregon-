import DataTable from '../../components/table';
import Chip from '../../components/chips';
import { Button } from '../../components/buttons';
import type { TableColumn } from '../../components/table';

interface Equipo {
  id: number;
  nombre: string;
  modelo: string;
  estado: 'disponible' | 'mantenimiento';
  ubicacion: string;
}

const equipos: Equipo[] = [
  {
    id: 1,
    nombre: 'Montacargas A',
    modelo: 'Toyota 8FGU25',
    estado: 'disponible',
    ubicacion: 'Almacén A',
  },
  {
    id: 2,
    nombre: 'Montacargas B',
    modelo: 'Hyster H50FT',
    estado: 'mantenimiento',
    ubicacion: 'Almacén B',
  },
  {
    id: 3,
    nombre: 'Montacargas C',
    modelo: 'CAT GP25N',
    estado: 'disponible',
    ubicacion: 'Patio',
  },
];

const columns: TableColumn<Equipo>[] = [
  { header: 'ID', accessor: e => e.id },
  {
    header: 'Equipo',
    accessor: e => e.nombre,
    className: 'cell-primary',
  },
  { header: 'Modelo', accessor: e => e.modelo },
  {
    header: 'Estado',
    accessor: e => (
      <Chip category="equipo" status={e.estado} />
    ),
  },
  { header: 'Ubicación', accessor: e => e.ubicacion },
  {
    header: 'Acciones',
    accessor: e => (
      <Button variant="secondary" onClick={() => alert(e.nombre)}>
        Ver
      </Button>
    ),
    className: 'cell-actions',
  },
];

export default function StylesTest() {
  return (
    <main style={{ padding: '40px' }}>
      <h1>Reusable Components</h1>

      <h2>Chips</h2>

      <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
        <Chip category="equipo" status="disponible" />
        <Chip category="equipo" status="mantenimiento" />
        <Chip category="equipo" status="preparacion" />
        <Chip category="equipo" status="fuera-servicio" />
      </div>

      <h2 style={{ marginTop: '40px' }}>Data Table</h2>

      <DataTable
        columns={columns}
        data={equipos}
        getRowKey={e => e.id}
      />
    </main>
  );
}
