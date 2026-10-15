pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Build') {
            steps {
                sh './mvnw -B -DskipTests compile'
            }
        }
        stage('Test') {
            steps {
                sh './mvnw -B test'
            }
        }
        stage('Coverage') {
            steps {
                sh './mvnw -B jacoco:report'
                archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: false
            }
        }
        stage('Docker Build') {
            steps {
                sh 'docker build -t ledger-api:${BUILD_NUMBER} .'
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: false
        }
    }
}
