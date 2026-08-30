# Design: align-sample-parity

## Context

Sample を platform 間パリティ検証装置にする規約 (cross/ADR-0016 / sample-parity.md) への適合作業。iOS を正とし Phase 1 (iOS 整備) → Phase 2 (Android 同期) で収束させる。samples-ios / samples-android の2能力にまたがるため L 級。技術判断は以下の4点。

## Goals / Non-Goals

- Goals: 全デモ画面の文言・構成の platform 間完全一致と、表記ゆれの再発防止
- Non-Goals: 本体ライブラリの API・挙動変更 (DatePicker の platform 差も本体側は触らない)

## Decisions

### Decision 1: DatePicker の platform 差は表示文言の中立化で吸収する
**採用案:** 入力 Cell 5 種デモの DatePicker Section の見出しを「DatePickerCell（ホイール）」「DatePickerCell（カレンダー）」、footer を「ホイール形式で日付を選択するデモ。」「カレンダー形式で日付を選択するデモ。」とし、platform API 名 (.wheels / .calendar / Spinner / Material) と platform 固有機能の説明 (Toolbar の「今日」ボタン等) を画面文言から排除する。Android の uiStyle は iOS の wheels / calendar に視覚的に対応する形式 (想定: Spinner / Material。Phase 2 で調査確定) を用い、picker 内部の文言差 (iOS のみの「今日」ボタン等、本体 API 差по由来のもの) は deviation.md に記録して本体側の統一課題とする。
**理由:** iOS 文言をそのまま Android に写すと「Toolbar に「今日」ボタン」等が Android 上で虚偽の説明になる。中立文言なら両 platform で真であり、一字一句一致も成立する。
**代替案:**
- **A: iOS 文言のまま、Android の不一致を全て deviation 記録** — Sample が Android 上で虚偽の説明を表示することになり、検証装置としての信頼を損なうため却下
- **B: Android 本体に todayText 相当・形式指定を追加してから揃える** — Non-Goals (本体変更なし) に反しスコープが跳ねるため却下。本体統一の要否は deviation 記録を起点に別変更で判断する

### Decision 2: メニュー文言と画面タイトルは表示名定義の一元化で再発防止する
**採用案:** 各 platform で画面表示名を1箇所に定義し (iOS: 画面一覧の定数定義、Android: 既存 Routes 相当に表示名を持たせる)、ルートメニュー項目と画面タイトルの双方が同じ定義を参照する。
**理由:** 現状の表記ゆれ (iOS 全7画面でメニュー≠タイトル) の根本原因が文言の二重手書きのため、構造で再発を防ぐ。
**代替案:**
- **A: 文言を目視で揃えるだけ (構造は現状維持)** — 今回は揃っても次の画面追加で再発する。規約が名指しする二重管理パターンを温存するため却下

### Decision 3: MAUI 互換 Theme はデモ間で共有定義にする
**採用案:** BasicCellsDemoView 内の private な Theme 定数群を Sample 共用の定義 (例: SampleTheme) に抽出し、基本 Cell 7 種デモと入力 Cell 5 種デモが同一定義を参照する。Android も BasicCellsDemoScreen の同等定義を共用化する。
**理由:** 同じ Theme を2画面にコピーすると色値の二重管理になり、デモ間の見た目一致が偶然に依存する。
**代替案:**
- **A: InputCellsDemoView に Theme 定数を複製** — 変更コスト最小だが色値の二重管理を新設することになるため却下

### Decision 4: 検証は「静的照合 + 動的操作確認」の2層とし、自動 UI テストは導入しない
**採用案:** 静的な文言・構成は「mock 照合 (ルートメニュー・入力 Cell 5 種デモ) + iOS/Android スクリーンショット並置 + 文言対照表チェック」、動的 Scenario (イベント表示更新・複数選択上限・日付状態独立・遷移先タイトル) はシミュレータ/エミュレータでの操作確認で検証する (tasks 1.9 / 2.7 / 3.1)。
**理由:** Sample は本体のテスト対象外のデモアプリであり、UI テスト基盤の新設は変更の目的 (パリティ収束) に対して過剰。Scenario は全て目視・操作で判定可能な粒度に定義してある。
**代替案:**
- **A: XCUITest / Compose UI テストで Scenario を自動化** — 再発検知には有効だが、2 platform 分のテスト基盤新設は本変更のスコープと釣り合わないため却下。パリティの継続監視が必要になったら別変更で検討

## Risks / Trade-offs

- DatePicker の uiStyle 対応 (Spinner / Material の割り当て) は Phase 2 の調査結果次第で deviation が増える可能性がある
- 中立文言化により、picker 実装形式の説明としての具体性は下がる (形式名を知りたい利用者はコードを見る前提)

## Migration Plan

Phase 1 (iOS) → Phase 2 (Android) の順で同一 change 内で実施。中間状態 (iOS のみ適用) は tasks.md が追跡する。

## Open Questions

なし (DatePicker の uiStyle 割り当てのみ Phase 2 冒頭の調査タスクで確定)

## ADR 候補

なし — 全 Decision が cross/ADR-0016 の範囲内の実装判断で、覆すコストも局所的。Decision 1 の deviation 記録から本体 API 統一の必要性が確定した場合は、その後続変更で起票する
