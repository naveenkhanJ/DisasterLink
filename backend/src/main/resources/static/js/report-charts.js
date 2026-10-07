// Prototype chart code; UC4 (Naveenkhan) replaces it with Chart.js, standards §1.
/*
 * report-charts.js — draws the UC4 report charts into elements with a data-chart attribute.
 *
 * Prototype renderer (P89): implementation-standards §10 names Chart.js for the UC4 charts; Chart.js could
 * not be downloaded into the prototype build, so this file draws the same charts as small SVGs from the same
 * data. Replace it with Chart.js when the screens are wired; the data attributes carry the values.
 *
 * Rules kept from frames-WF4.md: only scenario values are drawn; the occupancy chart shows the capacity line,
 * one labelled peak point and a labelled placeholder band (no invented vertices or y-values); events and
 * owners are told apart by marker shape or pattern plus text, never by colour alone.
 *
 * Markup examples:
 *   <div data-chart="timeline" data-events='[{"day":0.448,"label":"Issued · Level 2 – Amber","shape":"circle"}]'></div>
 *   <div data-chart="bars" data-bars='[["Push",30100],["SMS",42335]]' data-total='["Total (distinct)",42335]' data-max="45000"></div>
 *   <div data-chart="occupancy" data-capacity="6300" data-peak="5920" data-peak-day="1.5" data-records="1140"></div>
 *   <div data-chart="distribution" data-lines='[["Ratnapura · CARE Sri Lanka",1200,"dry-ration packs",0]]'></div>
 */
(function () {
  'use strict';

  var NS = 'http://www.w3.org/2000/svg';
  var DAYS = ['1 Oct', '2 Oct', '3 Oct', '4 Oct', '5 Oct', '6 Oct'];

  /** Builds an SVG element string; values are numbers or pre-escaped text only. */
  function esc(text) {
    return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function dayAxis(x0, x1, y) {
    var out = '<line x1="' + x0 + '" y1="' + y + '" x2="' + x1 + '" y2="' + y + '" stroke="#CBD5E1"/>';
    DAYS.forEach(function (label, i) {
      var x = x0 + (x1 - x0) * i / 5;
      out += '<line x1="' + x + '" y1="' + y + '" x2="' + x + '" y2="' + (y + 4) + '" stroke="#CBD5E1"/>';
      out += '<text x="' + x + '" y="' + (y + 16) + '" text-anchor="middle">' + label + '</text>';
    });
    return out;
  }

  function timeline(el, w) {
    var h = 110, base = h - 24, x0 = 24, x1 = w - 24;
    var events = JSON.parse(el.dataset.events || '[]');
    var out = dayAxis(x0, x1, base);
    events.forEach(function (e, i) {
      var x = x0 + (x1 - x0) * e.day / 5;
      var top = base - (i === 0 ? 50 : 26);
      out += '<line x1="' + x + '" y1="' + base + '" x2="' + x + '" y2="' + top + '" stroke="#CBD5E1"/>';
      out += e.shape === 'triangle'
        ? '<path d="M' + (x - 7) + ' ' + (top + 6) + ' L' + x + ' ' + (top - 7) + ' L' + (x + 7) + ' ' + (top + 6) + ' Z" fill="var(--chart-line)"/>'
        : '<circle cx="' + x + '" cy="' + top + '" r="6" fill="var(--chart-line)"/>';
      out += '<text class="strong" x="' + (i === 0 ? x - 2 : x + 12) + '" y="' + (i === 0 ? top - 14 : top + 4) + '">' + esc(e.label) + '</text>';
    });
    return [h, out];
  }

  function bars(el, w) {
    var h = Number(el.dataset.height || 180), left = 46, bottom = h - 22, top = 18, plot = bottom - top;
    var max = Number(el.dataset.max), gap = 12;
    var data = JSON.parse(el.dataset.bars || '[]');
    var total = el.dataset.total ? JSON.parse(el.dataset.total) : null;
    var n = data.length + (total ? 1 : 0);
    var bw = (w - left - 20 - gap * n - 14) / n;
    var out = '<defs><pattern id="hatch-total" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">'
      + '<rect width="6" height="6" fill="#fff"/><line x1="0" y1="0" x2="0" y2="6" stroke="var(--chart-bar)" stroke-width="3"/></pattern></defs>';
    [0, 1 / 3, 2 / 3, 1].forEach(function (f) {
      var y = bottom - plot * f, v = Math.round(max * f);
      out += '<line x1="' + left + '" y1="' + y + '" x2="' + (w - 4) + '" y2="' + y + '" stroke="#CBD5E1"' + (f ? ' stroke-dasharray="2 3"' : '') + '/>';
      out += '<text x="' + (left - 6) + '" y="' + (y + 4) + '" text-anchor="end">' + v.toLocaleString('en-US') + '</text>';
    });
    var x = left + gap;
    data.concat(total ? [total] : []).forEach(function (b, i) {
      var isTotal = total && i === data.length;
      if (isTotal) { x += 14; }
      var bh = plot * b[1] / max;
      out += '<rect x="' + x + '" y="' + (bottom - bh) + '" width="' + bw + '" height="' + bh + '" '
        + (isTotal ? 'fill="url(#hatch-total)" stroke="var(--chart-bar)" stroke-width="1.5"' : 'fill="var(--chart-bar)"') + '/>';
      out += '<text class="strong" x="' + (x + bw / 2) + '" y="' + (bottom - bh - 4) + '" text-anchor="middle">' + b[1].toLocaleString('en-US') + '</text>';
      out += '<text x="' + (x + bw / 2) + '" y="' + (bottom + 15) + '" text-anchor="middle" style="font-size:11px">' + esc(b[0]) + '</text>';
      x += bw + gap;
    });
    return [h, out];
  }

  function occupancy(el, w) {
    var h = Number(el.dataset.height || 220), top = 22, bottom = h - 22, x0 = 24, x1 = w - 24;
    var cap = Number(el.dataset.capacity), peak = Number(el.dataset.peak), day = Number(el.dataset.peakDay);
    var yCap = top + 18, yPeak = bottom - (bottom - yCap) * peak / cap, xPeak = x0 + (x1 - x0) * day / 5;
    var out = '<rect x="' + x0 + '" y="' + (yCap + 6) + '" width="' + (x1 - x0) + '" height="' + (bottom - yCap - 6) + '" fill="var(--map-fill)"/>';
    out += '<text x="' + ((x0 + x1) / 2) + '" y="' + ((yCap + bottom) / 2 + 22) + '" text-anchor="middle" style="font-size:11px">occupancy from the stored records ('
      + Number(el.dataset.records).toLocaleString('en-US') + ' records)</text>';
    out += '<line x1="' + x0 + '" y1="' + yCap + '" x2="' + x1 + '" y2="' + yCap + '" stroke="var(--chart-cap)" stroke-width="1.5" stroke-dasharray="6 4"/>';
    out += '<text class="strong" x="' + x1 + '" y="' + (yCap - 6) + '" text-anchor="end" style="font-weight:400">Combined capacity ' + cap.toLocaleString('en-US') + '</text>';
    out += '<circle cx="' + xPeak + '" cy="' + yPeak + '" r="6" fill="var(--chart-line)"/>';
    out += '<text class="strong" x="' + (xPeak + 10) + '" y="' + (yPeak + 16) + '">Peak ' + peak.toLocaleString('en-US') + ' · 2 Oct · '
      + Math.round(peak / cap * 100) + ' %</text>';
    out += dayAxis(x0, x1, bottom);
    return [h, out];
  }

  function distribution(el, w) {
    // lines: [label, quantity, unit, patternIndex]; one mini-chart per unit, each with its own scale.
    var lines = JSON.parse(el.dataset.lines || '[]'), row = Number(el.dataset.row || 15);
    var fills = ['var(--chart-bar)', 'url(#p-hatch)', 'url(#p-dots)', 'url(#p-cross)'];
    var out = '<defs>'
      + '<pattern id="p-hatch" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><rect width="6" height="6" fill="#fff"/><line x1="0" y1="0" x2="0" y2="6" stroke="var(--chart-bar)" stroke-width="3"/></pattern>'
      + '<pattern id="p-dots" width="5" height="5" patternUnits="userSpaceOnUse"><rect width="5" height="5" fill="#fff"/><circle cx="2.5" cy="2.5" r="1.4" fill="var(--chart-bar)"/></pattern>'
      + '<pattern id="p-cross" width="6" height="6" patternUnits="userSpaceOnUse"><rect width="6" height="6" fill="#fff"/><path d="M0 0L6 6M6 0L0 6" stroke="var(--chart-bar)"/></pattern></defs>';
    var units = [];
    lines.forEach(function (l) { if (units.indexOf(l[2]) < 0) { units.push(l[2]); } });
    var labelW = 0.55 * w, y = 0;
    units.forEach(function (unit) {
      var rows = lines.filter(function (l) { return l[2] === unit; });
      var max = Math.max.apply(null, rows.map(function (l) { return l[1]; }));
      out += '<text class="strong" x="0" y="' + (y + 11) + '" style="font-size:11px">' + esc(unit) + '</text>';
      y += 14;
      rows.forEach(function (l) {
        var bw = (w - labelW - 52) * l[1] / max;
        out += '<text x="0" y="' + (y + 10) + '" style="font-size:11px">' + esc(l[0]) + '</text>';
        out += '<rect x="' + labelW + '" y="' + (y + 1) + '" width="' + bw + '" height="' + (row - 5) + '" fill="' + fills[l[3] || 0] + '" stroke="var(--chart-bar)"/>';
        out += '<text class="strong" x="' + (labelW + bw + 4) + '" y="' + (y + 10) + '" style="font-size:11px;font-weight:400">' + l[1].toLocaleString('en-US') + '</text>';
        y += row;
      });
      y += 4;
    });
    return [y, out];
  }

  var RENDERERS = { timeline: timeline, bars: bars, occupancy: occupancy, distribution: distribution };

  document.querySelectorAll('[data-chart]').forEach(function (el) {
    var render = RENDERERS[el.dataset.chart];
    if (!render) {
      el.textContent = 'Unknown chart type: ' + el.dataset.chart;
      return;
    }
    var w = Math.floor(el.clientWidth || 300);
    var result = render(el, w);
    el.innerHTML = '<svg class="chart" xmlns="' + NS + '" width="' + w + '" height="' + result[0] + '">' + result[1] + '</svg>';
  });
})();
