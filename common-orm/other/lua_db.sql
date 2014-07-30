create table SYS_PARTITION_RULE
(
  BASE_NAME   VARCHAR2(30) not null,
  SCHEME_ID   number(5) not null,
  LAST_TIME   date,
  TYPE number(1)
)
/
create table SYS_PARTITION_SCHEME
(
  SCHEME_ID NUMBER(5) not null,
  LUA_CONTEXT varchar2(4000) not null,
  SCHEME_COMMENT varchar2(4000)
)
/
create table SYS_PARTITION_LOGIC_PARAM
(
  BASE_NAME   VARCHAR2(30) not null,
  PARAM_NAME  varchar2(30) not null,
  PARAM_VALUE varchar2(4000) ,
  PARAM_COMMENT varchar2(4000)
)
/
create table SYS_PARTITION_CONST_PARAM
(
  BASE_NAME   VARCHAR2(30) not null,
  PARAM_NAME  varchar2(30) not null,
  PARAM_VALUE varchar2(4000) ,
  PARAM_COMMENT varchar2(4000)
)
/
create table SYS_PARTITION_RELATION
(
  BASE_NAME   VARCHAR2(30) not null,
  PARAM_NAME  varchar2(30) not null,
  OP_TYPE  varchar2(4) not null,
  RELATION  number(1) not null,
  COLUMN_NAME varchar2(30)
)
/



prompt Disabling triggers for SYS_PARTITION_CONST_PARAM...
alter table SYS_PARTITION_CONST_PARAM disable all triggers;
prompt Disabling triggers for SYS_PARTITION_LOGIC_PARAM...
alter table SYS_PARTITION_LOGIC_PARAM disable all triggers;
prompt Disabling triggers for SYS_PARTITION_RELATION...
alter table SYS_PARTITION_RELATION disable all triggers;
prompt Disabling triggers for SYS_PARTITION_RULE...
alter table SYS_PARTITION_RULE disable all triggers;
prompt Disabling triggers for SYS_PARTITION_SCHEME...
alter table SYS_PARTITION_SCHEME disable all triggers;
prompt Deleting SYS_PARTITION_SCHEME...
delete from SYS_PARTITION_SCHEME;
commit;
prompt Deleting SYS_PARTITION_RULE...
delete from SYS_PARTITION_RULE;
commit;
prompt Deleting SYS_PARTITION_RELATION...
delete from SYS_PARTITION_RELATION;
commit;
prompt Deleting SYS_PARTITION_LOGIC_PARAM...
delete from SYS_PARTITION_LOGIC_PARAM;
commit;
prompt Deleting SYS_PARTITION_CONST_PARAM...
delete from SYS_PARTITION_CONST_PARAM;
commit;
prompt Loading SYS_PARTITION_CONST_PARAM...
insert into SYS_PARTITION_CONST_PARAM (base_name, param_name, param_value, param_comment)
values ('A', 'aa', '1', null);
insert into SYS_PARTITION_CONST_PARAM (base_name, param_name, param_value, param_comment)
values ('B', 'bb', '2', null);
insert into SYS_PARTITION_CONST_PARAM (base_name, param_name, param_value, param_comment)
values ('DEFAULT', 'DB_USER', 'OPT.', 'Schema');
insert into SYS_PARTITION_CONST_PARAM (base_name, param_name, param_value, param_comment)
values ('DEFAULT', 'DB_USER', 'OPT.', 'Schema');
commit;
prompt 4 records loaded
prompt Loading SYS_PARTITION_LOGIC_PARAM...
insert into SYS_PARTITION_LOGIC_PARAM (base_name, param_name, param_value, param_comment)
values ('default', 'aaa', '2342', null);
insert into SYS_PARTITION_LOGIC_PARAM (base_name, param_name, param_value, param_comment)
values ('DEFAULT', 'amount_level', '100', '£¿£¿£¿100W£¿£¿');
commit;
prompt 2 records loaded
prompt Loading SYS_PARTITION_RELATION...
insert into SYS_PARTITION_RELATION (base_name, param_name, op_type, relation, column_name)
values ('KERNEL_TEST', 'acct_id', '1234', 3, 'ITEM_VALUE51');
insert into SYS_PARTITION_RELATION (base_name, param_name, op_type, relation, column_name)
values ('KERNEL_TEST', 'amount_level', '1234', 3, 'ITEM_VALUE52');
insert into SYS_PARTITION_RELATION (base_name, param_name, op_type, relation, column_name)
values ('TABLE_A', 'ITEM1', '31', 3, 'COLUMN_1');
insert into SYS_PARTITION_RELATION (base_name, param_name, op_type, relation, column_name)
values ('table_b', 'item2', '32', 3, 'item2');
insert into SYS_PARTITION_RELATION (base_name, param_name, op_type, relation, column_name)
values ('TABLE_A', 'amount_level', '1234', 3, 'AMOUT_LEVEL');
commit;
prompt 5 records loaded
prompt Loading SYS_PARTITION_RULE...
insert into SYS_PARTITION_RULE (base_name, scheme_id, last_time, type)
values ('TABLE_A', 1, to_date('14-12-2011', 'dd-mm-yyyy'), 1);
insert into SYS_PARTITION_RULE (base_name, scheme_id, last_time, type)
values ('TABLE_B', 2, to_date('14-12-2011', 'dd-mm-yyyy'), 1);
insert into SYS_PARTITION_RULE (base_name, scheme_id, last_time, type)
values ('TABLE_C', 2, to_date('14-12-2011', 'dd-mm-yyyy'), 1);
insert into SYS_PARTITION_RULE (base_name, scheme_id, last_time, type)
values ('KERNEL_TEST', 3, to_date('14-12-2011', 'dd-mm-yyyy'), 1);
commit;
prompt 4 records loaded
prompt Loading SYS_PARTITION_SCHEME...
insert into SYS_PARTITION_SCHEME (scheme_id, lua_context, scheme_comment)
values (3, 'function partition_rule(tbIn)' || chr(10) || '    print(''[lua]begin...'') ' || chr(10) || '    table.foreach(tbIn, print)' || chr(10) || '    if tbIn[''amount_level''] ~= nil then' || chr(10) || '        if tbIn[''amount_level''][1] ~= nil then amount_min = tbIn[''amount_level''][1] end' || chr(10) || '        if tbIn[''amount_level''][2] ~= nil then amount_max = tbIn[''amount_level''][2] end' || chr(10) || '    end' || chr(10) || '' || chr(10) || '    if nil == amount_min then amount_min = 1 end' || chr(10) || '    if nil == amount_max then amount_max = 1 end  ' || chr(10) || '    acct_id = 3' || chr(10) || '' || chr(9) || '' || chr(9) || '' || chr(10) || '    tail_min = amount_min%1000000' || chr(10) || '    tail_max = amount_max%1000000' || chr(10) || '    tail_id = acct_id%10' || chr(10) || '        ' || chr(10) || '    print(''tail_min''..tail_min)' || chr(10) || '    print(''tail_max''..tail_max)    ' || chr(10) || '        ' || chr(10) || '    tbOut = {}' || chr(10) || '    for i=tail_min,tail_max do' || chr(10) || '        table_name = ''{?BASE_NAME}_''..i..''_''..tail_id' || chr(10) || '        tbOut[i] = {tail_id, table_name}' || chr(10) || '    end' || chr(10) || '--    tbOut[2]={''jay_key1'', ''jay_val1''}' || chr(10) || '--    tbOut[3]={''jay_key2'', ''jay_val2''}' || chr(10) || '--    tbOut[4]={''jay_key3'', ''jay_val3''}' || chr(10) || '' || chr(9) || '' || chr(10) || '    table.foreach(tbOut, print)' || chr(9) || '' || chr(10) || '' || chr(9) || '' || chr(9) || ' ' || chr(10) || '    return tbOut ' || chr(10) || 'end    ', null);
insert into SYS_PARTITION_SCHEME (scheme_id, lua_context, scheme_comment)
values (2, 'function partition_rule(/*table*/param)' || chr(10) || '' || chr(10) || '' || chr(10) || '  sTableName={?db_user}{?BASE_NAME}itoa(iTail)' || chr(10) || '' || chr(10) || '  return /*table*/result' || chr(10) || 'end;', null);
insert into SYS_PARTITION_SCHEME (scheme_id, lua_context, scheme_comment)
values (1, 'function partition_rule(table_partition_params)' || chr(10) || '    print(''[lua]begin...'') ' || chr(10) || '    table.foreach(table_partition_params, print)' || chr(10) || '    if table_partition_params[''amount_level''] ~= nil then' || chr(10) || '        if table_partition_params[''amount_level''][1] ~= nil then amount_min = table_partition_params[''amount_level''][1] end' || chr(10) || '        if table_partition_params[''amount_level''][2] ~= nil then amount_max = table_partition_params[''amount_level''][2] end' || chr(10) || '    end' || chr(10) || '' || chr(10) || '    if nil == amount_min then amount_min = 1 end' || chr(10) || '    if nil == amount_max then amount_max = 1 end  ' || chr(10) || '    acct_id = 3' || chr(10) || '    ' || chr(10) || '    tail_min = amount_min%100' || chr(10) || '    tail_max = amount_max%100' || chr(10) || '    tail_id = acct_id%10' || chr(10) || '        ' || chr(10) || '    print(''tail_min=''..tail_min)' || chr(10) || '    print(''tail_max=''..tail_max)    ' || chr(10) || '        ' || chr(10) || '    tbOut = {}' || chr(10) || '    for i=tail_min,tail_max do' || chr(10) || '        table_name = ''TABLE_A_''..i..''_''..tail_id' || chr(10) || '        tbOut[i] = {''db''..tail_id, table_name}' || chr(10) || '    end' || chr(10) || '--    tbOut[2]={''jay_key1'', ''jay_val1''}' || chr(10) || '--    tbOut[3]={''jay_key2'', ''jay_val2''}' || chr(10) || '--    tbOut[4]={''jay_key3'', ''jay_val3''}' || chr(10) || '  ' || chr(10) || '    table.foreach(tbOut, print)  ' || chr(10) || '     ' || chr(10) || '    return tbOut ' || chr(10) || 'end    ', '£¿£¿£¿£¿£¿£¿');
commit;
prompt 3 records loaded
prompt Enabling triggers for SYS_PARTITION_CONST_PARAM...
alter table SYS_PARTITION_CONST_PARAM enable all triggers;
prompt Enabling triggers for SYS_PARTITION_LOGIC_PARAM...
alter table SYS_PARTITION_LOGIC_PARAM enable all triggers;
prompt Enabling triggers for SYS_PARTITION_RELATION...
alter table SYS_PARTITION_RELATION enable all triggers;
prompt Enabling triggers for SYS_PARTITION_RULE...
alter table SYS_PARTITION_RULE enable all triggers;
prompt Enabling triggers for SYS_PARTITION_SCHEME...
alter table SYS_PARTITION_SCHEME enable all triggers;
