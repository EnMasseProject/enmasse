#!/usr/bin/env groovy
pipeline {
    agent {
        node {
            label 'enmasse'
        }
    }
    environment {
        STANDARD_JOB_NAME = 'enmasse-master-standard'
        BROKERED_JOB_NAME = 'enmasse-master-brokered'
        PLANS_JOB_NAME = 'enmasse-master-common'
        MAILING_LIST = credentials('MAILING_LIST')
    }
    parameters {
        string(name: 'CLEAN_REGISTRY', defaultValue: 'true', description: 'clean registry')
        string(name: 'REGISTRY_AGE', defaultValue: '20', description: 'registry older then REGISTRY_AGE (in hours) will be removed')
        string(name: 'MAILING_LIST', defaultValue: env.MAILING_LIST, description: 'mailing list when build failed')
    }
    options {
        timeout(time: 1, unit: 'HOURS')
    }
    stages {
        stage('wait for agent ready') {
            environment {
                EXPECTED_DIR = '/tmp/agent_ready'
            }
            steps {
                sh "./systemtests/scripts/wait_until_agent_ready.sh ${env.EXPECTED_DIR}"
            }
        }
        stage('cleanup registry') {
            environment {
                REGISTRY_URL = credentials('internal-registry')
                DOCKER_CREDENTIALS = credentials('docker-registry-credentials')
                DOCKER_PASS = "${env.DOCKER_CREDENTIALS_PSW}"
                DOCKER_USER = "${env.DOCKER_CREDENTIALS_USR}"
            }
            when {
                expression { params.CLEAN_REGISTRY == 'true' }
            }
            steps {
                sh "./systemtests/scripts/reg_cleaner.sh ${params.REGISTRY_AGE} ${env.DOCKER_PASS} ${env.REGISTRY_URL}"
            }
        }
        stage('clean') {
            steps {
                cleanWs()
                sh 'docker stop $(docker ps -q) || true'
                sh 'docker rm $(docker ps -a -q) -f || true'
                sh 'docker rmi $(docker images -q) -f || true'
            }
        }
        stage('checkout') {
            steps {
                checkout scm
                sh 'echo $(git log --format=format:%H -n1) > actual-commit.file'
                sh 'git submodule update --init --recursive'
                sh 'rm -rf artifacts && mkdir -p artifacts'
            }
        }
        stage('build') {
            steps {
                withCredentials([string(credentialsId: 'docker-registry-host', variable: 'DOCKER_REGISTRY')]) {
                    sh 'MOCHA_ARGS="--reporter=mocha-junit-reporter" COMMIT=$BUILD_TAG make'
                }
            }
        }
        stage('push docker image') {
            steps {
                withCredentials([string(credentialsId: 'docker-registry-host', variable: 'DOCKER_REGISTRY'), usernamePassword(credentialsId: 'docker-registry-credentials', passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                    sh 'TAG=$BUILD_TAG COMMIT=$BUILD_TAG make docker_tag'
                    sh '$DOCKER login -u $DOCKER_USER -p $DOCKER_PASS $DOCKER_REGISTRY'
                    sh 'TAG=$BUILD_TAG COMMIT=$BUILD_TAG make docker_push'
                }
            }
        }
        stage('execute brokered') {
            environment {
                ACTUAL_COMMIT = readFile('actual-commit.file')
            }
            steps {
                build job: env.BROKERED_JOB_NAME, wait: false, parameters:
                        [
                                [$class: 'StringParameterValue', name: 'BUILD_TAG', value: BUILD_TAG],
                                [$class: 'StringParameterValue', name: 'MAILING_LIST', value: params.MAILING_LIST],
                                [$class: 'StringParameterValue', name: 'TEST_CASE', value: 'brokered.**'],
                                [$class: 'StringParameterValue', name: 'COMMIT_SHA', value: env.ACTUAL_COMMIT],
                        ]
            }
        }
        stage('execute standard') {
            environment {
                ACTUAL_COMMIT = readFile('actual-commit.file')
            }
            steps {
                build job: env.STANDARD_JOB_NAME, wait: false, parameters:
                        [
                                [$class: 'StringParameterValue', name: 'BUILD_TAG', value: BUILD_TAG],
                                [$class: 'StringParameterValue', name: 'MAILING_LIST', value: params.MAILING_LIST],
                                [$class: 'StringParameterValue', name: 'TEST_CASE', value: 'standard.**'],
                                [$class: 'StringParameterValue', name: 'COMMIT_SHA', value: env.ACTUAL_COMMIT],
                        ]
            }
        }
        stage('execute common') {
            environment {
                ACTUAL_COMMIT = readFile('actual-commit.file')
            }
            steps {
                build job: env.PLANS_JOB_NAME, wait: false, parameters:
                        [
                                [$class: 'StringParameterValue', name: 'BUILD_TAG', value: BUILD_TAG],
                                [$class: 'StringParameterValue', name: 'MAILING_LIST', value: params.MAILING_LIST],
                                [$class: 'StringParameterValue', name: 'TEST_CASE', value: 'common.**'],
                                [$class: 'StringParameterValue', name: 'COMMIT_SHA', value: env.ACTUAL_COMMIT],
                        ]
            }
        }
    }
    post {
        always {
            //store test results from build and system tests
            junit '**/TEST-*.xml'

            //archive test results and openshift lofs
            archive '**/TEST-*.xml'
            archive 'artifacts/**'
            archive 'templates/install/**'
        }
        failure {
            echo "build failed"
            mail to: "$MAILING_LIST", subject: "EnMasse master build has finished with ${result}", body: "See ${env.BUILD_URL}"
        }
    }
}
