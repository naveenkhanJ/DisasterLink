import { ADDRESS_LABEL } from '../../screens/texts';
import { composeDescription, splitDescription } from '../descriptionAddress';

describe('composeDescription and splitDescription (standards §10 "The address", D-UC2-4)', () => {
  it('SC2 step 9 / P214: with no address the description is stored alone', () => {
    expect(composeDescription('Water is rising', '')).toBe('Water is rising');
    expect(splitDescription('Water is rising')).toEqual({
      description: 'Water is rising',
      address: '',
    });
  });

  it('SC2 step 9 / P214: an address is the labelled last line, and splitting gives both parts back', () => {
    const stored = composeDescription('Water is rising', 'Near the old bridge');

    expect(stored).toBe(`Water is rising\n${ADDRESS_LABEL}Near the old bridge`);
    expect(ADDRESS_LABEL).toBe('Address: ');
    expect(splitDescription(stored)).toEqual({
      description: 'Water is rising',
      address: 'Near the old bridge',
    });
  });

  it('SC2 step 9 / P214: a blank address (spaces only) is not stored', () => {
    expect(composeDescription('Water is rising', '   ')).toBe('Water is rising');
  });

  it('SC2 step 9 / P214: a description of several lines keeps them; only the last line is the address', () => {
    const description = 'Line one\nLine two';

    const stored = composeDescription(description, 'Kalutara bridge');

    expect(splitDescription(stored)).toEqual({ description, address: 'Kalutara bridge' });
  });

  it('SC2 step 9 / P214: an address line that is not the last line stays in the description', () => {
    const stored = `${ADDRESS_LABEL}Somewhere\nWater is rising`;

    expect(splitDescription(stored)).toEqual({ description: stored, address: '' });
  });

  it('SC2 step 9 / P214: an address with no description round-trips with an empty description', () => {
    const stored = composeDescription('', 'Kalutara bridge');

    expect(stored).toBe(`\n${ADDRESS_LABEL}Kalutara bridge`);
    expect(splitDescription(stored)).toEqual({ description: '', address: 'Kalutara bridge' });
    expect(splitDescription(`${ADDRESS_LABEL}Only an address`)).toEqual({
      description: '',
      address: 'Only an address',
    });
  });

  it('SC2 step 9 / P214: an unset description splits into two empty parts', () => {
    expect(splitDescription(undefined)).toEqual({ description: '', address: '' });
    expect(splitDescription('')).toEqual({ description: '', address: '' });
  });
});
