import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifactsLoader
import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import java.security.MessageDigest
import java.time.LocalDate

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
        // 업데이트 확인은 versionCode(정수)로 새 버전인지 비교한다. 릴리스마다 1씩 올린다
        versionCode = 1
        versionName = "1.0.0"

        // 업데이트 정보 파일 주소. 공개 저장소의 "최신 릴리스" 첨부 파일을 읽는다 (GitHub API는 쓰지 않는다: IP당 시간당 60번 제한).
        // 저장소가 아직 공개 전이거나 로컬 서버로 시험할 때는 -Paldot.updateUrl=... 로 바꿔 빌드한다.
        val updateUrl = (project.findProperty("aldot.updateUrl") as String?)
            ?: "https://github.com/Guest-01/aldot-widget/releases/latest/download/update.json"
        buildConfigField("String", "UPDATE_JSON_URL", "\"$updateUrl\"")
    }

    // 릴리스 서명 키. 키 파일과 비밀번호는 저장소에 두지 않고, 이 PC 사용자 폴더의 Gradle 설정
    // (C:\Users\<사용자>\.gradle\gradle.properties)이나 환경 변수에서 읽는다. 없으면 릴리스 APK는 서명 없이 만들어지고
    // makeReleaseUpdate는 멈춘다. 설정 방법은 README의 '서명 키'.
    signingConfigs {
        val keyFile = project.findProperty("aldot.signing.storeFile") as String?
        if (keyFile != null) {
            create("release") {
                storeFile = file(keyFile)
                // 비밀번호는 보통 사용자 환경 변수 ALDOT_SIGNING_PASSWORD로 준다 (파일에 적힌 값이 있으면 그쪽이 먼저)
                storePassword = (project.findProperty("aldot.signing.storePassword") as String?)
                    ?: System.getenv("ALDOT_SIGNING_PASSWORD")
                keyAlias = (project.findProperty("aldot.signing.keyAlias") as String?) ?: "aldot"
                // 요즘 키 파일 형식(PKCS12)은 키 비밀번호가 파일 비밀번호와 같다
                keyPassword = (project.findProperty("aldot.signing.keyPassword") as String?) ?: storePassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
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
        buildConfig = true
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

/**
 * 릴리스에 올릴 파일 두 개를 만든다: aldot-widget-<버전>.apk 와 update.json.
 * 앱은 update.json으로 새 버전을 알아채고, 적힌 크기·sha256으로 받은 APK가 온전한지 확인한다.
 * 바뀐 점은 저장소 맨 위의 release-notes.txt에서 읽는다 (한 줄에 하나, 앞의 "- "는 떼어 내고 #으로 시작하는 줄은 건너뛴다).
 *
 *   ./gradlew makeReleaseUpdate  →  app/build/outputs/update/release/
 *   ./gradlew makeDebugUpdate    →  app/build/outputs/update/debug/   (에뮬레이터 시험용)
 */
abstract class MakeUpdateTask : DefaultTask() {
    @get:InputFiles abstract val apkFolder: DirectoryProperty
    @get:Internal abstract val artifactsLoader: Property<BuiltArtifactsLoader>
    @get:InputFile @get:Optional abstract val notesFile: RegularFileProperty
    @get:OutputDirectory abstract val outputDir: DirectoryProperty
    /** 릴리스인데 서명 키가 설정되지 않았는지. 서명 없는 APK는 설치도 업데이트도 안 돼서 여기서 멈춘다 */
    @get:Input abstract val signingMissing: Property<Boolean>

    @TaskAction
    fun run() {
        if (signingMissing.get()) {
            error("릴리스 서명 키가 설정되지 않았어요. README의 '서명 키'대로 gradle.properties에 aldot.signing.* 를 적어 주세요")
        }
        val built = artifactsLoader.get().load(apkFolder.get()) ?: error("APK를 찾지 못했어요")
        val output = built.elements.single()
        val versionName = output.versionName ?: error("versionName이 없어요")
        val versionCode = output.versionCode ?: error("versionCode가 없어요")
        val apkName = "aldot-widget-$versionName.apk"

        val outDir = outputDir.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()
        val apk = File(output.outputFile).copyTo(File(outDir, apkName), overwrite = true)
        val sha256 = MessageDigest.getInstance("SHA-256").digest(apk.readBytes()).joinToString("") { "%02x".format(it) }
        val notes = notesFile.orNull?.asFile?.takeIf { it.exists() }?.readLines()
            ?.map { it.trim() }?.filter { it.isNotEmpty() && !it.startsWith("#") }
            ?.map { it.removePrefix("- ").trim() }
            .orEmpty()

        val json = linkedMapOf(
            "version" to versionName,
            "versionCode" to versionCode,
            "apk" to apkName,
            "size" to apk.length(),
            "sha256" to sha256,
            "released" to LocalDate.now().toString(),
            "notes" to notes,
        )
        File(outDir, "update.json").writeText(JsonOutput.prettyPrint(JsonGenerator.Options().disableUnicodeEscaping().build().toJson(json), true) + "\n")
        logger.lifecycle("업데이트 파일: ${outDir.path} ($apkName, ${apk.length()} bytes)")
    }
}

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        tasks.register<MakeUpdateTask>("make${name}Update") {
            group = "release"
            description = "릴리스에 올릴 APK와 update.json을 만든다 (${variant.name})"
            apkFolder.set(variant.artifacts.get(SingleArtifact.APK))
            artifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            notesFile.set(rootProject.layout.projectDirectory.file("release-notes.txt"))
            outputDir.set(layout.buildDirectory.dir("outputs/update/${variant.name}"))
            signingMissing.set(variant.buildType == "release" && android.signingConfigs.findByName("release") == null)
        }
    }
}
