alter table suppliers add column email varchar(255);

alter table app_users drop constraint app_users_role_check;
alter table app_users add constraint app_users_role_check
    check (role in ('OWNER','BRANCH_MANAGER','STAFF','PLATFORM_ADMIN','SUPPLIER'));
