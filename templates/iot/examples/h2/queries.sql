INSERT INTO devices (
    tenant_id,
    device_id,
    version,
    data
  ) VALUES (
    't1',
    'd1',
    'v1',
    '{}' FORMAT JSON
  );

SELECT * FROM devices;

INSERT INTO device_credentials (
    tenant_id,
    device_id,
    version,
    type,
    auth_id,
    data
  ) VALUES (
    't1',
    'd1',
    'v1',
    'hashed-password',
    'a1',
    '{"enabled": true, "secrets": []}' FORMAT JSON
  );

SELECT * FROM device_credentials;

DELETE FROM devices WHERE tenant_id='t1' AND device_id='d1';
