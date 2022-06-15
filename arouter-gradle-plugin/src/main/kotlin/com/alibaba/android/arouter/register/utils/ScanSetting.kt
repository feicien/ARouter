package com.alibaba.android.arouter.register.utils

object ScanSetting {
    const val PLUGIN_NAME = "com.alibaba.arouter"
    const val GENERATE_TO_CLASS_NAME = "com/alibaba/android/arouter/core/LogisticsCenter"
    const val GENERATE_TO_METHOD_NAME = "loadRouterMap"

    const val INTERFACE_PACKAGE_NAME = "com/alibaba/android/arouter/facade/template/"

    const val I_ROUTE_ROOT = "${INTERFACE_PACKAGE_NAME}IRouteRoot"
    const val I_INTERCEPTOR_GROUP = "${INTERFACE_PACKAGE_NAME}IInterceptorGroup"
    const val I_PROVIDER_GROUP = "${INTERFACE_PACKAGE_NAME}IProviderGroup"

}
