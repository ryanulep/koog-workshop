plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.exposed.core)
            implementation(libs.exposed.jdbc)
            implementation(libs.exposed.kotlin.datetime)
            implementation(libs.sqlite.jdbc)
        }
    }
}
