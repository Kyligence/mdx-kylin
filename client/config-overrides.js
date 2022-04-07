const { override, addDecoratorsLegacy, addLessLoader } = require('customize-cra');
const { addProgressPlugin, addSpeedMeasurePlugin, fixContextPath } = require('./scripts/plugins');

module.exports = override(
  fixContextPath(),
  addDecoratorsLegacy(),
  addLessLoader({
    // for ant design
    javascriptEnabled: true,
    math: 'always',
  }),
  // addHappyPackPlugin('babel-loader', 3),
  addSpeedMeasurePlugin(),
  addProgressPlugin(),
);
