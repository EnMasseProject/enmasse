{
  generate(secure, templateName, capacity)::
  local prefix = if secure then "tls-" else "";
  {
    "templateName": prefix + templateName,
    [if capacity != null then "templateParameters"]: {
      "STORAGE_CAPACITY": capacity
    }
  }
}
