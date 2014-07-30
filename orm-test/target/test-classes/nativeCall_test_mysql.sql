drop table d;
create table D(id int not null,
name varchar(40),
constraint PK_D primary key(id));


delimiter $$
drop procedure IF EXISTS insert_user$$
drop procedure IF EXISTS check_user$$


create  DEFINER=`root`@`%` procedure insert_user(IN argName varchar(40),IN argUserid int)
begin
  insert into D(name,id) values(argName,argUserid);
end
$$


create procedure check_user(IN _name varchar(40),OUT userCount int)
BEGIN
  select count(*) into userCount from D where name like _name;
END
$$
delimiter ;
