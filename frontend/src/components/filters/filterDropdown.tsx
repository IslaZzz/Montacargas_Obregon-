
import { useState } from 'react';
import type { FilterOption } from './filterTypes';

interface FilterDropdownProps {
  options?: FilterOption[];
  selectedValue: string;
  allLabel: string;
  onChange: (value: string) => void;
}

export default function FilterDropdown({
  options = [],
  selectedValue,
  allLabel,
  onChange,
}: FilterDropdownProps) {

  const [isOpen, setIsOpen] = useState(false);

  const safeOptions = options ?? [];

  const selectedLabel =
    safeOptions.find(option => option.value === selectedValue)?.label
    ?? allLabel;

  return (
    <div className="dropdown">

      <button
        type="button"
        className="dropdown-trigger"
        onClick={() => setIsOpen(previous => !previous)}
        aria-expanded={isOpen}
      >
        {selectedLabel}
        <span className="dropdown-arrow">▾</span>
      </button>

      {isOpen && (
        <div className="dropdown-menu open">

          <button
            type="button"
            onClick={() => {
              onChange('all');
              setIsOpen(false);
            }}
          >
            {allLabel}
          </button>

          {safeOptions.map(option =>  (
            <button
              type="button"
              key={option.value}
              onClick={() => {
                onChange(option.value);
                setIsOpen(false);
              }}
            >
              {option.label}
            </button>
          ))}

        </div>
      )}

    </div>
  );
}
