package com.bassengine.core

data class TuningInfo(
    val modelKey: String,
    val title: String,
    val detail: String,
    val known: Boolean
)

/**
 * A small, deliberately conservative model database. Unknown devices get a
 * generic neutral tuning instead of pretending to know their speaker array.
 */
object DeviceTuning {
    private val known = listOf(
        TuningInfo(
            "1010", "1010 对称双单元",
            "对称双单元，低频紧致，中频通透；建议预增益 0～+3 dB，低音 Q 2.8。", true
        ),
        TuningInfo(
            "PLK110", "一加 15 对称双单元",
            "一加 15 上下双扬声器，低频紧致、中频略薄；推荐哈曼曲线，预增益 -1～0 dB，环绕 45%。", true
        ),
        TuningInfo(
            "PJZ110", "一加 13 双扬声器",
            "上下双单元，低频下潜一般、中高频明亮；建议低音增强 + 虚拟低频，环绕 50%。", true
        ),
        TuningInfo(
            "2211133C", "小米 13 立体声",
            "顶部小单元 + 底部主单元，横向声场偏窄；建议宽场 55%，削减 8 kHz 齿音。", true
        ),
        TuningInfo(
            "SM-S9280", "Galaxy S24 Ultra",
            "对称双扬声器，解析力好；建议 Hi-Res 平直曲线，低音增强轻量开启。", true
        ),
        TuningInfo(
            "V2309A", "vivo X100 Pro",
            "双扬声器偏暖，低频量感足；建议低音 Q 2.2，预增益 -1 dB 防轰头。", true
        )
    )

    fun detect(model: String, manufacturer: String, lockedModel: String?): TuningInfo {
        val key = lockedModel?.takeIf { it.isNotBlank() } ?: model
        val candidates = listOf(key, model).filter { it.isNotBlank() }
        known.firstOrNull { info ->
            candidates.any { c ->
                c.equals(info.modelKey, ignoreCase = true) ||
                    c.contains(info.modelKey, ignoreCase = true)
            }
        }?.let {
            return if (lockedModel != null) it.copy(detail = it.detail + "（已手动锁定）") else it
        }
        val safeModel = model.ifBlank { "未知机型" }
        val safeMaker = manufacturer.ifBlank { "Android" }
        return TuningInfo(
            modelKey = key,
            title = "$safeMaker $safeModel",
            detail = "未收录该机型，使用通用双单元曲线：低频紧致、中频通透；可手动锁定档案后微调。",
            known = false
        )
    }

    fun allKnown(): List<TuningInfo> = known
}
