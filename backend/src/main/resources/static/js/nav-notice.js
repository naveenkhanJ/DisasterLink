/*
 * nav-notice.js — signposted navigation items (wireframe-spec §8 rule 12, P91).
 *
 * An item whose use case is in the use case diagram but not implemented in this version is a button with
 * data-soon="<label>". Clicking it shows a notice at the top of the page content instead of opening an empty page.
 * The notice is a status message (role="status") so screen readers announce it.
 */
(function () {
  'use strict';

  function showNotice(label) {
    var page = document.querySelector('main.page');
    if (!page) { return; }
    var existing = page.querySelector('.soon-notice');
    if (existing) { existing.remove(); }
    var box = document.createElement('div');
    box.className = 'notice soon-notice';
    box.setAttribute('role', 'status');
    box.textContent = label + ' — not available in this version.';
    page.insertBefore(box, page.firstChild);
    box.scrollIntoView({ block: 'nearest' });
  }

  if (typeof module !== 'undefined' && module.exports) { module.exports = { showNotice: showNotice }; }

  document.querySelectorAll('[data-soon]').forEach(function (button) {
    button.addEventListener('click', function () { showNotice(button.dataset.soon); });
  });
})();
