local multicast = std.extVar("multicast");
local persistence = std.extVar("persistence");
local secure = std.extVar("secure");
local storage = import "storage.jsonnet";
storage.template(multicast, persistence, secure)
