pipeline {
  //agent { label 'jadex-jenkins-agent' }
  agent any
  stages {
	stage('Prepare') {
	  steps {
	    script {
	      def versionprops = readProperties  file: 'src/main/buildutils/jadexversion.properties'
	      echo "Defined Jadex version is ${versionprops.jadexversion_major}.${versionprops.jadexversion_minor}"
	      def gitcmd = "git describe --match \"${versionprops.jadexversion_major}.${versionprops.jadexversion_minor}.*\" --abbrev=0"
	      echo gitcmd
          def version = sh (
          	script: gitcmd,
          	returnStdout: true
          )
          echo "Tagged Jadex version is ${version}"
          currentBuild.displayName = version
	    }
	  }
	}
	stage('Build and Test') {
	  steps {
		wrap([$class: 'Xvfb']) {
		  // todo: why build hangs with distzip and javadoc?
		  sh './gradlew -Pdist=publishdists clean :applications:micro:test :platform:base:test test -x javadoc -x processSchemas --continue'
		}
	  }
	}
	stage('Dist and Docs') {
	  parallel {
		stage('Dist') {
		  steps {
			sh './gradlew -Pdist=publishdists distZips -x javadoc'
		  }
		}
		stage('HTML/PDF Docs') {
		  steps {
			sh './gradlew -b docs/mkdocs-ng/build.gradle buildDocsZip buildDocsPdf'
		  }
		}
		stage('Javadocs') {
		  steps {
			sh './gradlew -Pdist=addonjavadoc javadocZip'
		  }
		}
	  }
	}
  }
  post {
    always {
      junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
    }
  }
}