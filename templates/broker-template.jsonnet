local multicast = std.extVar("multicast");
local persistence = std.extVar("persistence");
local broker = import "broker-common.jsonnet";
broker.generate_template(multicast, persistence, "false")
