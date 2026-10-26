package com.edan.kesef.wallet

import com.edan.kesef.data.CatIds

/**
 * ניחוש קטגוריה לפי שם בית העסק, לפי מילות מפתח.
 * זה רק ניחוש ראשון: ברגע שתשנה קטגוריה לעסק מסוים, האפליקציה זוכרת (MerchantRule)
 * והחוק שלך גובר על הרשימה הזו.
 * רוצה להוסיף עסק? פשוט מוסיפים מילה לרשימה המתאימה.
 */
object AutoCategorizer {

    private val keywords: Map<Long, List<String>> = mapOf(
        CatIds.GROCERIES to listOf(
            "shufersal", "שופרסל", "rami levy", "רמי לוי", "victory", "ויקטורי", "yohananof", "יוחננוף",
            "osher ad", "אושר עד", " mega ", "מגה", "tiv taam", "טיב טעם", "carrefour", "קרפור",
            "am:pm", "ampm", "yeinot bitan", "יינות ביתן", "hatzi hinam", "חצי חינם", "super", "סופר",
            "מכולת", "קשת טעמים", "freshmarket", "פרשמרקט",
        ),
        CatIds.FOOD_OUT to listOf(
            "wolt", "וולט", "10bis", "תן ביס", "cibus", "סיבוס", "aroma", "ארומה", "cafe", "קפה",
            "mcdonald", "מקדונלד", "burger", "בורגר", "pizza", "פיצה", "domino", "דומינו", "japanika",
            "ג'פניקה", "restaurant", "מסעדה", "falafel", "פלאפל", "shawarma", "שווארמה", "greg", "גרג",
            "landwer", "לנדוור", "kfc", "bbb", "מאפה", "bakery",
        ),
        CatIds.TRANSPORT to listOf(
            "rav kav", "רב-קו", "רב קו", "ravkav", "gett", "yango", "uber", "moovit", "מוביט",
            "pango", "פנגו", "cellopark", "סלופארק", "paz", "פז", "delek", "דלק", "sonol", "סונול",
            " ten ", "דור אלון", "dor alon", "yellow", "ילו", "רכבת", "israel railways", "אגד", "egged",
            "חניון", "parking",
        ),
        CatIds.BILLS to listOf(
            "partner", "פרטנר", "cellcom", "סלקום", "pelephone", "פלאפון", " hot ", "הוט", "bezeq",
            "בזק", "golan", "גולן", "electric", "חברת החשמל", " מי ", "water", "arnona", "ארנונה",
            "netflix", "spotify", "youtube", "apple.com", "google one", "icloud",
        ),
        CatIds.SHOPPING to listOf(
            "amazon", "אמזון", "aliexpress", "shein", "zara", "castro", "קסטרו", "h&m", " fox ", "פוקס",
            "terminal x", "טרמינל", "ikea", "איקאה", " ace ", "אייס", "ksp", " bug ", "באג", "max stock",
            "מקס סטוק", "home center", "הום סנטר", "decathlon", "דקטלון", "renuar", "רנואר",
        ),
        CatIds.HEALTH to listOf(
            "super-pharm", "super pharm", "סופר-פארם", "סופר פארם", "be pharm", "pharm", "פארם",
            "maccabi", "מכבי", "clalit", "כללית", "meuhedet", "מאוחדת", "leumit", "לאומית",
            "בית מרקחת", "pharmacy", "dentist", "רופא",
        ),
        CatIds.FUN to listOf(
            "cinema", "סינמה", "yes planet", "יס פלאנט", " lev ", "steam", "playstation", "xbox",
            " bar ", " בר ", "pub", "פאב", "eventim", "leaan", "לאן", "כרטיס", "ticket",
        ),
        CatIds.STUDY to listOf(
            "university", "אוניברסיטה", "מכללה", "college", "steimatzky", "סטימצקי", "צומת ספרים",
            "udemy", "coursera", "ספרים",
        ),
    )

    fun guess(merchant: String): Long? {
        val m = " ${merchant.lowercase()} "
        // מחפשים את מילת המפתח הארוכה ביותר שמתאימה. "super-pharm" צריך לנצח את "super".
        return keywords
            .flatMap { (cat, words) -> words.map { it to cat } }
            .filter { (word, _) -> m.contains(word.lowercase()) }
            .maxByOrNull { (word, _) -> word.length }
            ?.second
    }
}
