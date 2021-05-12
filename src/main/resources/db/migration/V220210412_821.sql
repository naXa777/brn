create table headphones
(
    id          bigint generated by default as identity
        constraint headphones_pkey
            primary key,
    description varchar(255),
    name        varchar(255),
    type        varchar(255),
    user_id     bigint
        constraint fkfd63f5wlyo1y7e4c4i746amn0
            references user_account
);

alter table if exists audiometry_history
    add headphones int8;
alter table if exists audiometry_history
    drop constraint ukekiuxjh0lvrl6mdkwifwskq5v;
alter table if exists audiometry_history
    add constraint headphones_foreign_keys foreign key (headphones) references headphones;
alter table if exists audiometry_history
    add constraint unique_columns unique (user_id, audiometry_task_id, start_time, headphones);
alter table if exists audiometry_history
    add constraint not_null_headphones check ( headphones > 0 )