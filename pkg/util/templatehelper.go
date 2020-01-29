/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

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

// Creates a new template helper and populates the values for all
// template properties.
func NewTemplateHelper(extraParams map[string]string) *TemplateHelper {

	params := Parameters{Params: extraParams}

	templatePath := "/templates"
	if _, err := os.Stat(templatePath); os.IsNotExist(err) {
		// ENV VAR should be set for local deployments
		templatePath = os.Getenv("TEMPLATE_DIR")
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
		"PrometheusRules-kube-metrics.yaml",
		"PrometheusRules-enmasse.yaml",
		"ServiceMonitor-enmasse.yaml",
		"GrafanaDashboard-brokers.yaml",
		"GrafanaDashboard-components.yaml",
		"GrafanaDashboard-routers.yaml",
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
