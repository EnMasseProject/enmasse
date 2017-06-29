{
  generate(name, uuid, type, description, templateName, capacity)::
  {
    "name": name,
    "uuid": uuid,
    "type": type,
    "description": description,
    "templateName": templateName,
    [if capacity != null then "templateParameters"]: {
      "STORAGE_CAPACITY": capacity
    }
  }
}
