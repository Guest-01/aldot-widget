package io.github.guest01.aldotwidget.data

import io.github.guest01.aldotwidget.data.api.AldotJson
import io.github.guest01.aldotwidget.data.api.LineDto
import io.github.guest01.aldotwidget.data.api.RealTimeResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class UsageMappingTest {

    // 2026-10-26 실제 응답에서 식별값만 지운 예시
    private val sample = """
        {"resultCode":"0000","resultMsg":"정상 처리되었습니다.","esimYn":null,
         "firstDay":"2026.10.01","lastDay":"2026.10.26","remainDay":5,
         "useDataList":[{"order":1,"svcTypCd":"PT","svcUnitCd":"U","alloValue":"3044253","useValue":"2192504","itemName":"월 기본 제공 데이터","qos":false}],
         "useVoicList":[{"order":2,"svcTypCd":"VM","svcUnitCd":"M","alloValue":"3484","useValue":"297","itemName":"음성통화"}],
         "useSmsList":[{"order":0,"svcTypCd":"SS","svcUnitCd":"O","alloValue":"59","useValue":"0","itemName":"문자"}],
         "useRingList":null,"overDataValue":null}
    """.trimIndent()

    private val line = LineDto(entrId = "1", xuserId = "2", encnTlno = "010-12**-34**", mvnopPtnNm = "티플러스", ppNm = "요금제")

    @Test
    fun `실제 응답을 스냅샷으로 바꾼다`() {
        val snapshot = AldotJson.decodeFromString<RealTimeResponse>(sample)
            .toSnapshot(line, Instant.EPOCH)

        assertEquals(Quota(total = 3044253, used = 2192504), snapshot.data)
        assertEquals(Quota(total = 3484, used = 297), snapshot.voice)
        assertEquals(Quota(total = 59, used = 0), snapshot.sms)
        assertEquals(5, snapshot.remainDays)
        assertEquals("2026.10.01", snapshot.periodStart)
    }

    @Test
    fun `여러 항목은 합산하고 숫자가 아닌 항목은 건너뛴다`() {
        val json = """
            {"useDataList":[
              {"alloValue":"1000","useValue":"100"},
              {"alloValue":"500","useValue":"50"},
              {"alloValue":"무제한","useValue":"1"}
            ]}
        """.trimIndent()
        val snapshot = AldotJson.decodeFromString<RealTimeResponse>(json).toSnapshot(line, Instant.EPOCH)

        assertEquals(Quota(total = 1500, used = 150), snapshot.data)
        assertNull(snapshot.voice)
    }

    @Test
    fun `사이트와 같은 표기로 바꾼다`() {
        assertEquals("2.9 GB", UsageFormat.dataKb(3044253))
        assertEquals("2.1 GB", UsageFormat.dataKb(2192504))
        assertEquals("831.8 MB", UsageFormat.dataKb(3044253 - 2192504))
        assertEquals("58분 4초", UsageFormat.voiceSeconds(3484))
        assertEquals("4분 57초", UsageFormat.voiceSeconds(297))
        assertEquals("5분", UsageFormat.voiceSeconds(300))
        assertEquals("7초", UsageFormat.voiceSeconds(7))
        assertEquals("59건", UsageFormat.smsCount(59))
    }
}
