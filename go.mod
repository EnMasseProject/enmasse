module github.com/enmasseproject/enmasse

go 1.12

// Replacements as per https://github.com/operator-framework/operator-sdk/blob/master/doc/migration/version-upgrade-guide.md#modules-3
// Pinned to kubernetes-1.14.9
replace (
	k8s.io/api => k8s.io/api v0.0.0-20191004102349-159aefb8556b
	k8s.io/apiextensions-apiserver => k8s.io/apiextensions-apiserver v0.0.0-20191114015135-f299f23b335b
	k8s.io/apimachinery => k8s.io/apimachinery v0.0.0-20191004074956-c5d2f014d689
	k8s.io/apiserver => k8s.io/apiserver v0.0.0-20191114014928-cb77609d7449
	k8s.io/cli-runtime => k8s.io/cli-runtime v0.0.0-20191114015235-8eaa37d82c10
	k8s.io/client-go => k8s.io/client-go v11.0.1-0.20191029005444-8e4128053008+incompatible
	k8s.io/cloud-provider => k8s.io/cloud-provider v0.0.0-20191114015453-79225fba1e26
	k8s.io/code-generator => k8s.io/code-generator v0.0.0-20190311093542-50b561225d70
	k8s.io/kube-aggregator => k8s.io/kube-aggregator v0.0.0-20191114014954-0e3176a6f3ed
	k8s.io/kube-openapi => k8s.io/kube-openapi v0.0.0-20190510232812-a01b7d5d6c22
	k8s.io/kubernetes => k8s.io/kubernetes v1.14.9
)

require (
	github.com/Nerzal/gocloak/v3 v3.7.0
	github.com/ghodss/yaml v1.0.0
	github.com/go-logr/logr v0.1.0
	github.com/google/uuid v1.1.1
	github.com/openshift/api v0.0.0-20200106203948-7ab22a2c8316
	// release-4.2
	github.com/openshift/client-go v0.0.0-20190813201236-5a5508328169
	github.com/operator-framework/operator-sdk v0.11.0
	github.com/spf13/pflag v1.0.5
	github.com/stretchr/testify v1.4.0
	go.uber.org/multierr v1.1.0
	go.uber.org/zap v1.10.0
	gopkg.in/yaml.v2 v2.2.4
	k8s.io/api v0.17.0
	k8s.io/apimachinery v0.17.0
	k8s.io/client-go v11.0.1-0.20190409021438-1a26190bd76a+incompatible
	k8s.io/code-generator v0.17.0
	k8s.io/klog v1.0.0
	k8s.io/kube-aggregator v0.0.0-20190404125450-f5e124c822d6
	sigs.k8s.io/controller-runtime v0.2.2
)
