import hudson.tasks.Shell
import jenkins.model.Jenkins
import hudson.model.FreeStyleProject
import hudson.model.labels.LabelAtom

import hudson.model.ChoiceParameterDefinition
import hudson.model.StringParameterDefinition
import hudson.model.ParametersDefinitionProperty

import org.jenkinsci.plugins.scripttrigger.ScriptTrigger
import org.jenkinsci.plugins.scripttrigger.LabelRestrictionClass

def getEnv(String param, String default_param = null) {
    def envParam = System.getenv(param)
    if (envParam == null) {
        if (default_param != null) {
            return default_param
        }
        System.err.println(" --> NextCODE ERROR: Expected '$param' to be set as a environment variable in this Docker container")
        System.exit(1)
    }
    return envParam
}

// Reference data version
def refVersion = getEnv('default_reference_data_version')
def enableDnanexusAutoimport = getEnv('enable_dnanexus_autoimport').toBoolean() // from docs: "If the trimmed string is "true", "y" or "1" (ignoring case) then the result is true otherwise it is false."

def fixMito = getEnv('fix_mito_in_hg19', 'false')

def setup_project_config = getEnv('setup_project_config', '/etc/jenkins/setup_projects_config.yml')

def csaBase = getEnv('csa_base')
def apiUrl = getEnv('csa_api_url')


def environs = [
        CSA_API_ENDPOINT         : apiUrl,
        CSA_BASE                 : csaBase,
        CSA_ENV                  : getEnv('csa_env'),
        CSA_API_USER             : getEnv('csa_api_user'),
        CSA_API_PASSWORD         : getEnv('csa_api_password'),
        CREDENTIAL_SERVICE_HOST  : getEnv('CREDENTIAL_SERVICE', ''),
        GEL2CSA_ENDPOINT         : getEnv('GEL2CSA_ENDPOINT', ''),
        PIPELINE_SERVICE_HOST    : getEnv('PIPELINE_SERVICE_HOST', ''),
        PIPELINE_SERVICE_USERNAME: getEnv('PIPELINE_SERVICE_USERNAME', ''),
        PIPELINE_SERVICE_PASSWORD: getEnv('PIPELINE_SERVICE_PASSWORD', ''),
        KEYCLOAK_SERVER          : getEnv('KEYCLOAK_SERVER', ''),
        KEYCLOAK_REALM           : getEnv('KEYCLOAK_REALM', 'wuxinextcode.com')
]

// Docker parameters
def dockerEnvs = "-e BUILD_URL=\$BUILD_URL "
for (envVar in environs) {
    if (envVar.value) {
        dockerEnvs = "$dockerEnvs -e ${envVar.key}='${envVar.value}'"
    }
}

def dockerRun = "docker run -a stdout -a stderr --rm --init"

// slave Docker image
def imageName = getEnv('tertiarypipeline_docker_image_name')
def imageTag = getEnv('tertiarypipeline_docker_image_tag')
def dockerImage = "${imageName}:${imageTag}"

// base docker params for rda (data owner) user uid
def userId = getEnv("data_owner_user_uid")
def groupId = getEnv("data_owner_user_gid")

def dockerParams = "-u ${userId}:${groupId}  -v ${csaBase}:${csaBase}  -v /var/log/nextcode/tertiarypipeline:/var/log/nextcode/tertiarypipeline "

// piece togeather the docker command line with all variables needed
def docker = "$dockerRun $dockerParams $dockerEnvs $dockerImage"

//
// TERTIARY PIPELINE JOBS
//
private static void create_or_update_jenkins_job(String name, Boolean enabled, ScriptTrigger trigger, ParametersDefinitionProperty parameter, Shell command, Boolean concurrentBuild, String slaveLabel) {
    String existance_status = (enabled ? "(enabled) " : "(disabled)")
    System.out.println("--== " + existance_status + " Creating or updating job '" + name + "' ==--")

    def job = Jenkins.instance.getItem(name)
    if (job == null) {
        job = Jenkins.instance.createProject(FreeStyleProject, name)
    }

    // Overwriting Name and Label
    job.displayName = name
    job.setConcurrentBuild(concurrentBuild)
    job.assignedLabel = new LabelAtom(slaveLabel)

    // Enabling or disabling the job
    job.makeDisabled(!enabled)

    // Removing old command(s) and adding the correct one back again
    for (builder in job.getBuilders()) {
        job.buildersList.remove(builder)
    }
    if (command != null) {
        job.buildersList.add(command)
    }

    // Removing old parameter and adding it back if it should be
    for (old_parameter in job.properties.values()) {
        job.removeProperty(old_parameter)
    }
    if (parameter != null) {
        job.addProperty(parameter)
    }

    // Removing old build trigger(s) and adding the correct one back again
    for (old_trigger in job.getTriggers()) {
        job.removeTrigger(old_trigger.key)
    }
    if (trigger != null) {
        job.addTrigger(trigger)
    }

    job.save()

    // We have to reload the job as soon as it is saves, else the ScriptTrigger plugin will yield errors. It will;
    // 1) have an NullPointerException in the Jenkins logs and
    // 2) loading the /job/TertiaryPipe/scripttriggerPollLog/ will give an error
    job.doReload()
}

private static ScriptTrigger create_script_trigger(String triggerScript, String triggerLabel, String cronFrequency, Boolean concurrentBuild) {
    def labelRestriction = null
    if (triggerLabel != '') { labelRestriction = new LabelRestrictionClass(triggerLabel) }
    def concurrentBuildTrigger = concurrentBuild

    return new ScriptTrigger(
            cronFrequency,
            labelRestriction,
            concurrentBuildTrigger,
            triggerScript,
            '', // no scriptFilePath, we provide the script directly
            '' // no expectations on exit codes
    )
}


// Job (variable) constants
def cronEveryMinute = '* * * * *'
def cronEveryHour = 'H * * * *'
def cronFiveTimesAnHour = 'H/5 * * * *'
def tpipe_slave_label = "TPIPE"
def jobEnabled = true

// CSA-BE01-Setup-Projects
csa_be01_trigger = create_script_trigger("curl -k ${apiUrl}/pending_runs.txt | grep 'prepare_projects'", '', cronEveryMinute, false)
csa_be01_paramet = new ParametersDefinitionProperty(new ChoiceParameterDefinition("PROJECT_SCOPE", (String[])["preparing", "active"], "Scope of CSA projects to setup"))
csa_be01_command = new Shell("$docker /bin/bash -c \"cd src/helper_scripts; ruby setup_projects.rb ${setup_project_config} \${PROJECT_SCOPE} \"")
create_or_update_jenkins_job("CSA-BE01-Setup-Projects", jobEnabled, csa_be01_trigger, csa_be01_paramet, csa_be01_command, false, tpipe_slave_label)

// CSA-TP01-Process-Cloned-Samples
csa_tp01a_trigger = create_script_trigger("curl -k ${apiUrl}/pending_runs.txt | grep 'process_cloned_samples'", '', cronFiveTimesAnHour, false)
csa_tp01a_paramet = null
csa_tp01a_command = new Shell("$docker /bin/bash -c \"cd src/helper_scripts; ./show_pending_runs process_cloned_samples | xargs -n 1 ./process_cloned_samples \"")
create_or_update_jenkins_job("CSA-TP01-Process-Cloned-Samples", jobEnabled, csa_tp01a_trigger, csa_tp01a_paramet, csa_tp01a_command, false, tpipe_slave_label)

// CSA-TP01-Process-Uploaded-Sample-Data
csa_tp01b_trigger = create_script_trigger("curl -k ${apiUrl}/pending_runs.txt | grep 'process_uploaded_files'", '', cronFiveTimesAnHour, false)
csa_tp01b_paramet = null
csa_tp01b_command = new Shell("$docker src/helper_scripts/process_uploaded_files")
create_or_update_jenkins_job("CSA-TP01-Process-Uploaded-Sample-Data", jobEnabled, csa_tp01b_trigger, csa_tp01b_paramet, csa_tp01b_command, false, tpipe_slave_label)

// CSA-TP02-Run-Pipeline-Tasks
csa_tp02_trigger = create_script_trigger("curl -k ${apiUrl}/pending_runs.txt | grep 'process_pipeline'", '', cronEveryMinute, true)
csa_tp02_paramet = null
csa_tp02_command = new Shell("$docker python src/jenkins_runner.py")
create_or_update_jenkins_job("CSA-TP02-Run-Pipeline-Tasks", jobEnabled, csa_tp02_trigger, csa_tp02_paramet, csa_tp02_command, true, tpipe_slave_label)

// CSA-TP03-Import-from-DNAnexus
csa_tp03_trigger = create_script_trigger("$docker /bin/bash -c \"cd src; python -m helper_scripts.trigger_import_from_dnanexus\"", tpipe_slave_label, cronEveryHour, false)
csa_tp03_paramet = null
csa_tp03_command = new Shell("$docker /bin/bash -c \"cd src; python -m helper_scripts.import_from_dnanexus\"")
create_or_update_jenkins_job("CSA-TP03-Import-from-DNAnexus", enableDnanexusAutoimport, csa_tp03_trigger, csa_tp03_paramet, csa_tp03_command, false, tpipe_slave_label)

// CSA-Update-Reference-Data
csa_urd_trigger = null
csa_urd_paramet = new ParametersDefinitionProperty(new StringParameterDefinition("REFERENCE_DATA_VERSION", "${refVersion}", "<BUILD>-<ENSEMBL>-<MINOR>-<PATHCH>"))
csa_urd_command = new Shell("$docker /bin/bash -c \"cd src; python -m helper_scripts.update_reference_data \${REFERENCE_DATA_VERSION} --fix_mito ${fixMito}\"")
create_or_update_jenkins_job("CSA-Update-Reference-Data", jobEnabled, csa_urd_trigger, csa_urd_paramet, csa_urd_command, false, tpipe_slave_label)

// CSA-AT01-Setup-Installation-Tests
csa_at01_trigger = null
csa_at01_paramet = null
csa_at01_command = new Shell("$docker src/helper_scripts/setup_installation_tests.sh")
create_or_update_jenkins_job("CSA-AT01-Setup-Installation-Tests", jobEnabled, csa_at01_trigger, csa_at01_paramet, csa_at01_command, false, tpipe_slave_label)

// CSA-AT02-Check-Installation-Tests
csa_at02_trigger = null
csa_at02_paramet = null
csa_at02_command = new Shell("$docker src/helper_scripts/check_installation_tests.sh")
create_or_update_jenkins_job("CSA-AT02-Check-Installation-Tests", jobEnabled, csa_at02_trigger, csa_at02_paramet, csa_at02_command, false, tpipe_slave_label)

// CSA-AT03-Cleanup-Installation-Tests
csa_at03_trigger = null
csa_at03_paramet = null
csa_at03_command = new Shell("$docker src/helper_scripts/cleanup_installation_tests.sh")
create_or_update_jenkins_job("CSA-AT03-Cleanup-Installation-Tests", jobEnabled, csa_at03_trigger, csa_at03_paramet, csa_at03_command, false, tpipe_slave_label)
