create or replace function interval_to_months(interval , OUT result Integer) AS $$
    select cast(extract(year from $1)*12+extract(month from $1) as Integer) as result
$$ language SQL;