// ============================================================================
// Xtrim — el resumen de la medición de Lighthouse (HU-30)
//
//   node scripts/lighthouse-summary.mjs <carpeta-con-los-informes>
//
// Imprime, por página: la nota de cada categoría, las métricas que importan
// y el peso de lo que se descarga.
// ============================================================================

import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const dir = process.argv[2] ?? '.';
const files = readdirSync(dir).filter((name) => name.endsWith('.json')).sort();

const METRICS = [
  ['first-contentful-paint', 'FCP', 'ms'],
  ['largest-contentful-paint', 'LCP', 'ms'],
  ['total-blocking-time', 'TBT', 'ms'],
  ['cumulative-layout-shift', 'CLS', ''],
  ['speed-index', 'SI', 'ms'],
];

for (const file of files) {
  const lhr = JSON.parse(readFileSync(join(dir, file), 'utf8'));
  const audits = lhr.audits;
  const categories = lhr.categories;

  console.log(`\n═══ ${file.replace(/\.json$/, '')}  ${lhr.finalDisplayedUrl}`);

  const scores = ['performance', 'accessibility', 'best-practices', 'seo']
    .map((name) => {
      const category = categories[name];
      return `${name}=${category ? Math.round(category.score * 100) : '—'}`;
    })
    .join('  ');
  console.log(`  Notas: ${scores}`);

  for (const [id, label] of METRICS) {
    const audit = audits[id];
    if (!audit) {
      continue;
    }
    const value = Math.round(audit.numericValue * 100) / 100;
    console.log(`  ${label.padEnd(5)} ${String(value).padStart(9)}   ${audit.displayValue ?? ''}`);
  }

  const bytes = audits['total-byte-weight'];
  if (bytes) {
    console.log(`  Peso  ${String(Math.round(bytes.numericValue / 1024)).padStart(9)} KB   ${bytes.displayValue ?? ''}`);
  }

  const requests = audits['network-requests']?.details?.items ?? [];
  if (requests.length) {
    const byType = new Map();
    for (const request of requests) {
      byType.set(
        request.resourceType,
        (byType.get(request.resourceType) ?? 0) + (request.transferSize ?? 0),
      );
    }
    const parts = [...byType]
      .sort((a, b) => b[1] - a[1])
      .map(([type, size]) => `${type} ${Math.round(size / 1024)} KB`);
    console.log(`  Por tipo: ${parts.join(' · ')}`);
  }

  const opportunities = Object.values(audits)
    .filter((audit) => audit.details?.type === 'opportunity' && audit.details.overallSavingsMs > 0)
    .sort((a, b) => b.details.overallSavingsMs - a.details.overallSavingsMs);
  for (const opportunity of opportunities.slice(0, 3)) {
    console.log(`  opportunity: ${opportunity.title} (${Math.round(opportunity.details.overallSavingsMs)} ms)`);
  }
}
