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

  it('WF2P-S01 (R7): the underline is a flow child after the label, 8 points below it, 2 points high, stretched with a 16-point inset', async () => {
    await renderBar('Report');

    const underlines = screen.getAllByTestId('tab-underline');

    expect(underlines).toHaveLength(2);
    for (const underline of underlines) {
      const style = StyleSheet.flatten(underline.props.style);
      expect(style.position).toBeUndefined();
      expect(style.marginTop).toBe(8);
      expect(style.height).toBe(2);
      expect(style.alignSelf).toBe('stretch');
      expect(style.marginHorizontal).toBe(16);
    }
  });

  it('WF2P-S01 (R7): in each of Home and Report the children are the icon, the label, then the underline', async () => {
    await renderBar('Home');
    const tabs = screen.getAllByRole('tab');

    for (const tab of tabs.slice(0, 2)) {
      const children = (tab.children as { props?: { testID?: string }; type?: unknown }[]).map(
        (child) => child.props?.testID ?? String(child.type),
      );
      expect(children[children.length - 1]).toBe('tab-underline');
      expect(children).toHaveLength(3);
    }
  });

  it('WF2P-S01 (R7): the underline is coloured on the active tab only and is there, transparent, on the others', async () => {
    await renderBar('Home');
    const tabs = screen.getAllByRole('tab');
    const colour = (index: number) =>
      StyleSheet.flatten(within(tabs[index]).getByTestId('tab-underline').props.style)
        .backgroundColor;

    expect(colour(0)).not.toBe('transparent');
    expect(colour(1)).toBe('transparent');
    expect(within(tabs[2]).queryByTestId('tab-underline')).toBeNull();
  });

  it('WF2P-S01 (R7): the label has an explicit line height of 15', async () => {
    await renderBar('Home');

    for (const name of ['Home', 'Report', 'Profile']) {
      expect(StyleSheet.flatten(screen.getByText(name).props.style).lineHeight).toBe(15);
    }
  });

  it('WF2P-S01 (R7): the icon and label offsets are the same whichever tab is active', async () => {
    await renderBar('Home');
    const offsets = () =>
      screen
        .getAllByRole('tab')
        .map((tab) => within(tab).getAllByText(/^(Home|Report|Profile)$/))
        .flat()
        .map((label) => {
          const { marginTop, lineHeight, fontSize } = StyleSheet.flatten(label.props.style);
          return [marginTop, lineHeight, fontSize];
        });
    const onHome = offsets();
    await screen.unmount();
    await renderBar('Report');

    expect(offsets()).toEqual(onHome);
  });

  it('WF2P-S01 (R7): on Profile "Not in this version" takes the underline place in the flow, 2 points below the label, and is not absolutely positioned', async () => {
    await renderBar('Home');

    const tag = screen.getByText('Not in this version');
    const style = StyleSheet.flatten(tag.props.style);
    const profile = screen.getAllByRole('tab')[2];

    expect(style.position).toBeUndefined();
    expect(style.bottom).toBeUndefined();
    expect(style.marginTop).toBe(2);
    expect(within(profile).queryByTestId('tab-underline')).toBeNull();
  });
});
