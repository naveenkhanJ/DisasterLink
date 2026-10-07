// Jest for the citizen app: the jest-expo preset. Coverage measures the skeleton modules; the prototype's screens,
// components, sample data and theme and the generated outlines are left out (the screens are measured when UC2
// writes them).
module.exports = {
  preset: 'jest-expo',
  testMatch: ['<rootDir>/src/**/__tests__/**/*.test.ts?(x)'],
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/screens/**',
    '!src/components/**',
    '!src/data/**',
    '!src/theme.ts',
    '!src/location/districtOutlines.ts',
  ],
  coverageThreshold: { global: { lines: 80, branches: 80 } },
};
