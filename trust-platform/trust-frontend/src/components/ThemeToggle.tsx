import { useState } from 'react';
import { applyTheme, getStoredTheme, type ThemeName } from '../theme/theme';

export function ThemeToggle() {
  const [theme, setTheme] = useState<ThemeName>(getStoredTheme());

  function toggle() {
    const next: ThemeName = theme === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    setTheme(next);
  }

  return (
    <button
      className="theme-toggle"
      onClick={toggle}
      title={theme === 'dark' ? 'التبديل إلى الوضع الفاتح' : 'التبديل إلى الوضع الداكن'}
      aria-label="تبديل المظهر"
    >
      {theme === 'dark' ? '☀️' : '🌙'}
    </button>
  );
}
