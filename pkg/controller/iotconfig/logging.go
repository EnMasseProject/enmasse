/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"sort"
	"strings"
)

func logbackDefault(rootLogger iotv1alpha1.LogLevel, loggers map[string]iotv1alpha1.LogLevel) string {

	// poor man's string enum ... everything we don't know, we reset to "info"
	switch rootLogger {
	case iotv1alpha1.LogLevelTrace:
	case iotv1alpha1.LogLevelDebug:
	case iotv1alpha1.LogLevelInfo:
	case iotv1alpha1.LogLevelWarning:
	case iotv1alpha1.LogLevelError:
	default:
		rootLogger = iotv1alpha1.LogLevelInfo
	}

	result := `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xml>
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{yyyy-MM-dd'T'HH:mm:ss.SSS'Z',GMT} %-5p [%c{0}] %m%n</pattern>
    </encoder>
  </appender>
`

	// set root logger

	result += `
  <root level="` + strings.ToUpper(string(rootLogger)) + `">
    <appender-ref ref="STDOUT" />
  </root>

`

	// add loggers

	// we need sort keys to get a stable order, a stable file, and thus a stable hash
	// as Go doesn't provide any help iterating over maps with sorted keys, we need to
	// implement it ourselves.

	keys := make([]string, 0)
	for k := range loggers {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	for _, k := range keys {
		v := loggers[k]
		result += `  <logger name="` + k + `" level="` + strings.ToUpper(string(v)) + `"/>
`
	}

	// add fixes

	result += `
  <!-- remove when https://github.com/eclipse-vertx/vert.x/pull/3316 is sorted -->
  <logger name="io.netty.channel.DefaultChannelPipeline" level="ERROR"/>
`

	// close

	result += `</configuration>`

	// return

	return result

}
