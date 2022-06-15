package com.alibaba.android.arouter.register.launch

import com.alibaba.android.arouter.register.core.ARouterRegisterTask
import com.alibaba.android.arouter.register.utils.Logger
import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project

class PluginLaunch : Plugin<Project> {

    override fun apply(project: Project) {
        val isApp = project.plugins.hasPlugin("com.android.application")
        if (isApp) {
            Logger.make(project)
            Logger.i("Project enable arouter-register plugin")

            val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
            androidComponents.onVariants(androidComponents.selector().all()) { variant ->
                val taskProvider = project.tasks.register("register${variant.name}ARouter", ARouterRegisterTask::class.java)

                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(taskProvider)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        ARouterRegisterTask::allJars,
                        ARouterRegisterTask::allDirs,
                        ARouterRegisterTask::output
                    )
            }
        }
    }
}
