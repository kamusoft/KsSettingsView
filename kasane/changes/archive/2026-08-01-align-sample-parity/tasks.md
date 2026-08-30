# Tasks: align-sample-parity

注: 本変更は samples のみで、単体テストの対象となる公開挙動を持たない。Scenario の検証はビルド + スクリーンショット照合 + 文言対照 (グループ 3) で行う。

## 1. iOS — Phase 1 (iOS を正に整える)

- [x] 1.1 入力 Cell 5 種デモ: 現在値プレビューを削除し、直近イベント1行表示 (文言形式は spec の定義に従う) に置き換え。「予約日」を「誕生日」から独立した状態に分離 (初期値 2026/06/01) (→ Requirement: 入力 Cell 5 種デモの様式統一)
- [x] 1.2 入力 Cell 5 種デモ: MAUI 互換 Theme を適用 (見た目の正: ui/mock/approved.png。Theme 定数は BasicCellsDemoView から共有定義に抽出して両デモで参照 — design.md Decision 3) (→ 同上)
- [x] 1.3 入力 Cell 5 種デモ: EntryCell Section footer に callback 経路の説明文言を追加 (→ 同上 / Scenario: callback 経路の意図明示)
- [x] 1.4 全7画面の画面タイトルをメニュー文言と一致 (Store / DSL はタイトル新設、基本 Cell 7 種・入力 Cell 5 種・共通フィールド統合・isVisible・Minimal Diffable 検証は文言修正)。表示名はメニューと画面が同一定義を参照する形 (定数の一元化) にして二重管理の再発を防ぐ (→ Requirement: メニュー文言と画面タイトルの同一性)
- [x] 1.5 ルートメニューをデモ群 / 検証群のグループ分けに変更 (→ Requirement: ルートメニューのデモ/検証グループ分離)
- [x] 1.6 StoreDemoView を StoreDemoView.swift に分離 (挙動変更なしのファイル整理)
- [x] 1.7 共通フィールド統合デモ: RadioCell「ダーク」に hintText「推奨」を追加し、Section header を Android と同一文言に変更 (→ Requirement: 共通フィールド統合デモの hintText 追随)
- [x] 1.8 DSL デモの動的 Section 見出しを「動的 Section（繰り返し）」に、入力 Cell 5 種デモの DatePicker Section 見出し・footer を中立文言に変更 (→ Requirement: DSL デモの動的 Section 見出しの中立化 / DatePicker Section の文言中立化)
- [x] 1.9 iOS Sample をビルドして検証: ルートメニューと入力 Cell 5 種デモは mock/approved.png と視覚照合、他画面は文言対照。動的 Scenario (各 Cell 操作でのイベント表示更新・複数選択の上限超過時の非更新・日付 Cell の状態独立・全7遷移先のタイトル一致) はシミュレータで操作確認 (→ samples-ios 全 Scenario)

## 2. Android — Phase 2 (iOS に同期)

- [x] 2.1 MainActivity.kt に同居する画面 (MenuScreen / StoreDemo / DSLDemoScreen 等) を1画面1ファイルに分割 (挙動変更なしのファイル整理)
- [x] 2.2 ルートメニューを Button 列から一覧形式に変更、文言・並び順・「デモ」見出しを iOS デモ群と一致 (→ Requirement: ルートメニューの一覧表示と文言一致)
- [x] 2.3 Store 方式デモの表示文言 (Section header / footer・Cell title・ボタン文言・追加 Cell 文言) を iOS に一致 (→ Requirement: Store 方式デモの表示文言一致)
- [x] 2.4 入力 Cell 5 種デモを iOS 構成に全面一致 (7 Section・Cell 数・全文言・全パラメータ・初期値・直近イベント表示。rootHeader 削除) (→ Requirement: 入力 Cell 5 種デモの iOS 一致)
- [x] 2.5 DatePickerCell の uiStyle 対応を調査し .wheels / .calendar 相当に変更。本体 API 差で不可能なら deviation.md に記録 (→ Requirement: DatePickerCell の表示形式対応)
- [x] 2.6 DSL デモの動的 Section 見出しを「動的 Section（繰り返し）」に変更 (→ Requirement: DSL デモの動的 Section 見出しの中立化)
- [x] 2.7 Android Sample をビルドして検証: iOS スクリーンショットと並置照合 (全画面)、動的 Scenario (複数選択の上限超過・イベント表示更新) はエミュレータで操作確認 (→ samples-android 全 Scenario)

## 3. 検証 (両 Phase 完了後)

- [x] 3.1 両 platform の全画面文言を突き合わせ、sample-parity.md のチェック項目 (画面の集合 / 表示文言 / 画面構成 / メニューとタイトルの一致) で対照確認 (→ 全 Requirement)
- [x] 3.2 一致不可能だった箇所が deviation.md に漏れなく記録されていることを確認 (→ Requirement: DatePickerCell の表示形式対応)
