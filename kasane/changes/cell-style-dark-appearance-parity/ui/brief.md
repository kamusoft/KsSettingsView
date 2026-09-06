# UI Brief: cell-style-dark-appearance-parity

## 画面と状態

新規デザインなし。本 change は Android の Cell 固有色の型変更と 3 platform の契約・テストの追加で、描画結果 (ライト / ダークとも) は本 change 前と同じであることが正。対象画面は Android Native Sample の基本 Cell デモ・共通フィールド統合デモ・入力 Cell デモ・日付選択デモ (Cell 固有色を明示している画面と、`Unspecified` の解決経路が通る画面)。

## リファレンス注釈

なし (references/ は持たない)。

## デザイントークン参照

色ロールと既定値は [スタイルの所有と実効値解決](../../../concepts/core/styling/style-resolution.md) の「既定色と外観の追随」。本 change で値は変えない。

## 承認モック

mock は作らない (新規デザインなし)。見た目の正は**実装着手前に同じ画面・同じ外観で撮った基準画像** (`verification/` に `before-<画面>-<light|dark>.png` として置く)。

## 照合記録

(tasks 8.2 の完了時に記入: 基準画像と最終画像 `after-<画面>-<light|dark>.png` の照合結果、差分ゼロの確認日、合意済み妥協があればその件数と内容)
