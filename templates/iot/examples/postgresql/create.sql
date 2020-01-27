CREATE TABLE IF NOT EXISTS devices (
	tenant_id varchar(256) NOT NULL,
	device_id varchar(256) NOT NULL,
	version varchar(36) NOT NULL,
	data jsonb,
	credentials jsonb,

	PRIMARY KEY (tenant_id, device_id)
);

CREATE TABLE IF NOT EXISTS device_states (
	tenant_id varchar(256) NOT NULL,
	device_id varchar(256) NOT NULL,
	last_known_gateway varchar(256),

	PRIMARY KEY (tenant_id, device_id)
);

-- create index for tenant only lookups
CREATE INDEX idx_devices_tenant ON devices (tenant_id);
CREATE INDEX idx_device_states_tenant ON device_states (tenant_id);

-- create index for device data search
CREATE INDEX idx_devices_data ON devices USING gin (data jsonb_path_ops);

-- create index for credentials lookup
CREATE INDEX idx_credentials ON devices USING gin (credentials jsonb_path_ops);
