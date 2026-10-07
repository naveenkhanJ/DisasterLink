// Jest runs from web/, but the scripts under test are the back end's static files, so the root is the repository.
const path = require('path');

module.exports = {
  rootDir: path.resolve(__dirname, '..'),
  roots: ['<rootDir>/backend/src/main/resources/static/js', '<rootDir>/web/test'],
  testMatch: ['<rootDir>/web/test/**/*.test.js'],
  testEnvironment: 'jsdom',
  transform: { '\\.js$': ['babel-jest', { configFile: path.resolve(__dirname, 'babel.config.js') }] },
  collectCoverageFrom: [
    'backend/src/main/resources/static/js/*.js',
    'backend/src/main/resources/static/js/offline/*.js',
    // districts.js is data; report-charts.js is the prototype's chart code that Naveenkhan replaces.
    '!backend/src/main/resources/static/js/districts.js',
    '!backend/src/main/resources/static/js/report-charts.js',
  ],
  // The default istanbul instrumentation reports no files on Windows (test-exclude 7 rejects backslash paths, also with
  // @babel/core 7), so V8 coverage is used.
  coverageProvider: 'v8',
  coverageDirectory: '<rootDir>/web/coverage',
  coverageThreshold: {
    global: { lines: 80, branches: 80 },
    // The browser offline module is the skeleton's own code (standards section 8): 90 % lines and branches.
    [path.resolve(__dirname, '../backend/src/main/resources/static/js/offline') + path.sep]: { lines: 90, branches: 90 },
  },
};
