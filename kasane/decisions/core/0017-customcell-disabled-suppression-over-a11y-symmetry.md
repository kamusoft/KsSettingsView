---
id: 0017
title: CustomCell の無効化は操作抑止を優先し、Android の読み上げ喪失による非対称を受け入れる
status: accepted
date: 2026-08-04
---

## Context

cell-types-custom の spec は「`isEnabled = false` のとき行タップに加えて content 内部の操作も抑止される SHALL」と定める。一方、視覚状態契約 (`concepts/core/styling/cell-visual-states.md`) の無効状態は「操作は抑止するが読み上げは残る」を前提としており、標準 Cell は Native control の disabled 表現でこの両立を得ている。

CustomCell の content は任意の Compose ツリーであり、Compose には「操作だけ無効化して読み上げは残す」機構が存在しない。`semantics(mergeDescendants = true)` + 子孫 action の no-op 上書きで遮断する案は実測で成立しないことを確認した — Compose の merged tree は「自身も畳み込みノードである子孫」(`clickable` / `Slider` がこれに該当) を独立ノードとして残す仕様のため、親の no-op が子に効かず `OnClick` が発火した。このため「操作抑止」と「読み上げ維持」が二者択一になった (実装中に second-opinion-002 指摘 #3 が semantics 経由の操作漏れを指摘したことが発端)。

## Decision

**操作抑止を優先する。**

- Android は `Modifier.clearAndSetSemantics { disabled() }` で content の semantics subtree を丸ごと置換し、accessibility service 経由の操作を遮断する。副作用として無効時の content は TalkBack の読み上げ対象から外れる
- iOS は `.disabled(true)` で操作を抑止しつつ VoiceOver の読み上げは残る
- この accessibility 挙動のプラットフォーム非対称は**意図的に受け入れる** (無効時の他の既定挙動 — 行タップ抑止・淡色化 — は本 change で両プラットフォームを揃えた。非対称のまま残るのはこの点だけ)

判断の理由: 無効なはずのコントロールが accessibility service 経由で動作する誤操作を防ぐことを重視した (2026-08-04 オーナー判断)。

## Alternatives Considered

- **読み上げ維持を優先する案** (`semantics(mergeDescendants = true)` + 子孫 action の no-op 上書きで操作だけ塞ぐ) — 上記のとおり Compose の merged tree 仕様により実測で遮断できず、無効なはずの `clickable` / `Slider` が TalkBack 経由で操作できてしまう。誤操作防止という無効化の本義を満たせないため却下

## Consequences

- 正: `isEnabled = false` の CustomCell は touch・accessibility service のどちらの経路でも操作できず、spec の「content 内部の操作も抑止される SHALL」を満たす
- 負: Android では無効 CustomCell の内容が TalkBack で読み上げられない (存在自体が accessibility ツリーから見えなくなる)
- 負: 無効時の accessibility 挙動が iOS (読み上げ残存) と Android (読み上げ喪失) で非対称のまま残る
- Compose に「操作のみ無効化して semantics を残す」機構が将来入った場合は、本 ADR を supersede して読み上げ維持へ寄せる余地がある

出典: `kasane/changes/archive/2026-08-04-add-cell-types-custom/deviation.md` (アクセシビリティの扱い) / `kasane/changes/archive/2026-08-04-add-cell-types-custom/second-opinion-002.md` 指摘 #3 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt` (`clearAndSetSemantics` の理由コメント) / 2026-08-04 オーナー判断
