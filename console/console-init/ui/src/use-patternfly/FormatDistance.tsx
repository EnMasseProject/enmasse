/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { formatDistance } from "date-fns";
import { ArgumentsType } from "./types";

export interface IFormatDistanceProps {
  date: string | Date | number;
  base?: Date;
  options?: ArgumentsType<typeof formatDistance>[2];
}
export const FormatDistance: React.FunctionComponent<IFormatDistanceProps> = ({
  date,
  base = new Date(),
  options
}) => {
  date = typeof date === "string" ? new Date(date) : date;
  return <>{formatDistance(date, base, options)}</>;
};
