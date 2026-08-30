# Exploration: android-numberpicker-modern-ui

## 課題 / 動機

- **既存不具合**: Android 版 NumberPickerCell で単位が UI に表示されない。調査の結果、表示バグではなく **`unit` プロパティ自体が Android 版に存在しない** (iOS は `unit` + AiForms 互換フォーマッタ `format(value:unit:)` を保有 — `ios/Sources/KsSettingsViewUI/NumberPickerCell.swift:35,129-139`)。修正は iOS とのプロパティパリティ追加になる
- **刷新**: 現行の選択 UI は `AlertDialog` + `android.widget.NumberPicker` (`NumberPickerCellViewHolder.kt:61-103`)。10年前からの古いスピナー形式でオーナーは刷新を希望
- **展開**: 成功したら DatePicker ホイール版へ展開したい。Android 版 DatePickerCell (Spinner 指定) は `calendarViewShown = false` の弱い実装で、Material テーマ環境ではカレンダーが表示される不具合あり (`DatePickerCellViewHolder.kt:120-130`)

## 検討した選択肢 (却下案と理由を含む)

- **A案: ボトムシート + `android.widget.NumberPicker` 流用** — 却下: Holo 時代の見た目 (divider) とテーマ非対応 (色制御がリフレクション頼み) が残る。DatePicker 展開時に `widget.DatePicker` の限界に戻り根治できない
- **B案: ボトムシート + 自作ホイール (RecyclerView + LinearSnapHelper)** — **採用**: 見た目を完全制御、追加依存なし、同じ部品を3連にして DatePicker ホイール版へ展開可能

## 決定事項

- B案採用 → **ADR-0007 (accepted)**: `kasane/decisions/android/0007-numberpickercell-bottom-sheet-custom-wheel.md`
- 器は ADR-0005 の `PickerSelectionSheet` と同系 (BottomSheetDialog 継承・ドラッグハンドル・ヘッダー・高さ制御・スタイル解決の骨格を再利用 — `PickerSelectionSheet.kt`)
- `unit` プロパティを iOS パリティで追加し表示に反映
- 本 change のスコープは「unit パリティ + NumberPickerCell のシート化」まで。DatePicker への展開は続編 change

## ADR 候補

- 作成済み: android/ADR-0007 (accepted)。採番注意: ADR-0006 は claude/timepickercell-color-adjust-c9c033 ブランチで採番済みのため欠番

## 未決の論点

- ホイールの視覚仕様の詳細 (行高・フェード・中央ハイライトの表現) — ksn-propose のモック段階で確定
- 単一ホイールの確定操作 (タップ即確定は不可のため、確定ボタン or シート dismiss で確定のどちらにするか) — 提案段階で確定
- ホイール部品の公開範囲 (内部部品に留めるか、将来の DatePicker 展開を見据えた命名・配置) — 提案段階で確定

## UI 素材 (ui/references/ の一覧と注釈)

- `current-kssettingsview-numberpicker.webp` — 現行 KsSettingsView 版。AlertDialog + NumberPicker、単位表示なし (「30」のみ)。刷新対象
- `aiforms-original-numberpicker.webp` — AiForms オリジナル (SettingsView.Maui)。「15 px」のように単位付きで表示される。単位表示の期待挙動の参照

## 変更級の推奨: M (理由)

- 公開 API 変更あり (`unit` プロパティ追加)
- 新規 UI 部品 (自作ホイール) の追加 + 選択 UI の器の刷新 — UI ありのためモック承認ゲートが必要
- 触る範囲は NumberPickerCell 系 (モデル / ViewHolder / シート) に閉じており、L ほどの横断性はない
- 先行 change (android-picker-selection-sheet) も同規模で M 相当の重さで運用された実績あり
