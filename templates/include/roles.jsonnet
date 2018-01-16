{
  // Role for address administrators allowed to create/delete addresses
  address_admin_role::
    {
      "apiVersion": "v1",
      "kind": "ClusterRole",
      "metadata": {
        "name": "enmasse-address-admin"
      },
      "rules": [
        {
          "apiGroups": [
            ""
          ],
          "resources": [
            "configmaps"
          ],
          "verbs": [
            "create",
            "delete",
            "list",
            "get",
            "watch",
            "update",
            "patch"
          ]
        }
      ]
    },

  // Role for address-controller service account
  namespace_admin_role::
    {
      "apiVersion": "v1",
      "kind": "ClusterRole",
      "metadata": {
        "name": "enmasse-namespace-admin"
      },
      "rules": [
        {
          "apiGroups": [
            "",
            "user.openshift.io"
          ],
          "resources": [
            "users"
          ],
          "verbs": [
            "impersonate"
          ]
        },
        {
          "apiGroups": [
            "",
            "extensions",
            "authorization.openshift.io",
            "route.openshift.io"
          ],
          "resources": [
            "clusterrolebindings",
            "rolebindings",
            "events",
            "policybindings",
            "deployments",
            "pods",
            "configmaps",
            "routes",
            "serviceaccounts",
            "secrets",
            "services",
            "persistentvolumeclaims"
          ],
          "verbs": [
            "create",
            "delete",
            "get",
            "list",
            "patch",
            "update",
            "watch"
          ]
        },
        {
          "apiGroups": [
            "rbac.authorization.k8s.io"
          ],
          "resources": [
            "clusterrolebindings",
            "rolebindings",
          ],
          "verbs": [
            "create",
            "delete",
            "get",
            "list",
            "patch",
            "update",
            "watch"
          ]
        },
        {
          "apiGroups": [
            "authentication.k8s.io"
          ],
          "resources": [
            "tokenreviews"
          ],
          "verbs": [
            "create"
          ]
        },
        {
          "apiGroups": [
            ""
          ],
          "resources": [
            "namespaces"
          ],
          "verbs": [
            "get",
            "list",
            "watch"
          ]
        }
      ]
    },

  event_reporter_role::
    {
      "apiVersion": "v1",
      "kind": "ClusterRole",
      "metadata": {
        "name": "event-reporter"
      },
      "rules": [
        {
          "apiGroups": [
            ""
          ],
          "resources": [
            "events"
          ],
          "verbs": [
            "create",
            "get",
            "update",
            "patch"
          ]
        }
      ]
    },


  cluster_reader::
  {
    "apiVersion": "v1",
    "kind": "ClusterRole",
    "metadata": {
      "name": "cluster-reader"
    },
    "rules": [
      {
        "apiGroups": [
          ""
        ],
        "resources": [
          "pods"
        ],
        "verbs": [
          "get",
          "watch",
          "list"
        ]
      }
    ]
  }
}
