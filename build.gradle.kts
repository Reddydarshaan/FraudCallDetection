plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
}

task("clean", type = org.gradle.api.tasks.Delete::class) {
    delete(rootProject.buildDir)
}