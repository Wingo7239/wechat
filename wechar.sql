create table user(
userId varchar(20),
username varchar(20),
deviceOs varchar(20),
createTime date,
role	varchar(10)
)


insert into  user(userId) values('gjs');

Select count(userid) from user where userid = 'gjs'