export type ThemeName = 'dark' | 'light';

const STORAGE_KEY = 'trust-theme';

export function getStoredTheme(): ThemeName {
  return localStorage.getItem(STORAGE_KEY) === 'light' ? 'light' : 'dark';
}

export function applyTheme(theme: ThemeName): void {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem(STORAGE_KEY, theme);
}

/** يُستدعى مرة واحدة عند إقلاع التطبيق قبل أول رسم، لتفادي وميض المظهر الخاطئ */
export function initTheme(): void {
  document.documentElement.setAttribute('data-theme', getStoredTheme());
}
