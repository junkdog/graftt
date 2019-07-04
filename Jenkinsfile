pipeline {
    agent {
        docker {
            image 'maven:3.6.1-jdk-8'
            args '-v /root/.m2:/root/.m2'
        }
    }
    options {
        skipStagesAfterUnstable()
    }
    parameters {
        string(name: 'NIGHTLY', defaultValue: 'false', description: 'Deploys -SNAPSHOTS')
    }
    stages {
        stage ('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
            }
        }
        stage ('Build and Test') {
            steps {
                sh 'mvn integration-test'
            }
        }
        stage('Install') {
            when {
                expression { return !params.NIGHTLY.toBoolean() }
            }
            steps {
                sh 'mvn install -DskipTests'
            }
        }
        stage('Deploy') {
            when {
                allOf {
                    branch 'master'
                    expression { return params.NIGHTLY.toBoolean() }
                }
            }
            steps {
                sh 'mvn deploy -DskipTests'
            }
        }
        stage('Report') {
            steps {
                junit '**/target/surefire-reports/*.xml'
                archiveArtifacts allowEmptyArchive: true, artifacts: '*/target/*.jar'
            }
        }
    }
}
