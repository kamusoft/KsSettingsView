---
id: 0001
title: 内容更新は payload 付き通知と change アニメーション無効で同一 ViewHolder を維持する
status: accepted
date: 2026-08-01
---

## Context

Android Host の内容更新 (同一 ID の Cell の reconfigure) は payload なし `notifyItemChanged(position)` で通知していた。既定の `DefaultItemAnimator` は payload が空だと `canReuseUpdatedViewHolder` が false になり、対象行の ViewHolder を再利用せず**新規生成してクロスフェード**する。この結果、EntryCell では 1 打鍵ごとに EditText インスタンスが差し替わり、InputConnection が作り直されて日本語 IME の composing (未確定文字列) が即時確定される不具合が発生した。毎打鍵の行クロスフェードによるちらつきと、高速入力時の取りこぼしも同根である。

コード内の「`notifyItemChanged` は ViewHolder を破棄せず同一 ViewHolder に bind する」というコメント前提が既定 ItemAnimator 下では誤っていたことが根本原因。真因は codex + ホスト側調査の独立並走 (androidx ソース照合 + Pixel 6a 実機実測) で確定した。

## Decision

内容更新経路で同一 ViewHolder への再 bind を**二重担保**で保証する:

1. `KsSettingsView` の RecyclerView 生成時に `supportsChangeAnimations = false` を設定する (change アニメーション由来の ViewHolder 差し替えを止める)
2. 内容更新の通知は payload 付き `notifyItemChanged(position, PAYLOAD_CONTENT)` とする (`PAYLOAD_THEME` と同じ流儀。payload 非空なら `DefaultItemAnimator` は ViewHolder を再利用する)。**payload なしの `notifyItemChanged` を内容更新経路に新設しない**
3. あわせて、ViewHolder の `bind()` ではフォーカス中の入力 View の IME 接続を再起動させる setter (`EditText.inputType` は AOSP 上で無条件に `InputMethodManager.restartInput` を呼ぶ) を、値が変わったときだけ適用する (差分ガード)

## Alternatives Considered

- **bind() 内の setter 差分ガードのみ**: 却下。payload なし通知のままでは ViewHolder 自体が毎打鍵差し替わるため、ガードした View ごと捨てられ効果がない (実機 A/B で実証済み。初回修正がこの案で失敗した)。ガードは同一 ViewHolder 再利用が成立した後の防御としてのみ有効。
- **入力元セルへ notify を返さない一方向化 (AiForms 準拠)**: 却下。AiForms が同不具合を持たない構造的理由ではあるが、Compose DSL が state round-trip を前提としており波及が大きい。また「自己 echo かどうか」の判定が汎用 Adapter へ漏れて脆く、フォーカス外からの更新を取りこぼす恐れがある。
- **supportsChangeAnimations = false 単独 / payload 付き通知単独**: どちらも単独で ViewHolder 再利用は成立するが、採用は両方。前者だけでは利用側や将来の変更が itemAnimator を差し替えた場合に防御が消え、後者だけでは payload なし通知の混入 (現存: RootHeaderFooterAdapter) に対して無防備になる。互いの穴を塞ぐ二重担保とする。

## Consequences

- 正: 日本語 IME の composing・変換候補が入力中維持される (Pixel 6a + Gboard の実機 A/B で確認)。フォーカス・カーソル位置も安定する。
- 正: 毎打鍵の行クロスフェード (ちらつき) と高速入力時の取りこぼしが解消される。
- 負: 内容変化時のクロスフェード演出が無くなる (追加・削除・移動のアニメーションは維持される)。
- 残課題: `RootHeaderFooterAdapter` の内容更新に payload なし通知が残存しており、現状は 1 (アニメーション無効化) のみに守られている。fix-root-accessory-payload-notify で本決定へ追随させる。

出典: fix-entrycell-ime-composition (exploration.md 追記 2026-08-01 / review-002.md) / ksn-dual-research 並走調査 (codex + ksn-researcher、androidx DefaultItemAnimator.java:669-673・SimpleItemAnimator.java:84-86 の照合と実機実測)
現行照合: 2026-08-01 確認。KsSettingsView.kt (supportsChangeAnimations = false)・KsSettingsListAdapter.kt (PAYLOAD_CONTENT)・EntryCellViewHolder.kt (inputType 差分ガード)・ContentUpdatePayloadTest.kt が本決定どおり実装済み。実機証跡は changes/archive/2026-08-01-fix-entrycell-ime-composition/verify-device-*.png。判定: 乖離なし (残課題の RootHeaderFooterAdapter を除く)。
現行照合: 2026-09-05 確認。RootHeaderFooterAdapter.kt (非 null 同士の差し替えを `notifyItemChanged(0, PAYLOAD_CONTENT)` で通知)・KsSettingsView.kt (PAYLOAD_CONTENT / PAYLOAD_HEADER_HEIGHT を PAYLOAD_THEME と同じ companion に集約)・RootHeaderFooterAdapterTest.kt (payload 検証)。Consequences の残課題は changes/archive/2026-09-05-fix-root-accessory-payload-notify で解消し、Alternatives の「現存: RootHeaderFooterAdapter」は解消済み。判定: 維持 (payload なしの内容更新通知は main に残っていない)。
