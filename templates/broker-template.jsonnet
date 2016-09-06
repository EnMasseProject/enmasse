local multicast = std.extVar("multicast");
local persistence = std.extVar("persistence");
local gen = import "broker-common.jsonnet";
gen.generate_template(multicast, persistence)
