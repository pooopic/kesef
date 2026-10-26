"""
ממיר את גיליון "סיכום פיננסי - הכנסות מול הוצאות" (Google Sheets, מיוצא כ-CSV)
לפורמט הייבוא של האפליקציה (docs/IMPORT_FORMAT.md).

שימוש:
    python tools/convert_financial_summary.py "גיליון.csv"

בגיליון כבר יש עמודת "סיווג" שמסמנת כל שורה:
    הוצאת אשראי / הוצאה ישירה בבנק / הכנסה  → נכנסות
    כפילות מקס                            → מדלגים: זה החיוב החודשי של הכרטיס בבנק,
                                             וההוצאות עצמן כבר נכנסות משורות האשראי.
"""

import csv
import re
import sys
from collections import Counter
from pathlib import Path

# הקטגוריות בגיליון → הקטגוריות הקבועות של האפליקציה. מה שלא מופיע כאן נשאר בשם המקורי.
EXPENSE_MAP = {
    "מזון וצריכה": "מזון וסופר",
    "מסעדות, קפה וברים": "אוכל בחוץ",
    "דלק, חשמל וגז": "תחבורה ורכב",  # חשמל וגז עוברים לדיור לפי שם העסק, ראו MERCHANT_RULES
    "תחבורה ורכבים": "תחבורה ורכב",
    "פנאי, בידור וספורט": "פנאי וחופשות",
    "טיסות ותיירות": "פנאי וחופשות",
    "רפואה ובתי מרקחת": "בריאות וטיפוח",
    "קוסמטיקה וטיפוח": "בריאות וטיפוח",
    "אופנה": "קניות ובית",
    "עיצוב הבית": "קניות ובית",
    "חשמל ומחשבים": "קניות ובית",
    "שירותי תקשורת": "מנויים וחשבונות",
    "תקשורת (פרטנר)": "מנויים וחשבונות",
    "שיקים": "דיור",  # השיקים הם שכר הדירה
    "עירייה וממשלה": "ביטוח, מיסים ועמלות",
    "ביטוח": "ביטוח, מיסים ועמלות",
    "עמלות בנק": "ביטוח, מיסים ועמלות",
    "העברת כספים": "העברות ומזומן",
    "העברות יוצאות": "העברות ומזומן",
    "משיכת מזומן": "העברות ומזומן",
    "ספרים ודפוס": "לימודים",
    "שונות": "אחר",
    "אחר - בנק": "אחר",
    "": "אחר",
}
INCOME_MAP = {
    "שכר עבודה": "משכורת",
    "מילואים": "מילואים",
    "מענקים ורשויות": "מענקים וקצבאות",
    "העברות והפקדות": "העברות והפקדות",
    "אחר - הכנסה": "אחר",
    "": "אחר",
}

# לפי שם בית העסק: גובר על הקטגוריה של הגיליון. (מילה בשם העסק → קטגוריה)
MERCHANT_RULES = [
    (("חברת החשמל", "אמישראגז", "סופרגז", "פזגז", "עירית", "עיריית", "מי כרמל", "ארנונה"), "דיור"),
    (("SPOTIFY", "CHATGPT", "GOOGLE ONE", "NETFLIX", "YOUTUBE", "APPLE.COM", "ICLOUD", "DROPBOX",
      "TRUECALLER", "PADDLE.NET", "פרטנר", "סלקום", "וואי-פיי הנהלת חשבונות"), "מנויים וחשבונות"),
    (("רב- פס", "משרד התחבורה", "כביש 6", "מנהרות הכרמל", "CELLO"), "תחבורה ורכב"),
]


def merchant_rule(merchant):
    upper = merchant.upper()
    for words, category in MERCHANT_RULES:
        if any(w.upper() in upper for w in words):
            return category
    return None


OUT_HEADER = ["תאריך", "סכום", "סוג", "קטגוריה", "תיאור", "אמצעי תשלום", "הערה"]


def money(text):
    """'1,234.50 ₪' → 1234.5"""
    t = (text or "").replace("₪", "").replace(",", "").strip()
    return float(t) if t else 0.0


def convert(path):
    with open(path, encoding="utf-8-sig") as f:
        records = list(csv.reader(f))
    header = [h.strip() for h in records[0]]
    col = {name: i for i, name in enumerate(header)}
    get = lambda r, name: r[col[name]].strip() if col.get(name, 99) < len(r) else ""

    out, skipped, cats = [], Counter(), Counter()
    for r in records[1:]:
        if not any(x.strip() for x in r):
            continue
        kind = get(r, "סיווג")
        if kind not in ("הוצאת אשראי", "הוצאה ישירה בבנק", "הכנסה"):
            skipped[kind or "ללא סיווג"] += 1
            continue

        is_income = kind == "הכנסה"
        amount = money(get(r, "סכום הכנסה") if is_income else get(r, "סכום הוצאה"))
        if amount == 0:
            skipped["סכום 0"] += 1
            continue

        # תשלומים: כל חיוב נרשם בתאריך החיוב שלו, כדי שיתפזר על פני החודשים
        tx_type = get(r, "סוג עסקה")
        date = get(r, "תאריך חיוב") if tx_type == "תשלומים" else get(r, "תאריך עסקה")

        if is_income:
            category = INCOME_MAP.get(get(r, "סוג הכנסה"), get(r, "סוג הכנסה"))
        else:
            category = merchant_rule(get(r, "שם בית העסק")) or                 EXPENSE_MAP.get(get(r, "סוג הוצאה"), get(r, "סוג הוצאה"))

        source = get(r, "המידע")  # "אשראי MAX" / "בנק לאומי"
        card = get(r, "4 ספרות כרטיס")
        account = f"MAX {card}" if "MAX" in source and card else source

        notes = []
        detail = get(r, "פירוט תשלום")
        if tx_type == "תשלומים" and re.search(r"מתוך", detail):
            notes.append(detail)
        currency = get(r, "מטבע מקורי")
        if currency and currency != "ILS":
            notes.append(f"{get(r, 'סכום מקורי')} {currency}")

        cats[("הכנסה: " if is_income else "") + category] += 1
        out.append([
            date,
            f"{amount:.2f}".rstrip("0").rstrip("."),
            "הכנסה" if is_income else "הוצאה",
            category,
            get(r, "שם בית העסק"),
            account,
            " · ".join(notes),
        ])

    out_path = Path(path).with_name(f"ייבוא - {Path(path).stem}.csv")
    with open(out_path, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(OUT_HEADER)
        w.writerows(out)

    exp = sum(float(r[1]) for r in out if r[2] == "הוצאה")
    inc = sum(float(r[1]) for r in out if r[2] == "הכנסה")
    print(f"נכתב: {out_path}")
    print(f"{len(out)} תנועות. הוצאות {exp:,.2f} ש\"ח, הכנסות {inc:,.2f} ש\"ח")
    print("דולגו:", dict(skipped))
    for name, n in cats.most_common():
        print(f"  {name}: {n}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    convert(sys.argv[1])
