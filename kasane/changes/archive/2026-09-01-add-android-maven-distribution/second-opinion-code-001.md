# セカンドオピニオン: add-android-maven-distribution (code-001)

**相方**: codex / **label**: so-code-add-android-maven-distribution / **日付**: 2026-09-01 / **対象**: HEAD (93a79d0) に対する作業ツリーの全変更 (untracked 含む)

---

# レビュー結果: add-android-maven-distribution

**日付**: 2026-09-01
**判定**: CHANGES_REQUESTED

## サマリー

module 統合、rename、MAUI binding、Sample 置換は概ね一貫しており、提示された2700件のテスト成功も確認材料として妥当です。ただし、公開依存の欠落とSNAPSHOT発行制御に Major が2件あります。

件数: Critical 0 / Major 2 / Minor 1 / Suggestion 0

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/test-execution.md`（テスト結果の報告）
- `kasane/handbook/cross/public-identifiers.md`（Gradle・csproj・配布座標）
- `kasane/handbook/cross/local-development-setup.md`（本体・Sampleビルド）
- `kasane/handbook/maui/integration-host-verification.md`（bindingのend-to-end確認）

## 指摘事項

### [🟠 Major] 公開APIの `PaddingValues` がcompile classpathへ公開されない

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:113`、`android/kssettingsview/build.gradle.kts:189`、`kasane/changes/add-android-maven-distribution/evidence/publish-to-maven-local.txt:112`

**問題点**: 公開型 `Theme` のコンストラクタが `androidx.compose.foundation.layout.PaddingValues` を露出していますが、`androidx.compose.foundation:foundation` は `implementation` です。実際の発行metadataでもfoundationはruntime依存になっており、ライブラリ1行だけを依存した利用者のcompile classpathには届きません。Sampleは自身がfoundationを直接依存しているため、この欠落を検出できません。tasks.md 1.2・3.3の完了条件も満たしていません。

**推奨修正**: `PaddingValues`を提供するartifactを`api`へ変更してください。例えばfoundation全体を`api`にするか、`foundation-layout`だけを`api`としてfoundation本体は`implementation`に保ちます。recyclerviewと同様にdeviationへ列挙漏れを記録し、直接foundationを持たない隔離consumerでコンパイルを確認してください。

### [🟠 Major] SNAPSHOT版がCentralのsnapshot repositoryへ発行可能

**該当箇所**: `android/gradle/libs.versions.toml:32`、`android/kssettingsview/build.gradle.kts:101`、`kasane/changes/add-android-maven-distribution/design.md:37`

**問題点**: 開発時versionは`0.1.0-SNAPSHOT`ですが、`publishToMavenCentral()`が無条件に設定されています。採用したプラグイン0.37.0は`-SNAPSHOT`を検出するとCentralのsnapshot repositoryへ送る実装なので、認証情報がある環境でCentral発行タスクを実行するとSNAPSHOTをアップロードできます。「SNAPSHOTは発行しない」というdesignの決定を実装できていません。

**推奨修正**: 解決済みproject versionがSNAPSHOTの場合はCentral発行設定を作らないか、Central向け全発行タスクを明示的に失敗させてください。`publishToMavenLocal`は引き続き利用可能にし、SNAPSHOT時にCentral向けタスクが実行不能であることを静的なタスク確認で担保します。

### [🟡 Minor] 実機証跡に通知アイコンが写り込んでいる

**該当箇所**: `kasane/changes/add-android-maven-distribution/evidence/maui-binding-and-integrationhost.txt:34`

**問題点**: 参照先の`evidence/android-integrationhost-initial.png`のステータスバーに複数のYouTube通知アイコンが写っています。実機画像では通知を表示しないという媒体規律に反します。通知本文や氏名は見えていませんが、public履歴へ残す証跡としては避けるべき情報です。

**推奨修正**: 通知を消した状態で再撮影するか、ステータスバーをクロップ／マスクした画像へ差し替えてください。

## アクションプラン

1. `PaddingValues`の依存を`api`へ公開し、隔離consumerで検証する。
2. SNAPSHOT時のCentral発行を明示的に禁止する。
3. 実機証跡から通知アイコンを除去する。
4. 修正後にpublication metadataと対象検証を再確認し、再レビューへ回す。

---

## 突き合わせ結果 (ホスト review-001 との照合、2026-09-01)

| 指摘 | 採否 | 根拠 |
|---|---|---|
| Major: `PaddingValues` (compose foundation) の `api` 漏れ | **確定** (双方一致) | ホスト review-001 も同一指摘。発行物の javap 全走査・POM / `.module` 検査で実証済み。修正は `api("androidx.compose.foundation:foundation-layout")` 1 行 (ホスト側の宣言元直接指定案を採る) |
| Major: SNAPSHOT が Central snapshot repository へ発行可能 | **採用** (相方のみ・根拠強) | 該当箇所特定・実害シナリオ (認証情報のある環境での誤アップロード) が具体的で、design Decision 3「SNAPSHOT は発行しない」の未実装指摘として妥当。plugin 0.37.0 の SNAPSHOT 挙動という前提は修正時に静的検証してから適用する |
| Minor: 実機証跡の通知アイコン写り込み | **採用** (相方のみ・根拠強) | evidence 媒体規律 (実機画像に通知を表示しない) への違反として検証可能。マスク or 再撮影で解消 |

ホスト側のみの指摘 (Minor: docs-refresh スキルの参照パス失効、Suggestion 4 件) は review-001.md の判定処理に従う。
