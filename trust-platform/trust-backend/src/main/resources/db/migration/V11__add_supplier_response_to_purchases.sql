-- ردّ المورّد على أمر الشراء. منفصل عن عمود status عمدًا: موافقة المورّد التزام
-- بالتوريد لا استلام فعلي، ودمجهما كان سيجعل الموافقة تُحدِث المخزون قبل وصول البضاعة.
-- إضافة عمود جديد بدل توسيع قيود status تتجنّب أيضًا تعديل هجرة V3 المطبَّقة سلفًا.

alter table purchases add column supplier_response varchar(255) not null default 'PENDING'
    check (supplier_response in ('PENDING','ACCEPTED','REJECTED'));
alter table purchases alter column supplier_response drop default;

alter table purchases add column supplier_responded_at date;
alter table purchases add column supplier_promised_date date;
alter table purchases add column supplier_rejection_reason varchar(255);
