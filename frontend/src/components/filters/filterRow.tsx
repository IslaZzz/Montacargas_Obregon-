
import type { FilterOption } from './filterTypes';

interface FilterRowProps {
  options: FilterOption[];
  selectedValue: string;
  allLabel: string;
  onChange: (value: string) => void;
}

export default function FilterRow({
  options,
  selectedValue,
  allLabel,
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
        </button>
      ))}

    </div>
  );
}
