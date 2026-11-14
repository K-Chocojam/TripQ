
package com.tripq.tripq

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

// 음악 문제 데이터
private data class MusicQuestion(
    val category: String,
    val answer: String,
    val choices: List<String>,
    val melodyId: Int,
    val audioUri: Uri? = null
)

// melodyId는 MusicMelodyPlayer의 멜로디 순서와 연결됨
private val musicQuestionBank = listOf(
    MusicQuestion("K-POP", "Dynamite",
        listOf("Dynamite", "Butter", "Spring Day", "DNA"), 0),

    MusicQuestion("K-POP", "Ditto",
        listOf("Ditto", "Hype Boy", "Attention", "Super Shy"), 1),

    MusicQuestion("K-POP", "LOVE DIVE",
        listOf("LOVE DIVE", "ELEVEN", "After LIKE", "I AM"), 2),

    MusicQuestion("발라드", "좋은 날",
        listOf("좋은 날", "너랑 나", "밤편지", "Celebrity"), 3),

    MusicQuestion("추억의 명곡", "벚꽃 엔딩",
        listOf("벚꽃 엔딩", "여수 밤바다", "첫사랑", "꽃송이가"), 4),

    MusicQuestion("추억의 명곡", "강남스타일",
        listOf("강남스타일", "젠틀맨", "새", "챔피언"), 5),

    MusicQuestion("발라드", "사건의 지평선",
        listOf("사건의 지평선", "비밀번호 486", "오르트구름", "혜성"), 6),

    MusicQuestion("K-POP", "Super Shy",
        listOf("Super Shy", "Ditto", "ETA", "Attention"), 7),

    MusicQuestion("K-POP", "DALLA DALLA",
        listOf("DALLA DALLA", "WANNABE", "LOCO", "Not Shy"), 8),

    MusicQuestion("발라드", "밤양갱",
        listOf("밤양갱", "사랑은 은하수 다방에서", "나의 사춘기에게", "여행"), 9)
)

// 사용자가 선택한 음악 파일로 퀴즈 생성
private fun createCustomMusicQuestions(
    context: android.content.Context,
    uris: List<Uri>
): List<MusicQuestion> {

    val tracks = uris.mapIndexedNotNull { index, uri ->

        val fileName = try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        } ?: uri.lastPathSegment ?: "내 음악 ${index + 1}"

        val title = fileName
            .substringAfterLast('/')
            .substringBeforeLast('.')
            .trim()

        if (title.isBlank()) null else uri to title
    }.distinctBy { it.second.lowercase() }

    return tracks.map { (uri, title) ->

        val wrongAnswers = (
                tracks.map { it.second } +
                        musicQuestionBank.map { it.answer }
                )
            .filterNot { it.equals(title, ignoreCase = true) }
            .distinct()
            .shuffled()
            .take(3)

        MusicQuestion(
            category = "내 음악",
            answer = title,
            choices = (wrongAnswers + title).shuffled(),
            melodyId = -1,
            audioUri = uri
        )
    }
}

private val musicCategories = listOf(
    "K-POP",
    "발라드",
    "추억의 명곡",
    "랜덤 플레이",
    "내 음악"
)

@Composable
fun MusicQuizScreen(
    initialMode: String,
    onBack: () -> Unit
) {
    var questions by remember {
        mutableStateOf(musicQuestionBank.shuffled())
    }

    var page by remember {
        mutableStateOf("CATEGORY")
    }

    var selectedCategory by remember {
        mutableStateOf("랜덤 플레이")
    }

    var questionIndex by remember {
        mutableIntStateOf(0)
    }

    var score by remember {
        mutableIntStateOf(0)
    }

    var player1Score by remember {
        mutableIntStateOf(0)
    }

    var player2Score by remember {
        mutableIntStateOf(0)
    }

    var currentPlayer by remember {
        mutableIntStateOf(1)
    }

    var selectedAnswer by remember {
        mutableStateOf<String?>(null)
    }

    var isAnswered by remember {
        mutableStateOf(false)
    }

    var timeLeft by remember {
        mutableIntStateOf(15)
    }

    var isCorrect by remember {
        mutableStateOf(false)
    }

    val isTeamMode = initialMode.contains("팀")
    val context = LocalContext.current

    var customMusicQuestions by remember {
        mutableStateOf<List<MusicQuestion>>(emptyList())
    }

    // 내 음악 퀴즈 시작
    fun startCustomMusic(uris: List<Uri>) {

        val newQuestions = createCustomMusicQuestions(
            context,
            uris
        )

        if (newQuestions.isEmpty()) return

        customMusicQuestions = newQuestions
        selectedCategory = "내 음악"
        questions = newQuestions

        questionIndex = 0
        score = 0
        player1Score = 0
        player2Score = 0
        currentPlayer = 1
        selectedAnswer = null
        isAnswered = false
        isCorrect = false
        timeLeft = 15

        page = "QUIZ"
    }

    // 휴대폰 음악 파일 선택기
    val musicPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            if (uris.isNotEmpty()) {

                uris.forEach { uri ->
                    try {
                        context.contentResolver
                            .takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                    } catch (_: Exception) {
                    }
                }

                startCustomMusic(uris)
            }
        }

    val currentQuestion = questions[questionIndex]

    val melodyPlayer = remember {
        MusicMelodyPlayer()
    }

    DisposableEffect(melodyPlayer) {
        onDispose {
            melodyPlayer.release()
        }
    }

    val shuffledChoices = remember(currentQuestion) {
        currentQuestion.choices.shuffled()
    }

    // 카테고리 선택 후 게임 시작
    fun startCategory(category: String) {
        selectedCategory = category

        questions = if (category == "랜덤 플레이") {
            musicQuestionBank.shuffled()
        } else {
            musicQuestionBank
                .filter { it.category == category }
                .shuffled()
        }

        questionIndex = 0
        score = 0
        player1Score = 0
        player2Score = 0
        currentPlayer = 1
        selectedAnswer = null
        isAnswered = false
        isCorrect = false
        timeLeft = 15
        page = "QUIZ"
    }

    // 다음 문제
    fun nextQuestion() {
        if (questionIndex < questions.lastIndex) {
            questionIndex++
            selectedAnswer = null
            isAnswered = false
            isCorrect = false
            timeLeft = 15

            if (isTeamMode) {
                currentPlayer = if (currentPlayer == 1) 2 else 1
            }
        } else {
            page = "RESULT"
        }
    }

    // 뒤로 가기
    BackHandler {
        if (page == "QUIZ" || page == "RESULT") {
            melodyPlayer.stop()
            page = "CATEGORY"
        } else {
            onBack()
        }
    }

    // 제한 시간
    LaunchedEffect(page, questionIndex, isAnswered) {
        if (page == "QUIZ" && !isAnswered) {
            timeLeft = 15

            for (remaining in 15 downTo 1) {
                delay(1000)
                timeLeft = remaining - 1

                if (timeLeft == 0) {
                    isAnswered = true
                    break
                }
            }
        }
    }

    // 문제별 멜로디 자동 재생
    LaunchedEffect(page, questionIndex, isAnswered) {
        if (page == "QUIZ" && !isAnswered) {

            val audioUri = currentQuestion.audioUri

            if (audioUri != null) {
                melodyPlayer.play(context, audioUri)
            } else {
                melodyPlayer.play(currentQuestion.melodyId)
            }

        } else {
            melodyPlayer.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F4))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 상단
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← 뒤로",
                color = Color(0xFF52695B),
                fontSize = 16.sp,
                modifier = Modifier.clickable {
                    if (page == "QUIZ" || page == "RESULT") {
                        melodyPlayer.stop()
                        page = "CATEGORY"
                    } else {
                        onBack()
                    }
                }
            )

            Text(
                text = "TRIPQ MUSIC",
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF294A38)
            )

            Text(
                text = if (isTeamMode) "팀전" else "개인전",
                color = Color(0xFF52695B),
                fontWeight = FontWeight.Bold
            )
        }

        // 카테고리 선택 화면
        if (page == "CATEGORY") {

            Spacer(modifier = Modifier.height(45.dp))

            Text(
                text = "🎧",
                fontSize = 65.sp
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "어떤 음악을 좋아하세요?",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF294A38)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "퀴즈를 진행할 음악 카테고리를 선택하세요.",
                fontSize = 14.sp,
                color = Color(0xFF819187),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            musicCategories.forEach { category ->

                val description = when (category) {
                    "K-POP" -> "최신 인기 아이돌 음악"
                    "발라드" -> "감성을 자극하는 음악"
                    "추억의 명곡" -> "오래도록 사랑받는 노래"
                    "내 음악" -> "휴대폰 음악 파일로 나만의 퀴즈 만들기"
                    else -> "모든 카테고리에서 무작위 출제"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp)
                        .clickable {
                            if (category == "내 음악") {
                                musicPickerLauncher.launch(
                                    arrayOf("audio/*")
                                )
                            } else {
                                startCategory(category)
                            }
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF294A38)
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = description,
                            fontSize = 13.sp,
                            color = Color(0xFF819187)
                        )
                    }
                }
            }
        }

        // 퀴즈 화면
        if (page == "QUIZ") {

            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = "🎧 음악 퀴즈",
                fontSize = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF294A38)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$selectedCategory  ·  ${questionIndex + 1} / ${questions.size} 문제",
                fontSize = 15.sp,
                color = Color(0xFF819187)
            )

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
                progress = { timeLeft / 15f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp),
                color = Color(0xFF6C9B7D),
                trackColor = Color(0xFFDCE7DE)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "남은 시간: ${timeLeft}초",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF52695B)
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isTeamMode) {
                Text(
                    text = "현재 ${currentPlayer}팀 차례!",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF527D62)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "🎵",
                        fontSize = 55.sp
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        text = "이 노래의 제목은?",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF294A38)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "정답을 선택해 주세요.",
                        fontSize = 14.sp,
                        color = Color(0xFF819187)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (!isAnswered) {
                                currentQuestion.audioUri?.let { uri ->
                                    melodyPlayer.play(context, uri)
                                } ?: melodyPlayer.play(currentQuestion.melodyId)
                            }
                        },
                        enabled = !isAnswered,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF527D62)
                        )
                    ) {
                        Text("🔊 멜로디 다시 듣기")
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    shuffledChoices.forEach { choice ->

                        val answerColor = when {
                            isAnswered &&
                                    choice == currentQuestion.answer ->
                                Color(0xFFD9EBDD)

                            isAnswered &&
                                    choice == selectedAnswer ->
                                Color(0xFFF8DADA)

                            else ->
                                Color(0xFFF2F5F2)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable {
                                    if (!isAnswered) {
                                        selectedAnswer = choice
                                        isCorrect =
                                            choice == currentQuestion.answer

                                        if (isCorrect) {
                                            if (isTeamMode) {
                                                if (currentPlayer == 1) {
                                                    player1Score += 10
                                                } else {
                                                    player2Score += 10
                                                }
                                            } else {
                                                score += 10
                                            }
                                        }

                                        isAnswered = true
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = answerColor
                            )
                        ) {
                            Text(
                                text = choice,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(15.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF294A38)
                            )
                        }
                    }

                    if (isAnswered) {

                        Spacer(modifier = Modifier.height(15.dp))

                        Text(
                            text = when {
                                selectedAnswer == null ->
                                    "시간 초과! 정답은 ${currentQuestion.answer}"

                                isCorrect ->
                                    "정답입니다! 🎉"

                                else ->
                                    "오답입니다. 정답은 ${currentQuestion.answer}"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF527D62),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                nextQuestion()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF527D62)
                            )
                        ) {
                            Text(
                                if (questionIndex == questions.lastIndex) {
                                    "결과 확인"
                                } else {
                                    "다음 문제"
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            if (isTeamMode) {
                Text(
                    text = "1팀 ${player1Score}점  |  2팀 ${player2Score}점",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF294A38)
                )
            } else {
                Text(
                    text = "현재 점수: ${score}점",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF294A38)
                )
            }
        }

        // 결과 화면
        if (page == "RESULT") {

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "🏆",
                fontSize = 75.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "퀴즈 완료!",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF294A38)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "선택 카테고리: $selectedCategory",
                fontSize = 15.sp,
                color = Color(0xFF819187)
            )

            Spacer(modifier = Modifier.height(25.dp))

            if (isTeamMode) {

                Text(
                    text = "1팀 최종 점수",
                    fontSize = 17.sp,
                    color = Color(0xFF819187)
                )

                Text(
                    text = "${player1Score}점",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF294A38)
                )

                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = "2팀 최종 점수",
                    fontSize = 17.sp,
                    color = Color(0xFF819187)
                )

                Text(
                    text = "${player2Score}점",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF294A38)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = when {
                        player1Score > player2Score -> "🎉 1팀 승리!"
                        player2Score > player1Score -> "🎉 2팀 승리!"
                        else -> "🤝 무승부!"
                    },
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF527D62)
                )

            } else {

                Text(
                    text = "최종 점수",
                    fontSize = 17.sp,
                    color = Color(0xFF819187)
                )

                Text(
                    text = "${score}점",
                    fontSize = 45.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF527D62)
                )

                Text(
                    text = "${questions.size}문제 중 ${score / 10}문제 정답",
                    fontSize = 17.sp,
                    color = Color(0xFF52695B)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    if (selectedCategory == "내 음악") {
                        startCustomMusic(
                            customMusicQuestions.mapNotNull { it.audioUri }
                        )
                    } else {
                        startCategory(selectedCategory)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF527D62)
                )
            ) {
                Text("같은 카테고리 다시 플레이")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    melodyPlayer.stop()
                    page = "CATEGORY"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF819187)
                )
            ) {
                Text("카테고리 선택으로")
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
