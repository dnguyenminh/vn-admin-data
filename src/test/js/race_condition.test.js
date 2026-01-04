// race_condition.test.js
// Regression test reproducing the 'Show Predicted' button flip race condition

jest.mock('../../../src/main/resources/static/js/apiClient.js', () => ({
  default: {
    getProvinces: jest.fn(() => Promise.resolve([])),
    getWards: jest.fn(() => Promise.resolve([])),
    getWardsGeoJson: jest.fn(() => Promise.resolve({})),
    getAddresses: jest.fn(() => Promise.resolve({ items: [] })),
  }
}));

beforeEach(() => {
  document.body.innerHTML = `
    <div id="map" style="width:600px;height:400px"></div>
    <select id="provinceSelect"></select>
    <select id="districtSelect"></select>
    <select id="wardSelect"></select>
    <input id="searchInput" />
    <input id="addressCombo" />
    <div id="addressResults" class="search-results"></div>
    <button id="showFcPredBtn">Show predicted</button>
  `;

  // Minimal Leaflet mock used by MapManager for unit tests
  const fakeLatLng = (lat, lng) => ({ lat, lng, distanceTo: (other) => Math.round(Math.sqrt((lat - other.lat) ** 2 + (lng - other.lng) ** 2) * 111000) });

  global.L = {
    map: jest.fn(() => ({ setView: jest.fn(), fitBounds: jest.fn(), invalidateSize: jest.fn(), getZoom: () => 12 })),
    tileLayer: jest.fn(() => ({ addTo: jest.fn(), on: jest.fn() })),
    layerGroup: jest.fn(() => ({ addTo: jest.fn(), clearLayers: jest.fn(), eachLayer: jest.fn(), addLayer: function(l){}})),
    geoJSON: jest.fn(() => ({ addTo: jest.fn(), addData: jest.fn(), clearLayers: jest.fn(), setStyle: jest.fn(), eachLayer: jest.fn() })),
    circleMarker: jest.fn(() => ({ addTo: jest.fn(), bindPopup: jest.fn(), setStyle: jest.fn(), getLatLng: () => ({ lat: 10.0, lng: 105.0 }), bringToFront: jest.fn() })),
    marker: jest.fn(() => ({ addTo: jest.fn(), bindPopup: jest.fn(), setZIndexOffset: jest.fn() })),
    polyline: jest.fn(() => ({ addTo: jest.fn(), bindTooltip: jest.fn() })),
    divIcon: jest.fn(() => ({})),
    latLng: (lat, lng) => fakeLatLng(lat, lng),
    LatLng: function(lat, lng){ return fakeLatLng(lat,lng); },
    latLngBounds: (arr) => ({ getNorthEast: () => arr[0], getSouthWest: () => arr[1] }),
  };
});

describe('Race condition regression', () => {
  test('preserves known exactness across address layer reloads so Show Predicted stays disabled', async () => {
    const App = require('../../../src/main/resources/static/js/app.js').default;
    const app = new App();

    // Simulate that the selected address is known to be exact (discovered by an earlier async call)
    app.selectedAddressId = 'ADDR-1';
    if (!app.map._addressExactById) app.map._addressExactById = {};
    app.map._addressExactById['ADDR-1'] = true;

    // Initial update should disable the button
    await app.updateShowFcPredEnabled();
    expect(document.getElementById('showFcPredBtn').disabled).toBe(true);

    // Now simulate a reload of addresses where the incoming feature does NOT include an is_exact flag
    const reloadGeo = { type: 'FeatureCollection', features: [{ type: 'Feature', properties: { id: 'ADDR-1', address: 'Reloaded 1', appl_id: 'T-1' }, geometry: { type: 'Point', coordinates: [105.0, 10.0] } }] };
    app.map.showAddressesGeojson(reloadGeo);

    // Sanity: our MapManager should have preserved the known exactness
    expect(app.map._addressExactById['ADDR-1']).toBe(true);

    // Re-run the update which previously would have re-enabled the button; it should remain disabled
    await app.updateShowFcPredEnabled();
    expect(document.getElementById('showFcPredBtn').disabled).toBe(true);
  });

  test('selecting an address via the combobox keeps Show Predicted disabled when address is exact', async () => {
    const App = require('../../../src/main/resources/static/js/app.js').default;
    const app = new App();

    // The map knows this address is exact
    if (!app.map._addressExactById) app.map._addressExactById = {};
    app.map._addressExactById['ADDR-1'] = true;

    // Simulate initial update (ver=1) that disables
    await app.updateShowFcPredEnabled();
    expect(document.getElementById('showFcPredBtn').disabled).toBe(true);

    // Now populate the address results in the UI (simulates a query result)
    app.ui.showAddressResults({ items: [{ id: 'ADDR-1', name: '123 Test St' }] });

    // Simulate user clicking the first result which will call handleAddressChange (bumping versions)
    const first = document.querySelector('#addressResults .search-item');
    expect(first).not.toBeNull();
    first.click();

    // Immediately call updateShowFcPredEnabled which captures the same ui version (simulating the race)
    await app.updateShowFcPredEnabled();

    // The button should remain disabled (the same-version enable should be ignored)
    expect(document.getElementById('showFcPredBtn').disabled).toBe(true);
  });

  test('reloading with empty address features preserves previously-known exactness', async () => {
    const App = require('../../../src/main/resources/static/js/app.js').default;
    const app = new App();

    // Setup known exactness for ADDR-1
    if (!app.map._addressExactById) app.map._addressExactById = {};
    app.map._addressExactById['ADDR-1'] = true;

    // Call showAddressesGeojson with an empty feature set (should not clear existing exactness)
    app.map.showAddressesGeojson({ type: 'FeatureCollection', features: [] });
    expect(app.map._addressExactById['ADDR-1']).toBe(true);

    // Also verify that updateShowFcPredEnabled still respects the preserved exactness
    app.selectedAddressId = 'ADDR-1';
    await app.updateShowFcPredEnabled();
    expect(document.getElementById('showFcPredBtn').disabled).toBe(true);
  });

  test('FC selection after address layer cleared (empty features) still respects preserved exactness', async () => {
    const App = require('../../../src/main/resources/static/js/app.js').default;
    const app = new App();

    // Initially known exact address
    if (!app.map._addressExactById) app.map._addressExactById = {};
    app.selectedAddressId = 'ADDR-1';
    app.map._addressExactById['ADDR-1'] = true;

    // Simulate a reload that clears address markers but should NOT clear exactness map
    app.map.showAddressesGeojson({ type: 'FeatureCollection', features: [] });

    // Mock checkins for the FC (so FC selection would normally enable)
    app.api.getCheckinsGeoJson = jest.fn(async () => ({ features: [ { id: 1 }, { id: 2 } ] }));

    await app.handleFcChange('FC-1');
    expect(document.getElementById('showFcPredBtn').disabled).toBe(true);
  });

  test('clearAll(false) preserves _addressExactById and prevents FC selection from enabling Show Predicted', async () => {
    const App = require('../../../src/main/resources/static/js/app.js').default;
    const app = new App();

    // Initially known exact address
    if (!app.map._addressExactById) app.map._addressExactById = {};
    app.selectedAddressId = 'ADDR-1';
    app.map._addressExactById['ADDR-1'] = true;

    // Simulate clearAll but keep provinces (should preserve exactness)
    app.map.clearAll(false);
    expect(app.map._addressExactById['ADDR-1']).toBe(true);

    // Mock checkins for the FC (so FC selection would normally enable)
    app.api.getCheckinsGeoJson = jest.fn(async () => ({ features: [ { id: 1 }, { id: 2 } ] }));

    await app.handleFcChange('FC-1');
    expect(document.getElementById('showFcPredBtn').disabled).toBe(true);
  });
