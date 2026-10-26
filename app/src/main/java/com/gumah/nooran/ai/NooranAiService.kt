package com.gumah.nooran.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Error(val message: String) : AiResult
}

class NooranAiService {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun reviewContentSafety(text: String): AiResult {
        return try {
            val prompt = """
                أنت مساعد تدقيق المحتوى الأدبي والإسلامي لمنصة "نُوران" الاجتماعية الهادئة.
                قم بفحص النص التالي من حيث:
                1. الألفاظ النابية أو العدائية أو الإساءة.
                2. علامات السبام والتكرار غير المفيد.
                3. مراعاة الأدب والسكينة والوقار في الطرح الإسلامي.
                (ملاحظة: لا تصدر فتاوى شرعية كأحكام ملزمة، بل ركّز على نبل الكلمة وطيب الأثر).
                
                النص للمراجعة:
                «$text»
                
                قدم تقريرًا لطيفًا ومختصرًا (2-3 أسطر باللغة العربية) يتضمن:
                - تقييم السلامة والسكينة.
                - اقتراح تحسين إن وجد.
            """.trimIndent()

            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val output = response.text
            if (!output.isNullOrBlank()) {
                AiResult.Success(output.trim())
            } else {
                AiResult.Error("لم يتمكن المساعد من تحليل النص حالياً.")
            }
        } catch (e: TimeoutCancellationException) {
            AiResult.Error("استغرق فحص المحتوى وقتاً طويلاً. يرجى المحاولة لاحقاً.")
        } catch (e: Exception) {
            Log.e("NooranAiService", "Safety check error", e)
            AiResult.Error("تعذر الاتصال بخدمة الذكاء الاصطناعي: ${e.localizedMessage}")
        }
    }

    suspend fun enhancePost(text: String, action: String): AiResult {
        return try {
            val instruction = when (action) {
                "polish" -> "أعد صياغة هذا المنشور بلغة عربية فصيحة، راقية، مفعمة بالنور والسكينة والأثر الطيب، دون تغيير المعنى."
                "summarize" -> "لخص هذه الكلمات في فكرة مركزة وعبارة بليغة تلهم القلوب."
                "title" -> "اقترح عنواناً جذاباً وهادئاً (3-5 كلمات) لهذا المحتوى."
                "hashtags" -> "اقترح 4 إلى 6 وسوم (هاشتاغات) هادفة وملائمة لهذا المنشور."
                "thread_organize" -> "قسّم ورتّب هذه الخاطرة أو المحتوى إلى سلسلة منشورات قصيرة مرقمة (1/3, 2/3, 3/3) سهلة القراءة."
                else -> "حسّن هذا النص ليترك أثراً مباركاً وطيباً."
            }

            val prompt = """
                أنت رفيق الكلمة الطيبة في منصة "نُوران" (كلمات تنير القلب).
                المهمة المطلوبة: $instruction
                
                النص الأصلي:
                «$text»
                
                أعطني النتيجة مباشرة باللغة العربية بأسلوب راقٍ ومختصر، بدون مقدمات أو خاتمات شكلية.
            """.trimIndent()

            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val output = response.text
            if (!output.isNullOrBlank()) {
                AiResult.Success(output.trim())
            } else {
                AiResult.Error("لم يتم استلام اقتراح.")
            }
        } catch (e: TimeoutCancellationException) {
            AiResult.Error("انتهت مهلة المساعد الذكي. يرجى إعادة المحاولة.")
        } catch (e: Exception) {
            Log.e("NooranAiService", "Enhance error", e)
            AiResult.Error("تعذر الاتصال بخدمة الذكاء الاصطناعي: ${e.localizedMessage}")
        }
    }
}
