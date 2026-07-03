alter table purchases add column supplier_id bigint;
alter table purchases add column decision_id bigint;
alter table purchases add column status varchar(255) not null default 'RECEIVED' check (status in ('SENT','RECEIVED'));
alter table purchases add column received_quantity double precision;
alter table purchases add column received_date date;
alter table purchases add column price_matched boolean;
alter table purchases add column has_damage boolean not null default false;
alter table purchases add column has_discrepancy boolean not null default false;

alter table purchases add constraint fk_purchases_supplier foreign key (supplier_id) references suppliers;
alter table purchases add constraint fk_purchases_decision foreign key (decision_id) references decisions;

create index idx_purchases_supplier on purchases (supplier_id);
create index idx_purchases_decision on purchases (decision_id);
