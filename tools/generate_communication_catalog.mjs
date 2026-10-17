// Rebuild only from the connector snapshot. Never fill editorial gaps or generate identities.
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import assert from 'node:assert/strict';

const root = fileURLToPath(new URL('../', import.meta.url));
const source = JSON.parse(readFileSync(resolve(root, 'docs/communication/source_v1.json'), 'utf8'));
const table = name => {
  const [headers, ...rows] = source.sheets[name].values;
  assert.equal(new Set(headers).size, headers.length);
  return rows.map(row => {
    assert.ok(row.length <= headers.length);
    return Object.fromEntries(headers.map((header, i) => [header, row[i] ?? '']));
  });
};
const categories = table('Categorías');
const vocabulary = table('Vocabulario');
const counts = {
  categories: categories.length,
  vocabulary: vocabulary.length,
  validated: vocabulary.filter(x => x.estado === 'VALIDADO').length,
  pending: vocabulary.filter(x => x.estado === 'PENDIENTE_ARASAAC').length,
  textOnly: vocabulary.filter(x => x.estrategia_visual === 'SOLO_TEXTO').length,
  pictogramPending: vocabulary.filter(x => x.estrategia_visual === 'PICTOGRAMA_PENDIENTE').length,
};
assert.deepEqual(Object.values(counts), [18, 132, 111, 21, 10, 11]);
assert.equal(new Set(categories.map(x => x.categoria_id)).size, 18);
assert.equal(new Set(vocabulary.map(x => x.vocab_id)).size, 132);
vocabulary.forEach(x => {
  assert.ok(categories.some(c => c.categoria_id === x.categoria_id));
  assert.ok(x.vocab_id && x.texto.trim() && Number.isInteger(x.orden) && x.orden > 0);
});
const catalog = {
  version: 1, contractVersion: '1.0', sourceUrl: source.sourceUrl,
  capturedOn: source.capturedOn, counts, categories, vocabulary,
};
const output = resolve(root, 'app/src/main/assets/communication/communication_catalog_v1.json');
const serialized = JSON.stringify(catalog, null, 2) + '\n';
if (process.argv.includes('--check')) assert.equal(readFileSync(output, 'utf8').replaceAll('\r\n', '\n'), serialized);
else writeFileSync(output, serialized, 'utf8');
console.log(JSON.stringify(counts));
