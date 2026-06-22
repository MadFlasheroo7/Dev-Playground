package pro.jayeshseth.devcontrols

import org.junit.Test
import org.junit.Assert.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class ExampleUnitTest {
    @Test
    fun testHttpServerHeadersReading() {
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort
        
        var serverFinished = false
        var serverError: Exception? = null
        
        val serverThread = thread {
            try {
                val socket = serverSocket.accept()
                val reader = socket.getInputStream().bufferedReader()
                val requestLine = reader.readLine()
                assertEquals("GET /test HTTP/1.1", requestLine)
                
                val headersMap = mutableMapOf<String, String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.trim().isEmpty()) break
                    val colonIdx = line.indexOf(':')
                    if (colonIdx != -1) {
                        val key = line.substring(0, colonIdx).trim().lowercase()
                        val value = line.substring(colonIdx + 1).trim()
                        headersMap[key] = value
                    }
                }
                
                assertEquals("localhost:$port", headersMap["host"])
                
                val outputStream = socket.getOutputStream()
                outputStream.write("HTTP/1.1 200 OK\r\nContent-Length: 4\r\nConnection: close\r\n\r\nTEST".toByteArray())
                outputStream.flush()
                
                socket.close()
                serverFinished = true
            } catch (e: Exception) {
                serverError = e
            }
        }
        
        // Client
        val clientSocket = Socket("localhost", port)
        val writer = clientSocket.getOutputStream().bufferedWriter()
        writer.write("GET /test HTTP/1.1\r\n")
        writer.write("Host: localhost:$port\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.flush()
        
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        val responseLine = reader.readLine()
        assertEquals("HTTP/1.1 200 OK", responseLine)
        
        clientSocket.close()
        serverThread.join(5000)
        
        if (serverError != null) {
            throw serverError!!
        }
        assertTrue(serverFinished)
        serverSocket.close()
    }
}