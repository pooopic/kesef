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

# הקטגוריות בגיליון → הקטגוריות של האפליקציה. מה שלא מופיע כאן נשאר בשם המקורי.
EXPENSE_MAP = {
    "מזון וצריכה": "סופר",
    "מסעדות, קפה וברים": "אוכל בחוץ",
    "תחבורה ורכבים": "תחבורה ודלק",
    "פנאי, בידור וספורט": "בילויים",
    "רפואה ובתי מרקחת": "בריאות",
    "שירותי תקשורת": "חשבונות",
    "תקשורת (פרטנר)": "חשבונות",
    "העברת כספים": "העברות",
    "העברות יוצאות": "העברות",
    "ספרים ודפוס": "לימודים",
    "שונות": "אחר",
    "אחר - בנק": "אחר",
    "": "אחר",
}
INCOME_MAP = {
    "שכר עבודה": "משכורת",
    "אחר - הכנסה": "אחר",
    "": "אחר",
}
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
            category = EXPENSE_MAP.get(get(r, "סוג הוצאה"), get(r, "סוג הוצאה"))

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
