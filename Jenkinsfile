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
    trigger {
        // nightly deploy job
        cron('H H * * *')
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
                not { triggeredBy "TimerTrigger" }
            }
            steps {
                sh 'mvn install -DskipTests'
            }
        }
        stage('Deploy') {
            when {
                allOf {
                    branch master
                    triggeredBy "TimerTrigger"
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
