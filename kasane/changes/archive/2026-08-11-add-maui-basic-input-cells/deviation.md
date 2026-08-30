# Deviation: add-maui-basic-input-cells

- 「ニックネーム (callback)」デモ (入力 Cell ページ): iOS/Android は値変更 callback 経路 vs binding 経路の対比デモだが、MAUI facade の `EntryCell` は値変更 callback / event を公開しないため、MAUI では5セルすべてが同じ TwoWay binding になる。footer 文言は 3 platform の文言一致を優先して据え置き。`EntryCell` への値変更通知の公開は本体側の統一課題として後続で扱う (sample-parity 規約「一致不可能箇所の deviation 記録」条項に基づく記録。review-001 Major-2) (2026-08-10)

- Android EntryCell の実機 IME フォーカス喪失 (既知不具合・別 change 送り): 双方向バインドの書き戻し Requirement は「連続入力を妨げない」ことを意図するが、Android 実機 (Pixel 6a) で日本語 IME の確定・BackSpace のたびに入力欄がフォーカスを失い連続入力不可を確認 (7.2)。iOS 実機は問題なし、Android native にも無い MAUI 統合固有の挙動。指示により本 change では修正せず別セッションで解消 (チップ起票済み)。Robolectric テスト緑は実 IME 挙動を保証しない既知パターンの再来 (2026-08-11)
- Android EntryCell のパスワードマスク修正: proposal Non-Goals は「Native の Cell 実装への機能追加」を除外するが、指示により native 層 `EntryCellViewHolder` の InputType 合成欠陥 (IsPassword + 非既定 Keyboard で平文表示 — review-001 Major-1) を本変更内で修正。理由: 本変更で MAUI から新たに到達可能になった機密情報の平文表示経路であり放置不可のため (2026-08-11)
- サンプルのアイコン表示 (iOS): spec (samples-maui) は配色の一致を SHALL とするが、指示により「サイズ感が同一であれば色・形状は不一致でよい」と許容。理由: native iOS は SF Symbols (青ティント)、MAUI は Material Symbols 由来 SVG (黒) で、グリフ資産の差は埋められないため (2026-08-10)
- Section の header 高さ: spec では未規定 (maui-cells / maui-bridge とも per-Section header 高さの公開・輸送を規定せず、Bridge 実装も「spec 対象外」として未輸送) → 指示により facade `Section.HeaderHeight` + 両OS Bridge の `headerHeight` 輸送を追加。理由: native サンプルが `headerHeight: 60` を使用しており、samples-maui の sample-parity (画面構成の一致) を完全にするため (2026-08-10)
