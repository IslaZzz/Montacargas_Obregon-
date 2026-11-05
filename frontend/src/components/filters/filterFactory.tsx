import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';

import '../../styles/filters.css';

// Filter components
import FilterRow from './filterRow';
import FilterDropdown from './filterDropdown';
import DatePicker from './datePicker';
import DateRangePicker from './dateRangePicker';

// Types
import type {
  FilterDefinition,
  FilterOption,
  FilterType,
  DateRangeValue
} from './filterTypes';

// Factory props
interface FilterFactoryProps<T> {
  data: T[];
  filters: FilterDefinition<T>[];
  children: (filteredData: T[]) => ReactNode;
  allLabel?: string;
}

export default function FilterFactory<T>({
  data,
  filters,
  children,
  allLabel = 'Todos',
}: FilterFactoryProps<T>) {

  // Selected values
  const [selectedFilters, setSelectedFilters] =
    useState<Record<string, string>>({});

  // Selected date ranges
  const [selectedDateRanges, setSelectedDateRanges] =
    useState<Record<string, DateRangeValue>>({});

  // Update individual filter
  const handleFilterChange = (
    id: string,
    value: string
  ) => {
    setSelectedFilters(previous => ({
      ...previous,
      [id]: value,
    }));
  };

  // Update date range
  const handleDateRangeChange = (
    id: string,
    field: 'from' | 'to',
    value: string
  ) => {
    setSelectedDateRanges(previous => ({
      ...previous,
      [id]: {
        from: previous[id]?.from ?? '',
        to: previous[id]?.to ?? '',
        [field]: value,
      },
    }));
  };

  // Reset all filters
  const resetFilters = () => {
    setSelectedFilters({});
    setSelectedDateRanges({});
  };

  // Apply filters to local dataset
  const filteredData = useMemo(() => {

    return data.filter(item =>
      filters.every(filter => {

        const selectedValue =
          selectedFilters[filter.id];

        const itemValue = filter.getValue(item);

        // DATE RANGE
        if (filter.type === 'date-range') {

          const range = selectedDateRanges[filter.id];

          if (!range || (!range.from && !range.to)) {
            return true;
          }

          if (!itemValue) return false;

          if (range.from && itemValue < range.from) {
            return false;
          }

          if (range.to && itemValue > range.to) {
            return false;
          }

          return true;
        }

        // Ignore inactive filters
        if (!selectedValue || selectedValue === 'all') {
          return true;
        }

        // Apply filter according to its type
        switch (filter.type) {

          case 'row':
          case 'dropdown':
          case 'date':
            return itemValue === selectedValue;

          case 'text':
            return itemValue
              .toLowerCase()
              .includes(selectedValue.toLowerCase());

          default:
            return true;
        }

      })
    );

  }, [data, filters, selectedFilters, selectedDateRanges]);

  // Check active filters
  const hasActiveFilters =
    Object.values(selectedFilters).some(
      value => value !== 'all' && value !== ''
    ) ||
    Object.values(selectedDateRanges).some(
      range => range.from !== '' || range.to !== ''
    );

  return (
    <section className="filter-factory">

      <div className="table-toolbar">

        <div className="filters-row">

          {filters.map(filter => {

            const selectedValue =
              selectedFilters[filter.id] ?? 'all';

            const options = filter.options ?? [];

            return (
              <div
                className="filter-group"
                key={filter.id}
              >

                <span className="filter-label">
                  {filter.label}
                </span>

                {/* ROW / CHIPS */}
                {filter.type === 'row' && (
                  <FilterRow
                    options={options}
                    selectedValue={selectedValue}
                    allLabel={allLabel}
                    onChange={value =>
                      handleFilterChange(filter.id, value)
                    }
                  />
                )}

                {/* DROPDOWN */}
                {filter.type === 'dropdown' && (
                  <FilterDropdown
                    options={options}
                    selectedValue={selectedValue}
                    allLabel={allLabel}
                    onChange={value =>
                      handleFilterChange(filter.id, value)
                    }
                  />
                )}

                {/* DATE */}
                {filter.type === 'date' && (
                  <DatePicker
                    value={
                      selectedValue === 'all'
                        ? ''
                        : selectedValue
                    }
                    onChange={value =>
                      handleFilterChange(
                        filter.id,
                        value || 'all'
                      )
                    }
                  />
                )}

                {/* DATE RANGE */}
                {filter.type === 'date-range' && (
                  <DateRangePicker
                    value={
                      selectedDateRanges[filter.id] ?? {
                        from: '',
                        to: '',
                      }
                    }
                    onChange={(field, value) =>
                      handleDateRangeChange(
                        filter.id,
                        field,
                        value
                      )
                    }
                  />
                )}

                {/* TEXT */}
                {filter.type === 'text' && (
                  <input
                    type="text"
                    className="filter-input"
                    placeholder={
                      filter.placeholder ??
                      `Buscar ${filter.label.toLowerCase()}...`
                    }
                    value={
                      selectedValue === 'all'
                        ? ''
                        : selectedValue
                    }
                    onChange={event =>
                      handleFilterChange(
                        filter.id,
                        event.target.value || 'all'
                      )
                    }
                  />
                )}

              </div>
            );
          })}

        </div>

        {/* RESET */}
        {hasActiveFilters && (
          <button
            type="button"
            className="filter-reset"
            onClick={resetFilters}
          >
            Limpiar filtros
          </button>
        )}

      </div>

      {/* RESULTS */}
      <div className="filter-results-info">
        {filteredData.length} resultados
      </div>

      {children(filteredData)}

    </section>
  );
}
