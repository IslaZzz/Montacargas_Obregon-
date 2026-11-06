import { Fragment } from 'react';
import type { ReactNode, Key } from 'react';
import '../styles/table.css';

export interface TableColumn<T> {
  // Texto o elemento, por ejemplo una casilla para seleccionar todo
  header: ReactNode;
  accessor: (row: T) => ReactNode;
  // Se aplica a la celda y al encabezado, para que los dos se alineen igual
  className?: string;
}

interface DataTableProps<T, S> {
  columns: TableColumn<T>[];
  data: T[];
  getRowKey?: (row: T, index: number) => Key;
  // Opcional: clase extra para una fila, por ejemplo para resaltarla
  getRowClassName?: (row: T, index: number) => string | undefined;
  // Opcional: qué hacer al hacer clic en una fila, por ejemplo abrir su detalle
  onRowClick?: (row: T) => void;
  // Opcional: filas hijas debajo de la fila (por ejemplo, los equipos de una renta).
  // Usan las mismas columnas que la tabla, así quedan alineadas con el encabezado.
  // Si regresa null o una lista vacía, no se muestran
  getSubfilas?: (row: T) => S[] | null;
  subcolumnas?: TableColumn<S>[];
  getSubfilaKey?: (subfila: S, index: number) => Key;
  emptyMessage?: string;
}

// Los clics en botones, enlaces o campos dentro de la fila hacen lo suyo, no el clic de la fila
const esControl = (elemento: EventTarget) =>
  elemento instanceof Element &&
  elemento.closest('a, button, input, select, textarea, label') !== null;

export default function DataTable<T, S = never>({
  columns,
  data,
  getRowKey,
  getRowClassName,
  onRowClick,
  getSubfilas,
  subcolumnas,
  getSubfilaKey,
  emptyMessage = 'No hay registros disponibles',
}: DataTableProps<T, S>) {

  return (
    <div className="table-container">
      <table className="data-table">

        <thead>
          <tr>
            {columns.map((column, index) => (
              <th key={index} className={column.className}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {data.length > 0 ? (
            data.map((row, rowIndex) => {
              const subfilas = subcolumnas ? getSubfilas?.(row) ?? [] : [];

              return (
                <Fragment key={getRowKey?.(row, rowIndex) ?? rowIndex}>
                  <tr
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

                  {subfilas.map((subfila, subIndex) => (
                    <tr key={getSubfilaKey?.(subfila, subIndex) ?? subIndex} className="fila-detalle">
                      {subcolumnas!.map((column, colIndex) => (
                        <td key={colIndex} className={column.className ?? ''}>
                          {column.accessor(subfila)}
                        </td>
                      ))}
                    </tr>
                  ))}
                </Fragment>
              );
            })
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