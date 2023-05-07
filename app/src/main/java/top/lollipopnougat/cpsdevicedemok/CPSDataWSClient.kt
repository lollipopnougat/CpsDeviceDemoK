package top.lollipopnougat.cpsdevicedemok

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class CPSDataWSClient(url: String) : WebSocketClient(URI.create(url)) {

    private var connectedHandler: () -> Unit = { Log.i("mws", "connected") }
    private var closedHandler: () -> Unit = { Log.i("mws", "disconnected") }
    private var messageHandler: (String?) -> Unit = { msg -> Log.i("mws", msg ?: "empty") }
    private var errorHandler: (Exception?) -> Unit = { ex -> Log.e("mws", ex.toString()) }
    private var notYetConnected = true


    var onOpenAction: () -> Unit
        get() {
            return this.connectedHandler
        }
        set(value) {
            this.connectedHandler = value
        }

    var onMessageAction: (msg: String?) -> Unit
        get() {
            return this.messageHandler
        }
        set(value) {
            this.messageHandler = value
        }

    var onCloseAction: () -> Unit
        get() {
            return this.closedHandler
        }
        set(value) {
            this.closedHandler = value
        }

    var onErrorAction: (ex: Exception?) -> Unit
        get() {
            return this.errorHandler
        }
        set(value) {
            this.errorHandler = value
        }

    val isNotYetConnected: Boolean
        get() {
            return notYetConnected
        }




    override fun onOpen(handshakedata: ServerHandshake?) {
        onOpenAction()
        notYetConnected = false
    }

    override fun onMessage(message: String?) {
        onMessageAction(message)

    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        onCloseAction()
    }

    override fun onError(ex: Exception?) {
        onErrorAction(ex)
    }

}