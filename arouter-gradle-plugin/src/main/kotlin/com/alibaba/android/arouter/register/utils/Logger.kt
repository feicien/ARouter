package com.alibaba.android.arouter.register.utils

import org.gradle.api.Project
import org.gradle.api.logging.Logger as GradleLogger

object Logger {
    private var logger: GradleLogger? = null

    fun make(project: Project) {
        logger = project.logger
    }

    fun i(info: String?) {
        if (info != null && logger != null) {
            logger!!.info("ARouter::Register >>> $info")
        }
    }

    fun e(error: String?) {
        if (error != null && logger != null) {
            logger!!.error("ARouter::Register >>> $error")
        }
    }

    fun w(warning: String?) {
        if (warning != null && logger != null) {
            logger!!.warn("ARouter::Register >>> $warning")
        }
    }
}
