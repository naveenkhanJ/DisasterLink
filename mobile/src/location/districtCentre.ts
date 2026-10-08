/**
 * districtCentre.ts — where the manual-location map starts: the centre of the citizen's registered
 * district (implementation-only; standards §10 "Manual location", P206; WF2P-S03a). The centre is
 * the middle of the bounding box of the district's largest outline ring in `DISTRICT_OUTLINES`.
 *
 * Serves CH-27 (SC2 A1 12c).
 */
import type { GeoPoint } from '../api/reportApi';
import type { District } from '../api/types';
import { DISTRICT_OUTLINES, type OutlineRing } from './districtOutlines';

/** The edges of a ring's bounding box, in degrees. */
interface Box {
  minLatitude: number;
  maxLatitude: number;
  minLongitude: number;
  maxLongitude: number;
}

function boundingBox(ring: OutlineRing): Box {
  const longitudes = ring.map(([longitude]) => longitude);
  const latitudes = ring.map(([, latitude]) => latitude);
  return {
    minLatitude: Math.min(...latitudes),
    maxLatitude: Math.max(...latitudes),
    minLongitude: Math.min(...longitudes),
    maxLongitude: Math.max(...longitudes),
  };
}

function area(box: Box): number {
  return (box.maxLatitude - box.minLatitude) * (box.maxLongitude - box.minLongitude);
}

/**
 * Finds the centre of a district.
 *
 * @param district the district
 * @returns the centre of the bounding box of the district's largest outline ring
 */
export function districtCentre(district: District): GeoPoint {
  const largest = DISTRICT_OUTLINES[district]
    .map(boundingBox)
    .reduce((best, box) => (area(box) > area(best) ? box : best));
  return {
    latitude: (largest.minLatitude + largest.maxLatitude) / 2,
    longitude: (largest.minLongitude + largest.maxLongitude) / 2,
  };
}
