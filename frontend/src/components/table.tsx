import type { ReactNode, Key } from 'react';
import '../styles/table.css';

export interface TableColumn<T> {
  // Texto o elemento, por ejemplo una casilla para seleccionar todo
  header: ReactNode;
  accessor: (row: T) => ReactNode;
  className?: string;
}

interface DataTableProps<T> {
  columns: TableColumn<T>[];
  data: T[];
  getRowKey?: (row: T, index: number) => Key;
  // Opcional: clase extra para una fila, por ejemplo para resaltarla
  getRowClassName?: (row: T, index: number) => string | undefined;
  // Opcional: qué hacer al hacer clic en una fila, por ejemplo abrir su detalle
  onRowClick?: (row: T) => void;
  emptyMessage?: string;
}

// Los clics en botones, enlaces o campos dentro de la fila hacen lo suyo, no el clic de la fila
const esControl = (elemento: EventTarget) =>
  elemento instanceof Element &&
  elemento.closest('a, button, input, select, textarea, label') !== null;

export default function DataTable<T>({
  columns,
  data,
  getRowKey,
  getRowClassName,
  onRowClick,
  emptyMessage = 'No hay registros disponibles',
}: DataTableProps<T>) {

  return (
    <div className="table-container">
      <table className="data-table">

        <thead>
          <tr>
            {columns.map((column, index) => (
              <th key={index}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {data.length > 0 ? (
            data.map((row, rowIndex) => (
              <tr
                key={getRowKey?.(row, rowIndex) ?? rowIndex}
                className={
                  [getRowClassName?.(row, rowIndex), onRowClick && 'fila-clicable']
                    .filter(Boolean)
                    .join(' ') || undefined
                }
                onClick={
                  onRowClick &&
                  ((evento) => {
                    if (!esControl(evento.target)) onRowClick(row);
                  })
                }
              >

                {columns.map((column, colIndex) => (
                  <td
                    key={colIndex}
                    className={column.className ?? ''}
                  >
                    {column.accessor(row)}
                  </td>
                ))}

              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={columns.length}>
                {emptyMessage}
              </td>
            </tr>
          )}
        </tbody>

      </table>
    </div>
  );
}