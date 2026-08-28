-- نسخة للتحقق على H2 فقط: الأصل يُسقط قيدًا باسم يولّده Postgres تحديدًا
-- (app_users_role_check) وهو غير موجود بذلك الاسم على H2. لا تُستخدم في الإنتاج.
alter table suppliers add column email varchar(255);
