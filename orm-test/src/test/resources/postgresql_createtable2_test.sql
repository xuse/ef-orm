DROP TABLE IF EXISTS test_columntypes_common;

CREATE TABLE test_columntypes_common
(
  smallintfield smallint NOT NULL,
  int2field int2,
  intfield2 integer,
  bigintfield bigint,
  decimalfield decimal,
  numericfield numeric,
  numericfield2 numeric(5,2),
  realfield real,
  floatfield float4 NOT NULL,
  doublefield double precision NOT NULL,
  doublefield2 double precision,
  serialfield serial NOT NULL,
  serialfield2 bigserial NOT NULL,
  varcharfield1 character varying(255),
  varcharfield2 varchar,
  charfield1 character(2),
  charfield2 char,
  booleanfield1 boolean NOT NULL,
  booleanfield2 bool,
  datefield date,
  timestampfield1 timestamp,
  timestampfield2 timestamptz,
  timefield1 time,
  timefield2 timetz,
  binaryfield bytea,
  textfield text,
  CONSTRAINT pk_test_columntypes_common PRIMARY KEY (serialfield2)
)
--WITH (
--  OIDS=FALSE
--);
--ALTER TABLE test_columntypes_common
--  OWNER TO test;
