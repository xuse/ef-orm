TRUNCATE TABLE test_columntypes_db2entity;

INSERT INTO test_columntypes_db2entity(
            smallintfield, int2field, intfield2, bigintfield, 
	    decimalfield, numericfield, numericfield2, realfield, floatfield, 
	    doublefield, doublefield2, serialfield, serialfield2, moneyfield, 
	    varcharfield1, varcharfield2, charfield1, charfield2, varbitfield1, varbitfield2, bitfield1, bitfield2, 
	    cidrfield, inetfield, macaddrfield, uuidfield, 
            booleanfield1, booleanfield2, 
	    datefield, timestampfield1, timestampfield2, timefield1, timefield2, intervalfield, 
	    binaryfield, textfield, 
            tsvectorfield, tsqueryfield, xmlfield, txidfield, 
	    boxfield, circlefield, linefield, lsegfield, pathfield, pointfield, polygonfield)
    VALUES (-32768, 32767, 2147483647, 9223372036854775807, 
	    99.99, 999.999, 999.99::numeric(5,2), 0.11111111, 0.99999999, 
            999999.999999, 999999.999999, DEFAULT, 999999999, 12.12::text::money,
	    'abc', 'abcd', 'a', 'b', '101010', '1000000', '10101010', '1', 
            '192.168.100.128/25', '192.168.1.1', '08:00:2b:01:02:03', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 
            true, false, 
	    '2012-07-27', '2012-07-27 13:00:00', '2012-07-27 13:00:00 America/New_York', '13:00:00', '13:00:00+8', '1-2', 
	    E'\\123456', 'text', 
            'a fat cat sat on a mat and ate a fat rat'::tsvector, 'fat & rat'::tsquery, '<foo>bar</foo>'::xml, null, 
	    '((0,0),(1,1))', '((1,1),2)', null, '((1,1),(2,2))', '((0,0),(1,1),(2,2))', '(5,5)', '((0,0),(1,1),(2,2))' );
