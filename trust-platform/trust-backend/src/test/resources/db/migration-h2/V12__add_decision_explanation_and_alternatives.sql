-- إكمال بطاقة القرار وفق دستور رؤية المنتج: كل توصية تجيب "ماذا لو نفّذت؟" و"ماذا لو
-- تجاهلت؟"، وتعرض القيود التي رُوعيت وأسباب درجة الثقة، وتقدّم بدائل بدل خيار واحد.
-- كلها مشتقة من حسابات المحرك القائمة - لا مصدر بيانات جديد.

alter table decisions add column if_ignored_summary varchar(500);
alter table decisions add column constraints_summary varchar(700);
alter table decisions add column confidence_reasons varchar(700);

-- البدائل JSON على القرار: تُقرأ معه دائمًا ولا يُستعلَم عنها منفردة، فجدول مستقل
-- كان سيضيف كلفة انضمام بلا فائدة
alter table decisions add column alternatives_json varchar(2000);
