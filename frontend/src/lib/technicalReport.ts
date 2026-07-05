export type TechnicalReportCell = string | number | null | undefined;

export interface TechnicalReportMetric {
  label: string;
  value: TechnicalReportCell;
}

export interface TechnicalReportData {
  appName: string;
  title: string;
  generatedAt: string;
  generatedBy: string;
  period: string;
  filters: string[];
  status: string;
  metrics: TechnicalReportMetric[];
  headers: string[];
  rows: TechnicalReportCell[][];
  totals: TechnicalReportCell[];
  observations: string[];
  confidentiality: string;
  logoUrl: string;
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

function safeSpreadsheetValue(value: TechnicalReportCell): string | number {
  if (typeof value === 'number') return value;
  const stringValue = value == null ? '' : String(value);
  return /^[=+\-@]/.test(stringValue) ? `'${stringValue}` : stringValue;
}

function printable(value: TechnicalReportCell): string {
  return value == null ? '' : String(value);
}

async function loadImageDataUrl(url: string): Promise<string | null> {
  try {
    const response = await fetch(url);
    if (!response.ok) return null;
    const blob = await response.blob();
    return await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(blob);
    });
  } catch {
    return null;
  }
}

function downloadFile(filename: string, blob: Blob) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export async function downloadTechnicalPdf(filename: string, report: TechnicalReportData) {
  const [{ jsPDF }, { default: autoTable }] = await Promise.all([import('jspdf'), import('jspdf-autotable')]);
  const documentPdf = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
  const logo = await loadImageDataUrl(report.logoUrl);
  const pageWidth = documentPdf.internal.pageSize.getWidth();
  const generatedLines = [
    `Generado: ${report.generatedAt}`,
    `Usuario: ${report.generatedBy}`,
    `Periodo: ${report.period}`,
    `Estado de información: ${report.status}`
  ];

  if (logo) {
    try {
      documentPdf.addImage(logo, 'PNG', 14, 10, 18, 18);
    } catch {
      // The report remains usable when a browser cannot decode the local logo.
    }
  }
  documentPdf.setFontSize(15);
  documentPdf.text(report.appName, logo ? 36 : 14, 16);
  documentPdf.setFontSize(11);
  documentPdf.text(report.title, logo ? 36 : 14, 23);
  documentPdf.setFontSize(8.5);
  generatedLines.forEach((line, index) => documentPdf.text(line, 14, 35 + index * 4.2));
  documentPdf.text(`Filtros: ${report.filters.join(' · ') || 'Sin filtros adicionales'}`, 100, 35, { maxWidth: pageWidth - 114 });

  const metricsRows = report.metrics.map((metric) => [metric.label, printable(metric.value)]);
  autoTable(documentPdf, {
    startY: 53,
    head: [['Resumen ejecutivo', 'Valor']],
    body: metricsRows.length > 0 ? metricsRows : [['Sin métricas disponibles', '—']],
    theme: 'grid',
    styles: { fontSize: 8, cellPadding: 2 },
    headStyles: { fillColor: [28, 74, 48] }
  });

  const afterMetrics = (documentPdf as unknown as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY || 60;
  const dataRows = report.rows.length > 0
    ? report.rows.map((row) => row.map(printable))
    : [report.headers.map((_, index) => index === 0 ? 'Sin información disponible para los filtros seleccionados.' : '')];

  autoTable(documentPdf, {
    startY: afterMetrics + 7,
    head: [report.headers],
    body: dataRows,
    foot: report.rows.length > 0 && report.totals.some((value) => printable(value) !== '') ? [report.totals.map(printable)] : undefined,
    theme: 'grid',
    styles: { fontSize: 7.2, cellPadding: 1.6, overflow: 'linebreak' },
    headStyles: { fillColor: [28, 74, 48] },
    footStyles: { fillColor: [232, 244, 235], textColor: [24, 56, 38] },
    margin: { bottom: 20 }
  });

  const afterTable = (documentPdf as unknown as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY || afterMetrics + 15;
  const notes = report.observations.length > 0 ? report.observations : ['Sin observaciones adicionales basadas en los datos disponibles.'];
  documentPdf.setFontSize(8.5);
  documentPdf.text('Observaciones', 14, afterTable + 8);
  documentPdf.setFontSize(7.7);
  documentPdf.text(notes.map((note) => `• ${note}`), 14, afterTable + 13, { maxWidth: pageWidth - 28, lineHeightFactor: 1.4 });

  const pages = documentPdf.getNumberOfPages();
  for (let page = 1; page <= pages; page += 1) {
    documentPdf.setPage(page);
    documentPdf.setFontSize(7);
    documentPdf.text(report.confidentiality, 14, documentPdf.internal.pageSize.getHeight() - 8);
    documentPdf.text(`Página ${page} de ${pages}`, pageWidth - 34, documentPdf.internal.pageSize.getHeight() - 8);
  }
  documentPdf.save(filename.endsWith('.pdf') ? filename : `${filename}.pdf`);
}

function escapeXml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '');
}

function columnName(index: number): string {
  let value = index;
  let result = '';
  while (value > 0) {
    const remainder = (value - 1) % 26;
    result = String.fromCharCode(65 + remainder) + result;
    value = Math.floor((value - 1) / 26);
  }
  return result;
}

function spreadsheetCell(reference: string, value: TechnicalReportCell, style = 0): string {
  const styleAttribute = style > 0 ? ` s="${style}"` : '';
  if (typeof value === 'number' && Number.isFinite(value)) {
    return `<c r="${reference}"${styleAttribute}><v>${value}</v></c>`;
  }
  const normalized = safeSpreadsheetValue(value);
  return `<c r="${reference}"${styleAttribute} t="inlineStr"><is><t xml:space="preserve">${escapeXml(String(normalized))}</t></is></c>`;
}

async function loadPngBytes(url: string): Promise<Uint8Array | null> {
  try {
    const response = await fetch(url);
    if (!response.ok) return null;
    const bytes = new Uint8Array(await response.arrayBuffer());
    const isPng = bytes.length >= 8
      && bytes[0] === 0x89
      && bytes[1] === 0x50
      && bytes[2] === 0x4E
      && bytes[3] === 0x47;
    return isPng ? bytes : null;
  } catch {
    return null;
  }
}

function technicalStylesXml(): string {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="5">
    <font><sz val="10"/><name val="Aptos"/></font>
    <font><b/><color rgb="FFFFFFFF"/><sz val="10"/><name val="Aptos"/></font>
    <font><b/><color rgb="FF183826"/><sz val="10"/><name val="Aptos"/></font>
    <font><i/><color rgb="FF53625A"/><sz val="9"/><name val="Aptos"/></font>
    <font><b/><color rgb="FF1C4A30"/><sz val="16"/><name val="Aptos"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1C4A30"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE8F4EB"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border><left style="thin"><color rgb="FFE6E9E7"/></left><right style="thin"><color rgb="FFE6E9E7"/></right><top style="thin"><color rgb="FFE6E9E7"/></top><bottom style="thin"><color rgb="FFE6E9E7"/></bottom><diagonal/></border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="6">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf>
    <xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf>
  </cellXfs>
</styleSheet>`;
}

export async function downloadTechnicalXlsx(filename: string, report: TechnicalReportData) {
  const { zipSync, strToU8 } = await import('fflate');
  const logoBytes = await loadPngBytes(report.logoUrl);
  const outputRows = report.rows.length > 0
    ? report.rows
    : [report.headers.map((_, index) => index === 0 ? 'Sin información disponible para los filtros seleccionados.' : '')];
  const reportWidth = Math.max(8, report.headers.length + 1);
  const reportLastColumn = columnName(reportWidth);
  const rows: string[] = [];
  const merges: string[] = [`B1:${reportLastColumn}1`, `B2:${reportLastColumn}2`, `C6:${reportLastColumn}6`];
  const pushRow = (rowIndex: number, cells: string[]) => rows.push(`<row r="${rowIndex}">${cells.join('')}</row>`);

  pushRow(1, [spreadsheetCell('B1', report.appName, 3)]);
  pushRow(2, [spreadsheetCell('B2', report.title, 4)]);
  pushRow(4, [
    spreadsheetCell('B4', 'Generado', 4), spreadsheetCell('C4', report.generatedAt),
    spreadsheetCell('E4', 'Usuario', 4), spreadsheetCell('F4', report.generatedBy)
  ]);
  pushRow(5, [
    spreadsheetCell('B5', 'Periodo', 4), spreadsheetCell('C5', report.period),
    spreadsheetCell('E5', 'Estado', 4), spreadsheetCell('F5', report.status)
  ]);
  pushRow(6, [spreadsheetCell('B6', 'Filtros', 4), spreadsheetCell('C6', report.filters.join(' · ') || 'Sin filtros adicionales')]);

  const metricStart = 8;
  pushRow(metricStart, [spreadsheetCell(`B${metricStart}`, 'Resumen ejecutivo', 1), spreadsheetCell(`C${metricStart}`, 'Valor', 1)]);
  (report.metrics.length > 0 ? report.metrics : [{ label: 'Sin métricas disponibles', value: '—' }]).forEach((metric, index) => {
    const rowIndex = metricStart + 1 + index;
    pushRow(rowIndex, [spreadsheetCell(`B${rowIndex}`, metric.label), spreadsheetCell(`C${rowIndex}`, metric.value)]);
  });

  const tableStart = Math.max(metricStart + Math.max(report.metrics.length, 1) + 3, 13);
  pushRow(tableStart, report.headers.map((header, index) => spreadsheetCell(`${columnName(index + 2)}${tableStart}`, header, 1)));
  outputRows.forEach((row, rowOffset) => {
    const rowIndex = tableStart + 1 + rowOffset;
    pushRow(rowIndex, row.map((value, index) => spreadsheetCell(`${columnName(index + 2)}${rowIndex}`, value)));
  });

  let lastDataRow = tableStart + outputRows.length;
  if (report.rows.length > 0 && report.totals.some((value) => printable(value) !== '')) {
    const totalRow = lastDataRow + 1;
    pushRow(totalRow, report.totals.map((value, index) => spreadsheetCell(`${columnName(index + 2)}${totalRow}`, value, 2)));
    lastDataRow = totalRow;
  }

  const notesRow = lastDataRow + 3;
  pushRow(notesRow, [spreadsheetCell(`A${notesRow}`, 'Observaciones', 4)]);
  const notes = report.observations.length > 0 ? report.observations : ['Sin observaciones adicionales basadas en los datos disponibles.'];
  notes.forEach((observation, index) => {
    const rowIndex = notesRow + 1 + index;
    merges.push(`A${rowIndex}:${reportLastColumn}${rowIndex}`);
    pushRow(rowIndex, [spreadsheetCell(`A${rowIndex}`, `• ${observation}`, 5)]);
  });
  const confidentialityRow = notesRow + notes.length + 2;
  merges.push(`A${confidentialityRow}:${reportLastColumn}${confidentialityRow}`);
  pushRow(confidentialityRow, [spreadsheetCell(`A${confidentialityRow}`, report.confidentiality, 5)]);

  const columns = Array.from({ length: reportWidth }, (_, index) => `<col min="${index + 1}" max="${index + 1}" width="${index === 0 ? 12 : 20}" customWidth="1"/>`).join('');
  const sheetXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetViews><sheetView workbookViewId="0"><pane ySplit="${tableStart}" topLeftCell="A${tableStart + 1}" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
  <cols>${columns}</cols>
  <sheetData>${rows.join('')}</sheetData>
  <mergeCells count="${merges.length}">${merges.map((ref) => `<mergeCell ref="${ref}"/>`).join('')}</mergeCells>
  <pageMargins left="0.3" right="0.3" top="0.5" bottom="0.5" header="0.2" footer="0.2"/>
  ${logoBytes ? '<drawing r:id="rId1"/>' : ''}
</worksheet>`;

  const now = new Date().toISOString();
  const files: Record<string, Uint8Array> = {
    '[Content_Types].xml': strToU8(`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  ${logoBytes ? '<Default Extension="png" ContentType="image/png"/>' : ''}
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  ${logoBytes ? '<Override PartName="/xl/drawings/drawing1.xml" ContentType="application/vnd.openxmlformats-officedocument.drawing+xml"/>' : ''}
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>`),
    '_rels/.rels': strToU8(`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>`),
    'xl/workbook.xml': strToU8(`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Informe técnico" sheetId="1" r:id="rId1"/></sheets></workbook>`),
    'xl/_rels/workbook.xml.rels': strToU8(`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>`),
    'xl/worksheets/sheet1.xml': strToU8(sheetXml),
    'xl/styles.xml': strToU8(technicalStylesXml()),
    'docProps/core.xml': strToU8(`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:creator>${escapeXml(report.generatedBy)}</dc:creator><dc:title>${escapeXml(report.title)}</dc:title><dc:subject>${escapeXml(report.appName)}</dc:subject><dcterms:created xsi:type="dcterms:W3CDTF">${now}</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">${now}</dcterms:modified></cp:coreProperties>`),
    'docProps/app.xml': strToU8(`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>BlueberryTrace</Application></Properties>`)
  };

  if (logoBytes) {
    files['xl/worksheets/_rels/sheet1.xml.rels'] = strToU8(`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/></Relationships>`);
    files['xl/drawings/drawing1.xml'] = strToU8(`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <xdr:oneCellAnchor><xdr:from><xdr:col>0</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>0</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from><xdr:ext cx="685800" cy="685800"/><xdr:pic><xdr:nvPicPr><xdr:cNvPr id="1" name="Logo BlueberryTrace"/><xdr:cNvPicPr/></xdr:nvPicPr><xdr:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill><xdr:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="685800" cy="685800"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor>
</xdr:wsDr>`);
    files['xl/drawings/_rels/drawing1.xml.rels'] = strToU8(`<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/logo.png"/></Relationships>`);
    files['xl/media/logo.png'] = logoBytes;
  }
  const archive = zipSync(files, { level: 6 });
  downloadFile(filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`, new Blob([archive.buffer as ArrayBuffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  }));
}

export function downloadTechnicalCsv(filename: string, headers: string[], rows: TechnicalReportCell[][]) {
  const clean = (value: TechnicalReportCell) => `"${printable(value).replace(/"/g, '""')}"`;
  const content = [headers, ...rows].map((row) => row.map(clean).join(';')).join('\n');
  fileDownload(filename.endsWith('.csv') ? filename : `${filename}.csv`, `\uFEFF${content}`, 'text/csv;charset=utf-8;');
}
