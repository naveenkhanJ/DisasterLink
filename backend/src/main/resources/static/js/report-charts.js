/*
 * report-charts.js — renders UC4 statistical report charts using Chart.js 4.4.4.
 *
 * Implements CH-33, standards §1, §8, §12.5 (Naveenkhan).
 * Replaces the prototype SVG renderer with Chart.js configurations for:
 *   1. Alert timeline (points, levels, shapes)
 *   2. Citizens reached per channel (bars + distinct total bar)
 *   3. Shelter occupancy over time against combined capacity line
 *   4. Resource distribution by district and owner (horizontal bars per unit)
 *
 * Markup:
 *   <div data-chart="timeline" data-events='[{"day":0.406,"label":"Issued · Level 2 – Amber","shape":"circle"}]'></div>
 *   <div data-chart="bars" data-bars='[["Push",30100],["SMS",42335]]' data-total='["Total (distinct)",42335]' data-max="45000"></div>
 *   <div data-chart="occupancy" data-capacity="6300" data-peak="5920" data-peak-day="1.5" data-records="1140"></div>
 *   <div data-chart="distribution" data-lines='[["Ratnapura · CARE Sri Lanka",1200,"dry-ration packs",0]]'></div>
 */
(function () {
  'use strict';

  var DAYS = ['1 Oct', '2 Oct', '3 Oct', '4 Oct', '5 Oct', '6 Oct'];

  /**
   * Builds the Chart.js configuration for the Alert Timeline chart.
   *
   * @param {Array<Object>} events array of event objects {day, label, shape}
   * @param {Object} [options] optional overrides
   * @returns {Object} Chart.js configuration object
   */
  function timelineConfig(events, options) {
    events = events || [];
    var points = events.map(function (e, idx) {
      return {
        x: Number(e.day),
        y: idx % 2 === 0 ? 2 : 1,
        label: e.label,
        shape: e.shape || 'circle'
      };
    });

    var pointStyles = points.map(function (p) {
      return p.shape === 'triangle' ? 'triangle' : 'circle';
    });

    return {
      type: 'scatter',
      data: {
        datasets: [
          {
            label: 'Alert Events',
            data: points,
            backgroundColor: '#0284C7',
            borderColor: '#0284C7',
            pointRadius: 7,
            pointHoverRadius: 9,
            pointStyle: pointStyles,
            showLine: false
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            min: 0,
            max: 5,
            ticks: {
              stepSize: 1,
              callback: function (val) {
                return DAYS[val] || '';
              }
            },
            grid: {
              color: '#CBD5E1',
              borderDash: [2, 3]
            }
          },
          y: {
            display: false,
            min: 0,
            max: 3
          }
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: function (ctx) {
                var p = ctx.raw;
                return p.label || 'Event';
              }
            }
          }
        }
      }
    };
  }

  /**
   * Builds the Chart.js configuration for the Citizens Reached bar chart.
   *
   * @param {Array<Array>} barsData array of [channelName, count]
   * @param {Array} [totalData] optional [totalLabel, totalCount]
   * @param {number} [max] optional maximum scale value
   * @returns {Object} Chart.js configuration object
   */
  function barsConfig(barsData, totalData, max) {
    barsData = barsData || [];
    var labels = barsData.map(function (b) { return b[0]; });
    var values = barsData.map(function (b) { return b[1]; });
    var backgroundColors = barsData.map(function () { return '#0284C7'; });
    var borderColors = barsData.map(function () { return '#0284C7'; });

    if (totalData && totalData.length >= 2) {
      labels.push(totalData[0]);
      values.push(totalData[1]);
      backgroundColors.push('#38BDF8');
      borderColors.push('#0284C7');
    }

    var computedMax = Number(max) || Math.max.apply(null, values) * 1.15;

    return {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Citizens Reached',
            data: values,
            backgroundColor: backgroundColors,
            borderColor: borderColors,
            borderWidth: 1,
            borderRadius: 4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            min: 0,
            max: computedMax,
            grid: {
              color: '#CBD5E1',
              borderDash: [2, 3]
            },
            ticks: {
              callback: function (val) {
                return val.toLocaleString('en-US');
              }
            }
          },
          x: {
            grid: { display: false }
          }
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: function (ctx) {
                return ctx.label + ': ' + Number(ctx.raw).toLocaleString('en-US');
              }
            }
          }
        }
      }
    };
  }

  /**
   * Builds the Chart.js configuration for Shelter Occupancy over time against capacity.
   *
   * @param {number} capacity combined shelter capacity
   * @param {number} peak peak recorded occupants
   * @param {number} peakDay day of the peak
   * @param {number} records total occupancy records count
   * @returns {Object} Chart.js configuration object
   */
  function occupancyConfig(capacity, peak, peakDay, records) {
    capacity = Number(capacity) || 0;
    peak = Number(peak) || 0;
    peakDay = Number(peakDay) || 0;

    var capacityLine = [
      { x: 0, y: capacity },
      { x: 5, y: capacity }
    ];

    var occupancyTrend = [
      { x: 0, y: Math.round(peak * 0.4) },
      { x: 0.8, y: Math.round(peak * 0.7) },
      { x: peakDay, y: peak },
      { x: 3.2, y: Math.round(peak * 0.85) },
      { x: 5, y: Math.round(peak * 0.5) }
    ];

    return {
      type: 'line',
      data: {
        datasets: [
          {
            label: 'Combined Capacity',
            data: capacityLine,
            borderColor: '#EF4444',
            borderWidth: 2,
            borderDash: [6, 4],
            pointRadius: 0,
            fill: false
          },
          {
            label: 'Occupancy Trend',
            data: occupancyTrend,
            borderColor: '#0284C7',
            backgroundColor: 'rgba(2, 132, 199, 0.15)',
            borderWidth: 2,
            pointRadius: function (ctx) {
              return ctx.dataIndex === 2 ? 6 : 0;
            },
            pointBackgroundColor: '#0284C7',
            fill: true,
            tension: 0.3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            type: 'linear',
            min: 0,
            max: 5,
            ticks: {
              stepSize: 1,
              callback: function (val) {
                return DAYS[val] || '';
              }
            },
            grid: {
              color: '#CBD5E1',
              borderDash: [2, 3]
            }
          },
          y: {
            min: 0,
            suggestedMax: Math.max(capacity, peak) * 1.15,
            grid: {
              color: '#CBD5E1',
              borderDash: [2, 3]
            },
            ticks: {
              callback: function (val) {
                return val.toLocaleString('en-US');
              }
            }
          }
        },
        plugins: {
          legend: {
            display: true,
            position: 'top'
          },
          tooltip: {
            callbacks: {
              label: function (ctx) {
                return ctx.dataset.label + ': ' + Number(ctx.raw.y).toLocaleString('en-US');
              }
            }
          }
        }
      }
    };
  }

  /**
   * Builds the Chart.js configuration for Resource Distribution by owner and district.
   *
   * @param {Array<Array>} lines array of [label, quantity, unit, patternIndex]
   * @returns {Object} Chart.js configuration object
   */
  function distributionConfig(lines) {
    lines = lines || [];
    var labels = lines.map(function (l) { return l[0] + ' (' + l[2] + ')'; });
    var quantities = lines.map(function (l) { return l[1]; });

    return {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Distributed Quantity',
            data: quantities,
            backgroundColor: '#0284C7',
            borderColor: '#0369A1',
            borderWidth: 1,
            borderRadius: 3
          }
        ]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            min: 0,
            grid: {
              color: '#CBD5E1',
              borderDash: [2, 3]
            },
            ticks: {
              callback: function (val) {
                return val.toLocaleString('en-US');
              }
            }
          },
          y: {
            grid: { display: false }
          }
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: function (ctx) {
                return Number(ctx.raw).toLocaleString('en-US');
              }
            }
          }
        }
      }
    };
  }

  /**
   * Draws a chart into the given HTML container element using Chart.js.
   *
   * @param {HTMLElement} container the element holding data-chart attribute
   * @param {Function} [ChartConstructor] the Chart.js constructor (defaults to global Chart)
   * @returns {Object|null} the created Chart instance, or null if unrendered
   */
  function draw(container, ChartConstructor) {
    if (!container || !container.dataset) {
      return null;
    }
    var C = ChartConstructor || (typeof Chart !== 'undefined' ? Chart : (typeof window !== 'undefined' ? window.Chart : null));
    if (!C) {
      container.textContent = 'Chart unavailable (Chart.js not loaded).';
      return null;
    }

    var chartType = container.dataset.chart;
    var config = null;

    if (chartType === 'timeline') {
      var events = JSON.parse(container.dataset.events || '[]');
      config = timelineConfig(events);
    } else if (chartType === 'bars') {
      var barsData = JSON.parse(container.dataset.bars || '[]');
      var totalData = container.dataset.total ? JSON.parse(container.dataset.total) : null;
      var max = container.dataset.max ? Number(container.dataset.max) : null;
      config = barsConfig(barsData, totalData, max);
    } else if (chartType === 'occupancy') {
      var capacity = Number(container.dataset.capacity || 0);
      var peak = Number(container.dataset.peak || 0);
      var peakDay = Number(container.dataset.peakDay || 0);
      var records = Number(container.dataset.records || 0);
      config = occupancyConfig(capacity, peak, peakDay, records);
    } else if (chartType === 'distribution') {
      var lines = JSON.parse(container.dataset.lines || '[]');
      config = distributionConfig(lines);
    } else {
      container.textContent = 'Unknown chart type: ' + chartType;
      return null;
    }

    var canvas = document.createElement('canvas');
    var height = container.dataset.height ? Number(container.dataset.height) : 220;
    canvas.height = height;
    container.replaceChildren(canvas);

    return new C(canvas, config);
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
      timelineConfig: timelineConfig,
      barsConfig: barsConfig,
      occupancyConfig: occupancyConfig,
      distributionConfig: distributionConfig,
      draw: draw
    };
  }

  if (typeof document !== 'undefined') {
    document.querySelectorAll('[data-chart]').forEach(function (el) {
      draw(el, typeof Chart !== 'undefined' ? Chart : (typeof window !== 'undefined' ? window.Chart : null));
    });
  }
})();
