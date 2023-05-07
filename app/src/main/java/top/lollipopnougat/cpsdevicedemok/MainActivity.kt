package top.lollipopnougat.cpsdevicedemok

//import android.content.Context
//import com.beust.klaxon.Klaxon
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import java.lang.ref.WeakReference

//import java.util.*


//const val LOCATION_PERMISSION = 1

class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION = 1
    //    private lateinit var textView: TextView
    private lateinit var latTextView: TextView
    private lateinit var lngTextView: TextView
    private lateinit var spdTextView: TextView
    private lateinit var tipView: TextView
    private lateinit var button: Button
    private lateinit var submitBtn: Button
    private lateinit var bindBtn: Button
//    private lateinit var inputBlock: TextInputEditText
//    private lateinit var checkBox: CheckBox

    //private lateinit var locationManager: LocationManager
    private lateinit var db: DBHelper

    //private var cpsClient: CPSDataWSClient? = null
    //private var bestProvider: String = LocationManager.GPS_PROVIDER
    val myHandler = MyHandler(this)
    private var isBind = false
    private var service: TimerTaskService? = null
    //private val jsonParser: Klaxon = Klaxon()

    class MyHandler(activity: MainActivity) : Handler(Looper.getMainLooper()) {
        //弱引用持有HandlerActivity , GC 回收时会被回收掉
        private val weakReference: WeakReference<MainActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun handleMessage(msg: Message) {
            val activity = weakReference.get()
            super.handleMessage(msg)
            if (null != activity) {
                when (msg.what) {
                    SERVICE_TOAST_MSG -> {
                        val msgToast = msg.data.getString(TOAST_TEXT) as String
                        activity.toast(msgToast)
                    }
                    SERVICE_MSG -> {
                        val lat = msg.data.getDouble(LAT_NAME)
                        val lng = msg.data.getDouble(LNG_NAME)
                        val spd = msg.data.getDouble(SPD_NAME)
                        activity.showLocation(lat, lng, spd)
                    }
                    SERVER_REPLY_MSG -> {
                        val text = msg.data.getString(REPLY_TEXT_NAME)
                        activity.showReply(text ?: "null")
                    }
                    SERVER_CONNECTED_MSG -> {
                        activity.connectedUIChange()
                    }
                    SERVER_DISCONNECTED_MSG -> {
                        activity.disconnectedUIChange()
                    }
                }
                //执行业务逻辑
                Log.i("myHandle", msg.toString())
            }
        }
    }

    // 连接服务用
    private var conn = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            isBind = true
            val myBinder = p1 as TimerTaskService.MyBind
            service = myBinder.service
            service!!.bindedActivityHandler = myHandler
            bindBtn.isEnabled = true
            bindBtn.text = getString(R.string.unbind_text)
            //toast("服务已连接")
            //Log.i("xiao", "ActivityA - onServiceConnected")
            //val num = service!!.getRandomNumber()
            //Log.i("xiao", "ActivityA - getRandomNumber = $num");
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBind = false
            bindBtn.text = getString(R.string.bind_text)
            toast("服务已停止")
            Log.i("123", "ActivityA - onServiceDisconnected")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        db = DBHelper(this, getVersionCode())
        //toast(db.uniqueId)
        setControlViews()
        bindMyService()
        //locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
            //requestPermissions是异步执行的
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION
            )
        } else {
            //bestProvider = locationManager.getBestProvider(
            //getLocationCriteria(), true
            //) ?: LocationManager.GPS_PROVIDER
            //showLocation(textView, locationManager)
        }

    }

    // 绑定服务
    private fun bindMyService() {
        val intent = Intent(this, TimerTaskService::class.java)
        intent.putExtra("from", "MainActivity")
        intent.putExtra("uid", db.uniqueId)
        bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    // 解绑服务
    private fun unbindMyService() {
        isBind = false
        bindBtn.text = getString(R.string.bind_text)
        //checkBox.isEnabled = true
//        inputBlock.isEnabled = true
        unbindService(conn)

    }

    // 获取设置UI控件
    private fun setControlViews() {

        //textView = findViewById(R.id.text)
        latTextView = findViewById(R.id.latText)
        lngTextView = findViewById(R.id.lngText)
        spdTextView = findViewById(R.id.spdText)
        button = findViewById(R.id.btn)
//        inputBlock = findViewById(R.id.uriInput)
        submitBtn = findViewById(R.id.submitBtn)
        bindBtn = findViewById(R.id.bindServ)
        tipView = findViewById(R.id.tip)
        //checkBox = findViewById(R.id.checkBox)

        button.setOnClickListener {
            if (service != null) {
                val srv = service!!

//                srv.initCPSClient(inputBlock.text.toString(), true)//checkBox.isChecked)

                srv.initCPSClient(getString(R.string.ws_server), true)//checkBox.isChecked)
                srv.toggleCPSConnectState()
                //checkBox.isEnabled = false
//                inputBlock.isEnabled = false
            }
        }
        bindBtn.setOnClickListener {
            if (isBind) {
                unbindMyService()
                submitBtn.isEnabled = false
                button.isEnabled = false
            }
            else {
                bindMyService()
                button.isEnabled = true
                button.text = getString(R.string.btn_con_text)
                submitBtn.text = getString(R.string.submit_text)
            }
        }

        submitBtn.setOnClickListener {
            if (service != null) {
                val srv = service!!
                if (srv.isTimerRunning) {
                    srv.stopCPSSend()
                    submitBtn.text = getString(R.string.submit_text)
                }
                else {
                    srv.startCPSSend()
                    submitBtn.text = getString(R.string.submit_off_text)
                }
            }
        }
    }

    // 获取应用版本号
    private fun getVersionCode(): Int {
        val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
        return PackageInfoCompat.getLongVersionCode(pInfo).toInt()
    }

    // GPS事件监听器
//    private val locationListener = object : LocationListener {
//        override fun onProviderDisabled(provider: String) {
//            toast("关闭了GPS")
//            textView.text = "关闭了GPS"
//        }
//
//        override fun onProviderEnabled(provider: String) {
//            toast("打开了GPS")
//            //showLocation(textView, locationManager)
//        }
//
//        override fun onLocationChanged(location: Location) {
//            toast("变化了")
//            Log.i("myinfo", "变化了")
//            //showLocation(textView, locationManager)
//        }
//
//        @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"not implemented\")"))
//        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//    }

    // GPS信息需求
//    private fun getLocationCriteria(): Criteria {
//        val criteria = Criteria()
//        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
//        criteria.accuracy = Criteria.ACCURACY_FINE
//        criteria.isSpeedRequired = true // 设置是否要求速度
//        criteria.isCostAllowed = true // 设置是否允许运营商收费
//        criteria.isBearingRequired = false // 设置是否需要方位信息
//        criteria.isAltitudeRequired = false // 设置是否需要海拔信息
//        criteria.powerRequirement = Criteria.NO_REQUIREMENT // 设置对电源的需求
//        return criteria
//    }

    // GPS信息展示到界面
//    private fun showLocation(textView: TextView, locationManager: LocationManager) {
//        val location = getLocation(locationManager)
//        textView.text = "lat: ${location?.latitude}, lng: ${location?.longitude}, speed: ${location?.speed}"
//
//        //getLocation(locationManager).toString()
//    }

    // GPS信息展示到界面
    fun showLocation(lat: Double, lng: Double, spd: Double) {
        //textView.text = "lat: ${lat}, lng: ${lng}, speed: ${spd}"
        latTextView.text = getString(R.string.lat_text_template, lat)
        lngTextView.text = getString(R.string.lng_text_template, lng)
        spdTextView.text = getString(R.string.spd_text_template, spd * 3.6)
        //getLocation(locationManager).toString()
    }

    fun showReply(text: String) {
        tipView.text = text
    }

    fun connectedUIChange() {
        submitBtn.isEnabled = true
        button.text = getString(R.string.btn_discon_text)
    }

    fun disconnectedUIChange() {
        submitBtn.isEnabled = false
        button.text = getString(R.string.btn_con_text)
        submitBtn.text = getString(R.string.submit_text)
    }

//    //获取位置信息
//    private fun getLocation(locationManager: LocationManager): Location? {
//
//        var location: Location? = null
//        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
//            toast("没有位置权限")
//        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            toast("没有打开GPS")
//        } else {
//            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//            if (location == null) {
//                toast("位置信息为空")
//                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
//                if (location == null) {
//                    toast("网络位置信息也为空")
//                } else {
//                    toast("当前使用网络位置")
//                }
//            }
//        }
//        return location
//    }

    // toast 简化调用
    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    //申请位置权限后回调，要刷新位置信息
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toast("获取了位置权限")
//                bestProvider = locationManager.getBestProvider(
//                    getLocationCriteria(), true
//                ) ?: LocationManager.GPS_PROVIDER
//
//                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener)
//                showLocation(textView, locationManager)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (hasLocationPermission == PackageManager.PERMISSION_GRANTED) {
            //locationManager.removeUpdates(locationListener)
        }
    }

    override fun onResume() {
        //挂上LocationListener, 在状态变化时刷新位置显示，因为requestPermissionss是异步执行的，所以要先确认是否有权限
        super.onResume()
        val hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (hasLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 绑定监听，有4个参数
            // 参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
            // 参数2，位置信息更新周期，单位毫秒
            // 参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息
            // 参数4，监听
            // 备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新

            // 1秒更新一次，或最小位移变化超过1米更新一次；
            // 注意：此处更新准确度非常低，推荐在service里面启动一个Thread，在run中sleep(10000);然后执行handler.sendMessage(),更新位置
            //locationManager.requestLocationUpdates(bestProvider, 1000, 0F, locationListener)
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0F, locationListener)
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000,0F, locationListener)
            //showLocation(textView, locationManager)
        }
    }

    override fun onDestroy() {
        myHandler.removeCallbacksAndMessages(null);
        unbindService(conn)
        //stopService(Intent(this, TimerTaskService::class.java))
        super.onDestroy()

    }


}