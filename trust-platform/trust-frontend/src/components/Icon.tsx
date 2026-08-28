import {
  AlertTriangle, ArrowDownToLine, ArrowLeft, ArrowUpRight, Banknote, BarChart3,
  Bell, Boxes, Building2, CalendarDays, Check, CheckCircle2, ChevronDown, ChevronLeft,
  ChevronRight, CircleAlert, ClipboardList, Coins, Droplets, FileText, Gauge, Handshake,
  Hourglass, Info, LayoutDashboard, LineChart, LogOut, Menu, Moon, Package, PackageSearch,
  PauseCircle, Pencil, Plus, RefreshCw, Scale, Settings, ShoppingCart, SlidersHorizontal,
  Store, Sun, Tag, Target, Trash2, TrendingDown, TrendingUp, Truck, User, Users, Wallet,
  Warehouse, X, Percent, Star, type LucideIcon,
} from 'lucide-react';

/**
 * مجموعة أيقونات المنتج.
 *
 * كانت الواجهة تستخدم 103 رموز تعبيرية (emoji) كأيقونات. الرمز التعبيري يُرسَم
 * بخط النظام، فيختلف شكله بين ويندوز وأندرويد وiOS، ولا يقبل لونًا ولا سماكة ولا
 * حجمًا من نظام التصميم - ولذلك يبدو دائمًا كحلٍّ مؤقّت.
 *
 * البديل مجموعة واحدة (Lucide، رخصة MIT) بسماكة خط موحّدة، تأخذ لونها من
 * currentColor فتتبع الوضع الفاتح والداكن تلقائيًا.
 *
 * الأسماء هنا دلالية لا شكلية: `risk` لا `triangle`. فلو تغيّر شكل تمثيل الخطر
 * لاحقًا يُبدَّل في هذا الملف وحده.
 */

const ICONS = {
  // ---- التنقّل ----
  dashboard: LayoutDashboard,
  sales: LineChart,
  inventory: Boxes,
  purchases: ShoppingCart,
  decisions: Target,
  profitability: Coins,
  liquidity: Droplets,
  pricing: Tag,
  reports: FileText,
  notifications: Bell,
  suppliers: Truck,
  settings: Settings,
  organizations: Building2,
  stagnant: Hourglass,
  groupOrders: Handshake,
  benchmarks: Scale,
  calibration: SlidersHorizontal,
  logout: LogOut,
  menu: Menu,

  // ---- الحالة والدلالة ----
  risk: AlertTriangle,
  opportunity: TrendingUp,
  warning: CircleAlert,
  info: Info,
  success: CheckCircle2,
  declining: TrendingDown,
  health: Gauge,

  // ---- الإجراءات ----
  approve: Check,
  modify: Pencil,
  defer: PauseCircle,
  dismiss: X,
  close: X,
  add: Plus,
  remove: Trash2,
  refresh: RefreshCw,
  download: ArrowDownToLine,
  next: ChevronLeft,
  prev: ChevronRight,
  expand: ChevronDown,
  back: ArrowLeft,
  goTo: ArrowUpRight,

  // ---- كيانات المجال ----
  money: Banknote,
  wallet: Wallet,
  item: Package,
  store: Store,
  branch: Warehouse,
  supplier: Truck,
  user: User,
  team: Users,
  date: CalendarDays,
  audit: ClipboardList,
  chart: BarChart3,
  margin: Percent,
  rating: Star,
  search: PackageSearch,

  // ---- المظهر ----
  light: Sun,
  dark: Moon,
} satisfies Record<string, LucideIcon>;

export type IconName = keyof typeof ICONS;

export function Icon({ name, size = 16, className, strokeWidth = 1.75, title }: {
  name: IconName;
  size?: number;
  className?: string;
  strokeWidth?: number;
  /** يُمرَّر فقط حين تحمل الأيقونة معنى لا يكرّره نص مجاور */
  title?: string;
}) {
  const Glyph = ICONS[name];
  return (
    <Glyph
      size={size}
      strokeWidth={strokeWidth}
      className={className}
      aria-hidden={title ? undefined : true}
      aria-label={title}
      role={title ? 'img' : undefined}
      focusable="false"
    />
  );
}
