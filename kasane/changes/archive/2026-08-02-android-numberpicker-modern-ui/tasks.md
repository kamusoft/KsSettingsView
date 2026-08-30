# Tasks: android-numberpicker-modern-ui

## 1. unit パリティ (モデル・フォーマッタ)

- [x] 1.1 `NumberPickerCell` に `unit: String = ""` を追加 (equals / hashCode への反映を含む) (→ Requirement: NumberPickerCell の unit と表示値の生成)
- [x] 1.2 iOS `format(value:unit:)` と同一規則の共通フォーマッタを実装し、valueText 自動表示に適用 (→ Requirement: NumberPickerCell の unit と表示値の生成)
- [x] 1.3 Compose DSL の TwoWay overload (`InputCellDsl.kt`) に `unit` 引数を追加 (→ Requirement: NumberPickerCell の unit と表示値の生成)
- [x] 1.4 フォーマッタ・valueText 優先順位・DSL 経路の単体テスト (→ Scenario: unit 指定時の自動表示 / unit 未指定時の自動表示 / valueText 明示指定は unit より優先される / Compose DSL overload から unit を指定できる)

## 2. ホイール部品 (内部)

- [x] 2.1 スナップ式ホイール部品を internal で新設 (RecyclerView + LinearSnapHelper。選択中行の強調・周辺行の減衰・スクロール静止時のスナップ。選択中候補の更新はスナップ静止時のみ) (→ Requirement: 選択候補の初期状態と選択操作)
- [x] 2.2 選択中候補のアクセシビリティ公開と操作アクション (unit 適用後の表示文字列の公開・前/次候補への変更アクション・端での不変) (→ Requirement: 候補のアクセシビリティ状態)
- [x] 2.3 ホイールの選択遷移の単体テスト (初期位置・スナップ静止時のみの選択更新・移動中確定の採用値) (→ Scenario: 初期選択は現在値 / 現在値が候補に含まれない場合は先頭候補 / 移動中の確定は直前にスナップ静止した候補を採用する)
- [x] 2.4 ホイール adapter への unit 適用テスト (valueText 明示時も候補は unit フォーマット) (→ Scenario: 選択面の候補表示にも同じフォーマットを適用する / valueText 明示指定は候補表示に影響しない)
- [x] 2.5 アクセシビリティ操作・公開状態更新のテスト (→ Scenario: 選択中候補が公開される / アクセシビリティ操作で候補を変更できる / 端の候補ではその方向へ変更されない)

## 3. シート統合 (NumberPickerCellViewHolder)

- [x] 3.1 数値選択シートを新設し、`AlertDialog` + `widget.NumberPicker` を置き換え (ドラッグハンドル + ヘッダー + ホイール。ヘッダー文字列は OS リソース) (→ Requirement: NumberPickerCell 選択面の提示)
- [x] 3.2 候補生成の移植と堅牢化 (step 刻み・step <= 0 fallback・min > max の警告ログと非提示・候補件数の 64bit 算出と Int 上限超過時の非提示・オーバーフロー安全な終端) (→ Scenario: step 刻みの候補列挙 / step が 0 以下なら 1 へ fallback する / min > max では選択面を提示しない / 候補件数が Int 上限を超える指定では提示しない / max 付近の step 加算でも列挙が終端する)
- [x] 3.3 確定・非確定 dismiss の経路実装 (OK で 1 回発火、キャンセル / 外側タップ / Back / 下スワイプは発火なし。候補領域の下方向操作は dismiss に伝播させない) (→ Requirement: 確定と非確定 dismiss / Scenario: 候補領域の下方向操作はシートを閉じない)
- [x] 3.4 強調色のスタイル解決とその3分岐テスト (accentColor → style.accentColor → Theme.cellAccentColor) (→ Requirement: 選択面の強調色)
- [x] 3.5 タイトル解決・無効 Cell の非提示を含む ViewHolder 統合テスト (→ Scenario: タイトルの解決 / 無効 Cell は選択面を提示しない / 確定で選択値を1回通知する / 非確定 dismiss は経路によらず callback を発火しない)
- [x] 3.6 既存テストの AlertDialog 提示期待 (InputCellsTest) をシート提示期待へ置換 (→ Requirement: NumberPickerCell 選択面の提示)

## 4. サンプル・視覚照合

- [x] 4.1 サンプルアプリの NumberPickerCell デモに `unit` を設定 (単位表示の実例を示す) (→ Requirement: NumberPickerCell の unit と表示値の生成)
- [x] 4.2 実装スクリーンショットと承認モック (mock/approved.png) の視覚照合。brief.md の検証条件 (スナップ・強調・初期位置・確定/破棄経路) を実機確認し、結果を brief.md へ記録 (→ Requirement: 選択候補の初期状態と選択操作 / 確定と非確定 dismiss)
