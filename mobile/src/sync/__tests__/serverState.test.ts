import { createServerState } from '../serverState';

describe('createServerState (standards §10 item 11, P231; UC2-7k F1)', () => {
  it('starts with the server not down', () => {
    expect(createServerState().isDown()).toBe(false);
  });

  it('holds what the strip says: down after setDown(true), not down after setDown(false)', () => {
    const state = createServerState();

    state.setDown(true);
    expect(state.isDown()).toBe(true);
    state.setDown(false);

    expect(state.isDown()).toBe(false);
  });

  it('two holders are independent', () => {
    const first = createServerState();
    const second = createServerState();

    first.setDown(true);

    expect(second.isDown()).toBe(false);
  });
});
