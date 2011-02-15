-- 0 init
create table db_version (
    version integer not null,
    created_at timestamp default current_timestamp,
    filename varchar,
    script varchar
);
alter table db_version add constraint unq_db_version_version unique (version);

create table dual(dummy varchar(1));
insert into dual(dummy) values ('X');
