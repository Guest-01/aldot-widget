package io.github.guest01.aldotwidget.data

import io.github.guest01.aldotwidget.data.api.AldotJson
import kotlinx.serialization.Serializable

/**
 * 빌드할 때 AboutLibraries 플러그인이 만든 res/raw/aboutlibraries.json의 필요한 부분.
 * 라이선스 원문과 Lucide 항목은 app/aboutlibraries/ 아래 파일에서 들어온다.
 */
@Serializable
private data class LicensesFile(
    val libraries: List<LibraryEntry> = emptyList(),
    val licenses: Map<String, LicenseEntry> = emptyMap(),
)

@Serializable
private data class LibraryEntry(
    val uniqueId: String,
    val name: String? = null,
    val artifactVersion: String? = null,
    val licenses: List<String> = emptyList(),
)

@Serializable
private data class LicenseEntry(
    val name: String? = null,
    val content: String? = null,
)

/** 화면에 보일 라이브러리 한 줄 */
data class OpenSourceLibrary(val name: String, val id: String, val version: String?)

/** 같은 라이선스를 쓰는 라이브러리 묶음. 원문은 묶음마다 한 번만 보여 준다 */
data class LicenseGroup(val name: String, val text: String?, val libraries: List<OpenSourceLibrary>)

/**
 * 라이선스별로 묶는다. 라이브러리가 많은 라이선스가 먼저, 묶음 안은 이름순.
 * 라이선스가 적혀 있지 않은 라이브러리는 "라이선스 정보 없음" 묶음으로.
 */
fun licenseGroups(json: String): List<LicenseGroup> {
    val file = AldotJson.decodeFromString(LicensesFile.serializer(), json)
    return file.libraries
        .groupBy { it.licenses.firstOrNull() }
        .map { (key, libraries) ->
            val license = key?.let { file.licenses[it] }
            LicenseGroup(
                name = license?.name ?: key ?: "라이선스 정보 없음",
                text = license?.content,
                libraries = libraries
                    .map { OpenSourceLibrary(it.name ?: it.uniqueId, it.uniqueId, it.artifactVersion?.ifBlank { null }) }
                    .sortedBy { it.name.lowercase() },
            )
        }
        .sortedByDescending { it.libraries.size }
}
