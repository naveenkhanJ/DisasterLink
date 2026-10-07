/** Tests of nav-notice.js: the "not available in this version" notice of the signposted navigation items. */
const SCRIPT = '../../backend/src/main/resources/static/js/nav-notice.js';

function loadScript() {
  let exported;
  jest.isolateModules(() => { exported = require(SCRIPT); });
  return exported;
}

beforeEach(() => {
  // jsdom does not implement scrollIntoView.
  Element.prototype.scrollIntoView = jest.fn();
});

describe('clicking a data-soon button', () => {
  beforeEach(() => {
    document.body.innerHTML =
      '<button type="button" data-soon="Hazards">Hazards</button>' +
      '<button type="button" data-soon="Rescue teams">Rescue teams</button>' +
      '<main class="page"><h2>Content</h2></main>';
    loadScript();
  });

  test('inserts one notice at the top of the page with the label', () => {
    document.querySelector('[data-soon="Hazards"]').click();

    const notices = document.querySelectorAll('main.page .soon-notice');
    expect(notices).toHaveLength(1);
    expect(notices[0].textContent).toBe('Hazards — not available in this version.');
    expect(notices[0].getAttribute('role')).toBe('status');
    expect(notices[0].className).toBe('notice soon-notice');
    expect(document.querySelector('main.page').firstChild).toBe(notices[0]);
    expect(Element.prototype.scrollIntoView).toHaveBeenCalledWith({ block: 'nearest' });
  });

  test('a second click replaces the first notice', () => {
    document.querySelector('[data-soon="Hazards"]').click();
    document.querySelector('[data-soon="Rescue teams"]').click();

    const notices = document.querySelectorAll('.soon-notice');
    expect(notices).toHaveLength(1);
    expect(notices[0].textContent).toBe('Rescue teams — not available in this version.');
  });
});

describe('showNotice', () => {
  test('does nothing when the page has no main.page', () => {
    document.body.innerHTML = '<div>no main here</div>';
    const { showNotice } = loadScript();

    showNotice('Hazards');

    expect(document.querySelector('.soon-notice')).toBeNull();
  });

  test('is exported and works without a click', () => {
    document.body.innerHTML = '<main class="page"></main>';
    const { showNotice } = loadScript();

    showNotice('Donations');

    expect(document.querySelector('.soon-notice').textContent).toBe('Donations — not available in this version.');
  });
});
