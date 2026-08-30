# セカンドオピニオン: android-datepicker-spinner-wheel (001 回目)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 提案一式 (proposal / specs / tasks / ui-brief) — spec-review モード
---
# レビュー結果: android-datepicker-spinner-wheel

**判定**: `NEEDS_DISCUSSION`  
**件数**: Critical 0 / Major 6 / Minor 3  
**検証**: 指定どおり静的レビューのみ。ビルド・テスト・ファイル書き込みは未実施。

## サマリー

ADR-0009 が定める「ボトムシート + 3連ホイール」という基本方針には整合しています。一方、日付範囲の状態遷移、`todayText` の適用範囲、既存公開 API の扱いなど、実装者だけでは決められない契約が残っています。このまま実装へ進むと、実装ごとに異なる解釈や既存挙動の消失が生じるため、先に仕様判断が必要です。

## 指摘事項

### [🟠 Major] todayText の Material モードでの意味論が矛盾している

**該当箇所**: [proposal.md:13](kasane/changes/android-datepicker-spinner-wheel/proposal.md:13)、[proposal.md:20](kasane/changes/android-datepicker-spinner-wheel/proposal.md:20)、[spec.md:72](kasane/changes/android-datepicker-spinner-wheel/specs/settings-view-android-ui/spec.md:72)、[tasks.md:16](kasane/changes/android-datepicker-spinner-wheel/tasks.md:16)

**問題点**: proposal は Material モードを Non-Goal としていますが、`todayText` Requirement は `uiStyle` を限定せず「選択面に操作を提示する」と要求しています。既定の `uiStyle` は Material なので、公開 API 利用者が `todayText` だけを指定した場合の挙動を判定できません。iOS パリティを掲げるなら、iOS では wheels/calendar の両方で today 操作が有効である点とも一致していません。

**推奨修正**: 次のどちらかを仕様で明示してください。

- Spinner 限定とし、Requirement/Scenario の GIVEN に `uiStyle = Spinner` を追加する。Material では無視することと、パリティが Spinner に限定されることも proposal に明記する。
- Material にも today 操作を追加し、Non-Goal を修正して専用 Scenario/task を追加する。

---

### [🟠 Major] min/max 境界で年・月変更後の選択日が一意に決まらない

**該当箇所**: [spec.md:24](kasane/changes/android-datepicker-spinner-wheel/specs/settings-view-android-ui/spec.md:24)、[spec.md:53](kasane/changes/android-datepicker-spinner-wheel/specs/settings-view-android-ui/spec.md:53)

**問題点**: 月末超過時の「末日へ丸め」は定義されていますが、min/max によって現在の月や日そのものが候補から消える場合が未定義です。例えば以下の結果を決められません。

- `minDate = 2020-04-15`、選択中が `2021-01-31` の状態で年を2020へ変更
- `maxDate = 2030-09-10` の境界年で、月を10月から9月へ変更
- `minDate > maxDate`
- `minDate = 2200-01-01`、`maxDate = null`。既定上限2100との組み合わせで候補が空になる場合

「各系列は常に1候補を選択中」とする Requirement と両立しない状態が発生します。

**推奨修正**: 有効範囲を正規化する単一規則を定め、すべての操作後に組み立てた日付をその範囲へ clamp するのか、月・日を個別に近傍候補へ移すのかを確定してください。`minDate > maxDate` と、明示境界が既定の1900〜2100を越える場合の扱いも Scenario 化してください。

---

### [🟠 Major] androidButtonColor の既存公開挙動が無言で消える

**該当箇所**: [proposal.md:25](kasane/changes/android-datepicker-spinner-wheel/proposal.md:25)、[spec.md:124](kasane/changes/android-datepicker-spinner-wheel/specs/settings-view-android-ui/spec.md:124)、[DatePickerCell.kt:24](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCell.kt:24)、[DatePickerCellViewHolder.kt:153](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:153)

**問題点**: 現行 Spinner は `androidButtonColor` を OK/CANCEL の文字色へ反映します。新仕様はヘッダー操作を `accentColor → CellStyle → Theme` で描画する前提ですが、`androidButtonColor` の移行先・優先順位・非推奨化を規定していません。置換後は既存の公開プロパティが実質的に無効になる可能性がありますが、proposal の Impact は公開 API 追加しか報告していません。

**推奨修正**: 次のいずれかを明文化し、Scenario とテストタスクを追加してください。

- シートのキャンセル/確定操作へ `androidButtonColor` を引き継ぐ。
- `accentColor` へ統合する優先順位を定める。
- Spinner では廃止・無効化する breaking change として Impact、KDoc、移行方針へ明記する。

---

### [🟠 Major] 年・月の確定選択をシートへ通知する内部 API が計画にない

**該当箇所**: [tasks.md:3](kasane/changes/android-datepicker-spinner-wheel/tasks.md:3)、[tasks.md:12](kasane/changes/android-datepicker-spinner-wheel/tasks.md:12)、[KsWheelView.kt:245](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:245)、[KsWheelView.kt:255](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:255)

**問題点**: 現行 `KsWheelView` はスナップ後に内部の `selectedIndex` を更新するだけで、外部への選択変更通知を持ちません。tasks 1.x はプログラム的スクロールと候補数更新しか予定していないため、`DateSelectionSheet` が年/月の変更を検知して月・日候補を再構成する経路がありません。アクセシビリティ操作でも同じ通知が必要です。

**推奨修正**: 「スナップ確定時だけ通知する」internal callback/API を task 1へ追加してください。通常スクロール、アクセシビリティ操作、プログラム的選択、候補更新による clamp の全経路について、通知タイミングと再入防止をテスト対象にしてください。

---

### [🟠 Major] 候補文字列のローカライズと系列のアクセシブル名が未定義

**該当箇所**: [spec.md:14](kasane/changes/android-datepicker-spinner-wheel/specs/settings-view-android-ui/spec.md:14)、[spec.md:138](kasane/changes/android-datepicker-spinner-wheel/specs/settings-view-android-ui/spec.md:138)、[variant-b-today-below-wheels.html:106](kasane/changes/android-datepicker-spinner-wheel/ui/mock/variant-b-today-below-wheels.html:106)

**問題点**: 承認モックは `2026年 / 8月 / 2日` を表示しますが、この形式を日本語限定で固定するのか、端末 Locale から生成するのかが決まっていません。現行の framework `DatePicker` は端末 Locale に追随するため、固定日本語実装は既存挙動からの退行になります。

またアクセシビリティ Requirement は選択値だけを公開させています。3つのノードが単に「2026」「8」「2」と読み上げられる実装でも字面上は適合しますが、どれが年・月・日か識別できません。

**推奨修正**: 以下を観察可能な契約として定義してください。

- 候補表示の Locale と年/月/日表記規則
- 各系列のアクセシブル名と現在値
- Locale に応じた列順を採るのか、承認モックどおり年→月→日で固定するのか
- 少なくとも日本語・英語 Locale の表示/アクセシビリティ Scenario

---

### [🟠 Major] LocalDate の有効範囲に対する資源上限が考慮されていない

**該当箇所**: [spec.md:26](kasane/changes/android-datepicker-spinner-wheel/specs/settings-view-android-ui/spec.md:26)、[tasks.md:11](kasane/changes/android-datepicker-spinner-wheel/tasks.md:11)、[KsWheelView.kt:75](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:75)

**問題点**: `minDate`/`maxDate` は `LocalDate` なので、両方が明示された場合は最大約20億年の年候補が仕様上有効です。「列挙」を `List` 生成として実装すると、シート提示前に OOM または長時間停止します。tasks とテストは通常範囲しか扱わず、有効入力全域で Requirement が成立する保証がありません。現行 `KsWheelView` はこの問題を避けるため、件数と `index → 表示文字列` の遅延解決になっています。

**推奨修正**: 次のどちらかを仕様で選択してください。

- サポートする年範囲へ明示的な上限を設け、範囲外入力の fallback を定める。
- 年候補も遅延 index 表現で扱うことを実装制約にし、`LocalDate.MIN/MAX` 相当の境界テストを追加する。

---

### [🟡 Minor] 空文字 todayText が iOS パリティと一致しない

**該当箇所**: [spec.md:74](kasane/changes/android-datepicker-spinner-wheel/specs/settings-view-android-ui/spec.md:74)、[EmbeddedPickerToolbar.swift:38](ios/Sources/KsSettingsViewUI/EmbeddedPickerToolbar.swift:38)、[EmbeddedPickerToolbar.swift:92](ios/Sources/KsSettingsViewUI/EmbeddedPickerToolbar.swift:92)

**問題点**: Android spec は「非 null なら提示」としますが、パリティ元の iOS は `nil` または空文字で非表示です。現在の文面どおりなら `todayText = ""` で空ラベルの操作が表示され、パリティとアクセシビリティの両方を損ないます。

**推奨修正**: `null` または空文字なら非表示とし、空文字 Scenario とテストを追加してください。

---

### [🟡 Minor] 「今日」Scenario が時刻依存で決定的に検証できない

**該当箇所**: [spec.md:76](kasane/changes/android-datepicker-spinner-wheel/specs/settings-view-android-ui/spec.md:76)、[tasks.md:29](kasane/changes/android-datepicker-spinner-wheel/tasks.md:29)

**問題点**: 「今日は範囲内」という前提だけでは、日付境界・端末タイムゾーン・テスト実行時刻に依存します。深夜0時をまたぐテストは不安定になり得ます。

**推奨修正**: 内部 `Clock`/today provider を注入可能にし、Scenario を「端末日付を2026-08-02に固定」のように決定的にしてください。端末既定タイムゾーンを使うことも明記すると判定可能になります。

---

### [🟡 Minor] ソース互換性の保証範囲と引数挿入位置が不足している

**該当箇所**: [proposal.md:27](kasane/changes/android-datepicker-spinner-wheel/proposal.md:27)、[DatePickerCell.kt:29](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCell.kt:29)、[InputCellDsl.kt:275](android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:275)

**問題点**: named 引数利用者だけをソース互換対象とする方針は書かれていますが、`todayText` の挿入位置と、既存の位置引数呼び出しが修正を要する可能性が明記されていません。実装者の配置判断によって破壊範囲が変わります。

**推奨修正**: UI data class と Compose DSL の正確な挿入位置を proposal/tasks に指定し、位置引数利用者は保証対象外であることを明記してください。旧形式の named/default 呼び出しが再コンパイル可能であることもテストしてください。

## アクションプラン

1. `todayText` の Spinner/Material 適用範囲と空文字契約を確定する。
2. min/max を含む日付状態遷移を、すべての操作に対して全域的に定義する。
3. `androidButtonColor` の維持・統合・廃止方針を決める。
4. `KsWheelView` の選択変更通知と候補更新契約を tasks に追加する。
5. Locale・アクセシビリティ名・大規模年範囲の契約と境界 Scenario を追加する。
6. 時計注入と公開 API 互換性の検証条件を明確化する。

## 突き合わせ結果 (2026-08-02、ホスト側 = ksn-propose Step 8 自己レビュー)

ホスト側自己レビューはいずれの指摘も検出していない (双方一致 0 件)。全指摘が「相方のみ」であり、根拠 (該当箇所の特定・実害シナリオ) の強さで判定した:

| # | 指摘 | 採否 | 扱い |
|---|---|---|---|
| M1 | todayText の Material モード意味論の矛盾 | **採用** | 設計判断 (Spinner 限定を推奨) — ユーザー提示 |
| M2 | min/max 境界の状態遷移が一意でない | **採用** | 正規化規則 (組み立てた日付を範囲へ丸め / 空範囲は提示せず警告ログ) を spec へ反映 |
| M3 | androidButtonColor の既存挙動が無言で消える | **採用** | 設計判断 (シートへ引き継ぎを推奨) — ユーザー提示 |
| M4 | 選択変更のスナップ確定通知 API が計画にない | **採用** | tasks へ反映 (通知 callback + 全経路テスト) |
| M5 | 候補文字列の Locale と系列のアクセシブル名が未定義 | **採用** | a11y 系列名は spec へ反映。表示の Locale 方針は設計判断 — ユーザー提示 |
| M6 | LocalDate 有効範囲に対する資源上限 | **採用** | numberpicker 対称の件数上限ルール + 既定範囲が空になる構成の扱いを spec へ反映 |
| m7 | 空文字 todayText の iOS パリティ不一致 | **採用** | null または空文字で非表示に spec 修正 |
| m8 | 「今日」Scenario の時刻依存 | **採用** | 端末既定タイムゾーンの明記 + today provider 注入を tasks へ反映 |
| m9 | todayText の挿入位置・互換範囲の不足 | **採用** | 挿入位置 (uiStyle 直後、iOS と同順) を proposal/tasks へ反映 |

採用 9 / 降格 0 / 未解決 0。判定は NEEDS_DISCUSSION → 設計判断 3 件 (M1/M3/M5) をユーザーに提示して確定する。

### 設計判断 3 件のオーナー確定 (2026-08-02)

- M1: **Spinner 限定** (Material では無視)。KDoc への明記はしない (すぐ腐るコメントは不要、とのオーナー判断)。spec に Material 無視の Scenario を追加
- M3: **androidButtonColor をシートへ引き継ぐ** (指定時はヘッダー確定/キャンセル操作色として最優先、未指定は accent 段階解決)
- M5: **候補表示は端末 Locale から導出** (自前文字列なし、列順は年→月→日固定)。spec に「候補表示の Locale 追随」Requirement を追加

全 9 件の反映が完了し、NEEDS_DISCUSSION は解消。
