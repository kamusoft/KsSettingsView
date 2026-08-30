# Deviation: align-timepicker-hour-cycle-across-platforms

- 時制の決定と候補系列 (android-timepicker、12時間制の系列順): spec では 12時間制を「時 1–12 / 分 0–59 / 午前・午後 の3系列」と後置き固定の並びで記述 → 指示により、系列の**順序**は端末 Locale の時刻パターン由来へ変更 (ja 等の AM/PM 前置き locale では 午前/午後・時・分、en 等では 時・分・AM/PM)。理由: iOS の埋め込み picker (UIDatePicker) が locale の時刻パターンで列順を組み替えるのに合わせ、3面の 12時間制提示を locale 準拠で揃えるオーナー意向 (2026-08-28)
