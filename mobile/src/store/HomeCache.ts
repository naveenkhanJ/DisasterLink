/**
 * HomeCache.ts — the phone's copy of the last home screen and the citizen's language (model
 * dictionary section 6: `HomeCache`; FQ3, PR13), kept in AsyncStorage. Offline the home screen is
 * built from it (SC2 A8). The language starts as the demo citizen's configured language and changes
 * with `changeLanguage`.
 */
import AsyncStorage from '@react-native-async-storage/async-storage';
import { HOME_STORAGE_KEY, LANGUAGE_STORAGE_KEY } from '../constants';
import type { Language } from '../api/types';
import type { HomeView } from '../report/ReportDraft';

/** The calls the capture controller makes on the phone's home cache. */
export interface HomeCache {
  /**
   * Keeps the home that was just loaded.
   *
   * @param view the home screen's data
   */
  saveHome(view: HomeView): Promise<void>;
  /**
   * Gives the last home that was kept.
   *
   * @returns the home, or `undefined` if none was kept yet
   */
  loadHome(): Promise<HomeView | undefined>;
  /**
   * Gives the citizen's language.
   *
   * @returns the chosen language, or the configured one if none was chosen
   */
  getLanguage(): Promise<Language>;
  /**
   * Keeps the citizen's language.
   *
   * @param language the new language
   */
  changeLanguage(language: Language): Promise<void>;
}

/**
 * Builds the cache over AsyncStorage.
 *
 * @param defaultLanguage the language before the citizen chooses one (`getEnv().citizenLanguage`)
 * @returns the cache
 */
export function createHomeCache(defaultLanguage: Language): HomeCache {
  return {
    async saveHome(view) {
      await AsyncStorage.setItem(HOME_STORAGE_KEY, JSON.stringify(view));
    },
    async loadHome() {
      const text = await AsyncStorage.getItem(HOME_STORAGE_KEY);
      return text ? (JSON.parse(text) as HomeView) : undefined;
    },
    async getLanguage() {
      return (
        ((await AsyncStorage.getItem(LANGUAGE_STORAGE_KEY)) as Language | null) ?? defaultLanguage
      );
    },
    async changeLanguage(language) {
      await AsyncStorage.setItem(LANGUAGE_STORAGE_KEY, language);
    },
  };
}
