
interface DatePickerProps {
  value: string;
  onChange: (value: string) => void;
}

export default function DatePicker({
  value,
  onChange,
}: DatePickerProps) {

  return (
    <input
      type="date"
      className="filter-date"
      value={value}
      onChange={event => onChange(event.target.value)}
    />
  );
}
