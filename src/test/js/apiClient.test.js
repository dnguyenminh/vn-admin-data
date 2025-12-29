import ApiClient from '../../../src/main/resources/static/js/apiClient.js';

describe('ApiClient', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.resetAllMocks();
  });

  test('search calls fetch with query', async () => {
    global.fetch.mockResolvedValueOnce({ json: async () => [{ id: 1, name: 'Hà Nội' }] });
    const res = await ApiClient.search('Ha');
    expect(global.fetch).toHaveBeenCalledWith('/api/map/search?q=Ha');
    expect(res).toEqual([{ id: 1, name: 'Hà Nội' }]);
  });

  test('getProvinces calls /api/map/provinces', async () => {
    global.fetch.mockResolvedValueOnce({ json: async () => [{ id: 1 }] });
    const res = await ApiClient.getProvinces();
    expect(global.fetch).toHaveBeenCalledWith('/api/map/provinces');
    expect(res).toEqual([{ id: 1 }]);
  });
});
