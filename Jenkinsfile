pipeline {
    agent {
        docker {
            image 'junkdog/mvn-3-jdk8'
            // -u root for /root/.m2 to be resolved
            args '-v /root/.m2:/root/.m2 -u root'
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
                sh 'mvn clean integration-test -B'
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
                sh 'mvn clean install -DskipTests -B'
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
                sh 'mvn clean deploy -DskipTests -B'
            }
        }
    }
}
