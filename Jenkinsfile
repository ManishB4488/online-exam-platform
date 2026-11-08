// Runs the Trivy scanner in its own container.
// The "trivy-cache" volume keeps its vulnerability database, so only the first run is slow.
def trivy(String args) {
    sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v trivy-cache:/root/.cache/ aquasec/trivy:latest ${args}"
}

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        // GitHub cannot reach a Jenkins on your own laptop,
        // so Jenkins asks GitHub for new commits every 2 minutes
        pollSCM('H/2 * * * *')
    }

    environment {
        COMPOSE_BAKE = 'false'
        DOCKER_BUILDKIT = '1'
        BACKEND_IMAGE = 'exam-backend'
        FRONTEND_IMAGE = 'exam-frontend'
    }

    stages {
        stage('Check tools') {
            steps {
                sh 'java -version'
                sh 'node --version'
                sh 'npm --version'
                sh 'docker --version'
                sh 'docker compose version'
            }
        }

        stage('Backend: build, test and coverage') {
            steps {
                dir('backend/exam-backend') {
                    sh 'sh ./mvnw -B verify'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/exam-backend/target/surefire-reports/*.xml'
                }
                success {
                    archiveArtifacts artifacts: 'backend/exam-backend/target/*.jar', fingerprint: true
                    archiveArtifacts artifacts: 'backend/exam-backend/target/site/jacoco/**', allowEmptyArchive: true
                    script {
                        // Adds up the lines from the JaCoCo report and shows the result on the build page
                        def summary = sh(returnStdout: true, script: '''awk -F, 'NR>1 {m+=$8; c+=$9} END {printf "Line coverage: %.1f%% (%d of %d lines)", 100*c/(m+c), c, m+c}' backend/exam-backend/target/site/jacoco/jacoco.csv''').trim()
                        echo summary
                        currentBuild.description = summary
                    }
                }
            }
        }

        stage('Frontend: install, lint and build') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run lint'
                    sh 'npm run build'
                }
            }
        }

        stage('Security: frontend packages') {
            steps {
                dir('frontend') {
                    // the full report, this never fails the build
                    sh 'npm audit --omit=dev || true'
                    // this fails the build only for CRITICAL problems
                    sh 'npm audit --omit=dev --audit-level=critical'
                }
            }
        }

        stage('Docker: build images') {
            steps {
                sh 'docker build -t ${BACKEND_IMAGE}:${BUILD_NUMBER} -t ${BACKEND_IMAGE}:latest backend/exam-backend'
                sh 'docker build -t ${FRONTEND_IMAGE}:${BUILD_NUMBER} -t ${FRONTEND_IMAGE}:latest frontend'
            }
        }

        stage('Security: scan Docker images') {
            steps {
                script {
                    def images = ["${env.BACKEND_IMAGE}:${env.BUILD_NUMBER}", "${env.FRONTEND_IMAGE}:${env.BUILD_NUMBER}"]
                    for (img in images) {
                        // 1. the report: every HIGH and CRITICAL problem that has a fix (never fails)
                        trivy("image --severity HIGH,CRITICAL --ignore-unfixed --exit-code 0 ${img}")
                        // 2. the gate: fails the build for CRITICAL problems that have a fix
                        trivy("image --severity CRITICAL --ignore-unfixed --exit-code 1 --quiet ${img}")
                    }
                }
            }
        }

        stage('Security: secrets in the code') {
            steps {
                script {
                    def repoUrl = env.GIT_URL ?: 'https://github.com/ManishB4488/online-exam-platform.git'
                    def ref = env.GIT_COMMIT ? "--commit ${env.GIT_COMMIT}" : '--branch main'
                    trivy("repo --scanners secret --exit-code 1 ${ref} ${repoUrl}")
                }
            }
        }

        stage('Docker: check compose file') {
            steps {
                sh 'docker compose --env-file .env.example config --quiet'
            }
        }
    }

    post {
        success {
            echo 'Build succeeded.'
        }
        failure {
            echo 'Build failed. Open the red stage and read its log.'
        }
    }
}