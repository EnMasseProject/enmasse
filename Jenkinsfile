node('enmasse') {
    currentBuild.result = 'failure'
    timeout(180) {
        catchError {
            stage ('checkout') {
                checkout scm
                sh 'git submodule update --init --recursive'
                sh 'rm -rf artifacts && mkdir -p artifacts'
            }
            stage ('build') {
                try {
                    withCredentials([string(credentialsId: 'docker-registry-host', variable: 'DOCKER_REGISTRY')]) {
                        sh 'MOCHA_ARGS="--reporter=mocha-junit-reporter" COMMIT=$BUILD_TAG make'
                        sh 'cat templates/install/openshift/enmasse.yaml'
                    }
                } finally {
                    junit '**/TEST-*.xml'
                }
            }
            stage ('push docker image') {
                withCredentials([string(credentialsId: 'docker-registry-host', variable: 'DOCKER_REGISTRY'), usernamePassword(credentialsId: 'docker-registry-credentials', passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                    sh 'TAG=$BUILD_TAG COMMIT=$BUILD_TAG make docker_tag'
                    sh '$DOCKER login -u $DOCKER_USER -p $DOCKER_PASS $DOCKER_REGISTRY'
                    sh 'TAG=$BUILD_TAG COMMIT=$BUILD_TAG make docker_push'
                }
            }
            stage('start openshift') {
                sh './systemtests/scripts/setup-openshift.sh'
                sh 'sudo chmod -R 777 /var/lib/origin/openshift.local.config'
            }
            stage('install clients'){
                sh 'sudo PATH=$PATH make client_install'
            }
            stage('install webdrivers'){
                sh 'sudo make webdriver_install'
            }
            stage('system tests') {
                withCredentials([string(credentialsId: 'openshift-host', variable: 'OPENSHIFT_URL'), usernamePassword(credentialsId: 'openshift-credentials', passwordVariable: 'OPENSHIFT_PASSWD', usernameVariable: 'OPENSHIFT_USER')]) {
                    try {
                        environment {
                            WORKSPACE = pwd()
                            PATH = "$PATH:$WORKSPACE/systemtests/web_driver"
                            DISPLAY = ':10'
                            ARTIFACTS_DIR = 'artifacts'
                            OPENSHIFT_PROJECT = "${JOB_NAME::16}${BUILD_NUMBER}"
                        }
                        sh 'Xvfb :10 -ac &'
                        sh 'PATH=$PATH:$(pwd)/systemtests/web_driver DISPLAY=:10 ARTIFACTS_DIR=artifacts OPENSHIFT_PROJECT=${JOB_NAME::16}${BUILD_NUMBER} ./systemtests/scripts/run_test_component.sh templates/install /var/lib/origin/openshift.local.config/master/admin.kubeconfig systemtests'
                        currentBuild.result = 'SUCCESS'
                    } catch(err) { // timeout reached or input false
                        echo "collect logs and archive artifacts"
                        sh 'OPENSHIFT_TEST_LOGDIR="/tmp/testlogs" ./systemtests/scripts/collect_logs.sh "artifacts"'
                        currentBuild.result = 'FAILURE'
                        throw err //to mark this stage red
                    } finally {
                       junit '**/TEST-*.xml'
                    }
                }
            }
        }
        stage('archive artifacts') {
            archive '**/TEST-*.xml'
            archive 'artifacts/**'
            archive 'templates/install/**'
        }
        stage('teardown openshift') {
            sh './systemtests/scripts/teardown-openshift.sh'
        }
        post {
            failure {
                echo "build failed"
                mail to: "$MAILING_LIST", subject: "EnMasse build has finished with ${result}", body: "See ${env.BUILD_URL}"
            }
        }
    }
}
