# Proposal: fix-decoration-theme-not-applied-on-initial-bind

## Why

初期 Theme を持つ `SettingsRootStore` を `bind` すると、**`ItemDecoration` だけが構築時の既定 `Theme()` のまま取り残される** (セパレータ色などが初期 Theme に追従しない)。

機構:

1. `bind(store)` は `setRootDirect(store.state.value, store.theme.value)` を呼ぶ
2. `setRootDirect` は `themeBacking` を直接更新するが `applyDecoration(style)` を呼ばない
3. その直後に張られる `store.theme.collect` は `themeBacking` が既に同値のため、同値スキップ (`if (themeBacking == value) return`) で `applyThemeInternal` を回避する
4. 結果、`ItemDecoration` にだけ初期 Theme が届かない

該当箇所 (行番号は 2026-08-03 時点):

- `android/ks-settingsview-ui/.../KsSettingsView.kt:290-308` — `bind`
- `KsSettingsView.kt:372-386` — `setRootDirect` (通知を出さない設計)
- `android/ks-settingsview-compose/.../KsSettingsViewComposable.kt:92` — 到達経路。`SettingsRootStore(initialRoot = initialRoot, initialTheme = theme)` を作って factory で `bind` するため、`KsSettingsView(theme = カスタム Theme) { ... }` の初回描画がこの経路に乗る

**再現は確認済み**: `fix-adapter-not-restored-on-reattach` の review-002 で、レビュアーが Robolectric の使い捨てプローブで実測している (probe3 = attach 済み View への `bind` / probe5 = attach 前の `bind` = Compose `AndroidView.factory` 相当。いずれも「`ItemDecoration` も初期 Theme になるか」で FAIL)。

発見経緯: `fix-adapter-not-restored-on-reattach` の review-002 における Minor 指摘。**同変更の適用前 (HEAD) でも同じく再現する既存不具合**であり、同変更で悪化も改善もしないことを実測で確認済みのため、独立した変更として切り出した。

証跡: `kasane/changes/fix-adapter-not-restored-on-reattach/review-002.md` の「[🟡 Minor] 初期 Theme 付き Store を bind したとき `ItemDecoration` に初期 Theme が届かない」節 (同変更の蒸留後は `kasane/changes/archive/` 配下へ移動する)。

## What Changes

対象能力: **settings-view-android-ui** (Android View Host)

- 初期 Theme が `ItemDecoration` にも届くようにする
- **`setRootDirect` に `applyDecoration(style)` を足すのが最短だが、単純な数行修正で済むとは限らない**。同メソッドの KDoc は「`AsyncListDiffer` 在中の `submitList` と競合する `notifyDataSetChanged` 多重呼び出しを避ける」ために通知を出さない設計であることを明記している。**android/ADR-0001 (内容更新は payload 付き通知と change アニメーション無効で同一 ViewHolder を維持する) の通知経路との切り分け**を踏まえて方式を決める
- 退行テストを追加する (初期 Theme 付き Store の `bind` で `ItemDecoration` が初期 Theme になること。attach 前 / attach 後の両経路)

## Non-Goals

- 公開 API の変更
- android/ADR-0001 の通知方式そのものの見直し (`setRootDirect` が通知を出さない設計理由を壊さない)
- iOS / MAUI (Android View Host 固有)
- Theme の同値スキップ (`themeBacking == value`) そのものの撤廃 — 再入・多重適用の抑制として機能しているため、撤廃ではなく初期適用の取りこぼしだけを塞ぐ

## Impact

- 破壊的変更なし。挙動変更は「初期 Theme を指定したときのセパレータ色等が初回描画から正しくなる」のみ
- リスク: `setRootDirect` の通知抑制設計に触れるため、`AsyncListDiffer` の `submitList` との競合と、ADR-0001 が保証する ViewHolder 維持契約を壊さないことをテストで担保する必要がある

## 級: S (想定)

バグ修正 / 単一能力内 / 公開 API 変更なし / 局所的かつ可逆。

ただし **ADR-0001 の通知設計そのものに触れる方式を採らざるを得ない場合は M へ引き上げる** (その場合はデルタスペックを作成し、ADR の改訂要否も判断する)。級の確定はユーザーが行う。

デルタスペックは S 級のため作成しない (verify は非適用)。受け入れ基準は tasks.md が持つ。独立文脈でのレビューは S 級でも必須。

domain: android
