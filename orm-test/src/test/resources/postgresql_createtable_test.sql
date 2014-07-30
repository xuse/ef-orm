DROP TABLE IF EXISTS test_columntypes_db2entity;

CREATE TABLE test_columntypes_db2entity
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
  moneyfield money,
  varcharfield1 character varying(255),
  varcharfield2 varchar,
  charfield1 character(2),
  charfield2 char,
  varbitfield1 bit varying(63),
  varbitfield2 varbit,
  bitfield1 bit(8),
  bitfield2 bit,
  cidrfield cidr,
  inetfield inet,
  macaddrfield macaddr,
  uuidfield uuid,
  booleanfield1 boolean NOT NULL,
  booleanfield2 bool,
  datefield date,
  timestampfield1 timestamp,
  timestampfield2 timestamptz,
  timefield1 time,
  timefield2 timetz,
  intervalfield interval,
  binaryfield bytea,
  textfield text,
  tsvectorfield tsvector,
  tsqueryfield tsquery,
  xmlfield xml,
  txidfield txid_snapshot,
  boxfield box,
  circlefield circle,
  linefield line,
  lsegfield lseg,
  pathfield path,
  pointfield point,
  polygonfield polygon,
  CONSTRAINT pk_test_columntypes_db2entity PRIMARY KEY (serialfield)
)
--WITH (
--  OIDS=FALSE
--);
--ALTER TABLE test_columntypes_db2entity
--  OWNER TO test;