explain analyze
select device_id, version, credentials
  from devices
  where
    tenant_id='enmasse-infra.iot/2020-01-31T13:47:37Z'
    AND credentials @> '[{"type":"hashed-password", "auth-id":"auth"}]'
;

explain analyze
select device_id, version, credentials
  from devices
  where
    tenant_id='myapp.iot/2020-01-29T09:54:57Z'
    AND credentials @> '[{"type":"hashed-password", "auth-id":"auth"}]'
;

explain analyze
select device_id, version, data
  from devices
    where tenant_id='enmasse-infra.iot/2020-01-31T13:47:37Z'
;

explain analyze
select device_id, version, credentials
  from devices
  where
    tenant_id='myapp.iot/2020-01-29T09:54:57Z'
    AND credentials->'hashed-password' ? 'auth-0760'
;
