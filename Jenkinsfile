pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
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

        stage('Backend: build and test') {
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

        stage('Docker: build images') {
            steps {
                sh 'docker build -t ${BACKEND_IMAGE}:${BUILD_NUMBER} -t ${BACKEND_IMAGE}:latest backend/exam-backend'
                sh 'docker build -t ${FRONTEND_IMAGE}:${BUILD_NUMBER} -t ${FRONTEND_IMAGE}:latest frontend'
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
