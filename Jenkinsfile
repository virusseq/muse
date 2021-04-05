def dockerRepo = "ghcr.io/cancogen-virus-seq/muse"
def gitHubRepo = "cancogen-virus-seq/muse"
def chartVersion = "0.1.0"
def commit = "UNKNOWN"
def version = "UNKNOWN"

pipeline {
    agent {
        kubernetes {
            label 'muse'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jdk
    tty: true
    image: openjdk:11
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
        privileged: true
    volumeMounts:
      - name: docker-graph-storage
        mountPath: /var/lib/docker
  - name: helm
    image: alpine/helm:2.12.3
    command:
    - cat
    tty: true
  - name: docker
    image: docker:18-git
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
  - name: docker-graph-storage
    emptyDir: {}
"""
        }
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    commit = sh(returnStdout: true, script: 'git describe --always').trim()
                }
                script {
                    version = readMavenPom().getVersion()
                }
            }
        }
        stage('Test') {
            steps {
                container('jdk') {
                    sh "./mvnw test"
                }
            }
        }
        stage('Build & Publish Develop') {
            when {
                branch "develop"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId: 'cancogen-github',
                            usernameVariable: 'GITHUB_APP',
                            passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                        sh 'docker login ghcr.io -u GITHUB_APP -p GITHUB_ACCESS_TOKEN'
                    }

                    // DNS error if --network is default
                    sh "docker build --network=host . -t ${dockerRepo}:edge -t ${dockerRepo}:${version}-${commit}"

                    sh "docker push ${dockerRepo}:${version}-${commit}"
                    sh "docker push ${dockerRepo}:edge"
                }
            }
        }

//        stage('deploy to cancogen-virus-seq-dev') {
//            when {
//                branch "develop"
//            }
//            steps {
//                build(job: "/provision/helm", parameters: [
//                    [$class: 'StringParameterValue', name: 'AP_RDPC_ENV', value: 'dev' ],
//                    [$class: 'StringParameterValue', name: 'AP_CHART_NAME', value: 'muse'],
//                    [$class: 'StringParameterValue', name: 'AP_RELEASE_NAME', value: 'muse'],
//                    [$class: 'StringParameterValue', name: 'AP_HELM_CHART_VERSION', value: "${chartVersion}"],
//                    [$class: 'StringParameterValue', name: 'AP_ARGS_LINE', value: "--set-string image.tag=${version}-${commit}" ]
//                ])
//            }
//        }

        stage('Release & Tag') {
            when {
                branch "master"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId: 'cancogen-github',
                            usernameVariable: 'GITHUB_APP',
                            passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                        sh "git tag ${version}"
                        sh "git push https://${GITHUB_APP}:${GITHUB_ACCESS_TOKEN}@github.com/${gitHubRepo} --tags"
                        sh 'docker login ghcr.io -u GITHUB_APP -p GITHUB_ACCESS_TOKEN'
                    }

                    // DNS error if --network is default
                    sh "docker build --network=host . -t ${dockerRepo}:latest -t ${dockerRepo}:${version}"

                    sh "docker push ${dockerRepo}:${version}"
                    sh "docker push ${dockerRepo}:latest"
                }
            }
        }

//        stage('deploy to cancogen-virus-seq-qa') {
//            when {
//                branch "master"
//            }
//            steps {
//                build(job: "/provision/helm", parameters: [
//                    [$class: 'StringParameterValue', name: 'AP_RDPC_ENV', value: 'qa' ],
//                    [$class: 'StringParameterValue', name: 'AP_CHART_NAME', value: 'muse'],
//                    [$class: 'StringParameterValue', name: 'AP_RELEASE_NAME', value: 'muse'],
//                    [$class: 'StringParameterValue', name: 'AP_HELM_CHART_VERSION', value: "${chartVersion}"],
//                    [$class: 'StringParameterValue', name: 'AP_ARGS_LINE', value: "--set-string image.tag=${version}" ]
//                ])
//            }
//        }
    }
}
