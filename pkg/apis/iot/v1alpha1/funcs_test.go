/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package v1alpha1

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestLoggingPureDefault(t *testing.T) {

	config := &IoTConfig{}

	root, loggers := CommonLoggingConfig{}.GetEffectiveConfiguration(config)
	assert.Equal(t, LogLevelInfo, root)
	assert.Len(t, loggers, 0)

}

func TestLoggingConfigDefault(t *testing.T) {

	config := &IoTConfig{
		Spec: IoTConfigSpec{
			Logging: LoggingConfig{
				Level: LogLevelWarning,
			},
		},
	}

	root, loggers := CommonLoggingConfig{}.GetEffectiveConfiguration(config)
	assert.Equal(t, LogLevelWarning, root)
	assert.Len(t, loggers, 0)

}

func TestLoggingConfigDefaultWithLoggers(t *testing.T) {

	config := &IoTConfig{
		Spec: IoTConfigSpec{
			Logging: LoggingConfig{
				Level: LogLevelWarning,
				Loggers: map[string]LogLevel{
					"foo": LogLevelTrace,
				},
			},
		},
	}

	root, loggers := CommonLoggingConfig{}.GetEffectiveConfiguration(config)
	assert.Equal(t, LogLevelWarning, root)
	assert.Len(t, loggers, 1)
	assert.Equal(t, LogLevelTrace, loggers["foo"])

}

func TestLoggingConfigOverrideRoot(t *testing.T) {

	config := &IoTConfig{
		Spec: IoTConfigSpec{
			Logging: LoggingConfig{
				Level: LogLevelWarning,
				Loggers: map[string]LogLevel{
					"foo": LogLevelTrace,
				},
			},
		},
	}

	root, loggers := CommonLoggingConfig{
		Level: LogLevelDebug,
	}.GetEffectiveConfiguration(config)
	assert.Equal(t, LogLevelDebug, root)
	assert.Len(t, loggers, 0)

}

func TestLoggingConfigOverrideRootWithLoggers(t *testing.T) {

	config := &IoTConfig{
		Spec: IoTConfigSpec{
			Logging: LoggingConfig{
				Level: LogLevelWarning,
				Loggers: map[string]LogLevel{
					"foo": LogLevelTrace,
				},
			},
		},
	}

	root, loggers := CommonLoggingConfig{
		Level: LogLevelDebug,
		Loggers: map[string]LogLevel{
			"bar": LogLevelError,
		},
	}.GetEffectiveConfiguration(config)
	assert.Equal(t, LogLevelDebug, root)
	assert.Len(t, loggers, 1)
	assert.Equal(t, LogLevelError, loggers["bar"])

}

func TestLoggingConfigOverrideRootWithLoggersOnly(t *testing.T) {

	config := &IoTConfig{
		Spec: IoTConfigSpec{
			Logging: LoggingConfig{
				Level: LogLevelWarning,
				Loggers: map[string]LogLevel{
					"foo": LogLevelTrace,
				},
			},
		},
	}

	root, loggers := CommonLoggingConfig{
		Loggers: map[string]LogLevel{
			"bar": LogLevelError,
		},
	}.GetEffectiveConfiguration(config)
	assert.Equal(t, LogLevelWarning, root)
	assert.Len(t, loggers, 1)
	assert.Equal(t, LogLevelError, loggers["bar"])

}

func TestQuarkusApplyLoggingToContainer(t *testing.T) {
	config := &IoTConfig{}
	opts := QuarkusContainerConfig{
		JavaContainerConfig: JavaContainerConfig{
			Logging: CommonLoggingConfig{
				Level: LogLevelInfo,
				Loggers: map[string]LogLevel{
					"foo": LogLevelTrace,
					"bar": LogLevelWarning,
				},
			},
		},
	}.ApplyLoggingToContainer(config, nil)

	assert.Len(t, opts, 3)
	assert.Equal(t, "-Dquarkus.log.level=info", opts[0])
	assert.Equal(t, "-Dquarkus.log.category.\"bar\".level=warn", opts[1])
	assert.Equal(t, "-Dquarkus.log.category.\"foo\".level=trace", opts[2])
}
