{
  generate(name, secure, type, description, templateName, capacity)::
  local prefix = if secure then "tls-" else "";
  {
    "name": name,
    "type": type,
    "description": description,
    "templateName": prefix + templateName,
    [if capacity != null then "templateParameters"]: {
      "STORAGE_CAPACITY": capacity
    }
  }
}
