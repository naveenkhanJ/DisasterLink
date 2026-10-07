import { render, screen } from '@testing-library/react-native';
import React from 'react';
import { AppHeader } from '../ui';

// A smoke test of component rendering: it renders the prototype's AppHeader and finds its visible text. It covers
// a file that the coverage settings leave out; it proves that the component tests of UC2 can run (S6b).
describe('AppHeader', () => {
  it('shows the badge and the app title', async () => {
    await render(<AppHeader />);

    expect(screen.getByText('DMC')).toBeTruthy();
    expect(screen.getByText('DMC Sri Lanka')).toBeTruthy();
  });
});
