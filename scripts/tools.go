// +build tools

package tools

// Keeps the code generator dependency known to Go modules (otherwise go mod tidy will remove them)
// https://github.com/golang/go/issues/27899

import (
	_ "k8s.io/code-generator/cmd/client-gen"
	_ "k8s.io/code-generator/cmd/deepcopy-gen"
	_ "k8s.io/code-generator/cmd/defaulter-gen"
	_ "k8s.io/code-generator/cmd/informer-gen"
	_ "k8s.io/code-generator/cmd/lister-gen"
)
