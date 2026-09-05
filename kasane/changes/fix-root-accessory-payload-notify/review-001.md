# レビュー結果: fix-root-accessory-payload-notify (001 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

合意済みスコープ (a) Root H/F の内容 payload 付き通知、(b) 内容 payload 定数 2 つの `KsSettingsView` companion への集約、(c) Root H/F テストへの payload 検証追加は、いずれも過不足なく実装されている。公開 API は増えておらず (`internal const` を維持)、定数の旧参照は main / test / concepts / skills のいずれにも残っていない。テストは 2704 件 / 失敗 0 で全件成功し、追加テスト 2 件はミューテーション実測で検出力を確認済み。

指摘は Minor 2 件・Suggestion 1 件で、いずれも本 change の正しさを損なうものではない。Critical / Major はなし。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規・改変コメントの許容参照と記述類型。`scripts/comment-policy-lint.py` は 761 ファイル / 禁止 0 件
- `kasane/handbook/cross/test-execution.md` (きっかけ: テスト実行・テスト結果の報告) — Android の `./gradlew test --rerun-tasks` と件数の得方 (`build/test-results/<タスク名>/TEST-*.xml`)
- `kasane/decisions/android/0001-*.md` (accepted) — 内容更新の payload 付き通知と二重担保
- `kasane/decisions/android/0012-*.md` (accepted) — `getChangePayload` からの payload 参照と「同一 ViewHolder 保証は view type 不変時に限る」
- 実装スキル: kotlin-impl-skill (hygiene / testing の観点)。`kasane/lessons/code-review.md` の重点観点 L-001 (ミューテーションによる検出力実測) を適用
- 適用外と判断: `cross/runtime-behavior-verification.md` (実行時挙動の不具合修正ではなく、既定 ItemAnimator への依存を外す防御の追加であり、現行構成では観測可能な挙動変化がない)、`cross/sample-parity.md` / `cross/public-identifiers.md` / `cross/user-skill-api-listing.md` (対象ファイルに触れていない)

## 検証したこと

### ビルドとテスト

`ANDROID_HOME` / JDK 17 の `JAVA_HOME` を与えて `./gradlew test --rerun-tasks` を実行し、BUILD SUCCESSFUL。XML 集計は下表のとおり全 2704 件 / 失敗 0 / エラー 0 (スキップ 0)。

| モジュール / タスク | tests | failures | errors |
|---|---|---|---|
| kssettingsview / testDebugUnitTest | 1185 | 0 | 0 |
| kssettingsview / testReleaseUnitTest | 1185 | 0 | 0 |
| kssettingsview-bridge / testDebugUnitTest | 167 | 0 | 0 |
| kssettingsview-bridge / testReleaseUnitTest | 167 | 0 | 0 |

`RootHeaderFooterAdapterTest` は 9 件 (既存 7 + 追加 2) すべて成功。

### 追加テストの検出力 (ミューテーション実測、lessons L-001)

`RootHeaderFooterAdapter.kt` の setter を `notifyItemChanged(0, KsSettingsView.PAYLOAD_CONTENT)` → `notifyItemChanged(0)` に一時変更して `RootHeaderFooterAdapterTest` を実行したところ、**追加した 2 件だけが FAILED**、既存 7 件 (件数のみ見る `notifyItemChanged_0` テストを含む) は成功した。追加テストがトートロジーではなく payload 有無を判別していること、既存テストがこの回帰を捕まえられていなかったことの両方が実測で確定した。実測後にファイルは原状復帰済み (diff は 8 insertions / 5 deletions に一致)。

### 3 引数版 onBindViewHolder への影響 (回帰の有無)

本 change で Root H/F の内容更新は初めて payload 付きとなり、`RootHeaderFooterAdapter.onBindViewHolder(holder, position, payloads)` の振り分けを通るようになる。`themeOnly` の条件は `payloads.all { it == PAYLOAD_THEME }` であり `PAYLOAD_CONTENT` は該当しないため `super` へ委譲され、フル bind に落ちる。この経路は `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RootAccessoryThemeRefreshTest.kt:462` (`View 形式の Root Header は別インスタンスへの差し替えで作り直される`) が実 RecyclerView 経由で押さえており、payload 付与後も成功している。Theme と内容が同一描画機会に重なった場合も `payloads = [PAYLOAD_THEME, PAYLOAD_CONTENT]` となり `themeOnly` が false になるため取りこぼさない。

Text ↔ View の形式切替を伴う差し替えでも、`RecyclerView.validateViewHolderForOffsetPosition` が view type 不一致を検出して新しい ViewHolder を得るため破綻しない (android/ADR-0012 が明記する「同一 ViewHolder 保証は view type 不変時に限る」と整合)。

### スコープと足場

- `exploration.md` の差分は「動機の補正 (2026-09-05 探索でコード裏取り)」「スコープ確定 (2026-09-05 ユーザー)」等、探索フェーズの記録であり、実装中の足場書き換えには当たらない
- スコープ外とされた「同一 View インスタンス再代入時の同値ガード」(未決の論点 ②) には手が入っていない
- `deviation.md` なし。無断の仕様逸脱は検出されなかった
- 旧参照 `KsSettingsListAdapter.PAYLOAD_*` はリポジトリ全体で 0 件 (exploration.md の却下案の記述を除く)。`kasane/concepts/` `kasane/handbook/` `skills/` `README*.md` にも payload 定数への言及はなく、追随漏れなし
- `explicitApi()` が有効な `kssettingsview` モジュールで `internal const val` としてコンパイルが通っており、公開 API は増えていない

## 指摘事項

### [🟡 Minor] `ReplaceCell` の公開 KDoc に payload なし通知の記述が残っている

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiff.kt:60`

**問題点**: 本 change 後、main ソースで内容更新の通知を **payload なし**で説明している箇所はここだけになった。

```
* - Android: アダプタが id → position を解決し `notifyItemChanged(position)`（再生成なしの再 bind）で反映する。
```

android/ADR-0001 の Context は「コード内の『`notifyItemChanged` は ViewHolder を破棄せず同一 ViewHolder に bind する』というコメント前提が既定 ItemAnimator 下では誤っていたことが根本原因」と、この文言そのものを根本原因として名指ししている。実装は `KsSettingsView.PAYLOAD_CONTENT` 付きで通知しており、この KDoc は現行仕様を誤って記述している。公開型 (`public data class ReplaceCell`) の doc コメントなので利用者にも届く。

本 change が触ったファイルではないが、扱っている主題は同一 (内容更新の通知形式の統一) であり、同じ change 内で直すのが自然な隣接課題と判断する。

**推奨修正**: 括弧内の断定を現行仕様に合わせる。公開 doc コメントなので内部用語 (ADR ID 等) は持ち込まず、機能の説明にとどめる。例:

```
* - Android: アダプタが id → position を解決し、部分更新の変更通知で同一 ViewHolder へ再 bind する。
```

### [🟡 Minor] `ChangeRecordingObserver` が KDoc ごと完全重複している

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapterTest.kt:107-121` / `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ContentUpdatePayloadTest.kt:118-133`

**問題点**: 追加された `ChangeRecordingObserver` は、同一パッケージ・同一テストソースセットにある既存クラスと**クラス名・KDoc 本文・実装が完全に一致**するコピーである。payload 記録の観測方法を変えたいとき (例: `itemCount` も記録する、複数通知の順序を検証する) に 2 箇所が独立に育ち、テストの流儀が割れる。

同パッケージには共有テストヘルパ `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt` が既にあり、置き場所は用意されている。

**推奨修正**: どちらか一方を `KsSettingsViewTestSupport.kt` (または同パッケージのトップレベル `internal class`) へ引き上げ、両テストから参照する。既存の `FullUpdateContentSyncTest` の記録クラスは記録内容が異なる (position / itemCount / payload の三つ組) ため統合対象に含めなくてよい。

### [🔵 Suggestion] 新規コメントが payload を「唯一の防御」と読ませる

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt:47-50` および `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapterTest.kt:18-20`

**問題点**: どちらも「payload なしの通知では `SimpleItemAnimator.canReuseUpdatedViewHolder` が false を返し、ViewHolder が新規生成されて旧行とクロスフェードする」と無条件に書いている。実際に false を返すのは**既定の `DefaultItemAnimator` 下**の話で、本プロジェクトは `KsSettingsView` 側で `supportsChangeAnimations = false` を設定しているため、現行構成では payload の有無に関わらず true が返る。

このファイルだけを読む人は「payload を外すと今すぐ壊れる」と受け取るが、実際には二層目の防御であり、payload が効くのは利用者やテーマが itemAnimator を差し替えた場合に限る。exploration.md の「動機の補正」もこの点を明示している。既存の `KsSettingsListAdapter.kt:49-54` は同じ説明の後に「`KsSettingsView` 側でも change アニメーションを無効化して二重に担保している」を添えており、そことも記述の厚みが揃わない。

**推奨修正**: 「既定 ItemAnimator 下では」の限定を足すか、`KsSettingsListAdapter` と同様に二重担保である旨を 1 文添える。テストクラスの KDoc も同様。

## アクションプラン

1. `SettingsRootDiff.kt:60` の `ReplaceCell` KDoc を現行仕様 (payload 付き部分更新) に合わせる — 隣接課題として本 change 内で処理するのが望ましい
2. `ChangeRecordingObserver` を共有テストヘルパへ引き上げて重複を解消する
3. `RootHeaderFooterAdapter.kt` / `RootHeaderFooterAdapterTest.kt` のコメントに二重担保の限定を足す

いずれも Critical / Major ではなく、1〜3 を見送っても本 change の実装自体は合意済みスコープを満たしている。

## 蒸留への申し送り

`kasane/decisions/android/0001-*.md` の Consequences「残課題: `RootHeaderFooterAdapter` の内容更新に payload なし通知が残存しており、現状は 1 (アニメーション無効化) のみに守られている。fix-root-accessory-payload-notify で本決定へ追随させる」と、末尾の現行照合「判定: 乖離なし (残課題の `RootHeaderFooterAdapter` を除く)」は、本 change の完了により解消済みとなる。ADR の追随は ksn-distill の責務のため、レビューでは書き換えず申し送りとして記す。
