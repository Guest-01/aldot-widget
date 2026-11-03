package io.github.guest01.aldotwidget.data

import io.github.guest01.aldotwidget.data.api.AldotException
import java.io.IOException

/**
 * 화면에 보일 오류 문장 (해요체, 홈 안내 카드에 들어가게 짧게).
 * 통신 라이브러리나 서버의 원래 메시지("timeout", "Unable to resolve host…")는 개발자용이라 로그에만 남긴다.
 */
object ErrorText {
    const val OFFLINE = "인터넷에 연결되어 있지 않아요"
    const val UNREACHABLE = "알닷 서버에 연결하지 못했어요"
    const val NO_LINE = "조회할 회선이 없어요"

    /** @param online 실패한 순간 인터넷이 연결돼 있었는지. 꺼져 있으면 서버 탓이 아니라고 알려 준다 */
    fun of(e: IOException, online: Boolean): String = when {
        e is AldotException.NoLine -> NO_LINE
        // 서버 오류는 알닷에 문의하거나 제보할 때 쓰도록 결과 코드를 작게 붙인다
        e is AldotException.Server -> "알닷 서버에 문제가 있어요 (${e.resultCode ?: "HTTP ${e.httpCode}"})"
        !online -> OFFLINE
        else -> UNREACHABLE
    }
}
