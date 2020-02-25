/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package resolvers

import (
	"encoding/json"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/util"
	"gopkg.in/yaml.v2"
)

func generateApplyCommand(object interface{}, namespace string) (string, error) {
	var namespaceArgument string
	if namespace != "" {
		namespaceArgument = fmt.Sprintf("--namespace=%s ", namespace)
	}

	bytes, err := json.Marshal(object)
	if err != nil {
		return "", err
	}

	jsonMap := make(map[string]interface{})
	err = json.Unmarshal(bytes, &jsonMap)
	if err != nil {
		return "", err
	}

	delete(jsonMap, "status")
	delete(jsonMap["metadata"].(map[string]interface{}), "uid")
	pruneEmpty(&jsonMap)
	bytes, err = yaml.Marshal(jsonMap)

	kubectl := "kubectl"
	if util.IsOpenshift() {
		kubectl = "oc"
	}

	if err == nil {
		shell := fmt.Sprintf("%s apply %s-f - << EOF \n%s\nEOF", kubectl, namespaceArgument, string(bytes))
		return shell, nil
	} else {
		return "", err
	}
}

func pruneEmpty(m *map[string]interface{}) {
	for k, v := range *m {
		if v == nil {
			delete(*m, k)
		} else {
			switch t := v.(type) {
			case map[string]interface{}:
				pruneEmpty(&t)
				if len(t) == 0 {
					delete(*m, k)
				}
			}
		}
	}
}
