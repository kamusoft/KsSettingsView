# Proposal: fix-ios-entrycell-writeback-race

## Why

iOS の EntryCell にも、Android で実機確認・修正した書き戻しレース (android/ADR-0014) と同型の構造がある
(コードで確認済み、exploration.md)。打鍵 → `onTextChanged` → 呼び出し側の Store コミット →
`reconfigureItems` による同一 Native cell の再 render、という往復の「配信スナップショット確定 → 再 render
適用」の窓に次の打鍵が挟まると、再 render が古い値で `textField.text` を代入して確定済みの打鍵を巻き戻す
(`UITextField` は `text` 代入でキャレットが末尾へ移るため以降の打鍵位置もずれる)。壊れた値は書き戻しで
アプリ状態まで確定する。SwiftUI DSL 経路 (Binding → `@State` → body 再評価) と MAUI 経路 (`ScheduleFlush`
の dispatcher post) は構造的に非同期で窓が確定しており、現行の同値ガードでは「入力欄が既に次の文字へ進んで
いる」ケースを止められない。入力セルの文字欠落はデータ破損級の欠陥なので、初回リリース前に塞ぐ。

## What Changes

- 能力: `settings-view-ios-ui` (EntryCell の内容更新反映の契約変更。Android 版 `settings-view-android-ui`
  デルタスペックと対称)
- ios/ADR-0004 (proposed) の Decision を実装する:
  - 同一 Cell (同じ `cell.id`) への再 render で `UITextField` が first responder の間は `text` を代入しない
  - フォーカス喪失時 (`textFieldDidEndEditing`) に、最後に render された `cell.text` と入力欄が食い違って
    いれば再同期する。再同期は `onTextChanged` を発火させない
  - 別 Cell への再 render・非フォーカス時の反映は従来どおり。`prepareForReuse` で同一性判定と再同期の
    基準値を破棄する。既存の同値ガードは非フォーカス時のキャレット維持として残す
- unit test の追加 (フォーカス中の stale render・喪失時再同期・同一性判定・プロパティ反映の優先順位)
- Simulator 向け再現スクリプト (Android の `repro-burst-loop.sh` を mobilecli + `dump ui` で移植) と、
  修正前後の A/B 証跡 (evidence.md)。対象は iOS サンプルの入力デモ画面のメール欄 (SwiftUI DSL 経路) と、
  Store デモ画面に追加する EntryCell (Store 直接経路)
- サンプル変更: `samples/ios` の `StoreDemoView` に、callback で `store.replaceCell` する EntryCell を 1 つ追加する
  (Store 直接経路の試験対象。現状のサンプルには Store 経路の EntryCell が存在しないため。second-opinion-spec-001
  Major-1)
- 蒸留への申し送り: 証跡取得後に ios/ADR-0004 を accepted へ昇格し、concepts (input-cells.md の
  「iOS の同型契約は未検証・未導入」、ios-native-host.md) を追随させる

## Non-Goals

- 世代トークン方式 (bridge DTO・core 輸送契約の変更) — ios/ADR-0004 で却下
- MAUI Controller 側のエコー配信抑止 — SwiftUI / Store 経路に効かないため入れない
- `markedTextRange` の個別ガード — フォーカス中ガードに包含されるため設けない
- 物理 iPhone での検証 — Simulator + 実 IME の A/B で完了とする (オーナー決定)。Simulator で再現が
  取れない場合のみ pixie4 (WDA) をフォールバックに使う
- NumberPicker / TimePicker / DatePicker の `embeddedField` — 既に `isFirstResponder` ガードを持ち、
  本件の対象外
- Native cell が交換される更新経路 (`replaceCell` での具象型変更・`replaceSection`・`replaceAll` の reload) —
  入力欄自体が作り直されて first responder を失うため、フォーカス中 SSoT 契約の保証対象に含めない
- MAUI サンプル (iOS) でのバースト試験 — 経路が SwiftUI / MAUI とも bridge の `replaceCell` に合流し、
  ui 層のガードで同時に塞がる。Android 版では MAUI 側も計測したが、iOS では .NET for iOS と現環境 Xcode
  の整合が未検証のため、native サンプルの 2 経路 (TwoWay / Store) で代表させる

## Impact

- 破壊的変更なし (公開 API 変更なし)。挙動契約の変更が 1 点: フォーカス中のプログラム的な text 更新は
  入力欄へ即時反映されず、フォーカス喪失時に反映される (編集中でない Cell は従来どおり即時)。あわせて
  「`onTextChanged` を受けて `cell.text` を更新しない構成ではフォーカス喪失時に入力欄が最後の render 値へ
  戻る」という利用側契約が Android と同様に必須化される
- 影響範囲: `ios/Sources/KsSettingsViewUI/EntryCellView.swift` のみ (SwiftUI DSL・MAUI・Store 直接利用の
  全経路に効く)
- リスク: フォーカス中の正当な外部更新 (入力値の正規化を即時反映する利用パターン) の見え方が変わる。
  喪失時再同期で最終値は収束する。`isEnabled = false` を first responder 中に代入したとき UIKit が自動で
  first responder を手放すかは未確認 — テストで固定し、手放さなければ実装側で明示 resign する

## 級: M

挙動契約の変更 + ADR 確定 + Simulator A/B 証跡を伴うため (公開 API 変更はないが S では収まらない)。

domain: ios
