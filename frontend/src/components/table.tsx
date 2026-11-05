import type { ReactNode, Key } from 'react';
import '../styles/table.css';

export interface TableColumn<T> {
  header: string;
  accessor: (row: T) => ReactNode;
  className?: string;
}

interface DataTableProps<T> {
  columns: TableColumn<T>[];
  data: T[];
  getRowKey?: (row: T, index: number) => Key;
  emptyMessage?: string;
}

export default function DataTable<T>({
  columns,
  data,
  getRowKey,
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
              <tr key={getRowKey?.(row, rowIndex) ?? rowIndex}>

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
