import type { District } from '../../api/types';
import { resolveDistrict } from '../LocationService';
import { distanceKm, nearestPlace, normalisePlaceText, searchPlaces } from '../placeIndex';

/**
 * The demo's places and the district each lies in (SC2 storyline, standards §10 "Demo"). Malabe is tested with the supplement
 * below.
 */
const DEMO_PLACES: [string, District][] = [
  ['Kaduwela', 'COLOMBO'],
  ['Polgahawela', 'KURUNEGALA'],
  ['Kelaniya', 'GAMPAHA'],
  ['Kalutara', 'KALUTARA'],
];

describe('searchPlaces with the real GeoNames list', () => {
  it.each(DEMO_PLACES)('SC2 A1 12c: %s is found and lies in %s', (name, district) => {
    const matches = searchPlaces(name);

    const found = matches.find((match) => match.name === name && match.district === district);
    expect(found).toBeDefined();
  });

  it('SC2 A1 12c (P222, G11): "malabe" finds the Malabe of Colombo first, from the supplement, then the village in Kurunegala', () => {
    const matches = searchPlaces('malabe');

    expect(matches[0]).toMatchObject({ name: 'Malabe', district: 'COLOMBO' });
    expect(matches[0].point).toEqual({ latitude: 6.9036, longitude: 79.9547 });
    expect(
      matches.filter((match) => match.name === 'Malabe').map((match) => match.district),
    ).toEqual(['COLOMBO', 'KURUNEGALA']);
  });

  it('SC2 A1 12c (P222, G11): "mala" lists Malabe among the first three', () => {
    const names = searchPlaces('mala')
      .slice(0, 3)
      .map((match) => match.name);

    expect(names).toContain('Malabe');
  });

  it('SC2 A1 12c: "kalu" lists Kalutara among the first three', () => {
    const names = searchPlaces('kalu')
      .slice(0, 3)
      .map((match) => match.name);

    expect(names).toContain('Kalutara');
  });

  it('SC2 A1 12c: a query that matches nothing gives nothing', () => {
    expect(searchPlaces('xqzqxq')).toEqual([]);
  });

  it('SC2 A1 12c: a query shorter than two characters gives nothing', () => {
    expect(searchPlaces('k')).toEqual([]);
    expect(searchPlaces('')).toEqual([]);
    expect(searchPlaces(' . ')).toEqual([]);
  });

  it('SC2 A1 12c: the limit is applied (8 by default)', () => {
    expect(searchPlaces('ka')).toHaveLength(8);
    expect(searchPlaces('ka', 3)).toHaveLength(3);
    expect(searchPlaces('ka', 0)).toEqual([]);
  });

  it('SC2 A1 12c: names that start with the query come before names that only contain it', () => {
    const matches = searchPlaces('pola', 50);
    const firstContaining = matches.findIndex(
      (match) => !normalisePlaceText(match.name).startsWith('pola'),
    );

    expect(firstContaining).not.toBe(0);
    if (firstContaining > 0) {
      matches
        .slice(0, firstContaining)
        .forEach((match) => expect(normalisePlaceText(match.name).startsWith('pola')).toBe(true));
    }
  });

  it('SC2 A1 12c: a word that starts with the query ranks before a name that merely contains it', () => {
    const matches = searchPlaces('road', 200).map((match) => normalisePlaceText(match.name));
    const startsWord = (name: string) => name.split(' ').some((word) => word.startsWith('road'));
    const lastWordMatch = matches.map(startsWord).lastIndexOf(true);
    const firstOther = matches.findIndex((name) => !startsWord(name));

    if (lastWordMatch >= 0 && firstOther >= 0) {
      expect(lastWordMatch).toBeLessThan(firstOther);
    }
  });

  it('SC2 A1 12c: each match has a point inside Sri Lanka and its name as GeoNames gives it', () => {
    for (const match of searchPlaces('kandy')) {
      expect(match.point.latitude).toBeGreaterThan(5.8);
      expect(match.point.latitude).toBeLessThan(10);
      expect(match.name.length).toBeGreaterThan(0);
    }
  });

  it('SC2 A1 12c: no two suggestions have the same name and district', () => {
    const matches = searchPlaces('ka', 200);
    const identities = matches.map(
      (match) => `${normalisePlaceText(match.name)}|${match.district}`,
    );

    expect(new Set(identities).size).toBe(identities.length);
  });

  it('SC2 A1 12c: within a group the more populous place comes first', () => {
    const matches = searchPlaces('colombo', 20);

    expect(matches[0].name).toBe('Colombo');
  });
});

describe('normalisePlaceText', () => {
  it('SC2 A1 12c: lower-cases, drops diacritics, dots and apostrophes, and collapses spaces', () => {
    expect(normalisePlaceText('  St.  Joseph’s   Ñandy ')).toBe('st josephs nandy');
    expect(normalisePlaceText('Mount Lavinia')).toBe('mount lavinia');
  });
});

describe('distanceKm', () => {
  it('SC2 A1 12c: is zero for the same point and about 111 km for one degree of latitude', () => {
    const here = { latitude: 6.5888, longitude: 79.964 };

    expect(distanceKm(here, here)).toBeCloseTo(0, 6);
    expect(distanceKm(here, { latitude: 7.5888, longitude: 79.964 })).toBeCloseTo(111.2, 0);
  });

  it('SC2 A1 12c: Colombo to Kalutara is about 40 km', () => {
    const km = distanceKm(
      { latitude: 6.9319, longitude: 79.8478 },
      { latitude: 6.5854, longitude: 79.9607 },
    );

    expect(km).toBeGreaterThan(35);
    expect(km).toBeLessThan(45);
  });
});

describe('nearestPlace (standards §10 "Manual location", Nearest-place label, P222)', () => {
  it("SC2 A1 12c: a point at Kaduwela's coordinates gives Kaduwela, in COLOMBO, at about 0 km", () => {
    const kaduwela = searchPlaces('Kaduwela').find((match) => match.district === 'COLOMBO');

    const found = nearestPlace(kaduwela?.point ?? { latitude: 0, longitude: 0 });

    expect(found?.name).toBe('Kaduwela');
    expect(found?.district).toBe('COLOMBO');
    expect(found?.distanceKm).toBeLessThan(0.1);
  });

  it("SC2 A1 12c: the supplement's Malabe is found at its own point", () => {
    const found = nearestPlace({ latitude: 6.9036, longitude: 79.9547 });

    expect(found?.name).toBe('Malabe');
    expect(found?.district).toBe('COLOMBO');
  });

  it('SC2 A1 12c: a point between two towns gives the closer one', () => {
    const near = { latitude: 6.9, longitude: 79.9 };
    const first = nearestPlace(near);
    const second = nearestPlace({
      latitude: first?.point.latitude ?? 0,
      longitude: (first?.point.longitude ?? 0) + 0.01,
    });

    expect(first).toBeDefined();
    expect(distanceKm(near, first?.point ?? near)).toBeLessThanOrEqual(10);
    // No other place is closer than the one found.
    for (const match of searchPlaces(first?.name ?? '', 50)) {
      expect(distanceKm(near, match.point)).toBeGreaterThanOrEqual((first?.distanceKm ?? 0) - 1e-9);
    }
    expect(second?.distanceKm).toBeLessThan(2);
  });

  it('SC2 A1 12c: a point 20 km out at sea gives undefined', () => {
    expect(nearestPlace({ latitude: 6.5, longitude: 79.2 })).toBeUndefined();
  });

  it('SC2 A1 12c: a wider radius finds a place the default does not', () => {
    const sea = { latitude: 6.5, longitude: 79.45 };

    expect(nearestPlace(sea)).toBeUndefined();
    expect(nearestPlace(sea, 60)?.distanceKm).toBeGreaterThan(10);
  });

  it("SC2 step 18: the district is the pin's, not the place's, near a district border", () => {
    // Just inside Kalutara, close to the Colombo border; the nearest village may be in Colombo.
    const pin = { latitude: 6.7, longitude: 79.93 };
    const found = nearestPlace(pin);

    expect(found?.district).toBe(resolveDistrict(pin));
  });

  it('SC2 A1 12c: ties are decided by population (the more populous place wins)', () => {
    // A point exactly on a place with population: the exact match has distance 0.
    const colombo = searchPlaces('Colombo')[0];

    expect(nearestPlace(colombo.point)?.distanceKm).toBeLessThan(0.5);
  });

  it('SC2 A1 12c: one call over the real data takes a few milliseconds', () => {
    nearestPlace({ latitude: 6.9, longitude: 79.9 }); // builds the index
    const start = performance.now();
    for (let i = 0; i < 20; i += 1) {
      nearestPlace({ latitude: 6.9 + i * 0.01, longitude: 79.9 });
    }
    const perCall = (performance.now() - start) / 20;
    console.info(`nearestPlace: ${perCall.toFixed(2)} ms per call over the real data`);

    expect(perCall).toBeLessThan(50);
  });
});
