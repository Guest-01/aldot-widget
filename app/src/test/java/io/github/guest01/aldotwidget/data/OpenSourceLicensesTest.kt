package io.github.guest01.aldotwidget.data

import io.github.guest01.aldotwidget.ui.settings.reflow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenSourceLicensesTest {

    private val json = """
        {
          "libraries": [
            {"uniqueId": "com.squareup.okhttp3:okhttp", "name": "okhttp", "artifactVersion": "5.5.1", "licenses": ["Apache-2.0"]},
            {"uniqueId": "androidx.activity:activity", "name": "Activity", "artifactVersion": "1.13.0", "licenses": ["Apache-2.0"]},
            {"uniqueId": "lucide.dev", "name": "Lucide Icons", "artifactVersion": "", "licenses": ["ISC"]},
            {"uniqueId": "x:unknown", "licenses": []}
          ],
          "licenses": {
            "Apache-2.0": {"name": "Apache License 2.0", "content": "Apache text", "hash": "Apache-2.0"},
            "ISC": {"name": "ISC License", "content": "ISC text"}
          }
        }
    """.trimIndent()

    @Test
    fun `라이선스별로 묶고 많은 쪽이 먼저, 묶음 안은 이름순`() {
        val groups = licenseGroups(json)
        assertEquals(listOf("Apache License 2.0", "ISC License", "라이선스 정보 없음"), groups.map { it.name })
        assertEquals(listOf("Activity", "okhttp"), groups[0].libraries.map { it.name })
        assertEquals("Apache text", groups[0].text)
    }

    @Test
    fun `빈 버전은 없는 것으로, 이름이 없으면 아이디로`() {
        val groups = licenseGroups(json)
        assertNull(groups[1].libraries.single().version)
        assertEquals("x:unknown", groups[2].libraries.single().name)
        assertNull(groups[2].text)
    }

    @Test
    fun `원문의 80자 줄바꿈은 합치고 문단은 남긴다`() {
        val text = "   Apache License\n   Version 2.0\n\n1. Definitions.\n\n      \"License\" shall mean the terms\n      and conditions."
        assertEquals(
            "Apache License Version 2.0\n\n1. Definitions.\n\n\"License\" shall mean the terms and conditions.",
            reflow(text),
        )
    }
}
