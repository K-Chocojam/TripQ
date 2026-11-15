package com.tripq.tripq

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.tripq.app.R
import com.tripq.tripq.ui.theme.TripQTheme

import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TripQTheme {
                TripQHomeScreen()
            }
        }
    }
}

// BRAIN 퀴즈 데이터 구조
data class BrainQuestion(
    val question: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String
)

// LOCAL 퀴즈 데이터 구조
private data class LocalQuestion(
    val question: String,
    val options: List<String>,
    val answer: String,
    val explanation: String
)


// 전체 화면 이동 관리
@Composable
fun TripQHomeScreen() {
    var selectedMenu by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedMode by remember { mutableStateOf<String?>(null) }

    var showGpsTest by remember { mutableStateOf(false) }
    var gpsLatitude by remember { mutableStateOf<Double?>(null) }
    var gpsLongitude by remember { mutableStateOf<Double?>(null) }

    // 앱 시작 시 스플래시 화면 표시
    // 시작 화면 애니메이션
    val splashProgress = remember { Animatable(0f) }
    val splashAlpha = remember { Animatable(0f) }

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // 시작 화면 부드럽게 나타나기
        splashAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700)
        )

        // 로딩 바가 실제로 차오르기
        splashProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3800)
        )

        showSplash = false
    }

    BackHandler(enabled = selectedMenu != null) {
        when {
            showGpsTest -> showGpsTest = false
            selectedMode != null -> selectedMode = null
            selectedCategory != null -> selectedCategory = null
            else -> selectedMenu = null
        }
    }

    // 화면 전환 조건
    when {
        // 시작 화면
        showSplash -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = splashAlpha.value
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tripq_splash),
                    contentDescription = "TripQ 시작 화면",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 실제 애니메이션 로딩 바
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.43f)
                        .height(20.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = -maxHeight * 0.135f)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.75f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(splashProgress.value)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF8BCB69))
                    )
                }
            }
        }

        selectedMenu == null -> {
            HomeScreen { menu ->
                selectedMenu = menu
                selectedCategory = null
                selectedMode = null
                showGpsTest = false
            }
        }

        selectedMenu == "TRIPQ BRAIN" && selectedCategory == null -> {
            BrainCategoryScreen(
                onCategoryClick = { selectedCategory = it },
                onBack = { selectedMenu = null }
            )
        }

        selectedMenu == "TRIPQ LOCAL" && showGpsTest -> {
            FusedLocationTestScreen(
                onLocationReceived = { latitude, longitude ->
                    gpsLatitude = latitude
                    gpsLongitude = longitude
                },
                onBack = { showGpsTest = false }
            )
        }

        selectedMenu == "TRIPQ LOCAL" -> {
            LocalQuizScreen(
                gpsLatitude = gpsLatitude,
                gpsLongitude = gpsLongitude,
                onGpsTestClick = { showGpsTest = true },
                onBack = {
                    selectedMenu = null
                    selectedCategory = null
                    selectedMode = null
                    showGpsTest = false
                }
            )
        }

        selectedMode == null -> {
            GameModeScreen(
                quizTitle = selectedMenu!!,
                onModeClick = { selectedMode = it },
                onBack = {
                    if (selectedCategory != null) {
                        selectedCategory = null
                    } else {
                        selectedMenu = null
                    }
                }
            )
        }

        selectedMenu == "TRIPQ BRAIN" -> {
            BrainQuizScreen(
                gameMode = selectedMode!!,
                category = selectedCategory!!,
                onHome = {
                    selectedMenu = null
                    selectedCategory = null
                    selectedMode = null
                }
            )
        }

        else -> {
            if (selectedMenu == "TRIPQ MUSIC") {
                MusicQuizScreen(
                    initialMode = selectedMode!!,
                    onBack = {
                        selectedMode = null
                    }
                )
            } else {
                GameReadyScreen(
                    quizTitle = selectedMenu!!,
                    gameMode = selectedMode!!,
                    onBack = {
                        selectedMode = null
                    }
                )
            }
        }
    }
}


// 홈 화면
@Composable
fun HomeScreen(onMenuClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F4))
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "TRIPQ 🔍",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF263D35)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Make Every Trip Fun!",
                    fontSize = 17.sp,
                    color = Color(0xFF65756C)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 22.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFDDEFE2)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFE6F5E9),
                                    Color(0xFFC9E7D1)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 54.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("🚙", fontSize = 90.sp)
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "여행 속 재미를 찾아보세요!",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF294A38)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "함께 달리고, 함께 맞히고,\n함께 웃다.",
                            fontSize = 15.sp,
                            color = Color(0xFF587363),
                            lineHeight = 23.sp
                        )
                    }
                }
            }

            Text(
                "WELCOME TO TRIPQ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF819187)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "오늘의 여행을 시작해 볼까요?",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263D35)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "원하는 퀴즈를 선택해 보세요!",
                fontSize = 15.sp,
                color = Color(0xFF7A8980)
            )
            Spacer(Modifier.height(28.dp))

            QuizMenuCard(
                emoji = "🎵",
                title = "TRIPQ MUSIC",
                description = "노래를 듣고 제목과 가수를 맞혀보세요!",
                backgroundColor = Color(0xFFFFE9D6),
                onClick = { onMenuClick("TRIPQ MUSIC") }
            )

            Spacer(Modifier.height(16.dp))

            QuizMenuCard(
                emoji = "🧠",
                title = "TRIPQ BRAIN",
                description = "상식과 넌센스 퀴즈에 도전해 보세요!",
                backgroundColor = Color(0xFFE6E2FA),
                onClick = { onMenuClick("TRIPQ BRAIN") }
            )

            Spacer(Modifier.height(16.dp))

            QuizMenuCard(
                emoji = "📍",
                title = "TRIPQ LOCAL",
                description = "현재 위치와 여행지에 관한 퀴즈!",
                backgroundColor = Color(0xFFDCEEF8),
                onClick = { onMenuClick("TRIPQ LOCAL") }
            )

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.height(25.dp))
            Text(
                "MAKE EVERY TRIP FUN!",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF819187)
            )
        }
    }
}

// 홈 화면 메뉴 카드
@Composable
fun QuizMenuCard(
    emoji: String,
    title: String,
    description: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 38.sp)
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF293B33)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    description,
                    fontSize = 13.sp,
                    color = Color(0xFF65756C)
                )
            }
        }
    }
}

// BRAIN 카테고리 선택 화면
@Composable
fun BrainCategoryScreen(
    onCategoryClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val categories = listOf(
        Triple("📚", "일반 상식", "일상 속 다양한 상식에 도전!"),
        Triple("🤣", "넌센스", "기발하고 재미있는 문제!"),
        Triple("🏛️", "역사·문화", "역사와 문화에 관한 퀴즈!"),
        Triple("🔬", "과학·자연", "과학과 자연의 신비를 알아봐요!"),
        Triple("🧸", "어린이 퀴즈", "가족 모두 함께 풀 수 있는 문제!")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F4))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "TRIPQ BRAIN 🧠",
            fontSize = 29.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF294A38)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "어떤 퀴즈에 도전할까요?",
            fontSize = 18.sp,
            color = Color(0xFF65756C)
        )
        Spacer(Modifier.height(25.dp))

        categories.forEach { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onCategoryClick(category.second) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category.first, fontSize = 30.sp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            category.second,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF294A38)
                        )
                        Text(
                            category.third,
                            fontSize = 13.sp,
                            color = Color(0xFF819187)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SimpleBackButton(onClick = onBack)
    }
}

// 개인전 / 팀전 선택 화면
@Composable
fun GameModeScreen(
    quizTitle: String,
    onModeClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F4))
            .padding(35.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "TRIPQ 🔍",
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF263D35)
        )
        Spacer(Modifier.height(28.dp))
        Text(
            quizTitle,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF294A38)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "어떤 방식으로 퀴즈를 즐길까요?",
            fontSize = 19.sp,
            color = Color(0xFF65756C)
        )
        Spacer(Modifier.height(38.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            GameModeCard(
                modifier = Modifier.weight(1f),
                emoji = "👤",
                title = "개인전",
                description = "나만의 실력으로\n퀴즈에 도전!",
                backgroundColor = Color(0xFFFFE9D6),
                onClick = { onModeClick("개인전") }
            )
            GameModeCard(
                modifier = Modifier.weight(1f),
                emoji = "👨‍👩‍👧‍👦",
                title = "팀전",
                description = "가족과 친구들과\n함께 퀴즈에 도전!",
                backgroundColor = Color(0xFFDCEEF8),
                onClick = { onModeClick("팀전") }
            )
        }

        Spacer(Modifier.height(35.dp))
        Text(
            "함께 달리고, 함께 맞히고, 함께 웃다.",
            fontSize = 14.sp,
            color = Color(0xFF819187)
        )
        Spacer(Modifier.height(25.dp))
        SimpleBackButton(onClick = onBack)
    }
}

// 개인전 / 팀전 카드
@Composable
fun GameModeCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    description: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(190.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 42.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF293B33)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                description,
                fontSize = 13.sp,
                color = Color(0xFF65756C),
                lineHeight = 19.sp
            )
        }
    }
}

// 일반 상식 퀴즈
val generalQuestions = listOf(
    BrainQuestion(
        "대한민국의 수도는 어디일까요?",
        listOf("부산", "서울", "대전", "인천"),
        1,
        "대한민국의 수도는 서울입니다."
    ),
    BrainQuestion(
        "1년은 몇 개월일까요?",
        listOf("10개월", "11개월", "12개월", "13개월"),
        2,
        "1년은 12개월입니다."
    ),
    BrainQuestion(
        "지구에서 가장 큰 대양은?",
        listOf("대서양", "인도양", "북극해", "태평양"),
        3,
        "태평양은 지구에서 가장 큰 대양입니다."
    ),
    BrainQuestion(
        "대한민국의 국기 이름은?",
        listOf("성조기", "태극기", "일장기", "오성홍기"),
        1,
        "대한민국의 국기는 태극기입니다."
    ),
    BrainQuestion(
        "컴퓨터의 두뇌라고 불리는 부품은?",
        listOf("키보드", "마우스", "모니터", "CPU"),
        3,
        "CPU는 중앙처리장치입니다."
    )
)

// 넌센스 퀴즈
val nonsenseQuestions = listOf(
    BrainQuestion(
        "왕이 넘어지면?",
        listOf("킹콩", "킹받네", "왕좌", "넘어진 왕"),
        0,
        "왕(King)이 콩! 하고 넘어져서 킹콩입니다."
    ),
    BrainQuestion(
        "세상에서 가장 뜨거운 바다는?",
        listOf("동해", "열바다", "남해", "불바다"),
        1,
        "열이 나는 바다라서 열바다입니다."
    ),
    BrainQuestion(
        "자동차가 놀라면?",
        listOf("카놀라유", "브레이크", "빵빵", "후진"),
        0,
        "자동차(Car)가 놀라면 카놀라유!"
    ),
    BrainQuestion(
        "소가 웃으면?",
        listOf("우하하", "소웃음", "음메", "하하소"),
        0,
        "소가 웃으면 우하하라고 해요."
    ),
    BrainQuestion(
        "신발이 화가 나면?",
        listOf("신발끈", "발끈", "운동화", "구두"),
        1,
        "발끈! 하고 화가 납니다."
    )
)

// 역사·문화 퀴즈
val historyQuestions = listOf(
    BrainQuestion(
        "훈민정음을 창제한 왕은?",
        listOf("태조", "세종대왕", "정조", "광해군"),
        1,
        "세종대왕은 훈민정음을 창제했습니다."
    ),
    BrainQuestion(
        "고구려의 건국자로 알려진 인물은?",
        listOf("주몽", "박혁거세", "온조", "김유신"),
        0,
        "주몽은 고구려를 세운 인물입니다."
    ),
    BrainQuestion(
        "조선의 수도였던 도시는?",
        listOf("평양", "경주", "한양", "공주"),
        2,
        "조선의 수도는 한양이었습니다."
    ),
    BrainQuestion(
        "우리나라의 전통 명절이 아닌 것은?",
        listOf("설날", "추석", "단오", "할로윈"),
        3,
        "할로윈은 한국의 전통 명절이 아닙니다."
    ),
    BrainQuestion(
        "경주에 있는 대표적인 천문 관측 시설은?",
        listOf("첨성대", "석굴암", "불국사", "다보탑"),
        0,
        "첨성대는 신라 시대의 천문 관측 시설입니다."
    )
)

// 과학·자연 퀴즈
val scienceQuestions = listOf(
    BrainQuestion(
        "물이 얼기 시작하는 온도는?",
        listOf("0도", "10도", "50도", "100도"),
        0,
        "일반적인 대기압에서 물은 0도에서 얼기 시작합니다."
    ),
    BrainQuestion(
        "태양계에서 가장 큰 행성은?",
        listOf("지구", "화성", "목성", "금성"),
        2,
        "목성은 태양계에서 가장 큰 행성입니다."
    ),
    BrainQuestion(
        "식물이 광합성을 할 때 필요한 것은?",
        listOf("햇빛", "플라스틱", "모래만", "소금"),
        0,
        "식물은 햇빛을 이용해 광합성을 합니다."
    ),
    BrainQuestion(
        "다음 중 포유류가 아닌 동물은?",
        listOf("고래", "박쥐", "펭귄", "돌고래"),
        2,
        "펭귄은 알을 낳는 조류입니다."
    ),
    BrainQuestion(
        "무지개는 보통 몇 가지 색으로 표현할까요?",
        listOf("3가지", "5가지", "7가지", "9가지"),
        2,
        "무지개는 일반적으로 7가지 색으로 표현합니다."
    )
)

// 어린이 퀴즈
val childrenQuestions = listOf(
    BrainQuestion(
        "강아지가 내는 소리는?",
        listOf("야옹", "멍멍", "음메", "꿀꿀"),
        1,
        "강아지는 멍멍 짖습니다."
    ),
    BrainQuestion(
        "바나나의 대표적인 색깔은?",
        listOf("파란색", "보라색", "노란색", "검은색"),
        2,
        "익은 바나나는 보통 노란색입니다."
    ),
    BrainQuestion(
        "다음 중 하늘을 나는 교통수단은?",
        listOf("버스", "기차", "비행기", "자전거"),
        2,
        "비행기는 하늘을 나는 교통수단입니다."
    ),
    BrainQuestion(
        "겨울에 하늘에서 내리는 하얀 것은?",
        listOf("모래", "눈", "꽃잎", "나뭇잎"),
        1,
        "겨울에는 눈이 내리기도 합니다."
    ),
    BrainQuestion(
        "다음 중 바다에 사는 동물은?",
        listOf("사자", "코끼리", "돌고래", "기린"),
        2,
        "돌고래는 바다에 사는 포유류입니다."
    )
)

// 카테고리별 문제 연결
val brainCategoryQuestions = mapOf(
    "일반 상식" to generalQuestions,
    "넌센스" to nonsenseQuestions,
    "역사·문화" to historyQuestions,
    "과학·자연" to scienceQuestions,
    "어린이 퀴즈" to childrenQuestions
)

// BRAIN 퀴즈 진행 화면
@Composable
fun BrainQuizScreen(
    gameMode: String,
    category: String,
    onHome: () -> Unit
) {
    val questions = brainCategoryQuestions[category] ?: generalQuestions

    var questionIndex by remember(category, gameMode) {
        mutableIntStateOf(0)
    }
    var score by remember(category, gameMode) {
        mutableIntStateOf(0)
    }
    var teamOneScore by remember(category, gameMode) {
        mutableIntStateOf(0)
    }
    var teamTwoScore by remember(category, gameMode) {
        mutableIntStateOf(0)
    }
    var currentTeam by remember(category, gameMode) {
        mutableIntStateOf(0)
    }
    var remainingTime by remember(category, gameMode) {
        mutableIntStateOf(15)
    }
    var selectedAnswer by remember(category, gameMode) {
        mutableStateOf<Int?>(null)
    }
    var isAnswered by remember(category, gameMode) {
        mutableStateOf(false)
    }
    var isFinished by remember(category, gameMode) {
        mutableStateOf(false)
    }

    val isTeamMode = gameMode == "팀전"
    val currentQuestion = questions[questionIndex]

    LaunchedEffect(questionIndex, isAnswered, isFinished) {
        remainingTime = 15

        if (!isAnswered && !isFinished) {
            while (remainingTime > 0) {
                delay(1000)
                remainingTime -= 1
            }
            if (remainingTime == 0) {
                isAnswered = true
            }
        }
    }

    if (isFinished) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7F4))
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🏆", fontSize = 75.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "QUIZ COMPLETE!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF294A38)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                category,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF68A77C)
            )
            Spacer(Modifier.height(25.dp))

            if (isTeamMode) {
                Text(
                    "최종 팀 대결 결과",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF52695B)
                )
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFE9D6)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔴", fontSize = 30.sp)
                            Text("1팀", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "$teamOneScore 점",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFDCEEF8)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔵", fontSize = 30.sp)
                            Text("2팀", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "$teamTwoScore 점",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(25.dp))

                val winnerText = when {
                    teamOneScore > teamTwoScore -> "🎉 1팀 승리!"
                    teamTwoScore > teamOneScore -> "🎉 2팀 승리!"
                    else -> "🤝 무승부!"
                }

                Text(
                    winnerText,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF348451)
                )
            } else {
                Text(
                    "$score 점",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4D9B68)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "총 ${questions.size}문제 중 ${score / 10}문제 정답!",
                    fontSize = 19.sp,
                    color = Color(0xFF65756C)
                )
            }

            Spacer(Modifier.height(35.dp))

            Button(
                onClick = {
                    questionIndex = 0
                    score = 0
                    teamOneScore = 0
                    teamTwoScore = 0
                    currentTeam = 0
                    remainingTime = 15
                    selectedAnswer = null
                    isAnswered = false
                    isFinished = false
                }
            ) {
                Text("다시 도전하기")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onHome
            ) {
                Text("메인 화면으로 돌아가기")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7F4))
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "TRIPQ BRAIN 🧠",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF294A38)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "$category · $gameMode",
                fontSize = 15.sp,
                color = Color(0xFF819187)
            )
            Spacer(Modifier.height(15.dp))

            if (isTeamMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentTeam == 0) {
                                Color(0xFFFFDDBD)
                            } else {
                                Color(0xFFFFEDE0)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (currentTeam == 0) "🔴 1팀 차례" else "1팀",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$teamOneScore 점",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentTeam == 1) {
                                Color(0xFFCDE8F8)
                            } else {
                                Color(0xFFE7F3FA)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (currentTeam == 1) "🔵 2팀 차례" else "2팀",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$teamTwoScore 점",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(15.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "QUESTION ${questionIndex + 1} / ${questions.size}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF294A38)
                )
                Text(
                    if (isTeamMode) "팀 대결" else "SCORE $score",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4D9B68)
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = {
                    (questionIndex + 1) / questions.size.toFloat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF68A77C),
                trackColor = Color(0xFFDDE7DF)
            )

            Spacer(Modifier.height(18.dp))

            Text(
                "⏱ 남은 시간  $remainingTime 초",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (remainingTime <= 5) {
                    Color(0xFFD94B4B)
                } else {
                    Color(0xFF52695B)
                }
            )

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("❓", fontSize = 45.sp)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        currentQuestion.question,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263D35),
                        lineHeight = 32.sp
                    )
                    Spacer(Modifier.height(25.dp))

                    currentQuestion.options.forEachIndexed { index, option ->
                        val optionColor = when {
                            isAnswered && index == currentQuestion.answer ->
                                Color(0xFFCDEBD4)
                            isAnswered && index == selectedAnswer ->
                                Color(0xFFFFDADA)
                            else -> Color(0xFFF0F3F0)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable {
                                    if (!isAnswered) {
                                        selectedAnswer = index
                                        isAnswered = true

                                        if (index == currentQuestion.answer) {
                                            if (isTeamMode) {
                                                if (currentTeam == 0) {
                                                    teamOneScore += 10
                                                } else {
                                                    teamTwoScore += 10
                                                }
                                            } else {
                                                score += 10
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = optionColor
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${'A' + index}.",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF52695B)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    option,
                                    fontSize = 16.sp,
                                    color = Color(0xFF263D35)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(15.dp))

            if (isAnswered) {
                val isCorrect = selectedAnswer == currentQuestion.answer

                Text(
                    text = if (selectedAnswer == null) {
                        "⏰ 시간 초과!"
                    } else if (isCorrect) {
                        "🎉 정답입니다! +10점"
                    } else {
                        "아쉬워요! 정답은 ${currentQuestion.options[currentQuestion.answer]}입니다."
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) {
                        Color(0xFF348451)
                    } else {
                        Color(0xFFD15C5C)
                    }
                )

                Spacer(Modifier.height(7.dp))
                Text(
                    currentQuestion.explanation,
                    fontSize = 13.sp,
                    color = Color(0xFF65756C)
                )
                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (questionIndex == questions.lastIndex) {
                            isFinished = true
                        } else {
                            questionIndex += 1
                            selectedAnswer = null
                            isAnswered = false
                            if (isTeamMode) {
                                currentTeam = 1 - currentTeam
                            }
                        }
                    }
                ) {
                    Text(
                        if (questionIndex == questions.lastIndex) {
                            "결과 확인하기"
                        } else {
                            "다음 문제"
                        }
                    )
                }
            } else {
                Text(
                    "정답을 선택해 주세요!",
                    fontSize = 14.sp,
                    color = Color(0xFF819187)
                )
            }
        }
    }
}

// LOCAL 지역별 퀴즈 데이터

private val localQuestionData = mapOf(

    "서울" to listOf(
        LocalQuestion(
            "경복궁을 처음 건립한 조선의 왕은?",
            listOf("태조", "세종", "정조", "태종"),
            "태조",
            "경복궁은 조선을 건국한 태조 이성계 때인 1395년에 창건되었어."
        ),
        LocalQuestion(
            "서울 남산에 있는 대표적인 탑은?",
            listOf("63빌딩", "N서울타워", "롯데타워", "월드컵타워"),
            "N서울타워",
            "남산 정상에는 서울의 대표적인 랜드마크인 N서울타워가 있어."
        ),
        LocalQuestion(
            "한강을 가로지르는 서울의 대표적인 다리는?",
            listOf("광안대교", "이순신대교", "마포대교", "거가대교"),
            "마포대교",
            "마포대교는 서울 한강에 놓인 다리 중 하나야."
        )
    ),

    "부산" to listOf(
        LocalQuestion(
            "부산의 대표적인 해수욕장은?",
            listOf("해운대", "경포대", "을왕리", "대천"),
            "해운대",
            "해운대해수욕장은 부산을 대표하는 관광 명소야."
        ),
        LocalQuestion(
            "부산의 유명한 바다 전망 명소인 다리는?",
            listOf("광안대교", "인천대교", "서해대교", "남해대교"),
            "광안대교",
            "광안대교는 광안리 앞바다를 가로지르는 부산의 랜드마크야."
        ),
        LocalQuestion(
            "부산의 대표적인 전통 시장은?",
            listOf("자갈치시장", "남대문시장", "광장시장", "중앙시장"),
            "자갈치시장",
            "자갈치시장은 부산의 대표적인 수산물 시장이야."
        )
    ),

    "제주" to listOf(
        LocalQuestion(
            "제주도에서 가장 높은 산은?",
            listOf("한라산", "지리산", "설악산", "태백산"),
            "한라산",
            "한라산은 해발 1,947m로 대한민국에서 가장 높은 산이야."
        ),
        LocalQuestion(
            "제주도의 상징적인 돌 조각상은?",
            listOf("장승", "돌하르방", "솟대", "해태"),
            "돌하르방",
            "돌하르방은 제주를 상징하는 대표적인 석상이야."
        ),
        LocalQuestion(
            "제주도에서 볼 수 있는 독특한 화산 지형은?",
            listOf("주상절리", "빙하 계곡", "사막 사구", "석회 동굴"),
            "주상절리",
            "제주 중문 대포해안에는 용암이 식으며 만들어진 주상절리가 있어."
        )
    ),

    "경주" to listOf(
        LocalQuestion(
            "경주에 있는 신라 시대의 천문 관측 시설은?",
            listOf("첨성대", "석굴암", "불국사", "분황사"),
            "첨성대",
            "첨성대는 신라 시대에 만들어진 천문 관측 시설로 알려져 있어."
        ),
        LocalQuestion(
            "경주에서 유명한 신라 시대의 사찰은?",
            listOf("불국사", "해인사", "통도사", "수덕사"),
            "불국사",
            "불국사는 경주를 대표하는 문화유산이야."
        ),
        LocalQuestion(
            "경주 동궁과 월지의 옛 이름은?",
            listOf("안압지", "월정교", "포석정", "임해전"),
            "안압지",
            "동궁과 월지는 오랫동안 안압지라는 이름으로 알려졌어."
        )
    ),

    "인천" to listOf(
        LocalQuestion(
            "인천에 있는 국제공항의 이름은?",
            listOf("인천국제공항", "김포국제공항", "김해국제공항", "제주국제공항"),
            "인천국제공항",
            "인천국제공항은 대한민국의 대표적인 국제 관문 공항이야."
        ),
        LocalQuestion(
            "인천의 대표적인 섬 관광지 중 하나는?",
            listOf("월미도", "우도", "거제도", "남이섬"),
            "월미도",
            "월미도는 인천 앞바다에 있는 대표적인 관광지야."
        ),
        LocalQuestion(
            "인천 차이나타운과 관련이 깊은 음식은?",
            listOf("짜장면", "냉면", "비빔밥", "막국수"),
            "짜장면",
            "인천 차이나타운은 한국식 짜장면의 역사와 관련이 깊어."
        )
    ),

    "강릉" to listOf(
        LocalQuestion(
            "강릉에 있는 유명한 해변은?",
            listOf("경포해변", "해운대", "을왕리", "광안리"),
            "경포해변",
            "경포해변은 강릉을 대표하는 동해안 해변이야."
        ),
        LocalQuestion(
            "강릉의 유명한 커피 거리가 있는 곳은?",
            listOf("안목해변", "정동진", "주문진", "대관령"),
            "안목해변",
            "안목해변에는 바다를 보며 커피를 즐길 수 있는 카페 거리가 있어."
        ),
        LocalQuestion(
            "강릉에서 태어난 조선 시대의 대표적인 문인은?",
            listOf("신사임당", "허균", "정약용", "김홍도"),
            "신사임당",
            "신사임당은 강릉과 인연이 깊은 조선 시대 예술가이자 문인이야."
        )
    ),

    // 신규 수도권 지역


    "수원" to listOf(
        LocalQuestion(
            "수원을 대표하는 유네스코 세계유산은?",
            listOf("수원화성", "남한산성", "화성행궁", "경복궁"),
            "수원화성",
            "수원화성은 정조 때 건설된 성곽으로 1997년 유네스코 세계유산에 등재됐어."
        ),
        LocalQuestion(
            "수원화성을 건설한 조선의 왕은?",
            listOf("정조", "태조", "세종", "영조"),
            "정조",
            "정조는 아버지 사도세자를 기리고 새로운 도시를 만들기 위해 수원화성을 건설했어."
        ),
        LocalQuestion(
            "수원의 대표적인 향토 음식으로 유명한 것은?",
            listOf("수원갈비", "춘천닭갈비", "전주비빔밥", "안동찜닭"),
            "수원갈비",
            "수원은 오랜 역사를 가진 갈비 음식으로 유명해."
        ),
        LocalQuestion(
            "수원화성의 북쪽 성문 이름은?",
            listOf("장안문", "팔달문", "창룡문", "화서문"),
            "장안문",
            "장안문은 수원화성의 북쪽에 위치한 성문이야."
        ),
        LocalQuestion(
            "수원화성의 남쪽 성문 이름은?",
            listOf("장안문", "팔달문", "창룡문", "화서문"),
            "팔달문",
            "팔달문은 수원화성의 남쪽에 위치한 성문이야."
        ),
        LocalQuestion(
            "정조가 수원화성 안에 세운 임시 궁궐은?",
            listOf("화성행궁", "경희궁", "창덕궁", "운현궁"),
            "화성행궁",
            "화성행궁은 정조가 수원에 행차했을 때 머물던 궁궐이야."
        ),
        LocalQuestion(
            "수원화성의 동쪽 성문 이름은?",
            listOf("창룡문", "장안문", "팔달문", "화서문"),
            "창룡문",
            "창룡문은 수원화성의 동쪽에 위치한 성문이야."
        ),
        LocalQuestion(
            "수원화성의 서쪽 성문 이름은?",
            listOf("화서문", "창룡문", "장안문", "팔달문"),
            "화서문",
            "화서문은 수원화성의 서쪽에 위치한 성문이야."
        ),
        LocalQuestion(
            "수원에 위치한 대표적인 프로축구 경기장은?",
            listOf("수원월드컵경기장", "서울월드컵경기장", "대전월드컵경기장", "인천축구전용경기장"),
            "수원월드컵경기장",
            "수원월드컵경기장은 수원에 위치한 축구 전용 경기장이야."
        ),
        LocalQuestion(
            "수원화성이 건설된 조선 시대는?",
            listOf("조선 후기", "조선 초기", "고려 후기", "삼국 시대"),
            "조선 후기",
            "수원화성은 정조 재위 시기인 조선 후기에 건설됐어."
        )
    ),



    "용인" to listOf(
        LocalQuestion(
            "용인에 위치한 대표적인 테마파크는?",
            listOf("에버랜드", "롯데월드", "서울랜드", "레고랜드"),
            "에버랜드",
            "에버랜드는 경기도 용인시에 위치한 대표적인 테마파크야."
        ),
        LocalQuestion(
            "용인에 위치한 전통문화 체험 관광지는?",
            listOf("한국민속촌", "북촌한옥마을", "하회마을", "전주한옥마을"),
            "한국민속촌",
            "한국민속촌에서는 전통 가옥과 생활문화를 체험할 수 있어."
        ),
        LocalQuestion(
            "용인에 위치한 유명한 드라마 촬영장은?",
            listOf("MBC 드라미아", "남산서울타워", "남이섬", "순천만"),
            "MBC 드라미아",
            "용인 대장금파크는 사극과 드라마 촬영지로 활용되는 곳이야."
        ),
        LocalQuestion(
            "용인에 위치한 대표적인 불교 사찰은?",
            listOf("와우정사", "불국사", "해인사", "통도사"),
            "와우정사",
            "와우정사는 용인에 있는 사찰로 다양한 불교 조형물로 알려져 있어."
        ),
        LocalQuestion(
            "용인에 위치한 도립 박물관은?",
            listOf("경기도박물관", "국립중앙박물관", "국립민속박물관", "부산박물관"),
            "경기도박물관",
            "경기도박물관은 용인시에 위치한 역사·문화 박물관이야."
        ),
        LocalQuestion(
            "용인에 위치한 자동차 관련 전시 시설은?",
            listOf("삼성화재교통박물관", "국립항공박물관", "철도박물관", "국립과천과학관"),
            "삼성화재교통박물관",
            "삼성화재교통박물관은 다양한 자동차와 교통 관련 자료를 전시하는 곳이야."
        ),
        LocalQuestion(
            "용인시는 어느 도에 속해 있을까?",
            listOf("경기도", "강원특별자치도", "충청남도", "경상북도"),
            "경기도",
            "용인시는 대한민국 경기도에 속한 도시야."
        ),
        LocalQuestion(
            "용인에 위치한 대표적인 놀이공원 주변 동물원은?",
            listOf("에버랜드 주토피아", "서울대공원", "대전 오월드", "어린이대공원"),
            "에버랜드 주토피아",
            "에버랜드에는 다양한 동물을 만날 수 있는 주토피아 구역이 있어."
        ),
        LocalQuestion(
            "용인에 위치한 자연휴양림은?",
            listOf("용인자연휴양림", "유명산자연휴양림", "대관령자연휴양림", "남해편백자연휴양림"),
            "용인자연휴양림",
            "용인자연휴양림은 숲속에서 휴식과 자연 체험을 즐길 수 있는 곳이야."
        ),
        LocalQuestion(
            "용인시의 대표적인 행정 중심 지역 중 하나는?",
            listOf("처인구", "해운대구", "강남구", "중구"),
            "처인구",
            "용인시는 처인구, 기흥구, 수지구의 3개 일반구로 구성되어 있어."
        )
    ),



    "성남" to listOf(
        LocalQuestion(
            "성남시에 위치한 대표적인 IT·첨단산업 중심지는?",
            listOf("판교테크노밸리", "구로디지털단지", "대덕연구단지", "마곡지구"),
            "판교테크노밸리",
            "판교테크노밸리에는 IT·게임·첨단기술 기업들이 모여 있어."
        ),
        LocalQuestion(
            "성남시 분당구에 위치한 대표적인 공원은?",
            listOf("중앙공원", "올림픽공원", "서울숲", "월드컵공원"),
            "중앙공원",
            "분당중앙공원은 성남시 분당구의 대표적인 녹지 공간이야."
        ),
        LocalQuestion(
            "성남시를 통과하는 대표적인 하천은?",
            listOf("탄천", "낙동강", "금강", "섬진강"),
            "탄천",
            "탄천은 성남시를 통과해 한강으로 이어지는 하천이야."
        ),
        LocalQuestion(
            "성남시에 위치한 프로축구 경기장은?",
            listOf("탄천종합운동장", "수원월드컵경기장", "서울월드컵경기장", "대전월드컵경기장"),
            "탄천종합운동장",
            "탄천종합운동장은 성남시 분당구에 위치한 경기장이야."
        ),
        LocalQuestion(
            "성남시의 대표적인 전통시장은?",
            listOf("모란시장", "자갈치시장", "광장시장", "남대문시장"),
            "모란시장",
            "모란시장은 성남을 대표하는 전통시장 중 하나야."
        ),
        LocalQuestion(
            "성남시는 어느 도에 속해 있을까?",
            listOf("경기도", "강원특별자치도", "충청북도", "경상남도"),
            "경기도",
            "성남시는 경기도에 속한 도시야."
        ),
        LocalQuestion(
            "성남시 분당구에 위치한 대표적인 쇼핑 시설은?",
            listOf("현대백화점 판교점", "신세계백화점 센텀시티", "롯데백화점 부산본점", "타임스퀘어"),
            "현대백화점 판교점",
            "현대백화점 판교점은 성남시 분당구 판교에 위치해 있어."
        ),
        LocalQuestion(
            "성남시의 대표적인 신도시 지역은?",
            listOf("분당", "일산", "평촌", "동탄"),
            "분당",
            "분당은 성남시에 조성된 대표적인 1기 신도시야."
        ),
        LocalQuestion(
            "성남시에 위치한 대표적인 문화시설은?",
            listOf("성남아트센터", "예술의전당", "세종문화회관", "국립극장"),
            "성남아트센터",
            "성남아트센터는 공연과 전시를 즐길 수 있는 문화예술 공간이야."
        ),
        LocalQuestion(
            "성남시의 행정구역에 해당하지 않는 곳은?",
            listOf("기흥구", "수정구", "중원구", "분당구"),
            "기흥구",
            "성남시는 수정구, 중원구, 분당구로 구성되어 있어. 기흥구는 용인시에 속해."
        )
    ),



    "화성" to listOf(
        LocalQuestion(
            "화성시에 위치한 대표적인 섬 관광지는?",
            listOf("제부도", "월미도", "우도", "남이섬"),
            "제부도",
            "제부도는 바닷길이 열리는 현상으로 유명한 화성시의 섬이야."
        ),
        LocalQuestion(
            "화성시에 위치한 대표적인 항구는?",
            listOf("전곡항", "자갈치항", "묵호항", "주문진항"),
            "전곡항",
            "전곡항은 요트와 해양레저 활동으로 유명한 화성시의 항구야."
        ),
        LocalQuestion(
            "화성시에서 발견된 공룡 관련 유적지는?",
            listOf("공룡알 화석산지", "고인돌 유적지", "석굴암", "첨성대"),
            "공룡알 화석산지",
            "화성시에는 공룡알 화석과 둥지 화석이 발견된 지역이 있어."
        ),
        LocalQuestion(
            "화성시의 대표적인 산업단지 중 하나는?",
            listOf("동탄산업단지", "구로디지털단지", "대덕연구단지", "창원국가산업단지"),
            "동탄산업단지",
            "동탄 지역에는 다양한 기업과 산업시설이 자리 잡고 있어."
        ),
        LocalQuestion(
            "화성시에 위치한 대표적인 신도시는?",
            listOf("동탄신도시", "분당신도시", "일산신도시", "평촌신도시"),
            "동탄신도시",
            "동탄신도시는 화성시의 대표적인 계획도시야."
        ),
        LocalQuestion(
            "화성시는 어느 도에 속해 있을까?",
            listOf("경기도", "경상북도", "전라남도", "강원특별자치도"),
            "경기도",
            "화성시는 경기도 남서부에 위치한 도시야."
        ),
        LocalQuestion(
            "화성시의 바닷가 관광지로 유명한 곳은?",
            listOf("궁평항", "경포항", "속초항", "부산항"),
            "궁평항",
            "궁평항은 화성시 서해안에 위치한 관광 명소야."
        ),
        LocalQuestion(
            "화성시에 위치한 대표적인 호수공원은?",
            listOf("동탄호수공원", "일산호수공원", "석촌호수", "광교호수공원"),
            "동탄호수공원",
            "동탄호수공원은 화성시 동탄 지역의 대표적인 휴식 공간이야."
        ),
        LocalQuestion(
            "화성시의 대표적인 역사 유적지 중 하나는?",
            listOf("융건릉", "종묘", "첨성대", "해인사"),
            "융건릉",
            "융건릉은 사도세자와 정조의 능이 있는 화성시의 세계유산이야."
        ),
        LocalQuestion(
            "화성시의 서해안 지역에서 즐길 수 있는 활동은?",
            listOf("갯벌 체험", "스키", "래프팅", "사막 투어"),
            "갯벌 체험",
            "화성시 서해안에서는 갯벌과 해양 생태를 체험할 수 있어."
        )
    ),



    "안양" to listOf(
        LocalQuestion(
            "안양시에 위치한 대표적인 예술 관광지는?",
            listOf("안양예술공원", "서울숲", "올림픽공원", "북서울꿈의숲"),
            "안양예술공원",
            "안양예술공원에는 다양한 야외 예술 작품이 설치되어 있어."
        ),
        LocalQuestion(
            "안양시를 통과하는 대표적인 하천은?",
            listOf("안양천", "한강", "낙동강", "금강"),
            "안양천",
            "안양천은 안양을 비롯한 수도권 지역을 흐르는 하천이야."
        ),
        LocalQuestion(
            "안양시에 위치한 프로농구 구단의 홈 경기장은?",
            listOf("안양 정관장 아레나", "잠실학생체육관", "고척스카이돔", "수원체육관"),
            "안양 정관장 아레나",
            "안양 정관장 아레나는 안양을 연고로 하는 프로농구 구단의 홈 경기장이야."
        ),
        LocalQuestion(
            "안양시는 어느 도에 속해 있을까?",
            listOf("경기도", "충청남도", "경상북도", "전라북도"),
            "경기도",
            "안양시는 경기도에 속한 도시야."
        ),
        LocalQuestion(
            "안양시에 위치한 대표적인 전통시장은?",
            listOf("중앙시장", "자갈치시장", "모란시장", "국제시장"),
            "중앙시장",
            "안양중앙시장은 안양의 대표적인 전통시장 중 하나야."
        ),
        LocalQuestion(
            "안양의 대표적인 문화예술 시설은?",
            listOf("평촌아트홀", "세종문화회관", "국립극장", "예술의전당"),
            "평촌아트홀",
            "평촌아트홀은 안양시에서 공연과 문화행사가 열리는 시설이야."
        ),
        LocalQuestion(
            "안양시에 위치한 대표적인 산은?",
            listOf("삼성산", "한라산", "지리산", "태백산"),
            "삼성산",
            "삼성산은 안양과 서울 관악구 일대에 걸쳐 있는 산이야."
        ),
        LocalQuestion(
            "안양시에 위치한 대표적인 쇼핑 중심지는?",
            listOf("평촌", "해운대", "명동", "서면"),
            "평촌",
            "평촌은 안양의 대표적인 상업·주거 중심 지역 중 하나야."
        ),
        LocalQuestion(
            "안양시의 행정구역에 해당하지 않는 곳은?",
            listOf("분당구", "만안구", "동안구", "없음"),
            "분당구",
            "안양시는 만안구와 동안구로 구성되어 있어. 분당구는 성남시에 속해."
        ),
        LocalQuestion(
            "안양시에 위치한 대표적인 공공 체육시설은?",
            listOf("안양종합운동장", "잠실종합운동장", "고척스카이돔", "부산아시아드주경기장"),
            "안양종합운동장",
            "안양종합운동장은 다양한 스포츠 경기가 열리는 지역 체육시설이야."
        )
    ),



    "고양" to listOf(
        LocalQuestion(
            "고양시에 위치한 대표적인 대형 전시장 이름은?",
            listOf("킨텍스", "코엑스", "벡스코", "엑스코"),
            "킨텍스",
            "킨텍스는 경기도 고양시에 위치한 대형 전시·컨벤션 시설이야."
        ),
        LocalQuestion(
            "고양시 일산에 위치한 유명한 공원은?",
            listOf("일산호수공원", "서울숲", "여의도공원", "올림픽공원"),
            "일산호수공원",
            "일산호수공원은 넓은 호수와 산책로로 유명한 고양시의 공원이야."
        ),
        LocalQuestion(
            "고양시에 위치한 역사 유적지는?",
            listOf("행주산성", "수원화성", "남한산성", "공산성"),
            "행주산성",
            "행주산성은 임진왜란 당시 권율 장군의 행주대첩으로 유명한 곳이야."
        ),
        LocalQuestion(
            "고양시는 어느 도에 속해 있을까?",
            listOf("경기도", "강원특별자치도", "충청북도", "경상남도"),
            "경기도",
            "고양시는 경기도 북서부에 위치한 도시야."
        ),
        LocalQuestion(
            "고양시에 위치한 대표적인 방송 제작 시설은?",
            listOf("일산 MBC 드림센터", "KBS 부산", "SBS 목동", "대전방송"),
            "일산 MBC 드림센터",
            "일산 MBC 드림센터는 고양시 일산에 위치한 방송 제작 시설이야."
        ),
        LocalQuestion(
            "고양시의 대표적인 꽃 축제는?",
            listOf("고양국제꽃박람회", "진해군항제", "보령머드축제", "화천산천어축제"),
            "고양국제꽃박람회",
            "고양국제꽃박람회는 다양한 꽃과 정원을 만날 수 있는 행사야."
        ),
        LocalQuestion(
            "고양시에 위치한 대표적인 호수공원은?",
            listOf("일산호수공원", "석촌호수", "동탄호수공원", "광교호수공원"),
            "일산호수공원",
            "일산호수공원은 고양시를 대표하는 휴식과 산책 공간이야."
        ),
        LocalQuestion(
            "고양시의 대표적인 프로야구 경기장은?",
            listOf("고양 국가대표 야구훈련장", "잠실야구장", "사직야구장", "대구삼성라이온즈파크"),
            "고양 국가대표 야구훈련장",
            "고양에는 국가대표 야구 훈련 시설이 있어."
        ),
        LocalQuestion(
            "고양시의 행정구역에 해당하지 않는 곳은?",
            listOf("분당구", "덕양구", "일산동구", "일산서구"),
            "분당구",
            "고양시는 덕양구, 일산동구, 일산서구로 구성되어 있어."
        ),
        LocalQuestion(
            "고양시에 위치한 대표적인 문화·전시 공간은?",
            listOf("아람누리", "세종문화회관", "예술의전당", "국립극장"),
            "아람누리",
            "고양아람누리는 공연과 전시를 즐길 수 있는 문화예술 공간이야."
        )
    )
)

// GPS 좌표와 가장 가까운 퀴즈 지역 찾기

private fun findNearestQuizRegion(
    latitude: Double,
    longitude: Double
): String {

    val regions = listOf(
        // 기존 지역
        Triple("서울", 37.5665, 126.9780),
        Triple("부산", 35.1796, 129.0756),
        Triple("제주", 33.4996, 126.5312),
        Triple("경주", 35.8562, 129.2247),
        Triple("인천", 37.4563, 126.7052),
        Triple("강릉", 37.7519, 128.8761),

        // 추가 수도권 지역
        Triple("수원", 37.2636, 127.0286),
        Triple("용인", 37.2411, 127.1776),
        Triple("성남", 37.4200, 127.1265),
        Triple("화성", 37.1995, 126.8312),
        Triple("안양", 37.3943, 126.9568),
        Triple("고양", 37.6584, 126.8320)
    )

    fun distanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) *
                    Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(
            Math.sqrt(a),
            Math.sqrt(1 - a)
        )

        return earthRadius * c
    }

    return regions.minByOrNull { region ->
        distanceKm(
            latitude,
            longitude,
            region.second,
            region.third
        )
    }?.first ?: "서울"
}


// LOCAL 전체 화면 이동 관리
@Composable
fun LocalQuizScreen(
    gpsLatitude: Double?,
    gpsLongitude: Double?,
    onGpsTestClick: () -> Unit,
    onBack: () -> Unit
) {
    var page by remember { mutableStateOf("HOME") }
    var quizMode by remember { mutableStateOf("개인전") }
    var selectedRegion by remember { mutableStateOf("서울") }
    var isGpsQuiz by remember { mutableStateOf(false) }

    var finalScore by remember { mutableIntStateOf(0) }
    var finalTeamOneScore by remember { mutableIntStateOf(0) }
    var finalTeamTwoScore by remember { mutableIntStateOf(0) }

    BackHandler(enabled = page != "HOME") {
        page = when (page) {
            "MODE" -> if (isGpsQuiz) "HOME" else "REGION"
            "QUIZ" -> "MODE"
            "RESULT" -> "HOME"
            else -> "HOME"
        }
    }

    when (page) {
        "HOME" -> {
            LocalHomePage(
                gpsLatitude = gpsLatitude,
                gpsLongitude = gpsLongitude,

                onGpsClick = {
                    if (gpsLatitude != null && gpsLongitude != null) {
                        isGpsQuiz = true
                        selectedRegion = findNearestQuizRegion(
                            gpsLatitude,
                            gpsLongitude
                        )
                        page = "MODE"
                    } else {
                        onGpsTestClick()
                    }
                },
                onRegionClick = {
                    isGpsQuiz = false
                    page = "REGION"
                },
                onGpsTestClick = onGpsTestClick,
                onBack = onBack
            )
        }

        "REGION" -> {
            RegionSelectPage(
                selectedRegion = selectedRegion,
                onRegionSelected = { selectedRegion = it },
                onNext = { page = "MODE" },
                onBack = { page = "HOME" }
            )
        }

        "MODE" -> {
            LocalModePage(
                region = selectedRegion,
                isGpsQuiz = isGpsQuiz,
                onModeSelected = {
                    quizMode = it
                    page = "QUIZ"
                },
                onBack = {
                    page = if (isGpsQuiz) "HOME" else "REGION"
                }
            )
        }

        "QUIZ" -> {
            LocalGamePage(
                region = selectedRegion,
                quizMode = quizMode,
                onFinish = { score, teamOne, teamTwo ->
                    finalScore = score
                    finalTeamOneScore = teamOne
                    finalTeamTwoScore = teamTwo
                    page = "RESULT"
                }
            )
        }

        "RESULT" -> {
            LocalResultPage(
                region = selectedRegion,
                quizMode = quizMode,
                score = finalScore,
                teamOneScore = finalTeamOneScore,
                teamTwoScore = finalTeamTwoScore,
                totalQuestions = (localQuestionData[selectedRegion]
                    ?: localQuestionData["서울"]!!).size,
                onRetry = { page = "QUIZ" },
                onHome = { page = "HOME" }
            )
        }
    }
}

// LOCAL 첫 화면
@Composable
private fun LocalHomePage(
    gpsLatitude: Double?,
    gpsLongitude: Double?,
    onGpsClick: () -> Unit,
    onRegionClick: () -> Unit,
    onGpsTestClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(20.dp)
    ) {
        Text(
            text = "📍 TRIPQ LOCAL",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172B4D)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "위치와 지역을 활용한 퀴즈를 즐겨보세요!",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        // GPS QUIZ 카드
        LocalMenuCard(
            emoji = "📍",
            title = "GPS QUIZ",
            description = "현재 위치를 기반으로 지역 퀴즈 시작",
            onClick = onGpsClick
        )

        Spacer(Modifier.height(10.dp))

        // GPS 연결 상태 표시
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (
                    gpsLatitude != null && gpsLongitude != null
                ) {
                    Color(0xFFD5EFDE)
                } else {
                    Color(0xFFFFE9D6)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(15.dp)
            ) {
                Text(
                    text = if (
                        gpsLatitude != null && gpsLongitude != null
                    ) {
                        "🟢 GPS 연결 완료"
                    } else {
                        "🟠 GPS 연결 필요"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF254B3A)
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = if (
                        gpsLatitude != null && gpsLongitude != null
                    ) {
                        "위도: $gpsLatitude\n경도: $gpsLongitude"
                    } else {
                        "GPS 테스트에서 위치를 먼저 수신해 주세요."
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF547B65)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 직접 지역 선택 카드
        LocalMenuCard(
            emoji = "🗺️",
            title = "SELECT REGION",
            description = "원하는 지역을 직접 선택하는 퀴즈",
            onClick = onRegionClick
        )

        Spacer(Modifier.height(16.dp))

        // GPS 테스트 카드
        LocalMenuCard(
            emoji = "🛰️",
            title = "GPS CONNECTION TEST",
            description = "차량 GPS 연결 및 위치 수신 테스트",
            onClick = onGpsTestClick
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("BACK")
        }
    }
}

// LOCAL 메뉴 카드
@Composable
private fun LocalMenuCard(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 38.sp)
            Spacer(Modifier.width(18.dp))

            Column {
                Text(
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF254B3A)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    description,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// 지역 선택 화면
@Composable
private fun RegionSelectPage(
    selectedRegion: String,
    onRegionSelected: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val regions = listOf("서울", "부산", "제주", "경주", "인천", "강릉")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8F4))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "지역 선택",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF254B3A)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "퀴즈를 풀고 싶은 지역을 선택해 줘.",
            color = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        regions.chunked(2).forEach { rowRegions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowRegions.forEach { region ->
                    val selected = selectedRegion == region

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clickable {
                                onRegionSelected(region)
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                Color(0xFFD3EBDD)
                            } else {
                                Color.White
                            }
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (if (selected) "✓ " else "") + region,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF254B3A)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("이 지역으로 퀴즈 시작")
        }

        Text(
            "뒤로 가기",
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp),
            color = Color.Gray
        )
    }
}

// LOCAL 개인전 / 팀전 선택 화면
@Composable
private fun LocalModePage(
    region: String,
    isGpsQuiz: Boolean,
    onModeSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8F4))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (isGpsQuiz) "GPS QUIZ" else "$region 지역 퀴즈",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF254B3A)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            if (isGpsQuiz) {
                "현재 위치 기반 퀴즈 미리보기"
            } else {
                "플레이 방식을 선택해 줘."
            },
            color = Color.Gray
        )

        if (isGpsQuiz) {
            Spacer(Modifier.height(18.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("📍 GPS 판별 지역: $region")
                    Text(
                        "실제 차량 위치와 가장 가까운 지역의 퀴즈야.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        LocalMenuCard(
            emoji = "👤",
            title = "개인전",
            description = "나의 점수에 도전하기",
            onClick = { onModeSelected("개인전") }
        )

        Spacer(Modifier.height(14.dp))

        LocalMenuCard(
            emoji = "👨‍👩‍👧‍👦",
            title = "팀전",
            description = "두 팀이 번갈아 문제 풀기",
            onClick = { onModeSelected("팀전") }
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "뒤로 가기",
            modifier = Modifier
                .clickable { onBack() }
                .padding(12.dp),
            color = Color.Gray
        )
    }
}

// LOCAL 퀴즈 진행 화면
@Composable
private fun LocalGamePage(
    region: String,
    quizMode: String,
    onFinish: (Int, Int, Int) -> Unit
) {

    val questions = remember(region, quizMode) {
        (localQuestionData[region] ?: localQuestionData["서울"]!!)
            .shuffled()
    }


    var questionIndex by remember(region, quizMode) {
        mutableIntStateOf(0)
    }
    var score by remember(region, quizMode) {
        mutableIntStateOf(0)
    }
    var teamOneScore by remember(region, quizMode) {
        mutableIntStateOf(0)
    }
    var teamTwoScore by remember(region, quizMode) {
        mutableIntStateOf(0)
    }
    var currentTeam by remember(region, quizMode) {
        mutableIntStateOf(1)
    }
    var remainingTime by remember(region, quizMode) {
        mutableIntStateOf(15)
    }
    var selectedAnswer by remember(region, quizMode) {
        mutableStateOf<String?>(null)
    }
    var answered by remember(region, quizMode) {
        mutableStateOf(false)
    }

    val question = questions[questionIndex]

    // 문제별 15초 타이머
    LaunchedEffect(questionIndex, answered) {
        if (!answered) {
            remainingTime = 15

            while (remainingTime > 0 && !answered) {
                delay(1000)
                remainingTime--
            }

            if (remainingTime == 0) {
                answered = true
            }
        }
    }

    fun submitAnswer(answer: String?) {
        if (answered) return

        selectedAnswer = answer
        answered = true

        if (answer == question.answer) {
            if (quizMode == "개인전") {
                score += 10
            } else if (currentTeam == 1) {
                teamOneScore += 10
            } else {
                teamTwoScore += 10
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8F4))
            .padding(22.dp)
    ) {
        Text(
            "TRIPQ LOCAL",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF71877B)
        )

        Text(
            "$region · $quizMode",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF254B3A)
        )

        Spacer(Modifier.height(12.dp))

        if (quizMode == "개인전") {
            Text(
                "내 점수: $score 점",
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                "TEAM 1  $teamOneScore 점     TEAM 2  $teamTwoScore 점",
                fontWeight = FontWeight.Bold
            )
            Text(
                "현재 차례: TEAM $currentTeam",
                color = Color(0xFF547B65)
            )
        }

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = {
                (questionIndex + 1) / questions.size.toFloat()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "문제 ${questionIndex + 1} / ${questions.size}",
            color = Color.Gray
        )

        Text(
            "남은 시간: ${if (answered && selectedAnswer == null) 0 else remainingTime}초",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (remainingTime <= 5) {
                Color.Red
            } else {
                Color(0xFF254B3A)
            }
        )

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Text(
                question.question,
                modifier = Modifier.padding(22.dp),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF254B3A)
            )
        }

        Spacer(Modifier.height(18.dp))

        question.options.forEachIndexed { index, option ->
            val isCorrect = option == question.answer
            val isSelected = option == selectedAnswer

            val optionColor = when {
                answered && isCorrect -> Color(0xFFD5EFDE)
                answered && isSelected -> Color(0xFFFFDDDD)
                else -> Color.White
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable {
                        submitAnswer(option)
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = optionColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${'A' + index}.",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF547B65)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(option, fontSize = 16.sp)
                }
            }
        }

        if (answered) {
            Spacer(Modifier.height(12.dp))

            Text(
                if (selectedAnswer == question.answer) {
                    "정답이야! +10점"
                } else {
                    "정답: ${question.answer}"
                },
                fontWeight = FontWeight.Bold,
                color = Color(0xFF254B3A)
            )

            Text(
                question.explanation,
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (questionIndex == questions.lastIndex) {
                        onFinish(score, teamOneScore, teamTwoScore)
                    } else {
                        questionIndex++
                        selectedAnswer = null
                        answered = false

                        if (quizMode == "팀전") {
                            currentTeam = if (currentTeam == 1) 2 else 1
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (questionIndex == questions.lastIndex) {
                        "결과 확인"
                    } else {
                        "다음 문제"
                    }
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
            Text(
                "정답을 선택해 줘.",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// LOCAL 결과 화면
@Composable
private fun LocalResultPage(
    region: String,
    quizMode: String,
    score: Int,
    teamOneScore: Int,
    teamTwoScore: Int,
    totalQuestions: Int,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F8F4))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏆", fontSize = 58.sp)

        Spacer(Modifier.height(16.dp))

        Text(
            "LOCAL QUIZ 완료!",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF254B3A)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "$region · $quizMode",
            color = Color.Gray
        )

        Spacer(Modifier.height(22.dp))

        if (quizMode == "개인전") {
            Text(
                "$score / ${totalQuestions * 10}점",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF254B3A)
            )
            Text(
                "정답 ${score / 10}개 / 총 ${totalQuestions}문제",
                color = Color.Gray
            )
        } else {
            Text(
                "TEAM 1  $teamOneScore 점",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "TEAM 2  $teamTwoScore 점",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    teamOneScore > teamTwoScore -> "🥇 TEAM 1 승리!"
                    teamTwoScore > teamOneScore -> "🥇 TEAM 2 승리!"
                    else -> "🤝 무승부!"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF254B3A)
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("다시 도전하기")
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LOCAL 메뉴로")
        }
    }
}

// MUSIC 준비 화면
@Composable
fun GameReadyScreen(
    quizTitle: String,
    gameMode: String,
    onBack: () -> Unit
) {
    val description = when (quizTitle) {
        "TRIPQ MUSIC" ->
            "좋아하는 음악을 듣고 제목과 가수를 맞혀보세요!"
        else ->
            "새로운 퀴즈를 준비하고 있어요!"
    }

    val emoji = when (quizTitle) {
        "TRIPQ MUSIC" -> "🎵"
        else -> "🚙"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F4))
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 65.sp)

        Spacer(Modifier.height(20.dp))

        Text(
            quizTitle,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF294A38)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            gameMode,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF52695B)
        )

        Spacer(Modifier.height(18.dp))

        Text(
            description,
            fontSize = 17.sp,
            color = Color(0xFF65756C)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "해당 퀴즈 기능은 다음 개발 단계에서 연결할 예정입니다.",
            fontSize = 13.sp,
            color = Color(0xFF819187)
        )

        Spacer(Modifier.height(30.dp))

        SimpleBackButton(onClick = onBack)
    }
}

// 공통 뒤로 가기 버튼
@Composable
fun SimpleBackButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(48.dp)
    ) {
        Text(
            "← 뒤로 가기",
            fontSize = 15.sp
        )
    }
}