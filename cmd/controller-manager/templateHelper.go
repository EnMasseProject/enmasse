/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"bytes"
	"fmt"
	"github.com/ghodss/yaml"
	"io/ioutil"
	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
	"k8s.io/apimachinery/pkg/runtime"
	"os"
	"text/template"
)

type Parameters struct {
	Params map[string]string
}

type TemplateHelper struct {
	Parameters   Parameters
	TemplatePath string
	TemplateList []string
}

// Creates a new templates helper and populates the values for all
// templates properties. Some of them (like the hostname) are set
// by the user in the custom resource
func NewTemplateHelper(extraParams map[string]string) *TemplateHelper {

	log.Info("Installing Monitoring Resources")

	params := Parameters{Params: extraParams}

	templatePath := "./templates/"
	if _, err := os.Stat(templatePath); os.IsNotExist(err) {
		templatePath = "../templates/build/enmasse-latest/install/bundles/monitoring"
		if _, err := os.Stat(templatePath); os.IsNotExist(err) {
			panic("cannot find templates")
		}
	}

	return &TemplateHelper{
		TemplatePath: templatePath,
		TemplateList: GetTemplateList(),
		Parameters:   params,
	}
}

func GetTemplateList() []string {
	templateList := []string{
		"010-PrometheusRules-kube-metrics.yaml",
		"010-PrometheusRules-enmasse.yaml",
		"010-ServiceMonitor-enmasse.yaml",
		"010-GrafanaDashboard-brokers.yaml",
		"010-GrafanaDashboard-components.yaml",
		"010-GrafanaDashboard-routers.yaml",
	}
	return templateList
}

// load a template from a given resource name. The templates must be located
// under ./templates and the filename must be <resource-name>.yaml
func (h *TemplateHelper) loadTemplate(name string) ([]byte, error) {
	path := fmt.Sprintf("%s/%s", h.TemplatePath, name)
	tpl, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, err
	}

	parser := template.New(path)

	parsed, err := parser.Parse(string(tpl))
	if err != nil {
		return nil, err
	}

	var buffer bytes.Buffer
	err = parsed.Execute(&buffer, h.Parameters)
	if err != nil {
		return nil, err
	}

	return buffer.Bytes(), nil
}

func (h *TemplateHelper) CreateResource(template string) (runtime.Object, error) {
	tpl, err := h.loadTemplate(template)
	if err != nil {
		return nil, err
	}

	resource := unstructured.Unstructured{}
	err = yaml.Unmarshal(tpl, &resource)

	if err != nil {
		return nil, err
	}

	return &resource, nil
}
