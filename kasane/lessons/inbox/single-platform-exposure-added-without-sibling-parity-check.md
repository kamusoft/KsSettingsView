---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-27
last-seen: 2026-08-27
evidence:
  - relax-android-host-prerequisites (Android spec に時制決定規則「format の a で 12/24h が決まる」を明文化し、12時間制デモセルを samples/android にだけ常設。iOS の時制は端末設定駆動で format では制御できない非対称を照合しておらず、オーナー指摘で発覚。非対称解消の検討が別 change の簡易起票として必要になった)
---

## ルール文

platform 固有 spec に利用者可視の挙動 (入力形式・提示形式の決定規則等) を新設・明文化するとき、または片側 platform のサンプルにだけ機能デモを常設するときは、相方 platform の同一機能の決定源・対応状況をコードで照合する。非対称が生じる場合は、その扱い (契約を揃える / 追跡を起票する / 意図的な platform 差として concepts に明記する) をオーナー判断の選択肢として提案・報告に明記してから進める。既存挙動の明文化であっても、明文化した瞬間にそれは契約になる — 相方 platform と揃わない契約を無言で固定しない。

## 経緯

- 2026-08-27 relax-android-host-prerequisites: TimePickerCell の選択面置換で、旧 MaterialTimePicker 実装の判定 (format に `a` を含めば 12 時間制) をデルタスペックの Requirement に明文化し、視覚検証の都合から 12 時間制デモセルを samples/android に常設した。iOS の選択面 (UIDatePicker(.time)) は locale 非上書きで端末の 24 時間表示設定に従い、format では時制を制御できない — この決定源の非対称を提案・実装のどの段階でも照合していなかった。sample-parity 上の片側先行としてオーナーに確認した際、「iOS と足並みを揃えて進むべき」との指摘を受け、非対称解消の検討 (align-timepicker-hour-cycle-across-platforms) を簡易起票することになった。
