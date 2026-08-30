# レビュー結果: upgrade-android-build-toolchain (001 回目)

**日付**: 2026-08-21
**判定**: NEEDS_DISCUSSION

**内訳**: 🔴 Critical 0 / 🟠 Major 0 / 🟡 Minor 2 / 🔵 Suggestion 4

## サマリー

ツールチェーン更新とバージョンカタログ導入の実装は、spec の 3 Requirement / 7 Scenario に対して過不足なく対応しており、コード品質の観点で修正を要する指摘はない。ビルド・テストはレビュアー側で再実行し、JDK 25 で debug / release 各 1261 tests / 0 failures、JDK 21 でも成功、samples の `:app:assembleDebug` と MAUI binding の `dotnet build` も成功を確認した。wrapper 再生成物は公式配布物との一致まで検証済みで、不審な差分はない。

一方で、**実装側では解決できないオーナー判断が 2 件残っている** — (1) デルタスペックの Scenario「MAUI binding からの native ビルド」の THEN が実態と食い違う記述 (deviation.md にオーナー確認待ちとして記録済み)、(2) Requirement 本文が要求する Android Studio の sync 成功が未検証 (tasks.md 4.4)。この 2 件のためコード変更を求める CHANGES_REQUESTED ではなく NEEDS_DISCUSSION とする。

### レビュー範囲についての注記

レビュー開始時点の作業ツリーには含まれていなかった変更が、レビュー実施中 (17:46) に追加された — `maui/…/KsSettingsView.Binding.Android.csproj`、`android/gradle.properties`、両 `gradle-wrapper.properties` への `distributionSha256Sum` 追加、および `deviation.md` の新設。**本レビューはこれらを含む 17:46 時点の作業ツリーを対象としている**。deviation.md に記録済みの 4 件は合意済み差分として扱い、spec 違反としては指摘していない (ただし内容の妥当性は個別に検証した。後述)。
なお、独立性を保つため `second-opinion-code-001.md` は読んでいない。

## 確認した観点 (指摘に至らなかったもの)

- **バージョン直書きの不在** (Scenario 準拠): `plugins` の `version "…"` / `compose-bom:` リテラル / `version = "…"` の直書きはいずれも 0 件。旧値 `8.7.3` / `2.0.21` / `8.10.2` の残存も 0 件
- **カタログ共有と版の一致**: `samples/android/settings.gradle.kts:26-30` が本体の toml を `versionCatalogs` で取り込み、`:app:buildEnvironment` / `:app:dependencies` で AGP 8.13.2 / Kotlin 2.4.10 / Compose BOM 2024.10.01 と本体一致を実測。samples の GAV 依存も `libs.versions.ks.settingsview.get()` を経由しており、本体の `version` と乖離し得る形になっていない
- **成果物の不変性**: `compileSdk = 35` / `minSdk = 29` / `JavaVersion.VERSION_17` / `jvmToolchain(17)` は 4 module とも差分に含まれず不変。ライブラリコードの変更は 0 件
- **テストコードの変更の妥当性** (`CustomCellTest.kt:165`): 変更前の形へ戻して `:ks-settingsview-ui:compileDebugUnitTestKotlin` を実行し、`e: … CustomCellTest.kt:168:57 Check for instance is always 'false'.` でコンパイルが失敗することを実測した。Kotlin 2.4.10 が強制する機械的修正であることが確定している。かつ同ファイル内の既存テスト (`:175`) が以前から使っている `val cell: Any` の書き方に揃っており、様式の一貫性も保たれている。検出力についても、旧形は `assertFalse(cell is DSLIconModifiableCell)` がコンパイル時に結果確定していた (＝トートロジー) のに対し、`Any` への広げ方は 3 つの `is` を実 runtime 検査に変えるため、回帰検出力はむしろ上がっている。実測後はバックアップとの shasum 一致で原状復帰を確認済み
- **wrapper 再生成物の真正性**: `gradle-wrapper.jar` (sha256 `497c8c2a…`) は公式 gradle-9.5.0 配布物内の `lib/plugins/gradle-wrapper-main-9.5.0.jar` に同梱された `gradle-wrapper.jar` とバイト単位で一致。`gradlew` / `gradlew.bat` は同配布物の `unixStartScript.txt` / `windowsStartScript.txt` と、テンプレート変数の展開分を除いて完全一致。`retries=0` / `retryBackOffMs=500` は `WrapperConfiguration` のコンストラクタ既定値そのもので、`wrapper` タスクの生成物であることを確認。実行権限 (100755) と `.bat` の CRLF も保存されている。android / samples の 3 ファイルは両ルートで同一
- **`distributionSha256Sum` の値** (deviation 4 件目): 公式配布元 `downloads.gradle.org/distributions/gradle-9.5.0-bin.zip.sha256` を取得し、`553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746` の一致を確認。ただし手元では配布物が既に展開済み (`.ok` マーカーあり) のため、**チェックサム検証が実際に走るクリーン環境のダウンロード経路は未実行**である点は留意されたい
- **`org.gradle.tooling.parallel` の実在性**: Gradle 9.5.0 の `StartParameterBuildOptions$ParallelToolingModelBuildingOption` が同名のプロパティを持つことを確認 (綴り誤りによる無言の無効化ではない)
- **`csproj` の入力追加** (deviation 1 件目): 追加された 3 ファイルは、catalog がバージョンの SSoT になった後に aar 内容を決める入力として妥当。既存の `$(KsAndroidRoot)*.kts` が `settings.gradle.kts` を、module 別 3 行が各 `build.gradle.kts` をカバーしており、束縛対象 (core / ui / bridge) に関する入力の取りこぼしは残っていない
- **コメント規約**: `python3 scripts/comment-policy-lint.py` は 669 ファイル / 禁止 0 件。本 diff で触れたコメント (`android/build.gradle.kts` の冒頭、`ui/build.gradle.kts:12-13`、`samples/android/app/build.gradle.kts:63`、`samples/android/settings.gradle.kts:24-25`、`libs.versions.toml` 全体) を個別に読み、change-id の裸参照・Phase/Round 等の通番・履歴記述・デルタスペック構文キーワードのいずれも含まないことを確認した。`android/build.gradle.kts` からは旧コメントの「後続の変更提案で〜」という時間軸記述が消えており、規約適合が改善している
- **MAUI binding の警告**: `dotnet build` は 0 エラー。`BG8A00` (Metadata.xml の `remove-node` が一致せず) は `Transforms/Metadata.xml:28-32` のコメントが「fixup 段で除去済みのため generator 段で出る想定の警告」と明記しており、生成 C# に `WhenMappings` が現れないことも確認したため、本更新による退行ではない

## 指摘事項

### [🟡 Minor] Requirement 本文が要求する Android Studio の sync が未検証

**該当箇所**: `kasane/changes/upgrade-android-build-toolchain/tasks.md:26` (4.4 未チェック)、spec の Requirement「JDK 25 の Gradle JVM でビルドできる」本文

**問題点**: Requirement 本文は「sync・ビルドが成功しなければならない」と定めており、Studio (JBR = JDK 25) の sync 失敗が本 change の動機そのものである。CLI 側は JDK 25 / 21 の双方で実測成功を確認できたが、GUI sync は誰も実行していない。エージェントは GUI を操作できないため、レビュアー側で代替検証もできない。

**推奨修正**: 実装の修正は不要。**オーナーが Android Studio の Gradle JVM を JDK 25 にした状態で `android/` と `samples/android/` を sync し、成功を確認して 4.4 をチェックする**。これが済むまでアーカイブ (蒸留) に進まないこと。sync が失敗した場合のみ、その内容を持ち帰って実装側の検討に戻す。

### [🟡 Minor] README / docs のツールチェーン記述が旧版のまま残る

**該当箇所**: `README.md:19` (`Kotlin / AGP 8.7.3 / Gradle 8.10.2`)、`android/README.md:43` (`AGP 8.7.3 / Gradle wrapper 8.10.2`)、`docs/overview.md:21` (`AGP 8.7+`)、`docs/overview.md:111-112` (`Gradle 8.10.2` / `Android Gradle Plugin 8.7.3`)

**問題点**: 更新後の値と食い違ったまま残っている。利用者・コントリビュータが最初に見る場所であり、放置すると「wrapper で固定」と書かれた値が実体と異なる状態が続く。

**推奨修正**: **本 change では直さないこと**。プロジェクト規約上、`docs/` と README 群の書き換えは `docs-refresh` スキル経由でのみ行い、自動発動は禁止されている。蒸留 (ksn-distill) の前後でオーナーに `docs-refresh` の実施を依頼する、という形でハンドオフするのが正しい経路。この指摘は「忘れずに依頼する」ためのものであり、実装者への修正要求ではない。

### [🔵 Suggestion] Kotlin 2.4 が `@SettingsRootDsl` の無効な付与を新たに報告している

**該当箇所**: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/` 配下 29 箇所 (`DSLHandles.kt` 12、`BasicCellDsl.kt` 8、`InputCellDsl.kt` 7、`CustomCellDsl.kt` 2)

**問題点**: ビルドログに `Applying DSL marker annotation 'SettingsRootDsl' to target 'top level function' has no effect. DSL marker annotations must only be applied to types. (KT-81567)` が出る。scope 制御自体は `SettingsRootScope` / `DSLScope` の**型**側の付与で効いているため実害はないが、トップレベル関数側の付与は無効であることが明示された。

**推奨修正**: 本 change の Non-Goals (ライブラリコードの変更をしない) の範囲外なので、ここでは触らない。無効な付与の除去は別 change として起票するのが妥当。

### [🔵 Suggestion] AGP 8.13.2 由来の Gradle 非推奨が Gradle 10 でエラーになる

**該当箇所**: `--warning-mode=all` で `Declaring dependencies using multi-string notation has been deprecated. This will fail with an error in Gradle 10.` (`com.android.tools.lint:lint-gradle:31.13.2` / `com.android.tools.build:aapt2:8.13.2-…:osx`)

**問題点**: プロジェクトのビルドスクリプト由来ではなく AGP 内部の宣言なので、こちら側で消せない。次に Gradle 10 系へ上げるときは AGP 側の対応版が前提になる。

**推奨修正**: 修正不要。次のツールチェーン更新の制約として、蒸留時に concepts / ロードマップ側へ「Gradle 10 へ進むには AGP の対応版が要る」という事実だけ残しておくと、次回の互換調査が短くなる。

### [🔵 Suggestion] Kotlin 2.4.10 化は将来の配布物の利用側要件を上げる

**該当箇所**: `android/gradle/libs.versions.toml:15` (`kotlin = "2.4.10"`)

**問題点**: `:app:dependencies` で `org.jetbrains.kotlin:kotlin-stdlib` が 2.4.10 に解決される。spec の「公開 API・実行時挙動・Java 17 ターゲット・compileSdk」は不変であり本 change の要求は満たしているが、Maven 公開後は利用側に stdlib 版と Kotlin metadata 水準の下限が伝播する。

**推奨修正**: 本 change での対応は不要。配信ロードマップ (package-distribution) 側で「配布物が要求する利用側 Kotlin の下限」を明示する項目として拾っておくとよい。

### [🔵 Suggestion] 新設した `.toml` はコメント規約の機械検査の対象外

**該当箇所**: `scripts/comment_policy_rules.py:19` (`TARGET_EXT` に `.toml` / `.properties` を含まない)、`android/gradle/libs.versions.toml`

**問題点**: コメント規約の適用範囲は「コメント構文を持つ全ファイル (ビルドスクリプト等も含む)」だが、本 change で初めて追加された `.toml` は lint と hook のどちらの検査も通らない。今回の toml のコメントは目視で規約適合を確認済み (参照は公式 URL とリポジトリ内ファイル名のみ) だが、今後の編集はラチェットの外側になる。

**推奨修正**: 本 change の範囲外。`TARGET_EXT` への `.toml` / `.properties` 追加は別途検討 (追加時は既存の `gradle.properties` 等の既存債務が一斉に出る可能性があるため、影響量を見てから)。

## アクションプラン

1. **(オーナー判断)** spec の Scenario「MAUI binding からの native ビルド」の THEN「4 module の release aar」は実態 (core / ui / bridge の 3 module) と食い違う。deviation.md に「オーナー確認待ち」として記録済み。binding の Exec に compose を足すのは maui/ADR-0006 の設計に反するため、**spec 側の記述誤りとして deviation を確定させる**のが妥当と考える
2. **(オーナー作業)** Android Studio (Gradle JVM = JDK 25) で `android/` / `samples/android/` の sync を確認し、tasks.md 4.4 をチェックする。これで verify は VALID になる
3. **(オーナー作業、蒸留前後)** `docs-refresh` を依頼して README / docs のツールチェーン記述を追従させる
4. (任意・別 change) `@SettingsRootDsl` の無効な付与 29 箇所の整理
5. (任意・蒸留時) Gradle 10 移行には AGP 対応版が必要という制約、および配布物の利用側 Kotlin 下限を、それぞれ長命層 / 配信ロードマップへ残す
