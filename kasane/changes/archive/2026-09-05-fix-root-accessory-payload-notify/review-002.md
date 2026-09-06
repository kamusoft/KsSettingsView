# レビュー結果: fix-root-accessory-payload-notify (002 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

前回 (review-001) の指摘 3 件はいずれも閉じている。`ReplaceCell` の公開 KDoc は payload 付き通知の記述に直り (内部用語の持ち込みなし)、`ChangeRecordingObserver` は共有テストヘルパへ 1 本化され、Root H/F 側のコメントには「既定の ItemAnimator 下では」の限定と二重担保の一文が入った。修正で新たな回帰は入っておらず、テストは 2704 件 / 失敗 0、共有ヘルパ化後も payload 検証の検出力はミューテーション実測で維持を確認した。

1 周目のスコープ全体 (Root H/F の payload 付き通知・payload 定数の `KsSettingsView` companion への集約・payload 検証テストの追加) も合意済みスコープを過不足なく満たしており、公開 API は増えていない。今回の指摘は Minor 1 件・Suggestion 2 件で、いずれもコメントの記述精度に関するもの。Critical / Major はなし。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規・改変コメントの許容参照・禁止類型・公開 doc コメントの内部用語禁止。`python3 scripts/comment-policy-lint.py --summary` は 761 ファイル / 禁止 0 件 (ただし検査範囲は規約より狭いため、公開 doc コメントの内部用語と記述精度は本文から判定した)
- `kasane/handbook/cross/test-execution.md` (きっかけ: テスト実行・結果報告) — `./gradlew test --rerun-tasks` と `build/test-results/<タスク名>/TEST-*.xml` からの件数集計
- `kasane/decisions/android/0001-content-update-preserves-viewholder.md` (accepted) — 内容更新は payload 付き通知 + change アニメーション無効の二重担保。Decision 2「payload なしの `notifyItemChanged` を内容更新経路に新設しない」と Consequences の残課題への追随として整合
- `kasane/decisions/android/0012-full-update-content-sync-diffcallback-and-setrootdirect.md` (accepted) — `getChangePayload` からの payload 参照。定数の参照元の書き換えのみで決定内容に変更なし
- 実装スキル (config `domain-skills.android.code-review`): kotlin-impl-skill — 言語イディオム / コード衛生 / テストの観点。今回の差分は言語層の論点を持たない (Coroutines・null 安全・sealed の変更なし)
- `kasane/lessons/code-review.md` L-001 (重点観点) — 検出力のミューテーション実測を適用 (下記)。「指摘しないこと」は昇格済みルールなし
- 適用外と判断: `cross/runtime-behavior-verification.md` (今回の修正はコメント・テスト配置・公開 KDoc の変更のみで実行時挙動を変えない)、`cross/sample-parity.md` / `cross/public-identifiers.md` / `cross/user-skill-api-listing.md` / `cross/local-development-setup.md` / `cross/release-procedure.md` / `cross/aiforms-origin-reference.md` (対象ファイル・作業に当たらない)

## 検証したこと

### ビルドとテスト

`ANDROID_HOME` と JDK 17 の `JAVA_HOME` を与えて `./gradlew test --rerun-tasks` を実行し BUILD SUCCESSFUL。XML 集計は全 2704 件 / 失敗 0 / エラー 0 / スキップ 0。

| モジュール / タスク | tests | failures | errors |
|---|---|---|---|
| kssettingsview / testDebugUnitTest | 1185 | 0 | 0 |
| kssettingsview / testReleaseUnitTest | 1185 | 0 | 0 |
| kssettingsview-bridge / testDebugUnitTest | 167 | 0 | 0 |
| kssettingsview-bridge / testReleaseUnitTest | 167 | 0 | 0 |

### 前回指摘の閉じ方

| 前回指摘 | 状態 | 確認内容 |
|---|---|---|
| Minor: `ReplaceCell` の公開 KDoc に payload なし通知の記述 | 閉 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiff.kt:60-62` が「payload 付きの `notifyItemChanged(position, payload)`（同一 ViewHolder への再 bind。payload なしでは既定の ItemAnimator が ViewHolder を作り直す）」に更新済み。公開 doc コメントに ADR ID・change 名等の内部用語は含まれず、内部専用の定数名も出していない (定数は internal のため名指しできない) |
| Minor: `ChangeRecordingObserver` の完全重複 | 閉 | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt:167-181` に `internal class` として 1 本化。`ContentUpdatePayloadTest` 側の private コピーと不要になった `RecyclerView` の import は削除され、`RootHeaderFooterAdapterTest` も同じクラスを参照している。記録内容の異なる `FullUpdateContentSyncTest` の recorder は前回の判断どおり据え置き |
| Suggestion: payload を「唯一の防御」と読ませるコメント | 閉 | `RootHeaderFooterAdapter.kt:48-51` と `RootHeaderFooterAdapterTest.kt:18-22` の双方に「既定の ItemAnimator 下では」の限定と「`KsSettingsView` 側の change アニメーション無効化と合わせた二重担保 (android/ADR-0001)」が入り、`KsSettingsListAdapter` 側の記述の厚みと揃った |

### 共有ヘルパ化後の検出力 (ミューテーション実測、lessons L-001)

観測クラスを 2 テストクラスで共有する形に変えたため、共有後も payload 有無を判別できるかを実測した。`RootHeaderFooterAdapter.kt` の setter を `notifyItemChanged(0)` に、`KsSettingsListAdapter.kt` の `submitListAndNotifyContent` を `notifyItemChanged(position)` に同時変更して該当 2 クラスを実行したところ、**payload を検証する 4 件だけが FAILED**した。

- `RootHeaderFooterAdapterTest > Text 形式の置換は内容 payload 付きで通知される`
- `RootHeaderFooterAdapterTest > View 形式の置換は内容 payload 付きで通知される`
- `ContentUpdatePayloadTest > submitContentUpdate は payload 付きで notifyItemChanged を発行する`
- `ContentUpdatePayloadTest > 複数 Cell の同時内容更新でも全てに payload が付く`

同じクラス内の他のテスト (件数だけを見る `notifyItemChanged_0` テストを含む) は成功し、共有ヘルパ経由でも両テストクラスが独立に検出力を保っていることが確定した。実測後に 2 ファイルを原状復帰し、`git diff --numstat` が実測前と一致 (`KsSettingsListAdapter.kt` 12/34、`RootHeaderFooterAdapter.kt` 9/5) することを確認。復帰後の再実行は BUILD SUCCESSFUL。

### スコープ・整合・追随漏れ

- 旧参照 `KsSettingsListAdapter.PAYLOAD_CONTENT` / `PAYLOAD_HEADER_HEIGHT` は main / test に 0 件。`kasane/concepts/` `kasane/handbook/` `skills/` `README*.md` にも payload 定数への言及はなく追随漏れなし
- `PAYLOAD_CONTENT` / `PAYLOAD_HEADER_HEIGHT` は `public companion object` 内で `internal const val` として宣言され、explicitApi 有効モジュールでコンパイル成功。別モジュール `kssettingsview-bridge` からの参照は公開の `PAYLOAD_THEME` のみで、internal 化による破綻はない
- スコープ外とされた「同一 View インスタンス再代入時の同値ガード」(未決の論点 ②) には手が入っていない。`deviation.md` なし、無断の仕様逸脱は検出されなかった
- `exploration.md` の差分は探索フェーズの記録 (動機の補正・スコープ確定) であり、実装中の足場書き換えには当たらない
- `python3 scripts/local-path-lint.py` 検出 0 件

## 指摘事項

### [🟡 Minor] 触れたコメントに存在しない型名 `KsAnyView.View` が残っている

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt:47`

**問題点**: 今回の修正でこの行は書き換えられている (末尾の句点追加) が、`KsAnyView.View` という型は存在しない。`KsAnyView` の実装は `Compose` と `AndroidView` の 2 つで、`View` を名乗るのは `RootAccessory.View` のほう。リポジトリ全体でこの綴りが出るのはこの 1 行だけで、grep で到達できない参照になっている。

comment-policy はコード識別子への参照を「grep で到達でき、消えれば同一コミット内で壊れに気づける」ものとして許容しているが、存在しない識別子はその前提を満たさない。この行のすぐ下に今回追加した 4 行の説明が続くため、読み手が最初に当たる 1 行が誤った型を指している状態になっている。

**推奨修正**: 指したい対象に合わせて `RootAccessory.View` (Root Accessory の View 形式) か、型名を出さず「中身 (`KsAnyView` が持つ利用者 View)」のような表現へ直す。

### [🔵 Suggestion] 3 引数版 `onBindViewHolder` の KDoc から payload なしの経路の記述が落ちた

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt:119-120`

**問題点**: 「それ以外」の列挙が `payload なしの内容差し替え` から `[KsSettingsView.PAYLOAD_CONTENT] を伴う内容差し替え` に置き換わった。内容更新については正しくなったが、payloads が空のまま 3 引数版に入る経路 (挿入直後・初回 bind・スクロールによる bind) は実装に残っており、`themeOnly` の `payloads.isNotEmpty()` ガードで `super` へ落ちる。列挙から payload なしが消えたことで、この経路が KDoc から読み取れなくなっている。

姉妹実装の `KsSettingsListAdapter.kt:205` は「[KsSettingsView.PAYLOAD_CONTENT] を含む・**payload なし**・text accessory・Cell」と payload なしを列挙に残しており、記述の厚みが揃っていない。

**推奨修正**: 列挙に `payload なし` を戻す (例: 「[KsSettingsView.PAYLOAD_CONTENT] を伴う内容差し替え・payload なしの bind・text 形式・Theme 以外の payload が混ざる通知」)。

### [🔵 Suggestion] 共有テストヘルパのファイル冒頭コメントが追加内容を含んでいない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTestSupport.kt:13-18`

**問題点**: 冒頭コメントはこのファイルを「メインループの待機と表示内容の観測ユーティリティ」と規定し、「待機 (idle / awaitConvergence / awaitDifferCommit) と観測 (committedTexts / visibleRowTexts) は…ここに 1 つだけ置く」と、置くものを列挙して範囲を宣言している。今回追加した `ChangeRecordingObserver` は変更通知の記録であり、この宣言のどちらにも属さない。ファイル冒頭だけを読んだ人が想定する中身と実際の中身がずれ、次に共有ヘルパを足す人がこのファイルを候補から外す可能性がある。

**推奨修正**: 冒頭コメントの範囲宣言に「変更通知の記録」を 1 項目足す (例: 「待機・表示内容の観測・変更通知の記録は…ここに 1 つだけ置く」)。

## アクションプラン

1. `RootHeaderFooterAdapter.kt:47` の `KsAnyView.View` を実在する型名 (または型名なしの表現) へ直す
2. `RootHeaderFooterAdapter.kt:119-120` の列挙に payload なしの経路を戻す
3. `KsSettingsViewTestSupport.kt:13-18` の範囲宣言に変更通知の記録を足す

3 件ともコメントの記述精度に関するもので、実装・テストの正しさには影響しない。1 は誤った型名という点で他より優先度が高いが、いずれも見送っても本 change は合意済みスコープを満たしている。

## 蒸留への申し送り

review-001 の申し送りをそのまま引き継ぐ。`kasane/decisions/android/0001-content-update-preserves-viewholder.md` の Consequences「残課題: `RootHeaderFooterAdapter` の内容更新に payload なし通知が残存」と、末尾の現行照合「判定: 乖離なし (残課題の `RootHeaderFooterAdapter` を除く)」は本 change の完了で解消済みとなる。あわせて Alternatives Considered の「payload なし通知の混入 (現存: RootHeaderFooterAdapter)」の現存表記も現行と合わなくなる。ADR の追随は ksn-distill の責務のため、レビューでは書き換えず申し送りとする。

また `kasane/decisions/android/0012-full-update-content-sync-diffcallback-and-setrootdirect.md` の Consequences 関連項「DiffCallback が `PAYLOAD_CONTENT` を参照するようになるため、payload 定数の置き場所の集約の重みが増す」は、本 change の集約で解消済みとなる (ADR 本文の `PAYLOAD_CONTENT` は型修飾なしの表記のため、定数の移動による記述の破綻はない)。
