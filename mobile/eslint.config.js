// Flat ESLint configuration: the Expo rules, then eslint-config-prettier to switch off the rules that
// Prettier owns (formatting is checked by Prettier, not ESLint).
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');
const prettierConfig = require('eslint-config-prettier');

module.exports = defineConfig([
  expoConfig,
  prettierConfig,
  { ignores: ['dist/*', 'coverage/*', 'node_modules/*', '.expo/*'] },
]);
