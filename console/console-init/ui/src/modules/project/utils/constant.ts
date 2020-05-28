/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

enum StatusTypes {
  ACTIVE = "Active",
  FAILED = "Failed",
  PENDING = "Pending",
  CONFIGURING = "Configuring",
  TERMINATING = "Terminating"
}

enum ProjectTypes {
  IOT = "IOT",
  MESSAGING = "MSG"
}

export { StatusTypes, ProjectTypes };
