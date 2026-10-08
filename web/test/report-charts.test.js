/** Tests of report-charts.js: the Chart.js configuration builders and draw() integration. */
const charts = require('../../backend/src/main/resources/static/js/report-charts.js');

describe('timelineConfig', () => {
  test('creates scatter configuration with point styling matching event shapes', () => {
    const events = [
      { day: 0.40625, label: 'Issued · Level 2 – Amber', shape: 'circle' },
      { day: 1.86458, label: 'Escalated · Level 3 – Red', shape: 'triangle' }
    ];

    const config = charts.timelineConfig(events);

    expect(config.type).toBe('scatter');
    expect(config.data.datasets).toHaveLength(1);
    const dataset = config.data.datasets[0];
    expect(dataset.data).toHaveLength(2);
    expect(dataset.data[0].x).toBe(0.40625);
    expect(dataset.data[0].label).toBe('Issued · Level 2 – Amber');
    expect(dataset.pointStyle).toEqual(['circle', 'triangle']);
    expect(config.options.scales.x.min).toBe(0);
    expect(config.options.scales.x.max).toBe(5);
  });

  test('handles empty events gracefully', () => {
    const config = charts.timelineConfig([]);
    expect(config.type).toBe('scatter');
    expect(config.data.datasets[0].data).toHaveLength(0);
  });
});

describe('barsConfig', () => {
  test('creates bar chart configuration with channels and total bar', () => {
    const barsData = [
      ['Push', 30100],
      ['SMS', 42335],
      ['Audible alert', 30100]
    ];
    const totalData = ['Total (distinct)', 42335];

    const config = charts.barsConfig(barsData, totalData, 45000);

    expect(config.type).toBe('bar');
    expect(config.data.labels).toEqual(['Push', 'SMS', 'Audible alert', 'Total (distinct)']);
    expect(config.data.datasets[0].data).toEqual([30100, 42335, 30100, 42335]);
    expect(config.options.scales.y.max).toBe(45000);
  });

  test('computes default max when max is not provided', () => {
    const barsData = [['Push', 1000]];
    const config = charts.barsConfig(barsData);
    expect(config.options.scales.y.max).toBeGreaterThan(1000);
  });
});

describe('occupancyConfig', () => {
  test('creates line chart configuration with capacity line and peak point', () => {
    const config = charts.occupancyConfig(6300, 5920, 1.5, 1140);

    expect(config.type).toBe('line');
    expect(config.data.datasets).toHaveLength(2);

    const capDataset = config.data.datasets[0];
    expect(capDataset.label).toBe('Combined Capacity');
    expect(capDataset.data).toEqual([{ x: 0, y: 6300 }, { x: 5, y: 6300 }]);
    expect(capDataset.borderDash).toEqual([6, 4]);

    const trendDataset = config.data.datasets[1];
    expect(trendDataset.label).toBe('Occupancy Trend');
    expect(trendDataset.data).toHaveLength(5);
    expect(trendDataset.data[2]).toEqual({ x: 1.5, y: 5920 });
  });
});

describe('distributionConfig', () => {
  test('creates horizontal bar chart configuration for relief distribution', () => {
    const lines = [
      ['Ratnapura · CARE Sri Lanka', 1200, 'dry-ration packs', 0],
      ['Ratnapura · Sri Lanka Army', 18000, 'litres', 1]
    ];

    const config = charts.distributionConfig(lines);

    expect(config.type).toBe('bar');
    expect(config.options.indexAxis).toBe('y');
    expect(config.data.labels).toEqual([
      'Ratnapura · CARE Sri Lanka (dry-ration packs)',
      'Ratnapura · Sri Lanka Army (litres)'
    ]);
    expect(config.data.datasets[0].data).toEqual([1200, 18000]);
  });
});

describe('draw', () => {
  function fakeChart() {
    const calls = [];
    class ChartMock {
      constructor(canvas, config) {
        this.canvas = canvas;
        this.config = config;
        calls.push({ canvas, config });
      }
    }
    return { ChartMock, calls };
  }

  function createContainer(attributes) {
    document.body.innerHTML = '<div data-chart="test" id="chart-el"></div>';
    const el = document.getElementById('chart-el');
    Object.keys(attributes).forEach((key) => {
      el.dataset[key] = attributes[key];
    });
    return el;
  }

  test('renders timeline chart into canvas element with Chart.js', () => {
    const { ChartMock, calls } = fakeChart();
    const el = createContainer({
      chart: 'timeline',
      events: JSON.stringify([{ day: 0.5, label: 'Alert', shape: 'circle' }])
    });

    const instance = charts.draw(el, ChartMock);

    expect(instance).toBeInstanceOf(ChartMock);
    expect(calls).toHaveLength(1);
    expect(calls[0].config.type).toBe('scatter');
    expect(el.querySelector('canvas')).not.toBeNull();
  });

  test('renders bars chart into canvas element with Chart.js', () => {
    const { ChartMock, calls } = fakeChart();
    const el = createContainer({
      chart: 'bars',
      bars: JSON.stringify([['Push', 100]]),
      total: JSON.stringify(['Total', 100]),
      max: '200'
    });

    const instance = charts.draw(el, ChartMock);

    expect(instance).toBeInstanceOf(ChartMock);
    expect(calls[0].config.type).toBe('bar');
  });

  test('renders occupancy chart into canvas element with Chart.js', () => {
    const { ChartMock, calls } = fakeChart();
    const el = createContainer({
      chart: 'occupancy',
      capacity: '5000',
      peak: '4000',
      peakDay: '2.0',
      records: '500'
    });

    const instance = charts.draw(el, ChartMock);

    expect(instance).toBeInstanceOf(ChartMock);
    expect(calls[0].config.type).toBe('line');
  });

  test('renders distribution chart into canvas element with Chart.js', () => {
    const { ChartMock, calls } = fakeChart();
    const el = createContainer({
      chart: 'distribution',
      lines: JSON.stringify([['Dist 1', 500, 'packs', 0]])
    });

    const instance = charts.draw(el, ChartMock);

    expect(instance).toBeInstanceOf(ChartMock);
    expect(calls[0].config.type).toBe('bar');
  });

  test('displays message when Chart constructor is missing', () => {
    const el = createContainer({ chart: 'timeline' });
    const instance = charts.draw(el, null);

    expect(instance).toBeNull();
    expect(el.textContent).toContain('Chart unavailable');
  });

  test('displays message for unknown chart type', () => {
    const { ChartMock } = fakeChart();
    const el = createContainer({ chart: 'unknown_type' });
    const instance = charts.draw(el, ChartMock);

    expect(instance).toBeNull();
    expect(el.textContent).toContain('Unknown chart type');
  });
});
