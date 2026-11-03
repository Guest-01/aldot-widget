package io.github.guest01.aldotwidget.data

import io.github.guest01.aldotwidget.data.api.AldotException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorTextTest {

    @Test
    fun `통신 라이브러리의 원래 메시지 대신 사람이 읽는 문장`() {
        assertEquals("알닷 서버에 연결하지 못했어요", ErrorText.of(SocketTimeoutException("timeout"), online = true))
        assertEquals("알닷 서버에 연결하지 못했어요", ErrorText.of(UnknownHostException("Unable to resolve host"), online = true))
    }

    @Test
    fun `인터넷이 꺼져 있으면 그렇다고 알려 준다`() {
        assertEquals("인터넷에 연결되어 있지 않아요", ErrorText.of(UnknownHostException("Unable to resolve host"), online = false))
        assertEquals("인터넷에 연결되어 있지 않아요", ErrorText.of(IOException(), online = false))
    }

    @Test
    fun `서버 오류는 결과 코드를 작게 붙인다`() {
        assertEquals("알닷 서버에 문제가 있어요 (N0001)", ErrorText.of(AldotException.Server(500, "N0001", "내부 메시지"), online = true))
        assertEquals("알닷 서버에 문제가 있어요 (HTTP 502)", ErrorText.of(AldotException.Server(502, null, null), online = true))
    }

    @Test
    fun `회선이 없으면 인터넷 상태와 상관없이`() {
        assertEquals("조회할 회선이 없어요", ErrorText.of(AldotException.NoLine(), online = false))
    }
}
