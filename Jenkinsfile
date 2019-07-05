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
    triggers {
        // nightly deploy job
        cron('H H * * *')
    }
    stages {
        stage ('Initialize') {
            steps {
                sh "echo `whoami`"
                echo "USER = ${env.USER}"
                echo "HOME = ${env.HOME}"
                echo "PATH = ${env.PATH}"
                echo "M2_HOME = ${env.M2_HOME}"
            }
        }
        stage ('Build and Test') {
            steps {
                sh 'mvn integration-test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: '*/target/*.jar'
                }
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
                    branch 'master'
                    triggeredBy 'TimerTrigger'
                }
            }
            steps {
                sh 'mvn deploy -DskipTests'
            }
        }
    }
}
