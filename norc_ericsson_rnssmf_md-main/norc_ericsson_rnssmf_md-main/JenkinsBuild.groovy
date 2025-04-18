
import java.util.*
import groovy.util.XmlParser



@NonCPS
String getVersion(String pomFile) {
	def versionMatcher=pomFile=~'<version>(.+)</version>'
	def version=versionMatcher[0][1]
	version
}

@NonCPS
String getArtifactId(String pomFile) {
	def artifactIdMatcher=pomFile=~'<artifactId>(.+)</artifactId>'
	def artifactId=artifactIdMatcher[0][1]
	artifactId				
}

@NonCPS
String getGroupId(String pomFile) {
	def groupIdMatcher=pomFile=~'<groupId>(.+)</groupId>'
	def groupId=groupIdMatcher[0][1]
	groupId			
}



pipeline {
     
	agent {
    kubernetes {
      label "nova-dos-${cto.devops.jenkins.Utils.getTimestamp()}"
      inheritFrom 'k8s-dind'
      containerTemplate {
        name 'mvn'
        image "registry1-docker-io.${ARTIFACTORY_FQDN}/maven:3.8.5-openjdk-17"
        workingDir '/home/jenkins'
        ttyEnabled true
        command 'cat'
        args ''
      }
    }
  }

      options {
        timestamps()
        timeout(time: 30, unit: "MINUTES")
		buildDiscarder(logRotator(daysToKeepStr: '20', artifactDaysToKeepStr: '1', numToKeepStr: '50'))
		disableConcurrentBuilds()
    }

    environment {

		
		ARTIFACTORY_KEY = credentials('nswps-artifactory')
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        DOMAIN = "BADOP"
        GIT_USER_ID = "ca_nswps_ace"
        ARTIFACTORY_BUILD_LOGIN = "nswps-artifactory"
        RELEASE_DATE = new Date().format('yy.M')
		EMAIL_RECEIPT = "${email_recipients}"
		BUILD_TIMESTAMP="${cto.devops.jenkins.Utils.getTimestamp()}"
		BUILD_VERSION=""
		PROJECT_KEY=sh(script: "echo ${env.JOB_NAME} |rev |cut -d '/' -f2|rev", returnStdout: true).trim()
    }

    
    stages {

	// Clone source code repo
		stage ("Clone Source Code") {
			steps {
			
			    
			        
					script {
					    //def url = "${source_code_url}"
						currentBuild.description = "NOrC_MD_build"
					}
					sh "echo \"${GIT_USER_ID} Checkout URL ${env.source_code_url} Branch ${env.source_code_branch}\""
					git credentialsId: "${GIT_USER_ID}", url: "${env.source_code_url}", branch: "${env.source_code_branch}"
					sh 'pwd ;ls -rlt'
			}
		}
       
	stage ("SonarQube preview")
		{
			steps {
					echo "Testing.."
					
		
			}
		}

		stage ("Publish SonarQube analysis")
		{
			steps {
					echo "Testing.."
					
		
			}
		}


    		stage ("Build MD Package") {	
		
			environment {
				
				TARGET_GID="${DOMAIN}.${PROJECT_KEY}"
		    }
			
            steps {
						
					
					script {
					    sh "printenv"
							 sh "pwd"
						dir("${pom_file_directory}") {
						 sh "pwd"
							
					
							
							def pomFile= readFile(file: 'pom.xml')
							def artifactVersion=getVersion(pomFile)
							def groupId=getGroupId(pomFile)
							def artifactId=getArtifactId(pomFile)
							groupIdPath=groupId.replace("\\.","/")
							def URL="https://repo.lab.pl.alcatel-lucent.com/artifactory/"+"${target_repository_type}"
							def releaseRepositoryId="${target_repository_type}"
					 
					        if ("nswps-mvn-candidates" == releaseRepositoryId){
															
							
								releaseVersion = artifactVersion.replaceAll("-SNAPSHOT","")
							
							} else {
								releaseVersion=artifactVersion;
							
							}
					        sh "mvn -U -s settings.xml -f pom.xml install"
							
							
							sh "mvn -X -U -s settings.xml -f pom.xml deploy:deploy-file -DrepositoryId=${releaseRepositoryId} -Durl=${URL} -Dfile=${pwd()}/${artifactId}-${artifactVersion}.zip -DgroupId=${groupId} -DartifactId=${artifactId} -Dpackaging=zip -Dversion=${releaseVersion} -DgeneratePom.description=RELEASE"
							
							}
			
					}
                }
		}
//		stage('Xray scan') {
//			steps {
//            invokeXrayScan('nswps-artifactory', false)
//			}
//		}		
    }
  post {

        success {
                 sh "echo 'success'"

				 script {
					def body = """
						${currentBuild.projectName}: Build Execution Success
						Project Name:   ${currentBuild.projectName}
						Build Location: ${currentBuild.absoluteUrl}
						Build Status:   ${currentBuild.currentResult}
						Build Duration: ${currentBuild.durationString}
				    """
					emailext (to:"${EMAIL_RECEIPT}", subject:"Success: ${currentBuild.fullDisplayName}", body: body)
				 }
        }
		
        failure {
                 sh "echo 'Failure'"
                 script {
				 
				
					def body = """
						${currentBuild.projectName}: Build Execution Failed
						Project Name:   ${currentBuild.projectName}
						Build Location: ${currentBuild.absoluteUrl}
						Build Status:   ${currentBuild.currentResult}
						Build Duration: ${currentBuild.durationString}
					"""
				 
					emailext (to:"${EMAIL_RECEIPT}", subject:"FAILURE: ${currentBuild.fullDisplayName}", body: body)
				}
        }
		
        always {
			
            deleteDir()
            cleanWs()
        }
    }
    }

