DROP TABLE IF EXISTS test_columntypes_special;

CREATE TABLE test_columntypes_special
(
  moneyfield money,
  varbitfield1 bit varying(63),
  varbitfield2 varbit,
  bitfield1 bit(8),
  bitfield2 bit,
  intervalfield interval,
  cidrfield cidr,
  inetfield inet,
  macaddrfield macaddr,
  uuidfield uuid,
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
  polygonfield polygon
)
WITH (
  OIDS=FALSE
);
ALTER TABLE test_columntypes_special
  OWNER TO root;
