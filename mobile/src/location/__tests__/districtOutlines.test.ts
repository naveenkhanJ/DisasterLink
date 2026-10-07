import { DISTRICTS, type District } from '../../api/types';
import { DISTRICT_OUTLINES, type OutlineRing } from '../districtOutlines';

/** Even-odd point-in-polygon, used by this test only; `resolveDistrict` is UC2's. */
function inside(longitude: number, latitude: number, ring: OutlineRing): boolean {
  let result = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const [xi, yi] = ring[i];
    const [xj, yj] = ring[j];
    if (yi > latitude !== yj > latitude && longitude < ((xj - xi) * (latitude - yi)) / (yj - yi) + xi) {
      result = !result;
    }
  }
  return result;
}

function districtsAt(longitude: number, latitude: number): District[] {
  return DISTRICTS.filter((district) =>
    DISTRICT_OUTLINES[district].some((ring) => inside(longitude, latitude, ring)),
  );
}

describe('DISTRICT_OUTLINES', () => {
  it('has an outline for each of the 25 District constants and no other key', () => {
    expect(DISTRICTS).toHaveLength(25);
    expect(Object.keys(DISTRICT_OUTLINES).sort()).toEqual([...DISTRICTS].sort());
  });

  it('gives every district at least one ring with enough points', () => {
    for (const district of DISTRICTS) {
      expect(DISTRICT_OUTLINES[district].length).toBeGreaterThan(0);
      for (const ring of DISTRICT_OUTLINES[district]) {
        expect(ring.length).toBeGreaterThanOrEqual(4);
      }
    }
  });

  it('closes every ring: the first point is the last', () => {
    for (const district of DISTRICTS) {
      for (const ring of DISTRICT_OUTLINES[district]) {
        expect(ring[0]).toEqual(ring[ring.length - 1]);
      }
    }
  });

  it("keeps every point inside Sri Lanka's bounding box", () => {
    for (const district of DISTRICTS) {
      for (const ring of DISTRICT_OUTLINES[district]) {
        for (const [longitude, latitude] of ring) {
          expect(longitude).toBeGreaterThan(79.4);
          expect(longitude).toBeLessThan(82.0);
          expect(latitude).toBeGreaterThan(5.8);
          expect(latitude).toBeLessThan(9.9);
        }
      }
    }
  });

  it('puts Kaduwela (6.9356 N, 79.9843 E) in COLOMBO and no other district', () => {
    expect(districtsAt(79.9843, 6.9356)).toEqual(['COLOMBO']);
  });

  it("puts SC2's Kalutara bridge (6.5888 N, 79.9640 E) in KALUTARA and no other district", () => {
    expect(districtsAt(79.964, 6.5888)).toEqual(['KALUTARA']);
  });

  it('puts a point in the sea in no district', () => {
    expect(districtsAt(79.5, 6.5)).toEqual([]);
  });
});
