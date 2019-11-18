module github.com/enmasseproject/enmasse

go 1.12

// Replacements as per https://github.com/operator-framework/operator-sdk/blob/master/doc/migration/version-upgrade-guide.md#modules-3
// Pinned to kubernetes-1.14.1
replace (
	k8s.io/api => k8s.io/api v0.0.0-20190409021203-6e4e0e4f393b
	k8s.io/apiextensions-apiserver => k8s.io/apiextensions-apiserver v0.0.0-20190409022649-727a075fdec8
	k8s.io/apimachinery => k8s.io/apimachinery v0.0.0-20190404173353-6a84e37a896d
	k8s.io/apiserver => k8s.io/apiserver v0.0.0-20190409021813-1ec86e4da56c
	k8s.io/cli-runtime => k8s.io/cli-runtime v0.0.0-20190409023024-d644b00f3b79
	k8s.io/client-go => k8s.io/client-go v11.0.1-0.20190409021438-1a26190bd76a+incompatible
	k8s.io/cloud-provider => k8s.io/cloud-provider v0.0.0-20190409023720-1bc0c81fa51d
	k8s.io/code-generator => k8s.io/code-generator v0.0.0-20190311093542-50b561225d70
	k8s.io/kube-aggregator => k8s.io/kube-aggregator v0.0.0-20190409022021-00b8e31abe9d
	k8s.io/kube-openapi => k8s.io/kube-openapi v0.0.0-20190510232812-a01b7d5d6c22
	k8s.io/kubernetes => k8s.io/kubernetes v1.14.1
)

require (
	github.com/go-logfmt/logfmt v0.4.0 // indirect
	github.com/go-logr/logr v0.1.0
	github.com/go-openapi/validate v0.18.0 // indirect
	github.com/google/uuid v1.1.1
	github.com/openshift/api v3.9.1-0.20190813152110-b5570061b31f+incompatible
	// release-4.2
	github.com/openshift/client-go v0.0.0-20190813201236-5a5508328169
	github.com/operator-framework/operator-sdk v0.12.0
	go.uber.org/multierr v1.1.0
	google.golang.org/genproto v0.0.0-20181016170114-94acd270e44e // indirect
	gopkg.in/yaml.v2 v2.2.2
	k8s.io/api v0.0.0
	k8s.io/apimachinery v0.0.0
	k8s.io/client-go v11.0.0+incompatible
	k8s.io/klog v0.3.3
	k8s.io/kube-aggregator v0.0.0-20190404125450-f5e124c822d6
	sigs.k8s.io/controller-runtime v0.3.0
)
