drop table if exists message;

create table message (
  id serial primary key,
  contents varchar(10) not null
);
