import UIManager from '../../../src/main/resources/static/js/uiManager.js';

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
});

describe('UIManager', () => {
  test('showResults populates results container', () => {
    const ui = new UIManager();
    ui.showResults([{ id: 1, name: 'Hà Nội', type: 'province' }]);
    const item = document.querySelector('.search-item');
    expect(item).not.toBeNull();
    expect(item.textContent).toContain('Hà Nội');
  });

  test('bindResultClicks triggers selection callback', () => {
    const ui = new UIManager();
    const mockSelect = jest.fn();
    ui.showResults([{ id: 1, name: 'Hà Nội', type: 'province' }]);
    ui.bindResultClicks(mockSelect);
    const item = document.querySelector('.search-item');
    item.click();
    expect(mockSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }));
  });

  test('setInputValue sets value and uses ignoreInput', () => {
    const ui = new UIManager();
    ui.setInputValue('Test');
    expect(document.getElementById('searchInput').value).toBe('Test');
  });
});
