export type CsvRow = Array<string | number | null | undefined>;

function cleanCell(value: string | number | null | undefined) {
  const normalized = value === null || value === undefined ? '' : String(value);
  const withoutBreaks = normalized.replace(/\r?\n|\r/g, ' ').trim();
  return `"${withoutBreaks.replace(/"/g, '""')}"`;
}

function fileDownload(filename: string, content: BlobPart, type: string) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export function downloadCsv(filename: string, headers: string[], rows: CsvRow[]) {
  const csv = [headers, ...rows]
    .map((row) => row.map(cleanCell).join(';'))
    .join('\n');
  fileDownload(
    filename.endsWith('.csv') ? filename : `${filename}.csv`,
    `\uFEFF${csv}`,
    'text/csv;charset=utf-8;'
  );
}

export function printCurrentView() {
  window.print();
}
