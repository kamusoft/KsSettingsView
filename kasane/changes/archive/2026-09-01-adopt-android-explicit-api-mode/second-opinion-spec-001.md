# セカンドオピニオン: adopt-android-explicit-api-mode (spec-001)
**相方**: claude / **label**: so-spec-adopt-android-explicit-api-mode / **日付**: 2026-09-01 / **対象**: proposal.md / specs/android-public-api-boundary/spec.md / tasks.md
---
レビュー完了です。判定は **CHANGES_REQUESTED**（Major 3 件 / Minor 3 件 / Suggestion 4 件）。依頼の制約に従い、ファイルは書かず静的レビューのみ（ビルド・テスト未実行）で返します。

## サマリー

方向性（初回リリース前に Strict で公開境界を強制し、契約でない 3 件を internal へ戻す）と ADR-0022 の論拠は妥当で、提案・spec・tasks・exploration・ADR の間に方針の矛盾はありません。Impact の「リポジトリ内に利用なし」も実測で裏が取れました（下記「確認できた点」）。一方、**この spec は「公開面が意図どおりに変わったこと」を検証できる形になっていません**。Strict の有効化が実際に効いていることも、~390 宣言の機械的改修で公開 API を取りこぼしていないことも、現在の Requirement / tasks を全部満たしても保証されません。

---

## 指摘事項

### 🟠 Major 1 — 「公開面が縮小 3 件以外に変化しない」ことを保証する Requirement もタスクも無い

**該当箇所**: `proposal.md:28`、`specs/android-public-api-boundary/spec.md:3-17`、`tasks.md:6,18`

**問題点**: proposal は「production source の広い範囲に機械的差分が生じるため、**Strict compilation を完了条件とし、公開・内部境界の取り違えを防ぐ**」としていますが、Explicit API Strict は *明示の欠落* しか検出しません。宣言を誤って `internal` にしても診断は 0 件のまま通ります。つまり本文が挙げる緩和策は、緩和したい失敗モード（過剰降格・公開 API の取りこぼし）に対して無効です。

spec 側にも「列挙した 3 件以外の公開面は不変」という Requirement が無いため、`Theme` や `CellViewHolder` の一部メンバーを internal 化してしまっても全 Scenario は成立し、ksn-verify も VALID を出します。Maven 初回リリース直前・~93 source への一括改修という条件で、これは実害に直結します。

なお、この change の起点となった不具合（`androidx.recyclerview` / `foundation-layout` の `api` 漏れ）は、**発行 aar の公開面全走査**でしか見つからなかったものです（`kasane/lessons/inbox/principle-with-enumeration-not-swept-against-public-surface.md`）。同じ走査手段を本 change の検証に入れていないのは惜しい。

**推奨修正**:
- Requirement を 1 本追加する。例:「Android 本体 module の release 発行物の公開宣言集合は、変更前の集合から `KsCellRegistry.viewTypeOf` / `isRegistered` / `SettingsRootStore.preview` を除いたものと SHALL 一致する」。Scenario は「変更前後の公開面を機械的に列挙して差分を取る → 差分が上記 3 件の削除のみ」で書ける。
- tasks に「変更前 HEAD と変更後で release aar（または Kotlin metadata）の public 宣言を列挙して差分を取り、3 件以外の増減が無いことを確認する」を追加する。恒久化するなら `binary-compatibility-validator` の `apiDump` を成果物としてコミットし、以後の公開面変更を差分レビュー可能にする案も検討に値する（Strict と補完関係で、Strict 単独では埋まらない穴をちょうど塞ぐ）。
- proposal.md:28 の「Strict compilation を完了条件とし…取り違えを防ぐ」は、実際の防御機構に合わせて書き直す。

---

### 🟠 Major 2 — Strict が本当に有効になったことの陽性対照が無い（Requirement 1 の Scenario 1 に対応タスクが無い）

**該当箇所**: `specs/android-public-api-boundary/spec.md:7-11`、`tasks.md:5,18`

**問題点**: `tasks.md:18` は「診断が 0 件であること」を確認しますが、この観測は次の 2 状態を区別できません。

1. Strict が有効で、全宣言が明示済み（意図した状態）
2. Strict が設定に入ったが**適用されていない**（no-op）

Explicit API mode の適用範囲は Android の variant 別 compilation（`compileDebugKotlin` / `compileReleaseKotlin`）と KGP の組み合わせに依存し、`kotlin { explicitApi() }` が main compilation に届くかどうかは版依存の挙動として報告されてきた領域です。しかも本 change は「まず全宣言に `public` を付ける」（`tasks.md:6`）ので、**設定が効いていなくても診断 0 件になる**という、最も気づきにくい形で no-op が成立します。exploration の未決論点でも「コンパイラ実測は Android SDK 未設定で停止」とあり、有効性は一度も実測されていません。

spec 側でも、Scenario「公開宣言の明示不足を拒否する」(`spec.md:7-11`) にマッピングされたタスクが 1 つも無く（`tasks.md:5,6` は有効化と明示、`18` は 0 件確認）、tasks と spec が非対応です。

**推奨修正**: tasks に陽性対照を追加する。例:「任意の公開宣言から visibility 修飾子（または戻り値型）を一時的に外し、`:kssettingsview:compileDebugKotlin` と `compileReleaseKotlin` が Explicit API の診断で失敗することを確認し、原状復帰する」。復帰は shasum 照合で確認する（`kasane/lessons/code-review.md` L-001 の手法がそのまま使える）。これで Scenario 1 が検証可能になり、`tasks.md:18` の「0 件」に意味が生まれます。

---

### 🟠 Major 3 — 「外部利用者」を GIVEN とする 4 Scenario の検証設計が未定義／同一 module のテストでは判定できない

**該当箇所**: `specs/android-public-api-boundary/spec.md:23-33, 39-49`、`tasks.md:15,16,17`

**問題点**:

1. `tasks.md:15,16` は「既存テストで照合」としていますが、既存テストは `android/kssettingsview/src/test/` にあり**同一 module**です。internal は同一 module から見えるため、これらのテストは Scenario の GIVEN（外部 Kotlin code）を満たさず、public / internal を区別できません。`android/kssettingsview/src/test/kotlin/.../KsCellRegistryTest.kt:69` や `SettingsRootStoreTest.kt:414` は降格後もそのまま通ります。つまり正の Scenario 4 件は実質 `tasks.md:17` の probe だけが担保します。
2. その `tasks.md:17` が「本体とは別の Kotlin compilation で…確認する」としか書いておらず、実装者が詰まります。probe をどこに置くか（新規モジュール／`samples/android` の composite build／`kotlin-compile-testing` 等のテスト内コンパイル）、**コミットするのか**、負の probe（コンパイルが失敗することが期待値）をどうやってビルドを壊さずに常設するか、が未決です。負の probe をそのままコミットすると `./gradlew build` が壊れ、コミットしなければ回帰ガードにも再現可能な証跡にもなりません。
3. リポジトリには実在する外部消費者 `samples/android`（`samples/android/settings.gradle.kts:39` の `includeBuild("../../android")` による composite build）がありますが、tasks のどこにも名前がありません。`android/` の Gradle build からは到達せず、`.github/workflows/verify-android.yml:53` の `./gradlew test` も `android/` 配下のみなので、**サンプルのコンパイル破壊は CI で検出されません**。過剰降格が起きた場合に唯一気づけるはずの経路が検証対象外です。

**推奨修正**:
- 正の probe は `samples/android` のコンパイル（`samples/android/gradlew :app:assembleDebug` 等）を明示的にタスク化する。これで「別 build の外部消費者」という GIVEN が実物で満たせます。
- 負の probe は方式を spec / tasks のどちらかで確定する。常設したいなら、`viewTypeOf` / `isRegistered` / `preview` の Kotlin metadata 上の visibility が `INTERNAL` であることを reflection で表明するテスト（コミット可・自動実行可）に置き換えるのが素直です。手動一回限りにするなら、証跡（コンパイルエラーの抜粋）を `evidence/` に残す旨をタスクに書く。
- `tasks.md:15,16` の「既存テストで照合」は、Scenario の担保にならない旨を書き分ける（内部利用の維持確認＝既存テスト、外部可視性の確認＝probe）。

---

### 🟡 Minor 4 — 「テスト source と Bridge module は対象外」に Scenario もタスクも無い

**該当箇所**: `specs/android-public-api-boundary/spec.md:5`、`tasks.md:5`

**問題点**: Requirement 本文が明確に対象外を宣言しているのに、それを確かめる Scenario がありません。実装は「KGP が test compilation を除外する」という暗黙の前提に依存します。`tasks.md:5` も「Bridge module の設定は変更しない」までで、Bridge / test に Strict が波及していないことの確認がありません（`tasks.md:19` の `./gradlew test` が通れば波及していないことは間接的に分かりますが、その論理が成果物に書かれていない）。

**推奨修正**: Scenario を 1 本足すか（「test source の暗黙 public 宣言があっても compilation が成功する」）、Requirement 本文を「本 change では Strict を本体 module の production compilation にのみ適用する」と実装レベルの断定に寄せ、`tasks.md:19` の合格条件に「Bridge / test compilation に Explicit API 診断が出ないこと」を明記する。

---

### 🟡 Minor 5 — 降格候補の列挙が公開面の全走査を経ていない

**該当箇所**: `exploration.md`「公開・内部境界の調査結果」/「未決の論点」、`tasks.md:10,11`

**問題点**: 降格 3 件は 2026-08-29 の skills-api-coverage 調査と相方のヒューリスティック（「暗黙 public 約 390 宣言」）由来で、コンパイラ／aar の全走査を経ていないと exploration 自身が書いています。これは `kasane/lessons/inbox/principle-with-enumeration-not-swept-against-public-surface.md` が警告する「原理＋未走査の列挙」と同型です。

私の手元照合では、`ui/` の internal 化はすでにかなり行き届いており（`KsCellRegistry.clear` / `createViewHolder` / `KsSettingsView.setRootDirect` / `EffectiveStyle` / `EmptyPlaceholderViewHolder` はいずれも `internal` 済み）、3 件という結論自体は妥当に見えます。ただし判定根拠が「走査した結果」ではなく「以前の調査の再掲」であるため、`tasks.md:6` の機械的明示の過程で新たに露出する候補（公開クラスのメンバー側）が拾われる保証がありません。

**推奨修正**: `tasks.md:6` の完了条件に「`public` を明示した宣言の一覧を出力し、利用者向け契約かどうかの keep / demote 判定を 1 度で通す（判定結果は deviation もしくは実装報告に残す）」を加える。Major 1 の公開面差分タスクと同じ走査で兼ねられます。

---

### 🟡 Minor 6 — iOS との公開面非対称が、どこにも追跡されないまま残る

**該当箇所**: `proposal.md` Non-Goals、`kasane/handbook/cross/user-skill-api-listing.md:38-40`

**問題点**: 本 change 後、`SettingsRootStore.preview` は Android で internal、iOS で public のままになります。同種の Registry 補助（iOS の `KsCellRegistry.resolveRendererType()` / `removeAll()`）も iOS では public です。Non-Goals は iOS を `ios-effectivestyle-visibility` に委ねていますが、同 change のスコープは `EffectiveStyle` 限定で、`preview` や Registry 補助は含まれていません（handbook の除外リストでも iOS 側は「低頻度の細部」であり「可視性引き下げ候補」ではない）。結果として、platform 間の非対称が誰の担当でもない状態で生まれます。

**推奨修正**: Non-Goals に「iOS 側 `SettingsRootStore.preview` / Registry 補助の非対称は本 change では解消せず、〈追跡先〉で扱う」と追跡先を明示する（別 change の起票、または handbook 除外リストへの注記）。判断そのものは変えなくてよく、行き先を残すだけで足ります。

---

### 🔵 Suggestion 7 — `tasks.md:16` の「追加または整理」は受け入れ基準が判定できない

「回帰ケースを追加または整理する」は、何もしなくても「整理した」と主張できます。「`SettingsRootStore(root, theme)` 経由で root / theme が初期化されることを検証するテストが存在する状態にする」と、到達状態で書くことを推奨します。

### 🔵 Suggestion 8 — 機械的差分と意味的差分をコミット分離する

`tasks.md:6`（~93 source への `public` 付与）と `tasks.md:10,11`（降格 3 件）が同一 diff に混ざると、レビューで意味のある変更を機械的変更の中から拾い出せません。タスク順どおりに commit を分ける旨を tasks に一言入れると、独立レビューの検出力が実質的に上がります。

### 🔵 Suggestion 9 — `tasks.md:5` の「`android/ADR-0022` の根拠コメント」

`kasane/handbook/cross/comment-policy.md` は常時適用で、コメントは単独で理解できる必要があります。ADR 番号だけを書いた設定コメントにならないよう、「なぜ Strict か（公開境界をコンパイラに強制させる）」を本文で書き、ADR 参照は補助に留める旨をタスク文に含めることを推奨します。

### 🔵 Suggestion 10 — Requirement 2 の Scenario 列挙に `CellViewHolder` が無い

`KsCellRegistry.register` の factory は `CellViewHolder<out Cell>` を返すため、外部利用者の独自 Cell 登録は `CellViewHolder`（`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellViewHolder.kt:39`）が public であることに依存します。proposal 本文では「Cell の公開 extension point」として触れられていますが、`spec.md:26` の WHEN には現れません。probe に含めるなら Scenario 側にも足すと、検証対象が spec だけで閉じます。

---

## 確認できた点（指摘なし）

- `proposal.md` Impact の「リポジトリ内の Sample・Bridge・MAUI・利用者向けドキュメントに利用はない」は実測どおり。`viewTypeOf` / `isRegistered` / `preview` の参照は `android/kssettingsview/src/main/` と同 module の `src/test/` のみで、`android/kssettingsview-bridge/`・`samples/`・`maui/`・`skills/`・`README*.md` に参照なし。
- MAUI binding は本体 aar を `Bind="false"` で同梱するのみ（`maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj:31-32`）で、internal 化に伴う JVM 側の name mangling は C# binding 表面に影響しない。`Transforms/Metadata.xml` も bridge パッケージのみを参照。
- `concepts/` には降格 3 件の記述が無く（`kasane/concepts/core/architecture/cell-renderer-registry.md` は `register` / `strictMode` / `CELL_VIEW_TYPE_MIN` のみ記述）、公開契約の正本との衝突は生じない。`proposal.md:15` の handbook 除外リスト更新（蒸留時）で追随範囲は足りている。
- release variant の Kotlin compilation は CI で継続的に走る（`.github/workflows/verify-android.yml:53` の `./gradlew test` が `testReleaseUnitTest` を含み、その前提として `compileReleaseKotlin` が実行される）。`spec.md:13-17` の「両 variant」要求は CI で維持される。
- exploration が public 維持と判断した型（`SettingsRootScope` / `SectionScope` / `SettingsRootDsl` / `SettingsRootDiff` / `VisibilityAware` / `DSLIconModifiableCell` / `DSLStyleModifiableCell` / `DSLReidentifiableCell` / `CustomCellEmptyContent`）はすべて実在し、公開シグネチャに現れるという判断も妥当。

## アクションプラン（優先度順）

1. **Major 1**: 公開面の不変性 Requirement を追加し、変更前後の公開宣言差分タスクを tasks に入れる。あわせて `proposal.md:28` の緩和策の記述を訂正する。
2. **Major 2**: Strict 有効化の陽性対照タスクを追加し、`spec.md:7-11` の Scenario にタスクを対応付ける。
3. **Major 3**: `tasks.md:17` の probe 方式（設置場所・コミット可否・負 probe の常設方法）を確定し、`samples/android` のコンパイルを検証対象に明示する。`tasks.md:15,16` の担保範囲を書き分ける。
4. **Minor 4/5/6**: 対象外の検証、公開宣言 keep/demote 判定の記録、iOS 非対称の追跡先を追記する。
5. **Suggestion 7〜10**: 受け入れ基準の到達状態化、コミット分離、コメント方針、Scenario への `CellViewHolder` 追加。

Major 1〜3 はいずれも「spec / tasks の設計で埋める」種類の指摘で、実装だけでは解消できません。足場の改訂が必要になるため、判定としては **CHANGES_REQUESTED**（提案フェーズへの差し戻し）とします。

## 突き合わせ結果

ホスト側の自己レビューは当初クリアだったが、相方のみの指摘のうち Major 1〜3 は該当箇所と実害シナリオが具体的であり、見逃しとして採用した。反映後はホスト側で Requirement / Scenario / tasks の対応と上位層との整合を再確認した。

- **採用 — Major 1**: 「公開 API 差分の限定」Requirement と、変更前後の release AAR を同一の `javap -public` 手順で全走査する baseline / 差分タスクを追加。proposal の完了条件も Strict 単独から ABI 差分併用へ修正
- **採用 — Major 2**: Strict 有効化直後・公開宣言明示前に debug / release compilation が Explicit API 診断で失敗する陽性対照タスクを追加
- **採用 — Major 3**: 独立 build の `samples/android` へ一時 probe source を置く方式を確定。正の probe と 3 件の負の probe を分離し、負の失敗理由を internal visibility に限定。通常 Sample build も追加
- **採用 — Minor 4**: test source と Bridge に Strict が波及しない Scenario と、test / Bridge compilation の確認タスクを追加
- **採用 — Minor 5**: 全公開 ABI 差分と、明示した宣言の keep / demote 全走査をタスク化
- **採用 — Minor 6**: iOS / Android の可視性対称化を Non-Goals に明記し、iOS の既存 API を本 change では変更しない境界を固定
- **採用 — Suggestion 7**: Store コンストラクタのテストを「存在する状態」という観察可能な完了条件へ変更
- **降格 — Suggestion 8**: 実装ワーカーへ git 操作を要求できないため commit 分離は採らない。機械的明示と意味的降格は tasks のグループを分けてレビュー可能性を確保済み
- **採用 — Suggestion 9**: build 設定コメントは Strict の理由を自己完結して書き、ADR は補助参照とするタスクへ修正
- **採用 — Suggestion 10**: Registry の外部利用 Scenario と正の probe に `CellViewHolder` を追加

確定 0 件 / 採用 9 件 / 降格 1 件 / 未解決 0 件。
