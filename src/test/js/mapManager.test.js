// We'll stub the Leaflet global `L` with minimal behavior used by MapManager

beforeEach(() => {
  global.L = {
    map: jest.fn(() => ({ setView: jest.fn(), fitBounds: jest.fn() })),
    tileLayer: jest.fn(() => ({ addTo: jest.fn() })),
    geoJSON: jest.fn(() => ({ addData: jest.fn(), clearLayers: jest.fn(), setStyle: jest.fn(), eachLayer: jest.fn() })),
    layerGroup: jest.fn(() => ({ addTo: jest.fn(), clearLayers: jest.fn() })),
    marker: jest.fn(() => ({ addTo: jest.fn() })),
    divIcon: jest.fn(() => ({}))
  };
});

describe('MapManager', () => {
  test('constructs map and layers', () => {
    const MapManager = require('../../../src/main/resources/static/js/mapManager.js').default;
    const m = new MapManager('map');
    expect(global.L.map).toHaveBeenCalledWith('map');
    expect(global.L.tileLayer).toHaveBeenCalled();
    expect(global.L.geoJSON).toHaveBeenCalled();
  });

  test('highlightDistrict sets style on matching layer and fits bounds', () => {
    // Prepare layers
    const layer1 = { feature: { properties: { id: '1' } }, setStyle: jest.fn(), getBounds: jest.fn(() => 'B1') };
    const layer2 = { feature: { properties: { id: '2' } }, setStyle: jest.fn() };

    // Make geoJSON return an object whose eachLayer calls back with our layers
    global.L.geoJSON = jest.fn(() => ({
      addData: jest.fn(),
      clearLayers: jest.fn(),
      setStyle: jest.fn(),
      eachLayer: (cb) => { cb(layer1); cb(layer2); }
    }));

    const MapManager = require('../../../src/main/resources/static/js/mapManager.js').default;
    const m = new MapManager('map');

    m.highlightDistrict('1');

    expect(layer1.setStyle).toHaveBeenCalledWith(expect.objectContaining({ fillColor: expect.any(String) }));
    expect(global.L.map().fitBounds).toHaveBeenCalledWith('B1');
    // non-matching layer had resetStyle invoked via districtLayer.resetStyle if available; we can't assert here, but no errors.
  });

  test('zoom change reduces fillOpacity at threshold and restores below', () => {
    const layer = { feature: { properties: { id: '1' } }, setStyle: jest.fn(), getBounds: jest.fn(() => 'B1') };
    global.L.geoJSON = jest.fn(() => ({
      addData: jest.fn(),
      clearLayers: jest.fn(),
      setStyle: jest.fn(),
      eachLayer: (cb) => { cb(layer); }
    }));

    // create map with getZoom and on
    let zoomHandler;
    global.L.map = jest.fn(() => ({ setView: jest.fn(), fitBounds: jest.fn(), getZoom: () => 14, on: (ev, h) => { if (ev === 'zoomend') zoomHandler = h; } }));

    const MapManager = require('../../../src/main/resources/static/js/mapManager.js').default;
    const m = new MapManager('map');

    // Simulate zoomend at zoom 14 (>= threshold)
    if (zoomHandler) zoomHandler();

    expect(m.provinceLayer.setStyle).toHaveBeenCalledWith(expect.objectContaining({ fillOpacity: 0 }));

    // simulate lower zoom
    m.map.getZoom = () => 10;
    if (zoomHandler) zoomHandler();
    expect(m.provinceLayer.setStyle).toHaveBeenCalledWith(expect.objectContaining({ fillOpacity: expect.any(Number) }));
  });

  test('highlightDistrict does not call fitBounds when fit=false', () => {
    const layer1 = { feature: { properties: { id: '1' } }, setStyle: jest.fn(), getBounds: jest.fn(() => 'B1') };
    global.L.geoJSON = jest.fn(() => ({
      addData: jest.fn(),
      clearLayers: jest.fn(),
      setStyle: jest.fn(),
      eachLayer: (cb) => { cb(layer1); }
    }));
    const MapManager = require('../../../src/main/resources/static/js/mapManager.js').default;
    const m = new MapManager('map');

    // Spy on fitBounds
    const fitSpy = m.map.fitBounds = jest.fn();

    m.highlightDistrict('1', { fit: false });
    expect(fitSpy).not.toHaveBeenCalled();
  });

  test('highlightWard does not call fitBounds when fit=false', () => {
    const layer1 = { feature: { properties: { id: 'w1' } }, setStyle: jest.fn(), getBounds: jest.fn(() => 'WB') };
    global.L.geoJSON = jest.fn(() => ({
      addData: jest.fn(),
      clearLayers: jest.fn(),
      setStyle: jest.fn(),
      eachLayer: (cb) => { cb(layer1); }
    }));
    const MapManager = require('../../../src/main/resources/static/js/mapManager.js').default;
    const m = new MapManager('map');

    const fitSpy = m.map.fitBounds = jest.fn();
    m.highlightWard('w1', { fit: false });
    expect(fitSpy).not.toHaveBeenCalled();
  });

  test('findAddressMarkerByLatLng finds marker within tolerance', () => {
    // provide a simple latLng implementation that calculates distance in meters from degree diffs
    global.L.latLng = jest.fn((a,b) => ({ lat: a, lng: b, distanceTo: function(other){ const d = Math.sqrt((this.lat - other.lat)**2 + (this.lng - other.lng)**2) * 111000; return d; } }));
    const marker = { getLatLng: jest.fn(() => global.L.latLng(10.0000, 20.0000)), openPopup: jest.fn() };
    const MapManager = require('../../../src/main/resources/static/js/mapManager.js').default;
    const m = new MapManager('map');
    m._addressMarkersById = { '1': marker };

    const found = m.findAddressMarkerByLatLng(10.0001, 20.0001, 50);
    expect(found).toBe(marker);
  });

  test('openPopupAtLatLng opens popup and recenters', () => {
    global.L.latLng = jest.fn((a,b) => ({ lat: a, lng: b, distanceTo: function(other){ const d = Math.sqrt((this.lat - other.lat)**2 + (this.lng - other.lng)**2) * 111000; return d; } }));
    const marker = { getLatLng: jest.fn(() => global.L.latLng(10.5000, 105.1000)), openPopup: jest.fn() };
    const MapManager = require('../../../src/main/resources/static/js/mapManager.js').default;
    const m = new MapManager('map');
    m._addressMarkersById = { 'foo': marker };
    m.map.setView = jest.fn();
    m.map.getZoom = jest.fn(() => 12);

    const res = m.openPopupAtLatLng(10.5000, 105.1000, 50);
    expect(res).toBe(true);
    expect(marker.openPopup).toHaveBeenCalled();
    expect(m.map.setView).toHaveBeenCalledWith(marker.getLatLng(), Math.max(12,15));
  });
});
