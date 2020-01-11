pipeline {
    agent {
        docker {
            image 'junkdog/mvn-3-jdk8'
            // -u root for /root/.m2 to be resolved
            args '-v /root/.m2:/root/.m2 -u root'
        }
    }
    environment {
        VERSION = readMavenPom().getVersion()
        IS_SNAPSHOT = readMavenPom().getVersion().endsWith("-SNAPSHOT")
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
                echo "Building ${env.VERSION}"
            }
        }
        stage ('Build and Test') {
            when {
                // integration test modules don't update versions with
                // the maven release plugin; this is done in a separate
                // commit, so skipping the tests until releases are done
                // through this pipeline
                environment name: 'IS_SNAPSHOT', value: 'YES'
            }
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
        stage ('Build (skip tests)') {
            when {
                environment name: 'IS_SNAPSHOT', value: 'NO'
            }
            steps {
                sh 'mvn clean integration-test -DskipTests -B'
            }
            post {
                archiveArtifacts allowEmptyArchive: true, artifacts: '*/target/*.jar'
            }
        }
        stage('Deploy -SNAPSHOT') {
            when {
                allOf {
                    branch 'develop'
                    triggeredBy 'TimerTrigger'
                }
            }
            steps {
                sh 'mvn clean deploy -DskipTests -B'
            }
        }
    }
}
