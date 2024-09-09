pluginManagement {
    repositories {
        google()
        jcenter()  // 注意：jcenter() 已經被棄用，建議替換為 mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://jitpack.io") }  // 修正自定義 Maven 庫的語法
        google()
        jcenter()  // 注意：jcenter() 已經被棄用，建議替換為 mavenCentral()
    }
}

rootProject.name = "restaurant logging"
include(":app")
