
package com.tripq.tripq

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.tts.listener.OnServerConnectionListener

import com.tripq.app.BuildConfig
import java.util.Locale

@Composable
fun PleosTtsTestScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var status by remember {
        mutableStateOf("음성 엔진 초기화 중...")
    }

    var isInitialized by remember {
        mutableStateOf(false)
    }

    var isConnected by remember {
        mutableStateOf(false)
    }

    var isReady by remember {
        mutableStateOf(false)
    }

    var testText by remember {
        mutableStateOf("안녕하세요. 트립큐 음성 테스트입니다.")
    }

    val clientId = BuildConfig.PLEOS_CLIENT_ID
    val clientSecret = BuildConfig.PLEOS_CLIENT_SECRET

    val tts = remember {
        TextToSpeech(
            context.applicationContext,
            Mode.SERVER
        )
    }

    DisposableEffect(tts) {

        var disposed = false
        var listenerAdded = false

        val eventListener = object : EventListener {

            override fun onReady() {
                if (!disposed) {
                    status = "음성 출력 준비 완료"
                    isReady = true
                }
            }

            override fun onStart() {
                if (!disposed) {
                    status = "음성 재생 중..."
                }
            }

            override fun onUpdatedRms(rms: Double) {
                // 음량 변화 이벤트
            }

            override fun onDone() {
                if (!disposed) {
                    status = "음성 재생 완료"
                }
            }

            override fun onStop() {
                if (!disposed) {
                    status = "음성 재생 중지"
                }
            }

            override fun onError(errorType: ErrorType) {
                if (!disposed) {
                    status = "TTS 오류: $errorType"
                    isReady = false
                    android.util.Log.e(
                        "TripQTTS",
                        "TTS ErrorType = $errorType"
                    )
                }
            }
        }

        // 1. SDK 초기화
        tts.initialize(object : OnInitializeListener {

            override fun onSuccess() {

                if (disposed) return

                isInitialized = true

                // 2. 초기화 성공 후 리스너 등록
                tts.addEventListener(eventListener)
                listenerAdded = true

                // 3. 한국어 설정
                tts.setLocale(Locale.KOREA)

                // 4. 인증 정보 확인
                if (clientId.isBlank() || clientSecret.isBlank()) {
                    status = "인증 정보가 없습니다. local.properties를 확인하세요."
                    return
                }

                status = "Pleos 서버 연결 중..."

                // 5. Pleos 서버 앱 등록
                tts.registerApp(
                    clientID = clientId,
                    clientSecret = clientSecret,
                    listener = object : OnServerConnectionListener {

                        override fun onConnected() {

                            if (!disposed) {
                                isConnected = true
                                status = "서버 연결 완료! 음성 출력 가능"
                            }
                        }

                        override fun onFailed(errorType: ErrorType) {

                            if (!disposed) {
                                isConnected = false
                                isReady = false
                                status = "서버 연결 실패: $errorType"
                            }
                        }
                    }
                )
            }

            override fun onFailure(errorType: ErrorType) {

                if (!disposed) {
                    status = "SDK 초기화 실패: $errorType"
                }
            }
        })

        onDispose {

            disposed = true

            if (listenerAdded) {
                tts.removeEventListener(eventListener)
            }

            tts.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "PLEOS HYBRID VOICE TEST",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = status,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SDK 초기화: ${if (isInitialized) "완료" else "대기"}"
        )

        Text(
            text = "서버 연결: ${if (isConnected) "완료" else "대기"}"
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = testText,
            onValueChange = {
                testText = it
            },
            label = {
                Text("읽을 문장")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (isConnected && testText.isNotBlank()) {
                    isReady = false
                    tts.speak(testText)
                }
            },
            enabled = isConnected && testText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("음성 출력 테스트")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                tts.stop()
            },
            enabled = isConnected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("음성 중지")
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = onBack
        ) {
            Text("홈으로 돌아가기")
        }
    }
}
