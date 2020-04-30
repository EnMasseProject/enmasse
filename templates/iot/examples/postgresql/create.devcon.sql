
CREATE TABLE IF NOT EXISTS device_states (
	tenant_id varchar(256) NOT NULL,
	device_id varchar(256) NOT NULL,
	last_known_gateway varchar(256),
	adapter_instance_id varchar(256),

	PRIMARY KEY (tenant_id, device_id)
);

-- create index for tenant only lookups

CREATE INDEX idx_device_states_tenant ON device_states (tenant_id);
