import { DISTRICTS, type District } from '../../api/types';
import { districtCentre } from '../districtCentre';
import { resolveDistrict } from '../LocationService';

/** Known centres of two districts, in degrees (the district towns' neighbourhood). */
const KNOWN: [District, number, number][] = [
  ['COLOMBO', 6.93, 79.88],
  ['KALUTARA', 6.58, 80.1],
];

describe('districtCentre', () => {
  it.each(KNOWN)(
    'SC2 A1 12c: the centre of %s is near the known centre',
    (district, latitude, longitude) => {
      const centre = districtCentre(district);

      expect(Math.abs(centre.latitude - latitude)).toBeLessThan(0.3);
      expect(Math.abs(centre.longitude - longitude)).toBeLessThan(0.3);
    },
  );

  it.each(KNOWN)('SC2 A1 12c: the centre of %s resolves to the same district', (district) => {
    expect(resolveDistrict(districtCentre(district))).toBe(district);
  });

  it('SC2 A1 12c: every district has a centre inside the box of Sri Lanka', () => {
    for (const district of DISTRICTS) {
      const { latitude, longitude } = districtCentre(district);
      expect(latitude).toBeGreaterThan(5.85);
      expect(latitude).toBeLessThan(9.9);
      expect(longitude).toBeGreaterThan(79.5);
      expect(longitude).toBeLessThan(81.95);
    }
  });
});
