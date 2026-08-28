export type ThemeName = 'dark' | 'light';

const STORAGE_KEY = 'trust-theme';

/**
 * الوضع الفاتح هو الافتراضي.
 *
 * المنتج يُستخدم نهارًا خلف طاولة محل، والقراءة فيه قراءة أرقام وجداول لا تصفّح
 * محتوى ترفيهي - والورق الفاتح أنسب لذلك. الوضع الداكن يبقى متاحًا لمن يختاره،
 * ويُحترَم اختياره بعد ذلك.
 */
export function getStoredTheme(): ThemeName {
  return localStorage.getItem(STORAGE_KEY) === 'dark' ? 'dark' : 'light';
}

export function applyTheme(theme: ThemeName): void {
  document.documentElement.setAttribute('data-theme', theme);
  document.documentElement.style.colorScheme = theme;
  localStorage.setItem(STORAGE_KEY, theme);
}

/** يُستدعى مرة واحدة عند إقلاع التطبيق قبل أول رسم، لتفادي وميض المظهر الخاطئ */
export function initTheme(): void {
  const theme = getStoredTheme();
  document.documentElement.setAttribute('data-theme', theme);
  document.documentElement.style.colorScheme = theme;
}
