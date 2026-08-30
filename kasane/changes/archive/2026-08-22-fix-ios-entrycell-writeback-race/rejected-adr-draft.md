---
id: 0004
title: フォーカス中の EntryCell 入力欄は値の SSoT — 内容更新の text 反映はフォーカス喪失まで遅延する
status: proposed
date: 2026-08-22
---

## Context

Android で実機確認・修正した EntryCell の書き戻しレース ([android/ADR-0014](../android/0014-entrycell-focused-editor-owns-text.md)) と同型の構造が iOS にも存在することを、コード読解で確認した。打鍵 → `EntryCellView.handleEditingChanged` → `onTextChanged` → 呼び出し側の Store コミット (`replaceCell`) → `reconfigureItems` による同一 Native cell の再 `render` という往復で、「配信スナップショット確定 → 再 render 適用」の窓に次の打鍵が挟まると、再 render が古い値で `textField.text` を代入し、確定済みの打鍵を巻き戻す。`UITextField` は `text` 代入で選択位置が末尾へ移るため、以降の打鍵位置もずれ、欠落・並び替えになる。壊れた値は書き戻しでアプリ状態まで確定する。

往復の窓が開く経路は次のとおり:

- SwiftUI DSL: Binding setter → `@State` 更新 → body 再評価 → `updateUIViewController` → `store.replaceCell`。構造的に非同期
- MAUI: `KsSettingsController.ScheduleFlush()` の dispatcher post を経て bridge の `replaceCell` へ。構造的に非同期 (Android と同じ)
- Store 直接利用: `onTextChanged` から `replaceCell` までは同期で、窓の有無は `dataSource.apply` の実適用タイミング次第 (未実測)

現行の `render` は同値ガード (AiForms 由来、日本語 IME の markedText を同値再代入で壊さないためのもの) しか持たず、`isFirstResponder` によるガードはない。`isProgrammaticUpdate` は `handleEditingChanged` の再入抑止であり、巻き戻し自体は防がない。android/ADR-0014 が指摘したとおり、同値ガードは「入力欄が既に次の文字へ進んでいる」ケースを止められない — 同値でないからこそ代入が走る。

実機での再現・解消確認はまだ行っていない。本決定は実機 A/B の証跡を得たうえで accepted へ昇格する (実行時挙動の検証規約)。

## Decision

`EntryCellView.render(cell:theme:)` の text 反映契約を android/ADR-0014 と対称にする:

- **同一 Cell (同じ `cell.id`) への再 render で `UITextField` が first responder の間は、`text` を代入しない** (キャレットも動かさない)。編集中の値の正 (SSoT) は入力欄自身であり、書き戻し経路から遅れて返る値 (エコー含む) で上書きしない。
- **フォーカス喪失時** (`textFieldDidEndEditing`) に、最後に render された `cell.text` と入力欄が食い違っていれば再同期する。再同期は `onTextChanged` を発火させない (再同期値を書き戻し経路へ逆流させない)。
- text 以外のプロパティ反映と、**別 Cell (異なる `cell.id`) への再 render・非フォーカス時の text 反映は従来どおり**。同一性判定は `cell.id` のみで行う (`EntryCell` の `Equatable` は text を含むため、equals / 参照比較は使わない)。`prepareForReuse` で同一性判定と再同期の基準値を破棄する。
- 既存の同値ガードは、非フォーカス時のキャレット維持として残す。`markedTextRange` による個別ガードは設けない — markedText は first responder 中にしか存在せず、フォーカス中は一切代入しない本ガードに包含される。

ui 層 1 箇所の修正で、SwiftUI DSL・MAUI・Store 直接利用のすべての経路に効く。

## Alternatives Considered

- **世代トークン方式** (打鍵通知に通番を付けて往復させ、古いスナップショットは反映しない): bridge DTO・core の輸送契約に手が入り、対称性のため Android 側も作り直しになる。ui 層 1 箇所で全経路に効く本案で足りるため却下 (android/ADR-0014 と同じ判断)。
- **MAUI Controller 側でエコー配信を抑止**: MAUI の窓は塞がるが、SwiftUI DSL / Store 直接経路に効かないため単独では不十分で不採用。
- **キャレット位置の保存・復元**: 代入前後でキャレットを補正しても、巻き戻しによる文字欠落自体は解けない対症療法のため却下。
- **`markedTextRange` の個別ガード追加**: IME 未確定中だけ代入を避ける案。フォーカス中のガードに包含され、非フォーカス時には markedText が存在しないため、追加する意味がなく不採用。

## Consequences

- 正: 書き戻しの往復より速い連続入力でも欠落・並び替え・キャレット移動が起きない (効果の実証は実機 A/B で行う)。
- 正: フォーカス中の代入がなくなるため、書き戻しエコーによる markedText (日本語 IME 未確定文字列) の破壊も消える。
- 正: Android と契約が対称になり、concepts の input-cells.md が「iOS の同型契約は未検証・未導入」としていた空席が埋まる。
- 負: フォーカス中のプログラム的な text 更新 (入力値の正規化を即時反映する利用パターン等) は入力欄へ即時反映されず、フォーカス喪失時に反映される。編集中でない Cell は従来どおり即時。
- 負: `onTextChanged` を受けて `cell.text` を更新しない利用構成では、フォーカス喪失時に入力欄が最後に render された text へ戻る。値 + callback 経路の利用側は callback を受けて `cell.text` を更新する必要がある (Android と同じ利用側契約)。
- 補足: `KsListCellBase.clearContentStackTrailingViews` が first responder を保持する view を除去しない保護と同じ「編集中の入力欄は保護する」方針に揃う。

出典: fix-ios-entrycell-writeback-race (exploration.md の机上確認と本探索の議論、2026-08-22) / 先例 android/ADR-0014
