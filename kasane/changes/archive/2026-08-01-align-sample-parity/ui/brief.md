# UI Brief: align-sample-parity

## 画面と状態

- **iOS ルートメニュー** (状態: 通常のみ): グループ分きリスト。デモ群6項目 + 検証群1項目 (「Minimal Diffable 検証」)
- **iOS 入力 Cell 5 種デモ** (状態: 通常のみ): 直近イベント1行 + 7 Section の設定リスト。基本 Cell 7 種デモと同一の MAUI 互換 Theme 適用で同系統の外観にする
- **Android ルートメニュー** (状態: 通常のみ): iOS デモ群と同一文言・並び順の一覧 (Button 列を廃止)。検証群なし
- **Android 入力 Cell 5 種デモ** (状態: 通常のみ): iOS と同一構成 (見た目の正は iOS 実装後スクリーンショット)
- loading / empty / error 状態なし (静的なデモ画面のため)

## リファレンス注釈

- references/ 画像なし
- **見た目の正は基本 Cell 7 種デモの既存実装** (samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift / samples/android/.../BasicCellsDemoScreen.kt)。入力 Cell 5 種デモはこれと同系統に揃えることが本変更の趣旨
- Android Phase 2 の視覚照合は、Phase 1 完了後の iOS 実機スクリーンショットを正とする

## デザイントークン参照

- MAUI 互換 Theme の色定義は BasicCellsDemoView.swift の `mauiTheme` 定数群 (PaleBackColorPrimary / AccentColor 等、AiForms MAUI 原典 Sample 由来) をそのまま共有する。concepts/ にトークン文書はなく、生値は mock と既存実装が持つ

## 承認モック

mock/plan-b.html を採用 (approved.png)。2026-07-31 ユーザー承認。案B = ルートメニューに「デモ」「検証」の両グループ見出しを表示する形。入力 Cell 5 種デモ部分は案A/B 共通。

承認後の改訂 (2026-07-31、second-opinion-001 反映): DatePicker Section の見出し・footer を中立文言に変更 (design.md Decision 1)、予約日の初期値を 2026/06/01 に変更 (状態独立化)。approved.png は改訂後を再取得済み。
