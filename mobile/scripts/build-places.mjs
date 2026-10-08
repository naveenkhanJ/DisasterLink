/**
 * build-places.mjs — writes src/location/placeData.ts from GeoNames' Sri Lanka file (implementation-only;
 * standards §10 "Manual location", P213; serves CH-27).
 *
 * Run from mobile/:  node scripts/build-places.mjs [path/to/LK.zip]
 * Without an argument it downloads https://download.geonames.org/export/dump/LK.zip; with one it reads
 * that local copy. It reads `LK.txt` (tab-separated, the columns are in GeoNames' readme.txt), keeps the
 * rows of feature class `P` (populated places) and writes one line per place, `name|latitude|longitude|
 * population`, coordinates rounded to 4 decimals, sorted by name. When `asciiname` differs from `name`
 * there is a second line with the same point. No dependency: the zip is read with node:zlib.
 *
 * The data is © GeoNames, Creative Commons Attribution 4.0 (CC BY 4.0), see THIRD-PARTY-NOTICES.md.
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { Buffer } from 'node:buffer';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { inflateRawSync } from 'node:zlib';

const here = dirname(fileURLToPath(import.meta.url));
const target = resolve(here, '../src/location/placeData.ts');
const SOURCE_URL = 'https://download.geonames.org/export/dump/LK.zip';

/** The columns of a GeoNames row (readme.txt), zero-based. */
const NAME = 1;
const ASCII_NAME = 2;
const LATITUDE = 4;
const LONGITUDE = 5;
const FEATURE_CLASS = 6;
const FEATURE_CODE = 7;
const POPULATION = 14;
const COLUMN_COUNT = 19;

/** The decimals kept in a coordinate. */
const DECIMALS = 4;

/** The signatures of the zip structures that are read. */
const END_OF_DIRECTORY = 0x06054b50;
const CENTRAL_ENTRY = 0x02014b50;
const LOCAL_HEADER = 0x04034b50;
const STORED = 0;
const DEFLATED = 8;

/** Reads one file out of a zip held in memory (stored or deflated entries only). */
function readZipEntry(zip, wanted) {
  let end = zip.length - 22;
  while (end >= 0 && zip.readUInt32LE(end) !== END_OF_DIRECTORY) {
    end -= 1;
  }
  if (end < 0) {
    throw new Error('Not a zip file: no end-of-directory record.');
  }
  let at = zip.readUInt32LE(end + 16);
  const entries = zip.readUInt16LE(end + 10);
  for (let i = 0; i < entries; i += 1) {
    if (zip.readUInt32LE(at) !== CENTRAL_ENTRY) {
      throw new Error('Damaged zip: bad central directory entry.');
    }
    const method = zip.readUInt16LE(at + 10);
    const compressedSize = zip.readUInt32LE(at + 20);
    const nameLength = zip.readUInt16LE(at + 28);
    const extraLength = zip.readUInt16LE(at + 30);
    const commentLength = zip.readUInt16LE(at + 32);
    const localOffset = zip.readUInt32LE(at + 42);
    const name = zip.toString('utf8', at + 46, at + 46 + nameLength);
    if (name === wanted) {
      if (zip.readUInt32LE(localOffset) !== LOCAL_HEADER) {
        throw new Error('Damaged zip: bad local header.');
      }
      const dataStart =
        localOffset + 30 + zip.readUInt16LE(localOffset + 26) + zip.readUInt16LE(localOffset + 28);
      const data = zip.subarray(dataStart, dataStart + compressedSize);
      if (method === STORED) {
        return data;
      }
      if (method === DEFLATED) {
        return inflateRawSync(data);
      }
      throw new Error(`Unsupported zip method ${method}.`);
    }
    at += 46 + nameLength + extraLength + commentLength;
  }
  throw new Error(`${wanted} is not in the zip.`);
}

async function loadZip() {
  const local = process.argv[2];
  if (local) {
    return { zip: readFileSync(resolve(local)), origin: `${SOURCE_URL} (local copy ${local})` };
  }
  const response = await fetch(SOURCE_URL);
  if (!response.ok) {
    throw new Error(`Download failed: HTTP ${response.status}`);
  }
  return { zip: Buffer.from(await response.arrayBuffer()), origin: SOURCE_URL };
}

const { zip, origin } = await loadZip();
const rows = readZipEntry(zip, 'LK.txt')
  .toString('utf8')
  .split('\n')
  .filter((line) => line.trim() !== '');

const counts = new Map();
const places = [];
for (const row of rows) {
  const columns = row.split('\t');
  if (columns.length < COLUMN_COUNT) {
    throw new Error(
      `A row has ${columns.length} columns, not ${COLUMN_COUNT}: ${row.slice(0, 80)}`,
    );
  }
  const code = `${columns[FEATURE_CLASS]}.${columns[FEATURE_CODE]}`;
  counts.set(code, (counts.get(code) ?? 0) + 1);
  if (columns[FEATURE_CLASS] !== 'P') {
    continue;
  }
  const latitude = Number(columns[LATITUDE]).toFixed(DECIMALS);
  const longitude = Number(columns[LONGITUDE]).toFixed(DECIMALS);
  const population = Number(columns[POPULATION]) || 0;
  const names = new Set([columns[NAME].trim()]);
  if (columns[ASCII_NAME].trim() !== '') {
    names.add(columns[ASCII_NAME].trim());
  }
  for (const name of names) {
    if (name.includes('|') || name === '') {
      continue;
    }
    places.push({ name, line: `${name}|${latitude}|${longitude}|${population}` });
  }
}
places.sort((a, b) => a.name.localeCompare(b.name, 'en') || a.line.localeCompare(b.line));
const data = places.map((place) => place.line).join('\n');

/**
 * Writes a string literal the way Prettier does (single quotes unless the text has more single than
 * double quotes), so the generated file passes `prettier --check`.
 */
function quote(text) {
  const singles = (text.match(/'/g) ?? []).length;
  const doubles = (text.match(/"/g) ?? []).length;
  const mark = singles > doubles ? '"' : "'";
  const body = text.replaceAll('\\', '\\\\').replaceAll('\n', '\\n').replaceAll(mark, `\\${mark}`);
  return `${mark}${body}${mark}`;
}

const today = new Date().toLocaleDateString('en-CA');
const header = [
  '/**',
  ' * placeData.ts — the populated places of Sri Lanka for the manual-location picker (type-ahead list).',
  ' *',
  ' * GENERATED FILE — do not edit. Generated by: node scripts/build-places.mjs (from mobile/)',
  ` * Source: ${origin}, downloaded ${today}; the feature class P (populated places) of LK.txt.`,
  ' * Licence: GeoNames, Creative Commons Attribution 4.0 (CC BY 4.0), https://creativecommons.org/licenses/by/4.0/',
  ' * — see THIRD-PARTY-NOTICES.md. The app shows the credit "Place names: GeoNames (CC BY 4.0)".',
  ' *',
  ` * Data only: ${places.length} lines, one place per line, \`name|latitude|longitude|population\`, coordinates`,
  ' * rounded to 4 decimals, sorted by name; a second line with the plain-ASCII name when it differs.',
  ' * It is parsed lazily by `placeIndex.ts`.',
  ' */',
  '',
  '/** The places, one per line: `name|latitude|longitude|population`. */',
  'export const PLACE_DATA: string =',
  `  ${quote(data)};`,
  '',
];
writeFileSync(target, header.join('\n'), 'utf8');

const summary = [...counts.entries()]
  .sort((a, b) => b[1] - a[1])
  .map(([code, n]) => `${code}=${n}`)
  .join(' ');
console.log(`LK.txt rows: ${rows.length}; feature codes: ${summary}`);
console.log(
  `Written ${places.length} place lines to ${target} (${Buffer.byteLength(header.join('\n'))} bytes).`,
);
