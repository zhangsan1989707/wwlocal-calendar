const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

export function formatMonth(date: Date) {
  return `${date.getFullYear()}年${date.getMonth() + 1}月`;
}

export function formatDate(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function getWeekdayName(index: number) {
  return weekNames[index];
}

export function addDays(date: Date, amount: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + amount);
  return next;
}

export function addMonths(date: Date, amount: number) {
  const next = new Date(date);
  next.setMonth(next.getMonth() + amount);
  return next;
}

export function startOfWeek(date: Date) {
  return addDays(date, -date.getDay());
}

export function getMonthCells(date: Date) {
  const first = new Date(date.getFullYear(), date.getMonth(), 1);
  const gridStart = startOfWeek(first);
  return Array.from({ length: 42 }, (_, index) => {
    const cellDate = addDays(gridStart, index);
    return {
      date: cellDate,
      key: formatDate(cellDate),
      inMonth: cellDate.getMonth() === date.getMonth(),
      isToday: formatDate(cellDate) === formatDate(new Date())
    };
  });
}

function pad(value: number) {
  return String(value).padStart(2, '0');
}
