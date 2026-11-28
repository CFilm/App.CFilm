package com.example.peliselprimazo.ui.utils

import android.content.Context

object SectionTitleTranslator {

    fun getLocalizedTitle(title: String, context: Context): String {
        val lang = LanguageManager.getCurrentLanguageCode(context)
        if (lang == "es") return title

        return when {
            title.contains("Continuar Viendo", ignoreCase = true) -> get("🍿 Continue Watching", "🍿 Continuar Assistindo", "🍿 Continuer à regarder", "🍿 Weiter ansehen", "🍿 Continua a guardare", "🍿 Продолжить просмотр", "🍿 继续观看", "🍿 Pokračovat ve sledování", "🍿 視聴を再開", "🍿 이어서 보기", "🍿 İzlemeye Devam Et", "🍿 متابعة المشاهدة", "🍿 देखना जारी रखें", "🍿 Lanjutkan Menonton", "🍿 Verder Kijken", "🍿 Kontynuuj oglądanie", "🍿 Tiếp tục xem", "🍿 รับชมต่อ", lang)
            title.contains("Recién Agregados", ignoreCase = true) -> get("🆕 Recently Added", "🆕 Adicionados Recentemente", "🆕 Récemment ajoutés", "🆕 Kürzlich hinzugefügt", "🆕 Aggiunti di recente", "🆕 Недавно добавленные", "🆕 最近添加", "🆕 Nedávno přidané", "🆕 新着作品", "🆕 최근 추가됨", "🆕 Yeni Eklenenler", "🆕 أضيف مؤخراً", "🆕 हाल ही में जोड़े गए", "🆕 Baru Ditambahkan", "🆕 Onlangs Toegevoegd", "🆕 Ostatnio dodane", "🆕 Mới thêm gần đây", "🆕 เพิ่มล่าสุด", lang)
            title.contains("Recomendados del Día", ignoreCase = true) -> get("✨ Recommended for You", "✨ Recomendados para Você", "✨ Recommandés pour vous", "✨ Empfehlungen des Tages", "✨ Consigliati per te", "✨ Рекомендации дня", "✨ 今日推荐", "✨ Doporučeno pro vás", "✨ 本日のおすすめ", "✨ 오늘의 추천작", "✨ Günün Tavsiyeleri", "✨ مقترحات اليوم", "✨ आज की सिफारिशें", "✨ Rekomendasi Hari Ini", "✨ Aanbevolen voor jou", "✨ Polecane dla Ciebie", "✨ Đề xuất cho bạn", "✨ แนะนำสำหรับคุณ", lang)
            title.contains("Tendencias Globales", ignoreCase = true) -> get("🔥 Trending Worldwide", "🔥 Tendências Globais", "🔥 Tendances mondiales", "🔥 Globale Trends", "🔥 Tendenze globali", "🔥 Мировые тренды", "🔥 全球热门", "🔥 Globální trendy", "🔥 世界のトレンド", "🔥 글로벌 트렌드", "🔥 Dünya Trendleri", "🔥 التوجهات العالمية", "🔥 ग्लोबल ट्रेंड्स", "🔥 Tren Global", "🔥 Wereldwijde Trends", "🔥 Globalne trendy", "🔥 Xu hướng toàn cầu", "🔥 เทรนด์ฮิตทั่วโลก", lang)
            title.contains("Estrenos", ignoreCase = true) -> get("🚀 New Releases", "🚀 Lançamentos", "🚀 Nouveautés", "🚀 Neuerscheinungen", "🚀 Nuove uscite", "🚀 Новинки", "🚀 最新上映", "🚀 Novinky", "🚀 新作リリース", "🚀 신작 출시", "🚀 Yeni Çıkanlar", "🚀 الإصدارات الجديدة", "🚀 नई रिलीज़", "🚀 Rilis Baru", "🚀 Nieuwe Releases", "🚀 Nowości", "🚀 Phim mới phát hành", "🚀 ผลงานใหม่ล่าสุด", lang)
            title.contains("Más Valorados", ignoreCase = true) -> get("⭐ Top Rated", "⭐ Mais Avaliados", "⭐ Les mieux notés", "⭐ Am besten bewertet", "⭐ I più votati", "⭐ Лучшие по рейтингу", "⭐ 高分好评", "⭐ Nejlépe hodnocené", "⭐ 高評価作品", "⭐ 최고 평점", "⭐ En Çok Beğenilenler", "⭐ الأعلى تقييماً", "⭐ टॉप रेटेड", "⭐ Rating Tertinggi", "⭐ Best Beoordeeld", "⭐ Najwyżej oceniane", "⭐ Đánh giá cao nhất", "⭐ คะแนนสูงสุด", lang)
            title.contains("Acción", ignoreCase = true) -> get("💥 Action & Adrenaline", "💥 Ação e Adrenalina", "💥 Action & Adrénaline", "💥 Action & Adrenalin", "💥 Azione & Adrenalina", "💥 Экшн и Адреналин", "💥 动作与刺激", "💥 Akce a Adrenalin", "💥 アクション＆アドレナリン", "💥 액션 & 스릴", "💥 Aksiyon ve Adrenalin", "💥 أكشن وإثارة", "💥 एक्शन और रोमांच", "💥 Aksi & Adrenalin", "💥 Actie & Adrenaline", "💥 Akcja i Adrenalina", "💥 Hành động & Kịch tính", "💥 แอ็กชันสุดมันส์", lang)
            title.contains("Terror", ignoreCase = true) -> get("😱 Horror & Thrill", "😱 Terror e Horror", "😱 Horreur & Frissons", "😱 Horror & Nervenkitzel", "😱 Horror & Brividi", "😱 Ужасы и Хорор", "😱 恐怖惊悚", "😱 Horor a Strach", "😱 ホラー＆スリラー", "😱 공포 & 호러", "😱 Korku ve Gerilim", "😱 رعب وإثارة", "😱 हॉरर और खौफ", "😱 Horor & Mencekam", "😱 Horror & Griezel", "😱 Horror i Groza", "😱 Kinh dị & Rùng rợn", "😱 สยองขวัญ", lang)
            title.contains("Comedia", ignoreCase = true) -> get("😂 Comedy & Laughs", "😂 Comédia e Risadas", "😂 Comédie & Rires", "😂 Komödie & Lachen", "😂 Commedia & Risate", "😂 Комедии и Смех", "😂 喜剧搞笑", "😂 Komedie a Zábava", "😂 コメディ＆爆笑", "😂 코미디 & 웃음", "😂 Komedi ve Kahkaha", "😂 كوميديا وضحت", "😂 कॉमेडी और हंसी", "😂 Komedi & Tawa", "😂 Komedie & Lachen", "😂 Komedia i Śmiech", "<ctrl42> Hài hước & Tiếng cười", "😂 ตลกเฮฮา", lang)
            title.contains("Drama", ignoreCase = true) -> get("🎭 Intense Drama", "🎭 Dramas Intensos", "🎭 Drame intense", "🎭 Intensives Drama", "🎭 Dramma intenso", "🎭 Драмы и Эмоции", "🎭 深度剧情", "🎭 Intenzivní Drama", "🎭 ドラマ＆感動", "🎭 감동 드라마", "🎭 Yoğun Dram", "🎭 دراما مؤثرة", "🎭 ड्रामा और भावनाएं", "🎭 Drama Mendalam", "🎭 Intens Drama", "🎭 Dramat i Emocje", "🎭 Phim tâm lý drama", "🎭 ดราม่าสุดเข้มข้น", lang)
            title.contains("Fantasía", ignoreCase = true) -> get("🦄 Fantasy & Magic", "🦄 Fantasia e Magia", "🦄 Fantastique & Magie", "🦄 Fantasy & Magie", "🦄 Fantasy & Magia", "🦄 Фэнтези и Магия", "🦄 奇幻冒险", "🦄 Fantazie a Magie", "🦄 ファンタジー＆魔法", "🦄 판타지 & 마법", "🦄 Fantazi ve Büyü", "🦄 خيال وسحر", "🦄 फंतासी और जादू", "🦄 Fantasi & Keajaiban", "🦄 Fantasie & Magie", "🦄 Fantastyka i Magia", "🦄 Phim kỳ ảo", "🦄 แฟนตาซีและเวทมนตร์", lang)
            else -> title
        }
    }

    private fun get(
        en: String, pt: String, fr: String, de: String, it: String,
        ru: String, zh: String, cs: String, ja: String, ko: String,
        tr: String, ar: String, hi: String, id: String, nl: String,
        pl: String, vi: String, th: String, lang: String
    ): String {
        return when (lang) {
            "en" -> en
            "pt" -> pt
            "fr" -> fr
            "de" -> de
            "it" -> it
            "ru" -> ru
            "zh" -> zh
            "cs" -> cs
            "ja" -> ja
            "ko" -> ko
            "tr" -> tr
            "ar" -> ar
            "hi" -> hi
            "id" -> id
            "nl" -> nl
            "pl" -> pl
            "vi" -> vi
            "th" -> th
            else -> en
        }
    }
}
