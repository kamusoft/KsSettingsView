# Proposal: fix-entrycell-writeback-caret-race

## Why

EntryCell への高速連続入力で文字の欠落・並び替えが発生し、壊れた値が書き戻しでアプリ状態まで
確定する (Pixel 6a 実機で再現済み: MAUI 経路は高再現率、native Compose/Store 経路でも 4/10)。
原因は書き戻しラウンドトリップのレース — 「配信スナップショット確定 → 次フレームの bind」の
窓に次の打鍵が挟まると、bind が古い値で `setText` + 末尾 `setSelection` して確定済みの打鍵を
巻き戻す (EntryCellViewHolder.bind)。さらに setText が IME の composing を破壊し、フォーカスと
IME 接続が生きたまま入力を受け付けなくなる二次被害も観測した。詳細は exploration.md。

## What Changes

- 能力: `settings-view-android-ui` (EntryCell の内容更新反映の契約変更)
- 案A「フォーカス中は EditText が値の SSoT」を実装する:
  - 同一 Cell への再バインドで `editText` がフォーカス中の間は、内容更新による text 差し替え
    (setText / setSelection) を行わない
  - フォーカス喪失時に、最後にバインドされた cell.text と EditText が食い違っていれば再同期する
  - text 以外のプロパティ反映 (色・enabled・hint・inputType 等) と、別 Cell への再バインド時の
    text 反映は従来どおり
- Robolectric テストの追加 (ガード・blur 再同期・別 Cell 再バインド)
- 実機検証: repro-burst-loop.sh による MAUI / native 両サンプルのバースト入力試験

## Non-Goals

- iOS の同型レース調査・修正 (未検証。UITextField 経路の構造確認と実機試験は別探索とし、
  確認され次第 ios ドメインの change として起こす)
- C# Controller 側のエコー配信抑止 (案C)。案A で両経路が塞がるため今回は入れない
- maui/ADR-0012 の書き戻し契約 (必須コミット・同値チェック) の変更 — C# 側は現行のまま

## Impact

- 破壊的変更なし (公開 API 変更なし)。挙動契約の変更が 1 点: フォーカス中のプログラム的な
  text 更新は入力欄へ即時反映されず、フォーカス喪失時に反映される (編集中でない Cell は従来
  どおり即時)。この契約は ADR 候補
- 影響範囲: android/ks-settingsview-ui の EntryCellViewHolder (native サンプル・MAUI サンプル
  の両方に効く)
- リスク: フォーカス中の正当な外部更新 (例: 入力値の正規化を即時反映する利用パターン) の
  見え方が変わる。blur 時再同期で最終値は収束する

## ADR 候補への申し送り

- 新決定「フォーカス中の EntryCell 入力欄は値の SSoT」は android/ADR-0001 (内容更新は
  ViewHolder を維持する) を**補完**する — 置換ではない。ただし ADR-0001 の Consequence
  「高速入力の取りこぼし解消」は本変更の実測 (書き戻しレースによる欠落・並び替え) で適用
  範囲が限定されたため、新 ADR で関係を明記する。また ADR-0001 が却下した「入力元への通知
  抑止 (一方向化)」との違い (本変更は通知経路を維持し、反映側だけをフォーカス期間ガードする)
  も新 ADR に記載する
- maui/ADR-0012 (書き戻しは必須コミット) は **proposed** 状態であり accepted な契約として
  依拠しない。現行挙動の根拠はコードとテスト

## 級: M

挙動契約の変更 + ADR 起票 + 両サンプルでの実機検証を伴うため (公開 API 変更はないが S では
収まらない)。

domain: android
