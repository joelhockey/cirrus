create table user (
    id identity,
    username varchar(255),
    salt varchar(64),
    hashed_password varchar(64),
    created_at timestamp default current_timestamp not null,
    version integer default 1 not null,
    updated_at timestamp default current_timestamp not null
);

insert into user (username, salt, hashed_password) values ('admin', '0000000000000000000000000000000000000000000000000000000000000000', 'b5775243225d4f5e4e41a55785003f8b56f656392539db5df48c77b791c803db');

