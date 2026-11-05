import type { DateRangeValue } from './filterTypes';

interface DateRangePickerProps {
  value: DateRangeValue;
  onChange: (
    field: 'from' | 'to',
    value: string
  ) => void;
}

export default function DateRangePicker({
  value,
  onChange,
}: DateRangePickerProps) {

  return (
    <div className="date-range-container">

      <div className="date-range-field">
        <label>Desde</label>

        <input
          type="date"
          className="filter-date"
          value={value.from}
          max={value.to || undefined}
          onChange={event =>
            onChange('from', event.target.value)
          }
        />
      </div>

      <span className="date-range-separator">—</span>

      <div className="date-range-field">
        <label>Hasta</label>

        <input
          type="date"
          className="filter-date"
          value={value.to}
          min={value.from || undefined}
          onChange={event =>
            onChange('to', event.target.value)
          }
        />
      </div>

    </div>
  );
}
