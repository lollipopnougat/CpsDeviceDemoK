package top.lollipopnougat.cpsdevicedemok

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.widget.Toast
import com.beust.klaxon.Klaxon
import java.util.*


class TimerTaskService : Service() {
    private val TAG = "msrv"
    private var taskIntervalMs = 1000L
    private var cpsClient: CPSDataWSClient? = null
    private lateinit var locationManager: LocationManager
    private var uniqueId = ""
    private val jsonParser: Klaxon = Klaxon()
    private lateinit var timer: Timer
    private var timerTask: TimerTask? = null
    private var activityHandler: Handler? = null
    private var uriStore: String = ""
    private var contactStore: Boolean = false
    private var timerRunning = false
    private val onServerReplyMessage: (String?) -> Unit = { data ->
        run {
            if (activityHandler != null && data != "") {
                val obj = jsonParser.parse<ServerReply>(data ?: getString(R.string.ws_server_reply_default))

                val msg = Message.obtain()
                msg.what = SERVER_REPLY_MSG
                if (obj != null) {
                    msg.data.putString(REPLY_TEXT_NAME, obj.suggest)
                }
                else {
                    msg.data.putString(REPLY_TEXT_NAME, getString(R.string.server_reply_tip))
                }
                activityHandler!!.sendMessage(msg)
            }
        }
    }

    private val onServerConnected: () -> Unit = {
        run {
            if (activityHandler != null) {
                val msg = Message.obtain()
                msg.what = SERVER_CONNECTED_MSG
                activityHandler!!.sendMessage(msg)
            }
        }
    }

    private val onServerDisconnected: () -> Unit = {
        run {
            if (activityHandler != null) {
                val msg = Message.obtain()
                msg.what = SERVER_DISCONNECTED_MSG
                activityHandler!!.sendMessage(msg)
            }
        }
    }

    private val onServerException: (Exception?) -> Unit = {e -> run {
        //toast(e?.message ?: "出错了")
        if (e is java.net.ConnectException) {
            toast(e.message ?: "连接出错")
        }
        //toast(e!!::class.java.name)
    } }


    class MyBind(private val serv: TimerTaskService) : Binder() {
        val service: TimerTaskService
            get() = serv

    }

    // GPS事件监听器
    private val locationListener = object : LocationListener {
        override fun onProviderDisabled(provider: String) {
            toast("关闭了GPS")
            //textView.text = "关闭了GPS"
        }

        override fun onProviderEnabled(provider: String) {
            toast("打开了GPS")
            //showLocation(textView, locationManager)
        }

        override fun onLocationChanged(location: Location) {
            //toast("变化了")
            Log.i("gps", "gps变化了")
            //showLocation(textView, locationManager)
        }

        @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"not implemented\")"))
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override fun onBind(intent: Intent): IBinder {
        uniqueId = intent.getStringExtra("uid") as String
        //val am = getSystemService(ALARM_SERVICE) as AlarmManager
        //val firstTime = Date().time
        //val sender = PendingIntent()
        //am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, (5 * 1000).toLong(), sender)
        return MyBind(this@TimerTaskService)
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        Log.i(TAG, "$TAG onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "$TAG onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onDestroy() {
        if (timerRunning) {
            stopCPSSend()
        }
        if (cpsClient != null && cpsClient!!.isOpen) {
            cpsClient!!.close()
        }
        super.onDestroy()
        toast("Service销毁")
        Log.i(TAG, "$TAG onDestroy")
    }


    val isClientNotYetConnected: Boolean
        get() {
            return cpsClient?.isNotYetConnected ?: false
        }

    val isClientOpen: Boolean
        get() {
            return cpsClient?.isOpen ?: false
        }

    var taskInterval: Long
        get() {
            return taskIntervalMs
        }
        set(value: Long) {
            taskIntervalMs = if (value > 100) {
                value
            } else {
                100L
            }
        }

    var bindedActivityHandler: Handler?
        get() {
            return activityHandler
        }
        set(value: Handler?) {
            activityHandler = value
        }

    val isTimerRunning: Boolean
        get() {
            return timerRunning
        }

    fun initCPSClient(uri: String, contactId: Boolean) {
        cpsClient = if (contactId) {
            cpsClient ?: CPSDataWSClient("$uri/$uniqueId")
        } else {
            cpsClient ?: CPSDataWSClient(uri)
        }
        uriStore = uri
        contactStore = contactId
        cpsClient!!.onMessageAction = onServerReplyMessage
        cpsClient!!.onOpenAction = onServerConnected
        cpsClient!!.onCloseAction = onServerDisconnected
        cpsClient!!.onErrorAction = onServerException

    }


    fun toggleCPSConnectState(): Boolean {
        if (cpsClient != null) {
            if (isTimerRunning) {
                stopCPSSend()
            }
            val client = cpsClient!!
            try {
                if (client.isNotYetConnected) {
                    client.connect()
                    return true
                } else if (client.isOpen) {
                    client.close()
                    return false
                } else if (client.isClosing) {
                    return false
                } else {
                    client.reconnect()
                    return true
                }
            } catch (e: IllegalStateException) {
                toast(getString(R.string.restart_tip))
            }
            catch (e: Exception) {
                toast(e.message ?: e.toString())
            }
        }
        return false
    }

    fun sendCPSData(): Location? {
        var location: Location? = null
        if (cpsClient != null && cpsClient!!.isOpen) {
            location = getLocation(locationManager)
            if (location != null) {
                val car =
                    CarState(uniqueId, location.latitude, location.longitude, location.speed.toDouble(), Date().time)
                val js = jsonParser.toJsonString(car)
                cpsClient!!.send(js)
            }
        }
        return location
    }

    fun startCPSSend() {
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            toast("没有位置权限")
        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            toast("没有打开GPS")
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0F, locationListener)
        }
        timer = Timer()
        timerTask = timerTask ?: object : TimerTask() {
            override fun run() {
                val location = sendCPSData()
                if (location != null && activityHandler != null) {
                    val msg = Message.obtain()
                    msg.what = SERVICE_MSG
                    msg.data.putDouble(LAT_NAME, location.latitude)
                    msg.data.putDouble(LNG_NAME, location.longitude)
                    msg.data.putDouble(SPD_NAME, location.speed.toDouble())
                    activityHandler!!.sendMessage(msg)
                }
            }
        }
        timer.schedule(timerTask, 0, taskIntervalMs)
        timerRunning = true
        Log.i("cps", "开始发送CPS信息")
    }

    fun stopCPSSend() {
        val hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (hasLocationPermission == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener)
        }
        timer.cancel()
        timerTask = null
        timerRunning = false
        Log.i("cps", "停止发送CPS信息")
    }

    // toast 简化调用
    private fun toast(text: String) {
        //Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        if (activityHandler != null) {
            val msg = Message.obtain()
            msg.what = SERVICE_TOAST_MSG
            msg.data.putString(TOAST_TEXT, text)
            activityHandler!!.sendMessage(msg)
        }
    }

    // 申请位置权限后回调，要刷新位置信息
    private fun getLocation(locationManager: LocationManager): Location? {

        var location: Location? = null
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            toast("没有位置权限")
        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            toast("没有打开GPS")
        } else {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location == null) {
                Log.i("gps", "位置信息为空")
                //toast("位置信息为空")
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (location == null) {
                    Log.i("gps", "网络位置信息也为空")
                    //toast("网络位置信息也为空")
                } else {
                    Log.i("gps", "当前使用网络位置")
                    //toast("当前使用网络位置")
                }
            }
        }
        return location
    }

    // GPS信息需求
    private fun getLocationCriteria(): Criteria {
        val criteria = Criteria()
        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.isSpeedRequired = true // 设置是否要求速度
        criteria.isCostAllowed = true // 设置是否允许运营商收费
        criteria.isBearingRequired = false // 设置是否需要方位信息
        criteria.isAltitudeRequired = false // 设置是否需要海拔信息
        criteria.powerRequirement = Criteria.NO_REQUIREMENT // 设置对电源的需求
        return criteria
    }
}