alter table decisions add column category varchar(255) not null default 'OPPORTUNITY' check (category in ('RISK','OPPORTUNITY'));
alter table decisions alter column category drop default;
