
package com.tripq.tripq

import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData

private const val FUSED_LOCATION_PERMISSION =
    "pleos.car.permission.FUSED_LOCATION"

private const val GPS_TAG = "TripQ_GPS"

@Composable
fun FusedLocationTestScreen(
    onLocationReceived: (Double, Double) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    var status by remember {
        mutableStateOf("GPS 테스트 준비 완료")
    }

    var locationText by remember {
        mutableStateOf("아직 수신된 위치가 없습니다.")
    }

    var supportText by remember {
        mutableStateOf("아직 확인하지 않았습니다.")
    }

    var callbackCount by remember {
        mutableIntStateOf(0)
    }

    var nullCount by remember {
        mutableIntStateOf(0)
    }

    var isListening by remember {
        mutableStateOf(false)
    }

    var fusedLocation by remember {
        mutableStateOf<FusedLocation?>(null)
    }

    var locationListener by remember {
        mutableStateOf<FusedLocationListener?>(null)
    }

    fun startLocation() {

        if (isListening) {
            status = "이미 위치 업데이트를 요청한 상태입니다."
            return
        }

        try {
            Log.d(GPS_TAG, "GPS 시작 요청")

            status = "Pleos GPS 지원 여부 확인 중..."

            val sdk = FusedLocation(context.applicationContext)

            Log.d(GPS_TAG, "FusedLocation 객체 생성 완료")

            if (!sdk.isSupported()) {

                supportText = "지원되지 않는 환경"
                status = "이 환경에서는 FusedLocation을 지원하지 않습니다."

                Log.e(GPS_TAG, "FusedLocation 미지원 환경")

                sdk.release()
                return
            }

            supportText = "Pleos FusedLocation 지원 확인 완료"

            Log.d(GPS_TAG, "FusedLocation 지원 확인 완료")

            status = "SDK 초기화 중..."

            sdk.initialize()

            Log.d(GPS_TAG, "SDK initialize() 호출 완료")

            // 위치 접근 안내 팝업은 호출하지 않습니다.
            // applicationContext에서 호출하면 Activity context 오류가 발생할 수 있습니다.

            val listener = object : FusedLocationListener {

                override fun onChanged(location: LocationData?) {

                    Log.d(
                        GPS_TAG,
                        "앱 onChanged 호출: $location"
                    )

                    if (location == null) {

                        nullCount++

                        locationText =
                            "콜백 수신됨\n" +
                                    "위치 데이터는 null입니다.\n" +
                                    "누적 null 수신: $nullCount"

                        status = "위치 콜백은 도착했지만 좌표가 없습니다."

                        Log.d(
                            GPS_TAG,
                            "위치 데이터 null 수신: $nullCount"
                        )

                        return
                    }

                    callbackCount++

                    val latitude = location.latitude
                    val longitude = location.longitude

                    locationText =
                        "위도: $latitude\n" +
                                "경도: $longitude\n" +
                                "정상 콜백 수: $callbackCount\n" +
                                "null 수신 수: $nullCount"

                    onLocationReceived(
                        latitude,
                        longitude
                    )

                    status = "GPS 위치 수신 성공!"

                    Log.d(
                        GPS_TAG,
                        "앱 좌표 수신 성공: 위도=$latitude, 경도=$longitude"
                    )
                }

                override fun onError(exception: Exception) {

                    status =
                        "GPS 오류 발생\n" +
                                "${exception.javaClass.simpleName}: ${exception.message}"

                    Log.e(
                        GPS_TAG,
                        "앱 GPS onError 호출",
                        exception
                    )
                }
            }

            fusedLocation = sdk
            locationListener = listener

            status = "실시간 위치 업데이트 등록 중..."

            Log.d(
                GPS_TAG,
                "registerFusedLocationCallback 호출 직전"
            )

            sdk.registerFusedLocationCallback(listener)

            Log.d(
                GPS_TAG,
                "registerFusedLocationCallback 호출 완료"
            )

            isListening = true

            status = "업데이트 등록 완료. 위치 수신 대기 중..."

        } catch (e: Exception) {

            isListening = false

            status =
                "GPS 초기화 또는 등록 오류\n" +
                        "${e.javaClass.simpleName}: ${e.message}"

            Log.e(
                GPS_TAG,
                "GPS 시작 실패",
                e
            )
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                Log.d(
                    GPS_TAG,
                    "Pleos 위치 권한 허용"
                )

                startLocation()

            } else {

                status = "Pleos 위치 권한이 허용되지 않았습니다."

                Log.e(
                    GPS_TAG,
                    "Pleos 위치 권한 거부"
                )
            }
        }

    val currentFusedLocation by rememberUpdatedState(fusedLocation)
    val currentLocationListener by rememberUpdatedState(locationListener)

    DisposableEffect(Unit) {
        onDispose {
            val sdk = currentFusedLocation
            val listener = currentLocationListener

            if (sdk != null && listener != null) {
                try {
                    sdk.unregisterFusedLocationCallback(listener)
                    Log.d(GPS_TAG, "GPS 리스너 해제 완료")
                } catch (e: Exception) {
                    Log.e(GPS_TAG, "GPS 리스너 해제 오류", e)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "TripQ GPS 테스트",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Pleos FusedLocation SDK 진단"
        )

        HorizontalDivider()

        Text(
            text = "SDK 지원 상태",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = supportText)

        HorizontalDivider()

        Text(
            text = "현재 상태",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = status)

        HorizontalDivider()

        Text(
            text = "GPS 위치 정보",
            style = MaterialTheme.typography.titleMedium
        )

        Text(text = locationText)

        Button(
            onClick = {

                if (isListening) {

                    status = "이미 GPS 업데이트를 요청했습니다."

                } else {

                    val granted =
                        ContextCompat.checkSelfPermission(
                            context,
                            FUSED_LOCATION_PERMISSION
                        ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {

                        startLocation()

                    } else {

                        permissionLauncher.launch(
                            FUSED_LOCATION_PERMISSION
                        )
                    }
                }
            },
            enabled = !isListening,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GPS 연결 및 위치 수신 시작")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("뒤로 가기")
        }
    }
}
