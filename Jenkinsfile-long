node('enmasse') {
    result = 'failure'
    timeout(600) {
        catchError {
            stage ('checkout') {
                checkout scm
                sh 'git submodule update --init --recursive'
                sh 'rm -rf artifacts && mkdir -p artifacts'
            }
            stage('start openshift') {
                sh './systemtests/scripts/setup-openshift.sh'
                sh 'sudo chmod -R 777 /var/lib/origin/openshift.local.config'
            }
            stage('deploy enmasse') {
                withCredentials([string(credentialsId: 'openshift-host', variable: 'OPENSHIFT_URL'), usernamePassword(credentialsId: 'openshift-credentials', passwordVariable: 'OPENSHIFT_PASSWD', usernameVariable: 'OPENSHIFT_USER')]) {
                    sh 'sudo chmod 777 -R ./systemtests/scripts/'
                    sh 'OPENSHIFT_PROJECT=$BUILD_TAG ./systemtests/scripts/deploy_enmasse.sh enmasse-latest /var/lib/origin/openshift.local.config/master/admin.kubeconfig'
                    sh './systemtests/scripts/wait_until_up.sh 4 $BUILD_TAG'
                }
            }
            stage('install webdrivers'){
                sh 'sudo make webdriver_install'
            }
            stage('run longtime test suite') {
                withCredentials([string(credentialsId: 'openshift-host', variable: 'OPENSHIFT_URL'), usernamePassword(credentialsId: 'openshift-credentials', passwordVariable: 'OPENSHIFT_PASSWD', usernameVariable: 'OPENSHIFT_USER')]) {
                    try {
                        sh 'Xvfb :10 -ac &'
                        sh 'PATH=$PATH:$(pwd)/systemtests/web_driver DISPLAY=:10 ARTIFACTS_DIR=artifacts OPENSHIFT_PROJECT=$BUILD_TAG ./systemtests/scripts/run_test_component.sh templates/install /var/lib/origin/openshift.local.config/master/admin.kubeconfig systemtests "marathon.*Test"'
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
            sh './systemtests/scripts/teardown-openshift.sh'
        }
        stage('notify mailing list') {
            if (result.equals("failure")) {
                mail to: "$MAILING_LIST", subject: "EnMasse build has finished with ${result}", body: "See ${env.BUILD_URL}"
            }
        }
    }
}
