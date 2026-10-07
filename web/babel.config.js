// Jest's native ES-module support is experimental, so the plain scripts are transformed with babel-jest
// (implementation standards section 8). The scripts themselves stay plain browser scripts.
module.exports = {
  presets: [['@babel/preset-env', { targets: { node: 'current' } }]],
};
