import type { District } from '../../api/types';
import { distanceKm, normalisePlaceText, searchPlaces } from '../placeIndex';

/**
 * The demo's places and the district each lies in (SC2 storyline, standards §10 "Demo"). Malabe is not here: see
 * the test below.
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

  it('SC2 A1 12c: Malabe is in the list only as the village in KURUNEGALA, not as the Malabe of COLOMBO (a gap of the GeoNames data, reported in UC2-7d)', () => {
    const malabe = searchPlaces('Malabe').filter((match) => match.name === 'Malabe');

    expect(malabe.map((match) => match.district)).toEqual(['KURUNEGALA']);
    expect(searchPlaces('Malabe').some((match) => match.district === 'COLOMBO')).toBe(false);
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
