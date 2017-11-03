node('enmasse') {
    result = 'failure'
    timeout(120) {
        catchError {
            stage ('checkout') {
                checkout scm
                sh 'git submodule update --init --recursive'
                sh 'rm -rf artifacts && mkdir -p artifacts'
            }
            stage ('build') {
                try {
                    withCredentials([string(credentialsId: 'docker-registry-host', variable: 'DOCKER_REGISTRY')]) {
                        sh 'make build_amqp_module'
                        sh 'MOCHA_ARGS="--reporter=mocha-junit-reporter" TAG=$BUILD_TAG make'
                        sh 'cat templates/install/openshift/enmasse.yaml'
                    }
                } finally {
                    junit '**/TEST-*.xml'
                }
            }
            stage ('push docker image') {
                withCredentials([string(credentialsId: 'docker-registry-host', variable: 'DOCKER_REGISTRY'), usernamePassword(credentialsId: 'docker-registry-credentials', passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                    sh 'TAG=$BUILD_TAG make docker_tag'
                    sh '$DOCKER login -u $DOCKER_USER -p $DOCKER_PASS $DOCKER_REGISTRY'
                    sh 'TAG=$BUILD_TAG make docker_push'
                }
            }
            stage('start openshift') {
                sh './systemtests/scripts/setup-openshift.sh'
                sh 'sudo chmod -R 777 /var/lib/origin/openshift.local.config'
            }
            stage('install clients'){
                sh 'sudo make client_install'
            }
            stage('system tests') {
                withCredentials([string(credentialsId: 'openshift-host', variable: 'OPENSHIFT_URL'), usernamePassword(credentialsId: 'openshift-credentials', passwordVariable: 'OPENSHIFT_PASSWD', usernameVariable: 'OPENSHIFT_USER')]) {
                    try {
                        sh 'ARTIFACTS_DIR=artifacts OPENSHIFT_PROJECT=$BUILD_TAG ./systemtests/scripts/run_test_component.sh templates/install /var/lib/origin/openshift.local.config/master/admin.kubeconfig systemtests'
                    } finally {
                        junit '**/TEST-*.xml'
                    }
                }
            }
            result = 'success'
        }
        stage('archive artifacts') {
            archive '**/TEST-*.xml'
            archive 'artifacts/**'
            archive 'templates/install/**'
        }
        stage('teardown openshift') {
            sh 'oc cluster down'
        }
        stage('notify mailing list') {
            if (result.equals("failure")) {
                mail to: "$MAILING_LIST", subject: "EnMasse build has finished with ${result}", body: "See ${env.BUILD_URL}"
            }
        }
    }
}
