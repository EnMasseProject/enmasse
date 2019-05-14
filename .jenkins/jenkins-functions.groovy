def storeArtifacts(String artifactDir) {
    sh "./systemtests/scripts/store_kubernetes_info.sh '${artifactDir}/openshift-info/' || true"
    sh "./systemtests/scripts/collect_logs.sh '/tmp/testlogs' '${artifactDir}/openshift-logs' || true"
    sh 'rm -rf /tmp/testlogs'
}

def tearDownOpenshift() {
    echo "tear down openshift"
    sh 'sudo ./systemtests/scripts/teardown-openshift.sh'
}

def installOCclient() {
    echo "install oc client"
    sh './systemtests/scripts/install-oc-client.sh'
}

def makeLinePlot() {
    plot csvFileName: 'duration_sum_report.csv',
            csvSeries: [[
                                file: 'artifacts/openshift-logs/logs/timeMeasuring/duration_sum_report.csv',
                                exclusionValues: '',
                                displayTableFlag: false,
                                inclusionFlag: 'OFF',
                                url: '']],
            group: 'TimeReport',
            title: 'Sum of test operations',
            style: 'line',
            exclZero: false,
            keepRecords: false,
            logarithmic: false,
            numBuilds: '',
            useDescr: false,
            yaxis: '',
            yaxisMaximum: '',
            yaxisMinimum: ''
}

def makeStackedPlot() {
    plot csvFileName: 'duration_sum_report.csv',
            csvSeries: [[
                                file: 'artifacts/openshift-logs/logs/timeMeasuring/duration_sum_report.csv',
                                exclusionValues: '',
                                displayTableFlag: false,
                                inclusionFlag: 'OFF',
                                url: '']],
            group: 'TimeReport',
            title: 'Sum of test operations (stacked)',
            style: 'stackedArea',
            exclZero: false,
            keepRecords: false,
            logarithmic: false,
            numBuilds: '',
            useDescr: false,
            yaxis: '',
            yaxisMaximum: '',
            yaxisMinimum: ''
}

def runSystemtests(String coreDir, String profile, String testCases) {
    sh "sudo ./systemtests/scripts/enable_core_dumps.sh ${coreDir}"
    sh "./systemtests/scripts/run_test_component.sh '${profile}' '${testCases}'"
}

def startOpenshift() {
    sh './systemtests/scripts/setup-openshift.sh "systemtests"'
    sh 'sudo chmod -R 777 /var/lib/origin/ || true'
}

def waitUntilAgentReady() {
    sh "./systemtests/scripts/wait_until_agent_ready.sh /tmp/agent_ready"
}

def buildEnmasse() {
    sh 'make'
    sh 'make docker_build docker_tag'
}

def postGithubPrComment(def file) {
    echo "Posting github comment"
    def repository_url = scm.userRemoteConfigs[0].url
    def repository_name = repository_url.replace("https://github.com/","").replace(".git","")
    echo "Going to run curl command"
    withCredentials([string(credentialsId: 'enmasse-ci-github-token', variable: 'GITHUB_TOKEN')]) {
        sh "curl -v -H \"Authorization: token ${GITHUB_TOKEN}\" -X POST -H \"Content-type: application/json\" -d \"@${file}\" \"https://api.github.com/repos/${repository_name}/issues/${ghprbPullId}/comments\" > out.log 2> out.err"
        def output=readFile("out.log").trim()
        def output_err=readFile("out.err").trim()
        echo "curl output=$output output_err=$output_err"
    }
}

def postAction(String coresDir, String artifactDir, String debug) {
    if (debug == 'false') {
        sh "sudo unlink ./go/src/github.com/enmasseproject/enmasse || true"
        def status = currentBuild.result
        storeArtifacts(artifactDir)
        makeLinePlot()
        makeStackedPlot()
        //store test results from build and system tests
        junit testResults: '**/target/**/TEST-*.xml', allowEmptyResults: true
        //archive test results and openshift logs
        archiveArtifacts '**/target/**/TEST-*.xml'
        archiveArtifacts 'templates/build/**'
        sh "sudo ./systemtests/scripts/compress_core_dumps.sh ${coresDir} ${artifactDir}"
        sh "sudo ./systemtests/scripts/wait_until_file_close.sh ${artifactDir}"
        try {
            archiveArtifacts "${artifactDir}/**"
        } catch (all) {
            echo "Archive failed"
        } finally {
            echo "Artifacts are stored"
        }
        if (status == null) {
            currentBuild.result = 'SUCCESS'
        }
        sh "./systemtests/scripts/check_and_clear_cores.sh ${coresDir}"
    }
}

def installEnmasse(String tag, Boolean skipDependencies, Boolean upgrade, Boolean generateTemplates, Boolean installIoT = false) {
    if (generateTemplates) {
        sh "make -C templates clean"
        sh 'make templates || true'
    }
    sh "DEPLOY_IOT=${installIoT} ./systemtests/scripts/deploy_enmasse.sh false 'templates/build/enmasse-${tag}' ${skipDependencies} ${upgrade}"
}

def sendMail(address, jobName, buildUrl) {
    mail to:"${address}", subject:"EnMasse build of job ${jobName} has failed", body:"See ${buildUrl}"
}

return this
