# Deviation: align-sample-parity

実装フェーズで発生した、足場アーティファクト (spec / mock) と実装の合意済み差分。

## 足場アーティファクト間の食い違い / オーナー指示による差分

- 入力 Cell 5 種デモ「通知種別」(PickerCell 複数選択) の初期選択値: 承認済み mock (ui/mock/approved.png / plan-b.html) では「メール, アプリ内」→ 実装は現行維持の「メール, SMS」(`[0, 2]`)。理由: spec の「パラメータは現行を維持する」記述をオーナー判断で mock より優先 (mock 側は作成時の近似ズレ)。Android も同値に揃える (2026-08-01)
- 共通フィールド統合デモの `accentColor`: spec / tasks に記述なし (提案段階の棚卸し漏れ。相方レビュー second-opinion-002 が検出) → オーナー指示により今回のスコープに含め、両 platform が同一 RGBA の共有パレット (SampleTheme) を参照する形に統一。値は「iOS を正とする」本変更の原則に従い iOS の `UIColor.systemXxx` の light appearance 実値を採用。**iOS は semantic color を固定色に置き換えるため dark mode での色追随を失う**。理由: 規約 sample-parity.md「各 Cell に渡すパラメータを一致させる」への適合を優先 (2026-08-01)

## Phase 2 (Android) — 本体公開 API の platform 差

Android に対応する公開 API が存在せず samples 側では解消できない。本体の**公開 API パリティ課題**として、後続変更で「揃えるか揃えないか」を判断する (規約 cross/conventions/sample-parity.md の定める手順)。

- 入力 Cell 5 種デモの DatePickerCell: iOS は `todayText: "今日"` を渡し picker 内に「今日」ボタンを表示 → Android は `todayText` 相当の公開 API を持たないため当該ボタンなし。理由: 本体 API の platform 差 (2026-08-01)
- 入力 Cell 5 種デモ「サイズ」(NumberPickerCell): iOS は `unit: "px"` で Cell の valueText と Picker UI の双方に "px" suffix が付く → Android は `unit` 相当の公開 API を持たないため、Cell の valueText は Sample 側で `valueText = "<value> px"` を組み立てて一致させ、**Picker UI (数値ホイール) 側には suffix が付かない**。Section footer 文言「Picker UI と Cell の valueText に "px" suffix が付く。」は Android では Picker UI 部分が成立しない。理由: 本体 API の platform 差 (2026-08-01)
- 共通フィールド統合デモ「ログアウト」/ isVisible デモ「B-3（Button）」(いずれも `titleColor` 未指定の ButtonCell): iOS は本体 `Theme.defaultButtonTitleColor` (= `.systemBlue`。iOS 26.5 の light appearance で `#0088FF`) 由来の青、Android は Material 3 `colorScheme.primary` 由来の紫 `#6750A4` で描画される。Sample が渡すパラメータには差がなく (両 platform とも未指定)、本体の**既定 titleColor の platform 差**。Sample 側では解消できない (色を明示指定すると「既定色のデモ」という意図が壊れる)。後続変更で既定色を揃えるか否かを判断する。tasks 3.1 の対照確認で検出 (2026-08-01)

## Phase 2 (Android) — 本体 Android 実装の不具合

公開 API は両 platform に存在するが、Android 側の挙動が仕様どおりでない。API 設計の議論ではなく**後続の bug fix 変更**で扱う。

- 入力 Cell 5 種デモ「誕生日」(DatePickerCell（ホイール）): spec では iOS の `.wheels` に視覚的に対応する形式 → 実装では `DatePickerUIStyle.Spinner` を指定しているが、実機 (Pixel 6a / Android 16) では **カレンダー形式で表示される**。理由: 本体 `DatePickerCellViewHolder.showSpinnerDatePicker` が spinner モードの指定手段 (spinner スタイルを与える `ContextThemeWrapper` 等) を実装していない**バグ**。`android.widget.DatePicker` のモードは `android:datePickerMode` スタイル属性でのみ決まり、`calendarViewShown = false` は spinner モード時に CalendarView を隠すフラグであってモード切替の手段ではない (calendar モードでは no-op)。本体コードのコメントが主張する「spec MUST を満たす唯一の手段」という前提自体が誤り (2026-08-01)
- 入力 Cell 5 種デモ「通知種別」の上限超過操作: spec Scenario では「選択は受け付けられない」→ Android は選択値としては受け付けられない (確定値・直近イベント表示とも変化しない) が、**選択ダイアログ上のチェックボックスは押下時にチェックされたまま残る** (iOS は視覚的にも選択されない)。理由: 本体 `PickerCellViewHolder` の上限到達時の `listView.setItemChecked(which, false)` による表示戻しが実機で反映されない (2026-08-01)
