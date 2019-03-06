/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

const DefaultLogbackConfig = `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xml>
<configuration>
	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern>%d{yyyy-MM-dd'T'HH:mm:ss.SSS'Z',GMT} %-5p [%c{0}] %m%n</pattern>
		</encoder>
	</appender>
	<root level="INFO">
		<appender-ref ref="STDOUT" />
	</root>
	<springProfile name="dev">
		<logger name="org.eclipse.hono" level="DEBUG"/>
	</springProfile>
	<springProfile name="prod">
		<logger name="org.eclipse.hono" level="INFO"/>
	</springProfile>
</configuration>
`
