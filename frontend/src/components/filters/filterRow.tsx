
import type { FilterOption } from './filterTypes';

interface FilterRowProps {
  options: FilterOption[];
  selectedValue: string;
  allLabel: string;
  allCount?: number; // Opcional: total de registros, se muestra junto a allLabel
  onChange: (value: string) => void;
}

export default function FilterRow({
  options,
  selectedValue,
  allLabel,
  allCount,
  onChange,
}: FilterRowProps) {

  return (
    <div className="filters-row">

      <button
        type="button"
        className={`filter-chip ${
          selectedValue === 'all' ? 'active' : ''
        }`}
        onClick={() => onChange('all')}
      >
        {allLabel}
        {allCount !== undefined && (
          <span className="filter-chip-count">{allCount}</span>
        )}
      </button>

      {options.map(option => (
        <button
          type="button"
          key={option.value}
          className={`filter-chip ${
            selectedValue === option.value ? 'active' : ''
          }`}
          onClick={() => onChange(option.value)}
        >
          {option.label}
          {option.count !== undefined && (
            <span className="filter-chip-count">{option.count}</span>
          )}
        </button>
      ))}

    </div>
  );
}
