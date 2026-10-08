// Jest for the citizen app: the jest-expo preset. Coverage measures the app code: the skeleton modules, the UC2 logic,
// the screens (src/screens), the components (src/components) and App.tsx (UC2-6, P181). Only the generated district
// outlines and place data and the tests themselves are left out.
module.exports = {
  preset: 'jest-expo',
  testMatch: ['<rootDir>/src/**/__tests__/**/*.test.ts?(x)'],
  collectCoverageFrom: [
    'App.tsx',
    'src/**/*.{ts,tsx}',
    '!src/**/__tests__/**',
    '!src/location/districtOutlines.ts',
    '!src/location/placeData.ts',
  ],
  coverageThreshold: { global: { lines: 80, branches: 80 } },
};
