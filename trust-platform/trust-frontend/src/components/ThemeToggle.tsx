import { useState } from 'react';
import { applyTheme, getStoredTheme, type ThemeName } from '../theme/theme';
import { Icon } from './Icon';

export function ThemeToggle() {
  const [theme, setTheme] = useState<ThemeName>(getStoredTheme());

  function toggle() {
    const next: ThemeName = theme === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    setTheme(next);
  }

  const label = theme === 'dark' ? 'التبديل إلى الوضع الفاتح' : 'التبديل إلى الوضع الداكن';

  return (
    <button className="theme-toggle" onClick={toggle} title={label} aria-label={label}>
      {/* الأيقونة تُظهر الوجهة لا الحالة الراهنة - الشمس تعني "انتقل إلى الفاتح" */}
      <Icon name={theme === 'dark' ? 'light' : 'dark'} size={16} />
    </button>
  );
}
