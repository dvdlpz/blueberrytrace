import type { ReactNode } from 'react';
import { Search } from 'lucide-react';

interface FilterToolbarProps {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  children?: ReactNode;
}

export function FilterToolbar({ value, onChange, placeholder, children }: FilterToolbarProps) {
  return (
    <div className="filter-toolbar">
      <label className="filter-toolbar__search">
        <Search size={16} />
        <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
      </label>
      {children}
    </div>
  );
}
