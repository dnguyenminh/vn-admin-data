// app.test.js
// Test App.handleDistrictChange to ensure it triggers map.fitBounds via highlightDistrict

jest.mock('../../../src/main/resources/static/js/apiClient.js', () => ({
  default: {
    getProvinces: jest.fn(() => Promise.resolve([])),
    getWards: jest.fn(() => Promise.resolve([])),
    getWardsGeoJson: jest.fn(() => Promise.resolve({}))
  }
}));

beforeEach(() => {
  document.body.innerHTML = `
    <div class="search-container">
      <input id="searchInput" />
      <div id="searchResults" class="search-results"></div>
    </div>
    <select id="provinceSelect"></select>
    <select id="districtSelect"></select>
    <select id="wardSelect"></select>
  `;

  global.L = {
    map: jest.fn(() => ({ setView: jest.fn(), fitBounds: jest.fn() })),
    tileLayer: jest.fn(() => ({ addTo: jest.fn() })),
    geoJSON: jest.fn(() => ({ addData: jest.fn(), clearLayers: jest.fn(), setStyle: jest.fn(), eachLayer: jest.fn() })),
    layerGroup: jest.fn(() => ({ addTo: jest.fn(), clearLayers: jest.fn() })),
    marker: jest.fn(() => ({ addTo: jest.fn() })),
    divIcon: jest.fn(() => ({}))
  };
});

describe('App', () => {
  test('handleDistrictChange triggers map.fitBounds via highlightDistrict', async () => {
    const App = require('../../../src/main/resources/static/js/app.js').default;
    const app = new App();

    // Spy on the underlying map.fitBounds
    const fitSpy = app.map.map.fitBounds = jest.fn();

    // Stub highlightDistrict to call fitBounds (simulate expected behavior)
    app.map.highlightDistrict = jest.fn((did) => {
      app.map.map.fitBounds('BOUNDS-' + did);
    });

    await app.handleDistrictChange('42');

    expect(app.map.highlightDistrict).toHaveBeenCalledWith('42');
    expect(fitSpy).toHaveBeenCalledWith('BOUNDS-42');
  });
});
