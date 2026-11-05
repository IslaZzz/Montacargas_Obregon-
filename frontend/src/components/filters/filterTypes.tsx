
export type FilterType =
  | 'row'
  | 'dropdown'
  | 'date'
  | 'date-range'
  | 'text';

export interface FilterOption {
  label: string;
  value: string;
}

export interface DateRangeValue {
  from: string;
  to: string;
}

export interface FilterDefinition<T> {
  id: string;
  label: string;
  type: FilterType;
  options?: FilterOption[];
  getValue: (item: T) => string;
  placeholder?: string;
}
