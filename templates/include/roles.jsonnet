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
  enmasse_admin_role::
    {
      "apiVersion": "v1",
      "kind": "ClusterRole",
      "metadata": {
        "name": "enmasse-admin"
      },
      "rules": [
        {
          "apiGroups": [
            "user.openshift.io",
            ""
          ],
          "resources": [
            "users"
          ],
          "verbs": [
            "get"
          ]
        },
        {
          "apiGroups": [
            "project.openshift.io",
            ""
          ],
          "resources": [
            "projectrequests"
          ],
          "verbs": [
            "create"
          ]
        },
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
            "authentication.k8s.io"
          ],
          "resources": [
            "tokenreviews"
          ],
          "verbs": [
            "create"
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
