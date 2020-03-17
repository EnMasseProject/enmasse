CREATE TABLE IF NOT EXISTS device_registrations (
	tenant_id varchar(256) NOT NULL,
	device_id varchar(256) NOT NULL,
	version varchar(36) NOT NULL,

	data json,

	PRIMARY KEY (tenant_id, device_id)
);

CREATE TABLE IF NOT EXISTS device_credentials (
	tenant_id varchar(256) NOT NULL,
	device_id varchar(256) NOT NULL,

	type varchar(64) NOT NULL,
	auth_id varchar(64) NOT NULL,

	data json,

	PRIMARY KEY (tenant_id, device_id),
	FOREIGN KEY (tenant_id, device_id) REFERENCES device_registrations (tenant_id, device_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS device_states (
	tenant_id varchar(256) NOT NULL,
	device_id varchar(256) NOT NULL,

	last_known_gateway varchar(256),

	PRIMARY KEY (tenant_id, device_id)
);

-- create index for tenant only lookups
CREATE INDEX idx_device_registrations_tenant ON device_registrations (tenant_id);
CREATE INDEX idx_device_credentials_tenant ON device_credentials (tenant_id);
CREATE INDEX idx_device_states_tenant ON device_states (tenant_id);
