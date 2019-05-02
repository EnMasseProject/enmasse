/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package logs

import (
	"fmt"
	"os"
	"runtime"

	"github.com/enmasseproject/enmasse/pkg/util"

	"github.com/enmasseproject/enmasse/version"
	"github.com/go-logr/logr"

	sdkVersion "github.com/operator-framework/operator-sdk/version"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
)

func InitLog() {
	development := os.Getenv("DEVELOPMENT") == "true"
	logf.SetLogger(logf.ZapLogger(development))
}

func PrintVersions(log logr.Logger) {
	log.Info(fmt.Sprintf("Go Version: %s", runtime.Version()))
	log.Info(fmt.Sprintf("Go OS/Arch: %s/%s", runtime.GOOS, runtime.GOARCH))
	log.Info(fmt.Sprintf("operator-sdk Version: %v", sdkVersion.Version))
	log.Info(fmt.Sprintf("EnMasse Version: %v", version.Version))
	log.Info(fmt.Sprintf("OpenShift?: %v", util.IsOpenshift()))
}
