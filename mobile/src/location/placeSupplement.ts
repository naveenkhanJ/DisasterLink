/**
 * placeSupplement.ts — places that GeoNames' populated-places list lacks, for the manual-location
 * picker (implementation-only; standards §10 "Manual location", Supplement, P222; serves CH-27). It is
 * read by `placeIndex.ts` next to `PLACE_DATA`; an entry ranks before a GeoNames entry of the same name.
 * Each entry is hand-written from a named source and is a phone-side label only: it is never stored or
 * sent.
 *
 * Same form as `placeData.ts`: one place per line, `name|latitude|longitude|population`.
 *
 * Sources:
 * - Malabe — Wikipedia, "Malabe" (https://en.wikipedia.org/wiki/Malabe), Colombo District,
 *   6°54′13″N 79°57′17″E (6.90361, 79.95472). GeoNames' populated places list only a village of this
 *   name in Kurunegala District.
 */

/** The supplementary places, one per line: `name|latitude|longitude|population`. */
export const PLACE_SUPPLEMENT: string = 'Malabe|6.9036|79.9547|0';
