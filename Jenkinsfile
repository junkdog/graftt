pipeline {
    agent any
//    agent {
//        docker {
//            image 'maven:3-alpine'
//            args '-v /root/.m2:/root/.m2'
//        }
//    }
    options {
        skipStagesAfterUnstable()
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
            steps {
                sh 'mvn install -DskipTests'
            }
            post {
                always {
                    // todo: kotlin-junit reports?
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
    }
}