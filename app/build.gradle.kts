plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutlibraries.android)
}

android {
    namespace = "io.github.guest01.aldotwidget"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.guest01.aldotwidget"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.okhttp.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
// 오픈소스 라이선스 목록 (설정 → 오픈소스 라이선스). 빌드할 때 의존성과 라이선스를 모아 res/raw/aboutlibraries.json을 만든다
aboutLibraries {
    // 빌드가 인터넷 상태에 좌우되지 않게 라이선스 원문을 온라인에서 찾지 않는다 (라이브러리에 적힌 정보만)
    offlineMode = true
    collect {
        // 라이선스 원문(Apache 2.0, BSD-3, ISC)과 Gradle 의존성이 아닌 Lucide 아이콘은 여기 파일로 넣는다
        configPath = file("aboutlibraries")
    }
    export {
        // 쓰지 않는 필드는 빼서 앱 크기를 줄인다
        excludeFields.addAll("developers", "funding", "description", "scm", "organization", "website")
    }
}
