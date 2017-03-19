package hudson.plugins.release.pipeline.ReleaseStep.DescriptorImpl

import hudson.model.Item
import hudson.model.BuildableItemWithBuildWrappers
import hudson.model.StringParameterDefinition
import hudson.plugins.release.ReleaseWrapper;
def st = namespace('jelly:stapler')
def l = namespace('/lib/layout')
//TODO: add support of warnings
//TODO: support of folders???
l.ajax {
    def jobName = request.getParameter('job')
    if (jobName != null) {
        def contextName = request.getParameter('context')
        def context = contextName != null ? app.getItemByFullName(contextName) : null
        def project = app.getItem(jobName, context, Item)

        if (project != null) {
            if (project instanceof BuildableItemWithBuildWrappers) {
                def wrapper = project.getBuildWrappersList().get(ReleaseWrapper.class)

                if (wrapper != null) {
                    def parameterDefinitionList = wrapper.getParameterDefinitions()
                    if (parameterDefinitionList.isEmpty()) {
                        parameterDefinitionList.add(new StringParameterDefinition("RELEASE_VERSION", "", "Release version"))
                        parameterDefinitionList.add(new StringParameterDefinition("DEVELOPMENT_VERSION", "", "Next development version"))
                    }

                    table(width: '100%', class: 'parameters') {
                        for (parameterDefinition in parameterDefinitionList) {
                            tbody {
                                st.include(it: parameterDefinition, page: parameterDefinition.descriptor.valuePage)
                            }
                        }
                    }
                } else {
                    text("${project.fullDisplayName} does not have the release configuration")
                }
            } else {
                text("${project.fullDisplayName} is of wrong type and can't be released: " +
                     "(${project.class.simpleName} does not implement BuildableItemWithBuildWrappers)")
            }
        } else {
            text("No such item ${jobName}")
        }
    } else {
        text('No item specified')
    }
}
