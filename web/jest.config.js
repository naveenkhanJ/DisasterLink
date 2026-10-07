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
    // districts.js is data; report-charts.js is the prototype's chart code that Naveenkhan replaces.
    '!backend/src/main/resources/static/js/districts.js',
    '!backend/src/main/resources/static/js/report-charts.js',
  ],
  // The default istanbul instrumentation reports no files with @babel/core 8 and babel-jest 30, so V8 coverage is used.
  coverageProvider: 'v8',
  coverageDirectory: '<rootDir>/web/coverage',
  coverageThreshold: { global: { lines: 80 } },
};
