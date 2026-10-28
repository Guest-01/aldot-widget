package io.github.guest01.aldotwidget.data.api

import kotlinx.serialization.Serializable

// 알닷 API 응답 형식. 필요한 필드만 선언하고 나머지는 무시한다 (Json.ignoreUnknownKeys).
// 서버가 필드를 빼거나 null로 보내도 죽지 않도록 전부 nullable + 기본값으로 둔다.

@Serializable
data class ResultHeader(
    val resultCode: String? = null,
    val resultMsg: String? = null,
)

@Serializable
data class LineInfoResponse(
    val resultCode: String? = null,
    val resultMsg: String? = null,
    val entrSvcList: List<LineDto>? = null,
)

@Serializable
data class LineDto(
    val entrId: String? = null,
    val xuserId: String? = null,
    /** 마스킹된 전화번호, 예: 010-12**-34** */
    val encnTlno: String? = null,
    /** 파트너사 이름, 예: 티플러스 */
    val mvnopPtnNm: String? = null,
    /** 요금제 이름 */
    val ppNm: String? = null,
)

@Serializable
data class RealTimeResponse(
    val resultCode: String? = null,
    val resultMsg: String? = null,
    val firstDay: String? = null,
    val lastDay: String? = null,
    val remainDay: Int? = null,
    val useDataList: List<UsageItemDto>? = null,
    val useVoicList: List<UsageItemDto>? = null,
    val useSmsList: List<UsageItemDto>? = null,
)

/**
 * 단위는 리스트마다 다르다: 데이터 = KB, 음성 = 초, 문자 = 건.
 * 숫자가 문자열로 온다.
 */
@Serializable
data class UsageItemDto(
    val itemName: String? = null,
    val svcTypCd: String? = null,
    val svcUnitCd: String? = null,
    val alloValue: String? = null,
    val useValue: String? = null,
)
