/** Tests of district-map.js: the pure mapping functions and one draw() with a fake Leaflet. */
const map = require('../../backend/src/main/resources/static/js/district-map.js');

describe('parseSelected', () => {
  test('splits the comma list and trims each name', () => {
    expect(map.parseSelected('Ratnapura, Kalutara ,Galle')).toEqual(['Ratnapura', 'Kalutara', 'Galle']);
  });

  test('gives an empty list for absent, empty or blank text', () => {
    expect(map.parseSelected(undefined)).toEqual([]);
    expect(map.parseSelected('')).toEqual([]);
    expect(map.parseSelected(' , ,')).toEqual([]);
  });
});

describe('parseDot', () => {
  test('reads latitude and longitude', () => {
    expect(map.parseDot('6.5888,79.964')).toEqual([6.5888, 79.964]);
    expect(map.parseDot(' 6.5 , 79.9 ')).toEqual([6.5, 79.9]);
  });

  test('is null when absent', () => {
    expect(map.parseDot(undefined)).toBeNull();
    expect(map.parseDot('')).toBeNull();
  });

  test('is null when malformed', () => {
    expect(map.parseDot('6.5')).toBeNull();
    expect(map.parseDot('6.5,79.9,1')).toBeNull();
    expect(map.parseDot('north,east')).toBeNull();
    expect(map.parseDot('6.5,')).toBeNull();
    expect(map.parseDot(',79.9')).toBeNull();
    expect(map.parseDot('Infinity,79.9')).toBeNull();
  });
});

describe('mapWidth', () => {
  test('keeps the proportions of the island at the given height', () => {
    expect(map.mapWidth(360)).toBe(Math.round((360 * (82.0 - 79.5) / (9.9 - 5.85)) * 0.993));
    expect(map.mapWidth(220)).toBe(135);
  });

  test('grows with the height', () => {
    expect(map.mapWidth(400)).toBeGreaterThan(map.mapWidth(200));
  });
});

describe('toLatLngRings', () => {
  test('swaps GeoJSON lon/lat into Leaflet lat/lon in every ring', () => {
    const rings = [[[79.8, 6.9], [80.0, 7.0]], [[81.0, 8.0]]];
    expect(map.toLatLngRings(rings)).toEqual([[[6.9, 79.8], [7.0, 80.0]], [[8.0, 81.0]]]);
  });

  test('does not change its input', () => {
    const rings = [[[79.8, 6.9]]];
    map.toLatLngRings(rings);
    expect(rings).toEqual([[[79.8, 6.9]]]);
  });
});

describe('districtClass', () => {
  test('marks a selected district', () => {
    expect(map.districtClass('Kalutara', ['Ratnapura', 'Kalutara'])).toBe('district is-selected');
  });

  test('leaves other districts plain', () => {
    expect(map.districtClass('Galle', ['Ratnapura'])).toBe('district');
    expect(map.districtClass('Galle', [])).toBe('district');
  });
});

describe('draw', () => {
  /** A fake Leaflet that records the calls draw() makes. */
  function fakeLeaflet() {
    const calls = { maps: [], bounds: [], polygons: [], tooltips: [], markers: [], added: 0 };
    const chain = (record) => {
      const layer = {
        bindTooltip(name) { calls.tooltips.push(name); return layer; },
        addTo(target) { calls.added += 1; layer.target = target; return layer; },
      };
      record(layer);
      return layer;
    };
    const L = {
      map(host, options) {
        const m = { fitBounds(b, o) { calls.bounds.push([b, o]); } };
        calls.maps.push({ host, options });
        return m;
      },
      polygon(rings, options) { return chain(() => calls.polygons.push({ rings, options })); },
      circleMarker(latLng, options) { return chain(() => calls.markers.push({ latLng, options })); },
    };
    return { L, calls };
  }

  const districts = {
    Kalutara: [[[79.9, 6.5], [80.0, 6.6]]],
    Colombo: [[[79.8, 6.9], [79.9, 7.0]]],
  };

  function container(attributes) {
    document.body.innerHTML = '<div class="district-map" id="m"></div>';
    const el = document.getElementById('m');
    Object.keys(attributes).forEach((key) => { el.dataset[key] = attributes[key]; });
    return el;
  }

  test('creates a sized Leaflet map without controls and fits it to the island', () => {
    const { L, calls } = fakeLeaflet();
    const el = container({ height: '220', selected: 'Kalutara' });

    map.draw(el, L, districts);

    expect(calls.maps).toHaveLength(1);
    const host = calls.maps[0].host;
    expect(host.style.height).toBe('220px');
    expect(host.style.width).toBe(map.mapWidth(220) + 'px');
    expect(el.firstChild).toBe(host);
    expect(calls.maps[0].options).toMatchObject({ zoomControl: false, attributionControl: false, dragging: false });
    expect(calls.bounds).toEqual([[[[5.85, 79.5], [9.9, 82.0]], { padding: [0, 0] }]]);
  });

  test('draws one polygon per district in name order, swapped to lat/lon, selected ones marked', () => {
    const { L, calls } = fakeLeaflet();

    map.draw(container({ selected: 'Kalutara' }), L, districts);

    expect(calls.tooltips).toEqual(['Colombo', 'Kalutara']);
    expect(calls.polygons[0].rings).toEqual([[[6.9, 79.8], [7.0, 79.9]]]);
    expect(calls.polygons[0].options.className).toBe('district');
    expect(calls.polygons[1].options.className).toBe('district is-selected');
    expect(calls.markers).toHaveLength(0);
    expect(calls.added).toBe(2);
  });

  test('uses the default height of 360 when none is given', () => {
    const { L, calls } = fakeLeaflet();

    map.draw(container({}), L, districts);

    expect(calls.maps[0].host.style.height).toBe('360px');
  });

  test('adds the report dot when data-dot is well formed, and none when it is malformed', () => {
    const withDot = fakeLeaflet();
    map.draw(container({ dot: '6.5888,79.964' }), withDot.L, districts);
    expect(withDot.calls.markers).toHaveLength(1);
    expect(withDot.calls.markers[0].latLng).toEqual([6.5888, 79.964]);
    expect(withDot.calls.markers[0].options.className).toBe('report-dot');

    const badDot = fakeLeaflet();
    map.draw(container({ dot: 'somewhere' }), badDot.L, districts);
    expect(badDot.calls.markers).toHaveLength(0);
  });

  test('says so in the container when Leaflet or the district data is missing', () => {
    const el = container({});

    map.draw(el, undefined, districts);
    expect(el.textContent).toContain('District map unavailable');

    const el2 = container({});
    map.draw(el2, fakeLeaflet().L, undefined);
    expect(el2.textContent).toContain('District map unavailable');
  });
});
