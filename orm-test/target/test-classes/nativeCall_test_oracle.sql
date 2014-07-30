drop table D;
create table D(id number(6) not null,
				name varchar2(40),
				constraint PK_D primary key(id));

CREATE OR REPLACE PACKAGE TESTPACKAGE  AS
 TYPE TYPE_USER IS REF CURSOR;
end TESTPACKAGE;
/


create or replace PROCEDURE GET_ALL_USER(p_CURSOR out TESTPACKAGE.TYPE_USER) IS
BEGIN
    OPEN p_CURSOR FOR SELECT * FROM D;
END GET_ALL_USER;
/

create or replace procedure insert_user(
       argName in varchar2,argUserid in number
) as
begin
  insert into D values(argUserid,argName);
end insert_user;
/

create or replace procedure check_user(
       name in varchar2,userCount out number
) as
begin
  select count(*) into userCount from D;
end check_user;
/
