import { render, screen, within } from '@testing-library/react-native';
import React from 'react';
import { StyleSheet } from 'react-native';
import { TabBar, type Tab } from '../ui';

/** The flattened style of every tab, in order (Home, Report, Profile). */
function tabStyles(): Record<string, unknown>[] {
  return screen.getAllByRole('tab').map((tab) => StyleSheet.flatten(tab.props.style));
}

async function renderBar(active: Tab): Promise<void> {
  await render(
    <TabBar active={active} onHome={jest.fn()} onReport={jest.fn()} onSoon={jest.fn()} />,
  );
}

describe('the tab bar (standards §10 item 15, P222; UC2-7e G5) — the visual check is iPhone only', () => {
  it('WF2P-S01 (G5): every tab has the same layout: from the top, the same padding, and no border that changes its content box', async () => {
    await renderBar('Home');

    for (const style of tabStyles()) {
      expect(style.justifyContent).toBe('flex-start');
      expect(style.paddingTop).toBe(8);
      expect(style.borderBottomWidth).toBeUndefined();
      expect(style.flex).toBe(1);
    }
  });

  it('WF2P-S01 (G5): the style offsets of the three tabs are the same whichever tab is active', async () => {
    await renderBar('Home');
    const home = tabStyles();
    await screen.unmount();
    await renderBar('Report');
    const report = tabStyles();

    expect(report).toEqual(home);
    expect(new Set(home.map((style) => JSON.stringify(style))).size).toBe(1);
  });

  it('WF2P-S01 (G5): the underline is on the active tab only, absolutely positioned 2 points high with a 16-point inset', async () => {
    await renderBar('Report');

    const underlines = screen.getAllByTestId('tab-underline');

    expect(underlines).toHaveLength(1);
    const style = StyleSheet.flatten(underlines[0].props.style);
    expect(style.position).toBe('absolute');
    expect(style.height).toBe(2);
    expect(style.left).toBe(16);
    expect(style.right).toBe(16);
    expect(style.bottom).toBe(0);
  });

  it('WF2P-S01 (G5): the underline is in the Home tab when Home is active, and Profile and Report have none', async () => {
    await renderBar('Home');
    const tabs = screen.getAllByRole('tab');

    expect(within(tabs[0]).queryByTestId('tab-underline')).not.toBeNull();
    expect(within(tabs[1]).queryByTestId('tab-underline')).toBeNull();
    expect(within(tabs[2]).queryByTestId('tab-underline')).toBeNull();
  });

  it('WF2P-S01 (G5): "Not in this version" is absolutely positioned 3 points from the bottom, so it does not push the Profile icon', async () => {
    await renderBar('Home');

    const style = StyleSheet.flatten(screen.getByText('Not in this version').props.style);

    expect(style.position).toBe('absolute');
    expect(style.bottom).toBe(3);
  });
});
