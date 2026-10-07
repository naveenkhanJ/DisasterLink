/*
 * district-map.js — draws the district map of Sri Lanka into every element with class "district-map".
 *
 * Skeleton-owned (implementation-standards §1, P119): Leaflet with the 25 district outlines of districts.js as
 * a polygon layer. No tiles (there is no tile server), no zoom or pan controls, no attribution box: a static
 * map, styled by app.css through the classes "district", "is-selected" and "report-dot".
 *
 * Markup:  <div class="district-map" data-selected="Ratnapura,Kalutara" data-height="220"
 *               data-dot="6.5888,79.964"></div>
 * Needs:   leaflet.js (global L) and districts.js (window.LKA_DISTRICTS) loaded first.
 *
 * The data-to-picture mapping is in plain functions (parseSelected, parseDot, mapWidth, toLatLngRings,
 * districtClass) that Jest tests without a browser; draw() only hands their results to Leaflet.
 */
(function () {
  'use strict';

  /** The island as [south, west], [north, east] in latitude/longitude. */
  var BOUNDS = [[5.85, 79.5], [9.9, 82.0]];
  /** Width divided by height of the map; cos(7.9°) ≈ 0.993 keeps the island's proportions at this latitude. */
  var ASPECT = (82.0 - 79.5) / (9.9 - 5.85) * 0.993;
  var DEFAULT_HEIGHT = 360;
  var MAP_OPTIONS = {
    zoomControl: false, attributionControl: false, dragging: false, scrollWheelZoom: false,
    doubleClickZoom: false, boxZoom: false, keyboard: false, touchZoom: false, zoomSnap: 0
  };

  /** Reads the data-selected text "Ratnapura, Kalutara" into ['Ratnapura', 'Kalutara']. */
  function parseSelected(text) {
    return (text || '').split(',').map(function (s) { return s.trim(); }).filter(Boolean);
  }

  /** Reads the data-dot text "lat,lon" into [lat, lon]; null when it is absent or malformed. */
  function parseDot(text) {
    if (!text) { return null; }
    var parts = String(text).split(',');
    if (parts.length !== 2 || parts[0].trim() === '' || parts[1].trim() === '') { return null; }
    var lat = Number(parts[0]);
    var lon = Number(parts[1]);
    return isFinite(lat) && isFinite(lon) ? [lat, lon] : null;
  }

  /** The width in pixels that fits the island's proportions at a given height. */
  function mapWidth(height) {
    return Math.round(height * ASPECT);
  }

  /** Swaps the GeoJSON [lon, lat] points of every ring into the [lat, lon] order Leaflet wants. */
  function toLatLngRings(rings) {
    return rings.map(function (ring) {
      return ring.map(function (p) { return [p[1], p[0]]; });
    });
  }

  /** The CSS classes of a district's polygon: selected districts are filled by app.css. */
  function districtClass(name, selected) {
    return selected.indexOf(name) >= 0 ? 'district is-selected' : 'district';
  }

  /** Draws one map into a container with the given Leaflet namespace and district outlines. */
  function draw(container, L, districts) {
    if (!L || !districts) {
      // Fail visibly rather than silently: a library or the data file is missing or loaded after this script.
      container.textContent = 'District map unavailable (leaflet.js or districts.js not loaded).';
      return;
    }
    var height = Number(container.dataset.height || DEFAULT_HEIGHT);
    var selected = parseSelected(container.dataset.selected);
    var host = document.createElement('div'); // Leaflet needs a sized element of its own
    host.style.width = mapWidth(height) + 'px';
    host.style.height = height + 'px';
    container.replaceChildren(host);
    var map = L.map(host, MAP_OPTIONS);
    map.fitBounds(BOUNDS, { padding: [0, 0] });
    Object.keys(districts).sort().forEach(function (name) {
      L.polygon(toLatLngRings(districts[name]), {
        className: districtClass(name, selected), weight: 0.8, opacity: 1, fillOpacity: 1
      }).bindTooltip(name).addTo(map);
    });
    var dot = parseDot(container.dataset.dot);
    if (dot) {
      L.circleMarker(dot, { radius: 6, weight: 2, opacity: 1, fillOpacity: 1, className: 'report-dot' }).addTo(map);
    }
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
      parseSelected: parseSelected, parseDot: parseDot, mapWidth: mapWidth,
      toLatLngRings: toLatLngRings, districtClass: districtClass, draw: draw
    };
  }

  document.querySelectorAll('.district-map').forEach(function (container) {
    draw(container, window.L, window.LKA_DISTRICTS);
  });
})();
