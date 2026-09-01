# レビュー結果: add-android-maven-distribution (002 回目)

**日付**: 2026-09-01
**判定**: CHANGES_REQUESTED → **APPROVED** (修正サイクル後の再確認で全指摘の解消を確認。末尾の「再確認」節を参照)

## サマリー

前回サイクルで確定・採用された 5 件のうち 4 件は実物で解消を確認できた — `foundation-layout` の `api` 追加は発行メタデータ (POM の compile / `.module` の api variant) まで届いており、SNAPSHOT ガードは手元で再実行して実際に発火することを確かめ、実機スクショ 2 枚は通知アイコンごとステータスバーがクロップされ、handbook 2 本の timestamp も揃っている。修正が新たな欠陥を持ち込んだ形跡もない (テスト 2700 件 / 失敗 0、comment-policy lint 禁止 0 件、足場アーティファクトの書き換えなし)。残る 1 件は `.agents/skills/docs-refresh/SKILL.md` の参照更新が**同一ファイル内で片側しか適用されていない**ことで、本変更が偽にした事実記述が 1 文残っている。1 行の修正で閉じるため差し戻す。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 本サイクルで増えたコメントは `android/kssettingsview/build.gradle.kts` のガード解説ブロックのみ。作業文書のパス・変更識別子の裸参照・`review-001` のようなローカル通番を含まず、ファイル単体で意味が通る。`scripts/comment-policy-lint.py` を再実行し 699 ファイル / 禁止 0 件
- `kasane/handbook/cross/public-identifiers.md` — `**/build.gradle.kts` を触るため。`group = jp.kamusoft` / artifactId `kssettingsview` / version の宣言元 (catalog `kssettingsview` キー) が「Maven 座標の現在地」節の記述と一致することを再確認
- `kasane/handbook/cross/test-execution.md` — テストを実行し件数を報告するため。`cd android && ./gradlew test` は全タスク up-to-date (= build.gradle.kts の今回の変更を含む現在の入力に対して記録済み出力が有効)。件数は `build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` を集計し、統合 module 1183 × 2 + bridge 167 × 2 = 2700 件 / failures 0 / errors 0
- `kasane/handbook/cross/local-development-setup.md` / `kasane/handbook/cross/user-skill-api-listing.md` — 本変更が改訂しているため diff を確認 (今回の差分は timestamp と旧 module 名 1 語のみで、本文の意味は不変)
- ksn-core `references/ui-artifacts.md` の撮影規律 (実機で通知・ステータスバーの個人要素を写さない)、`references/evidence.md` の置き場・sanitize 規律 — 差し替えたスクショ 2 枚と新規 evidence 1 本の判定に適用
- ksn-core `references/delta-spec.md` の足場凍結 — 20:24 (review-001 執筆時刻) 以降に更新されたファイルを全走査し、proposal.md / design.md / specs/ / tasks.md がいずれも無変更であることを確認 (更新は build.gradle.kts・docs-refresh SKILL.md・handbook 2 本・deviation.md・evidence/ のみ)
- 適用外と判定: `cross/sample-parity.md`・`cross/runtime-behavior-verification.md`・`cross/aiforms-origin-reference.md`・`maui/performance-verification.md`・`ios/*`
- レビュースキル: `kotlin-impl-skill` (Gradle Kotlin DSL のレビュー観点を本サイクルの新規コードに適用)
- lessons: `kasane/lessons/code-review.md` (L-001)、`kasane/lessons/process.md` (L-003 = 証跡の実在と提出コードの対応を判定条件にする / L-005 = 到達可能な修正はこのサイクル内で直す / L-006 = 不在の断定は全走査してから)

## 前回指摘の解消確認

### (1) 🟠 Major `foundation-layout` の `api` 追加と発行メタデータへの反映 — 解消

- `android/kssettingsview/build.gradle.kts:191` に `api("androidx.compose.foundation:foundation-layout")` が入り、`foundation` は `implementation` に残っている (推奨した宣言元直接指定の形)
- `~/.m2` の実発行物を再検査: POM は `androidx.compose.foundation:foundation-layout` が `<scope>compile</scope>`、`.module` は `releaseVariantReleaseApiPublication` に出現。`foundation` 本体は runtime のみ
- 型の所在を裏取り: Gradle キャッシュの `foundation-layout` aar の `classes.jar` に `androidx/compose/foundation/layout/PaddingValues.class` が実在し、この artifact が `Theme.sectionMargin` の型の宣言元であることを確認
- 版なし宣言が外部消費者で解決することも確認: `.module` の api variant に `androidx.compose:compose-bom` が `org.gradle.category: platform` 属性つきで載り、POM 側も `<scope>import</scope>` の dependencyManagement を持つ。`runtime` / `ui` と同じ経路で `foundation-layout` の版が届く
- 列挙漏れの再発がないことも確認: 発行 aar の `classes.jar` を javap で全走査して公開シグネチャに現れる外部パッケージを抽出し、`api` に無いもの (`appcompat` / `constraintlayout` / `material` / `material3` / `lifecycle`) は Kotlin ソース側で `internal` 宣言 (`CellBaseViews` / `MinHeightConstraintLayout` 等) か関数内ローカルにしか現れないことを、公開トップレベル宣言と公開メンバーの両方を走査して確認
- `deviation.md:4` に recyclerview と同型の記録があり、`evidence/publish-to-maven-local.txt` の依存スコープ節も取り直されている (記載と実発行物が一致)

### (2) 🟠 Major SNAPSHOT 時の Central 発行ガード — 解消

- `android/kssettingsview/build.gradle.kts:150-172` に、version が `-SNAPSHOT` で終わるときだけ Central 向けタスクへ `doFirst` で例外を付ける実装
- 網羅性を独立に確認: `./gradlew :kssettingsview:tasks --all` の Publishing グループに実在する Central 系タスクは `publishToMavenCentral` / `publishAndReleaseToMavenCentral` / `publishAllPublicationsToMavenCentralRepository` / `publishMavenPublicationToMavenCentralRepository` / `prepareMavenCentralPublishing` / `enableAutomaticMavenCentralPublishing` / `dropMavenCentralDeployment` の 7 本で、後始末用の `drop` を除く 6 本がすべて `centralPublishTaskNames` に入っている。SNAPSHOT 時に実アップロードを行う `publishMavenPublicationToMavenCentralRepository` 自身も直接ガードされているため、`--continue` で依存タスクの失敗を飛ばしてもアップロード経路には到達しない
- 実効を自分で再現: `./gradlew --offline :kssettingsview:publishMavenPublicationToMavenCentralRepository -PmavenCentralUsername=<dummy> -PmavenCentralPassword=<dummy>` が `prepareMavenCentralPublishing` でガード例外により FAILED。`evidence/snapshot-central-publish-guard.txt` の表と一致する
- ローカル発行は阻害されない (`publishToMavenLocal` / `publishMavenPublicationToMavenLocal` は対象外。実際に発行物が 20:30 に再生成されている)
- version 条件が効いていることの根拠 (非 SNAPSHOT ではガードが発火せず Central API へ到達する) も evidence に記録されている

### (3) 🟡 Minor 実機スクショの通知アイコン除去 — 解消

`evidence/android-integrationhost-initial.png` / `evidence/android-integrationhost-recreated.png` を両方開いて確認。ステータスバーが上端ごと落ちており、通知アイコン・時刻・電池・キャリア表記はいずれも写っていない。`evidence/maui-binding-and-integrationhost.txt:34-35` にクロップした事実と「照合に使う行はすべてクロップ後の画像に含まれる」旨が記載され、実際に期待表の全行 (root header / 3 section / root footer / header 色) が画像内に収まっている。

### (4) 🟡 Minor `.agents/skills/docs-refresh/SKILL.md` の参照パス更新 — **未解消 (部分適用)**

`:180` の `android/ks-settingsview-ui/build.gradle.kts` → `android/kssettingsview/build.gradle.kts` は適用済み。ただし同ファイル `:709` に、本変更が偽にした事実記述が残っている (下の指摘事項を参照)。

### (5) 🔵 Suggestion handbook 2 本の timestamp — 解消

`kasane/handbook/cross/local-development-setup.md` / `kasane/handbook/cross/user-skill-api-listing.md` とも `timestamp: 2026-09-01`。差分は timestamp と旧 module 名 1 語のみで、本文の意味は変わっていない。

## 指摘事項

### [🟡 Minor] docs-refresh スキルに、本変更が偽にした Gradle `group` の記述が残っている

**該当箇所**: `.agents/skills/docs-refresh/SKILL.md:709`

**問題点**:

「Maven `groupId` の記述は ADR-0002 の `jp.kamusoft` と現行 Gradle `group` (`jp.kamusoft.kssettingsview`) の食い違いがあるため、利用者向けの配布座標として断定的に書かない」と書かれている。本変更は `android/build.gradle.kts` の subprojects 一括設定で `group = "jp.kamusoft"` を与え、旧 `android/ks-settingsview-core/build.gradle.kts:21` にあった `group = "jp.kamusoft.kssettingsview"` を消した。したがって

- 括弧内の「現行 Gradle `group` = `jp.kamusoft.kssettingsview`」は事実として誤り
- 「断定的に書かない」の**理由として挙げられている食い違い自体が存在しない** (座標は `jp.kamusoft:kssettingsview` に一意に定まった)

指示の実務上の帰結 (Central へ未公開である旨を添える) は phase-7 / phase-8 が終わるまで有効なままだが、docs-refresh を起動した作業者・エージェントは誤った現行値を根拠として読むことになり、`skills/` と README へ誤った groupId を書き戻す経路が残る。

これは前回の Minor (4) と同一ファイル・同一原因 (本変更の識別子統合による失効) であり、`:180` だけが直されて `:709` が残った部分適用である。本変更が既に編集しているファイル内の 1 文で、修正は数語で閉じる (lessons process/L-005)。

**推奨修正**:

括弧内の現行値を `jp.kamusoft` に直し、「断定的に書かない」理由を食い違いではなく「Central へまだ公開していない」に置き換える。例:

> Maven `groupId` は ADR-0002 と現行 Gradle `group` がともに `jp.kamusoft` で一致している。ただし Central への公開は未実施のため、利用者向けの配布座標は公開が未導入である旨とともに記述する。

### [🔵 Suggestion] Central 向けタスク名の列挙が発行プラグインの版に暗黙に結び付いている

**該当箇所**: `android/kssettingsview/build.gradle.kts:150-157`

**問題点**: `centralPublishTaskNames` は 0.37.0 が実際に生やすタスク名を手で列挙したもので、現時点では網羅を確認できている (上記 (2))。ただしプラグインを上げたときに (a) 新しい Central 向けタスクが増えても静かに素通りし、(b) 名前が変わると集合内の項目が死に名になってガードに穴が開く。どちらも失敗が無音で、気づく契機は「SNAPSHOT が Central に上がった後」になる。コメントにも版依存である旨の注意が無い。

**推奨修正**: いずれか。

- 名前一致を述語にして将来のタスクも拾う: `if (name.contains("MavenCentral") && name != "dropMavenCentralDeployment")` (現行 7 本の内訳と一致し、列挙の保守が要らなくなる)
- 列挙を残すなら、集合の各要素が実在タスク名に一致することを `tasks.names` で照合して不一致なら失敗させ、併せて「この列挙は maven-publish 0.37.0 のタスク名に対応する。版を上げたら `tasks --all` で確認する」旨をコメントに足す

phase-8 の release workflow でこのガードが最後の安全網になるため、そこに入る前に決めておくのが安い。

### [🔵 Suggestion] ガードの例外メッセージに文字列連結由来の余分な空白が残る

**該当箇所**: `android/kssettingsview/build.gradle.kts:165-168`

**問題点**: 実行時の出力は次のようになり、日本語の文中に空白が 2 か所入る (連結する各行が先頭に空白を持つため)。

```
SNAPSHOT (0.1.0-SNAPSHOT) は Maven Central へ発行しない。 リリース版の version を
gradle/libs.versions.toml の kssettingsview キーへ 設定してから実行する。
```

内容は正確で行動指示も明快だが、リリース作業者が最初に読む文面なので整えておきたい。

**推奨修正**: 連結の継ぎ目から先頭空白を落とす (`"...発行しない。" + "リリース版の version を ..."`)。

## 前サイクルからの申し送り (本変更では対応不要)

review-001 の Suggestion のうち、蒸留・別 change へ送るとしたものは今回も状況が変わっていないため、そのまま引き継ぐ。

- `kasane/decisions/android/0016-single-module-single-maven-artifact.md:4` の status を蒸留時に `accepted` へ昇格させる
- `kasane/concepts/android/architecture/build-toolchain.md` ほかに残る旧 module 名・旧 GAV の追随対象一覧を蒸留へ渡す (ADR の `出典:` 行は歴史記述として据え置く判断を蒸留で明示する)
- Explicit API mode の導入可否を別 change として検討する

## アクションプラン

1. **[Minor]** `.agents/skills/docs-refresh/SKILL.md:709` の Gradle `group` 記述を現行値に直し、「断定的に書かない」理由を未公開である事実に置き換える
2. **[Suggestion]** `centralPublishTaskNames` を述語化するか、列挙の実在照合と版依存の注記を足す
3. **[Suggestion]** ガードの例外メッセージから連結由来の余分な空白を落とす
4. 1 を適用すれば再検証はファイル読み直しだけで足りる (ビルド・テスト・発行物・実機証跡はいずれも再取得不要)

---

## 再確認 (2026-09-01、修正サイクル後)

**判定: APPROVED** — 上の Minor 1 件・Suggestion 2 件はいずれも解消。新規指摘なし。

差分の範囲を確認した (この再確認サイクルで更新されたのは `.agents/skills/docs-refresh/SKILL.md`・`android/kssettingsview/build.gradle.kts`・`evidence/snapshot-central-publish-guard.txt` の 3 点のみ。proposal.md / design.md / specs/ / tasks.md は無変更で足場凍結を維持)。

### [🟡 Minor] docs-refresh の失効記述 — 解消

- `.agents/skills/docs-refresh/SKILL.md:709` は「ADR-0002 と現行 Gradle `group` がともに `jp.kamusoft` で一致しており、artifactId は `kssettingsview` の 1 本である。ただし Maven Central への公開は未実施のため、利用者向けの配布座標には公開が未導入である旨を添えて記述する」に書き換えられ、現状 (`android/build.gradle.kts` の `group = "jp.kamusoft"` / phase-7・8 未了) と一致する
- 併せて修正された `:696` の表記規則も確認。「Android の artifact / project 名は lowercase でブランド名の内部にハイフンを入れない」は `kasane/handbook/cross/public-identifiers.md` の命名方針節と一致しており、`kssettingsview-bridge` のようなブランド名とサブモジュールの境目のハイフンを誤検出させない書き方になっている (旧「artifactId は kebab-case」は android/ADR-0016 後の規約と食い違っていた)
- 同ファイルを独立に全走査した (`ks-settingsview` / `ks.settingsview` / `kebab` / `artifactId` / `groupId` / `jp.kamusoft` / `aar` / `gradlew` / `samples/android` / `rootProject` / モジュール表記)。本変更が失効させた記述はこれ以上残っていない。`:182` `:184` `:366` はいずれも本変更の影響を受けない一般記述
- `:704` の正規表現リテラル (`ks-settings-view` / `jp\.kamusoft\.ks-settingsview` 等) は誤形の検出パターンであって現行値の主張ではないため、据え置きの判断に同意する

### [🔵 Suggestion] Central 向けタスクの述語化 — 解消

- `android/kssettingsview/build.gradle.kts` のガードが `name.contains("MavenCentral") && name != "dropMavenCentralDeployment"` に変更され、列挙の保守が不要になった
- 網羅と過剰捕捉の両方を自分で確認: `./gradlew :kssettingsview:tasks --all` のタスク名から `MavenCentral` を含むものを抽出すると `dropMavenCentralDeployment` / `enableAutomaticMavenCentralPublishing` / `prepareMavenCentralPublishing` / `publishAllPublicationsToMavenCentralRepository` / `publishAndReleaseToMavenCentral` / `publishMavenPublicationToMavenCentralRepository` / `publishToMavenCentral` の 7 本ちょうど。述語は発行経路 6 本を漏れなく拾い、無関係なタスクを巻き込まない
- 除外の実効を確認: `./gradlew --offline :kssettingsview:dropMavenCentralDeployment -PmavenCentralUsername=<dummy> -PmavenCentralPassword=<dummy>` はガード例外ではなく `deploymentId` 未指定というタスク自身の入力検証で失敗する。後始末経路が塞がれていない
- 発火の実効を再確認: 同条件の `publishToMavenCentral` は `prepareMavenCentralPublishing` でガード例外により FAILED
- 述語を選んだ理由 (列挙はタスク増減で無音の穴が開く) と除外が 1 本だけである理由がコメントに自己完結で書かれており、`scripts/comment-policy-lint.py` は 699 ファイル / 禁止 0 件

### [🔵 Suggestion] 例外メッセージの空白 — 解消

実行時の出力が `SNAPSHOT (0.1.0-SNAPSHOT) は Maven Central へ発行しない。リリース版の version を gradle/libs.versions.toml の kssettingsview キーへ設定してから実行する。ローカルでの発行物確認には publishToMavenLocal を使う。` になり、連結の継ぎ目の空白が消えていることを実行して確認した。

### evidence の整合

`evidence/snapshot-central-publish-guard.txt` の更新内容を実物と突き合わせた。

- 第 2 節のタスク名照合表 (7 本・対象 6 / 除外 1) は、自分で取った `tasks --all` の抽出結果と完全に一致
- 第 3 節のエラーメッセージ引用は、空白除去後の実出力と一致
- 追記された `dropMavenCentralDeployment` の非発火確認も再現できた
- 第 3 節の 7 行の発火表は述語化の前後で挙動が変わらない範囲であり、代表として `publishToMavenCentral` を再実行して同じ結果になることを確認した

### 再取得しなかったもの

ビルド・テスト (2700 件 / 失敗 0)・発行物 (POM / `.module` / aar / sources jar)・実機スクショは、この再確認サイクルの差分 (コメントと述語・ドキュメント文言) が入力に影響しないため再取得していない。本文「照合した規約」の各項目の判定はそのまま有効。

### 引き継ぎ

「前サイクルからの申し送り」節の 3 件 (android/ADR-0016 の accepted 昇格、concepts の旧 module 名の追随対象一覧、Explicit API mode の別 change 検討) は本変更の対象外のまま蒸留・後続へ引き継ぐ。
