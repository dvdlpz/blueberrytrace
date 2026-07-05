import type { ReactNode } from 'react';
import { PackageOpen } from 'lucide-react';
import { EmptyState } from './EmptyState';

export interface TableColumn<T> {
  key: keyof T | string;
  label: string;
  render?: (item: T) => ReactNode;
}

interface DataTableProps<T> {
  title: string;
  description: string;
  columns: TableColumn<T>[];
  items: T[];
  emptyTitle?: string;
  emptyDescription?: string;
  emptyAction?: ReactNode;
  countLabel?: string;
}

export function DataTable<T extends { id: number }>({
  title,
  description,
  columns,
  items,
  emptyTitle = 'Aún no hay registros',
  emptyDescription = 'Los registros aparecerán aquí cuando se complete la primera operación.',
  emptyAction,
  countLabel = 'registros'
}: DataTableProps<T>) {
  return (
    <section className="panel-card">
      <div className="panel-card__header">
        <div>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>
        <span className="panel-card__count">{items.length} {countLabel}</span>
      </div>

      {items.length > 0 ? (
        <div className="data-table-wrap">
          <table className="data-table">
            <thead>
              <tr>{columns.map((column) => <th key={String(column.key)}>{column.label}</th>)}</tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  {columns.map((column) => {
                    const raw = item[column.key as keyof T];
                    return <td key={String(column.key)}>{column.render ? column.render(item) : String(raw ?? '')}</td>;
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <EmptyState compact icon={<PackageOpen size={24} />} title={emptyTitle} description={emptyDescription} action={emptyAction} />
      )}
    </section>
  );
}
