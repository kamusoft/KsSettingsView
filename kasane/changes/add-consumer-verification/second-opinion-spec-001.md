# セカンドオピニオン: add-consumer-verification (spec-001)
**相方**: codex / **label**: so-spec-add-consumer-verification / **日付**: 2026-09-02 / **対象**: proposal.md / specs/ / tasks.md (提案一式)
---
# レビュー結果: add-consumer-verification

**日付**: 2026-09-02  
**判定**: **NEEDS_DISCUSSION**

## サマリー

配布経路を消費者側から検証する方向性は妥当ですが、現仕様には「誤った取得元でも CI が成功する」偽陽性経路が Android と NuGet に残っています。また、reusable workflow の入力・成果物受け渡し、`main` の branch protection、smoke 成功条件に proposal / spec / tasks 間の不整合があります。

単純な実装修正ではなく、workflow 契約と今回保証する smoke の範囲を決め直す必要があるため `NEEDS_DISCUSSION` と判定します。依頼どおり静的レビューのみで、ビルド・テスト・ファイル書き込みは行っていません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/public-identifiers.md`（application ID・配布座標・csproj / Gradle 構成）
- cross/ADR-0018、0019、0020、0025、0026
- Android ビルドツールチェーン、MAUI facade／NuGet pack 構成
- 先行する SwiftPM・MAUI 消費者検証証跡

## 指摘事項

### [🔴 Critical] Android dry-run が Maven Central へフォールバックできる

**該当箇所**: `specs/consumer-verification/spec.md:32`、`proposal.md:18`、`tasks.md:7`

**問題点**: `mavenLocal()` と `mavenCentral()` の両方に `content { includeGroup("jp.kamusoft") }` を設定しても、前者は排他的になりません。ローカルに指定版がない場合、同じ group を許可している Central が引き続き検索されます。公式 Gradle 文書も通常の repository-level content filter は排他的ではないと明記しています。[Gradle: Filtering Repository Content](https://docs.gradle.org/current/userguide/filtering_repository_content.html)

これにより、過去に公開済みの版を指定した負ケースが Central から解決され、核心 Scenario「ローカルに無ければ失敗する」を偽陽性で通過できます。

**推奨修正**: dry-run は `exclusiveContent` で `jp.kamusoft` を `mavenLocal()` に排他的に割り当てる仕様へ変更してください。負ケースでは公開済み版を指定しても Central に到達せず失敗することを検査してください。

---

### [🔴 Critical] NuGet の global packages cache が mapping を迂回し、取得元判定も `project.assets.json` だけではできない

**該当箇所**: `specs/consumer-verification/spec.md:34`、`specs/consumer-verification/spec.md:91`、`tasks.md:6`、`tasks.md:20`

**問題点**: packageSourceMapping は対象パッケージが global packages folder に存在すると適用されず、source lookup 自体が行われません。隔離されていない `~/.nuget/packages` に同じ版があれば、ローカルフィードが空でも restore が成功し得ます。[Microsoft: Package Source Mapping](https://learn.microsoft.com/en-us/nuget/consume-packages/package-source-mapping)

また、`project.assets.json` の `project.restore.sources` は構成されたソース一覧であり、各パッケージを実際に取得したソースではありません。先行証跡も、隔離した packages path の `.nupkg.metadata` を使って取得元を確認しています。

**推奨修正**:

- 検証ごとに空の `NUGET_PACKAGES` / `RestorePackagesPath` を指定する。
- 必要なら `--no-http-cache` も使用する。
- version は `project.assets.json`、実取得元は `.nupkg.metadata` または restore の取得ログから検査する。
- ローカルフィードを空にした負ケースを、既存ユーザーキャッシュの影響を受けない状態で実行する。

---

### [🟠 Major] 単一の workflow `version` と platform 別開発版が両立しない

**該当箇所**: `specs/consumer-verification/spec.md:18`、`specs/verification-ci/spec.md:5`、`tasks.md:26`

**問題点**: reusable workflow は単一の `version` を受け取りますが、dry-run の既定版は Android が `0.1.0-SNAPSHOT`、MAUI が `0.0.0-dev`、iOS は不要です。`ci.yml` が「開発用 version」を1つ渡す場合、少なくとも一方のローカル成果物と一致しません。

**推奨修正**: 次のどちらかを仕様として確定してください。

- 推奨: `dry-run` で version 未指定なら platform ごとの既定値を使い、`smoke` では単一 version を必須にする。
- または、dry-run でも3 platform の成果物を同じ開発版へ統一する。

---

### [🟠 Major] publish 成果物を reusable workflow へ渡す契約がない

**該当箇所**: `proposal.md:20`、`specs/consumer-verification/spec.md:57`、`specs/verification-ci/spec.md:5`

**問題点**: proposal は release の publish job が作った成果物でフィード準備を置き換えるとしていますが、workflow の入力は `mode` と `version` だけです。GitHub Actions の caller job と called workflow 内の各 job はファイルシステムを共有しないため、ローカルパスを渡すだけでは成果物に到達できません。workflow 間でファイルを渡すには artifact の upload/download 等が必要です。[GitHub Docs: Reusing workflows](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows?learn=getting_started&learnProduct=actions)

加えて、1本の `verify-consumers.yml` に3 platform job を置く構成は、platform 別 reusable workflow を定めた `cross/ADR-0025` とも緊張します。

**推奨修正**:

- artifact 名・run ID・取得方法を workflow 契約へ追加する。
- 各 job が対応 artifact を download する構成を design に定義する。
- consumer workflow を platform 別3本に分けるか、ADR-0025 の適用外とする理由を明示して合意する。

---

### [🟠 Major] iOS dry-run の取得元証跡を `Package.resolved` だけでは残せない

**該当箇所**: `specs/consumer-verification/spec.md:91`、`tasks.md:21`

**問題点**: path-based dependency は version constraint を持たず、全依存が path-based の場合は pins が空になり `Package.resolved` 自体が除去され得ます。したがって、dry-run で identity・ローカル取得元・version を同ファイルから示す要件は成立しません。[SwiftPM: ResolvedPackagesStore](https://github.com/swiftlang/swift-package-manager/blob/main/Sources/PackageGraph/ResolvedPackagesStore.swift)

**推奨修正**:

- dry-run: 生成済み `Package.swift`、`swift package show-dependencies`、build log から path と identity を出力する。
- smoke: `Package.resolved` から URL・revision・version を出力する。
- iOS dry-run では version が存在しないことも明示する。

---

### [🟠 Major] `main` のマージ保護が spec と tasks で食い違う

**該当箇所**: `specs/verification-ci/spec.md:51`、`proposal.md:24`、`tasks.md:41`

**問題点**: spec は `develop` と `main` の双方に7 checks・PR必須・直 push拒否を要求しますが、proposal と tasks は `develop` の checks 追加しか扱いません。既存ロードマップでは未作成の `main` の保護は phase-8 所有とされています。

さらに、新しい3 check の正確な context 名も確定していません。branch protection は文字列完全一致なので、「3 job」だけでは再取得検査の期待値を定義できません。

**推奨修正**: 今回の Requirement を `develop` に限定し、`main` は phase-8 へ明示的に延期してください。あわせて `consumer / ios` 等の固定 status check 名を tasks/spec に記載してください。

---

### [🟠 Major] smoke の成功 Scenario を今回検証できない

**該当箇所**: `specs/consumer-verification/spec.md:49`、`tasks.md:35`

**問題点**: spec は3レジストリからの解決・ビルド成功を要求しますが、task 5.4 は未発行を理由に「解決失敗すること」しか確認しません。これは URL や座標の生成検査にはなりますが、smoke の正ケースを検証したことにはなりません。

**推奨修正**: 次のどちらかを合意してください。

- smoke の成功検証は phase-8 の完了条件とし、今回保証するのは構成生成と期待された未公開エラーまでと明記する。
- または、制御された prerelease を3チャネルへ発行して正ケースを検証する。

---

### [🟠 Major] MAUI iOS app の Simulator RID が未規定

**該当箇所**: `specs/consumer-verification/spec.md:65`、`tasks.md:19`

**問題点**: 「Release ビルド」「実機署名は範囲外」だけでは、MAUI iOS app が device RID を選び署名要求へ進む可能性があります。先行証跡では `-p:RuntimeIdentifier=iossimulator-arm64` を明示して成功させています。

**推奨修正**: MAUI iOS 消費者は Simulator RID を明示する契約にし、runner architecture に応じた RID の選択方法と、署名を要求しないことを Scenario に追加してください。

---

### [🟠 Major] M 級判定が Kasane の級判定と一致しない

**該当箇所**: `proposal.md:26`、`proposal.md:45`

**問題点**: proposal 自身が新規能力 `consumer-verification` と既存能力 `verification-ci` の2能力を挙げ、3 platform、外部レジストリ、reusable workflow、GitHub branch protection を横断しています。これは「1能力内」の M 級ではなく、複数能力・外部連携を含む L 級基準に該当します。実際、成果物輸送や cache 隔離など design が必要な論点が proposal/tasks に分散しています。

**推奨修正**: L 級へ再分類し、`design.md` に workflow 境界、artifact 輸送、cache 隔離、取得元証明、失敗時の扱いを集約してください。

---

### [🟡 Minor] README の対象コードブロック数が誤っている

**該当箇所**: `proposal.md:22`、`tasks.md:22`

**問題点**: 「3コードブロック」とありますが、実際の最小例は iOS、Android、MAUI XAML、MAUI C# の4ブロックです（`README.md:98`、`:124`、`:147`、`:163`）。実装次第では `MauiProgram` が lint 対象から漏れます。

**推奨修正**: 「4コードブロック・4対応ファイル」と明記し、各見出し・language fence・出力先の対応表を定義してください。

---

### [🟡 Minor] workflow input の既定値・型・不正値の扱いが未定義

**該当箇所**: `specs/verification-ci/spec.md:5`

**問題点**: `workflow_call` input の `type`、`required`、default、および `mode=foo`・空 version・不正 semver の失敗条件が決まっていません。実装によっては未知の mode を暗黙に smoke または dry-run として扱えます。

**推奨修正**: `mode` / `version` の型と既定値を仕様化し、許可値以外はフィード準備前に失敗する Scenario を追加してください。

---

### [🟡 Minor] 「tag 数 0」は継続利用できる副作用検査ではない

**該当箇所**: `tasks.md:32`

**問題点**: 配信リポジトリの tag 数 0 は初回リリース前しか成立しません。リリース後は dry-run が無関係でも失敗します。また Portal / NuGet.org の「変化なし」の観測方法が定義されていません。

**推奨修正**: 実行前後の同一性比較、または workflow に書き込み権限・publish credentials・push/upload コマンドが存在しないことの静的検査へ置き換えてください。

## アクションプラン

1. Android を `exclusiveContent`、NuGet を隔離 packages path + `.nupkg.metadata` 検査へ変更する。
2. workflow の version、artifact 輸送、platform 分割方針を design として確定する。
3. smoke 成功検証と `main` branch protection の所有フェーズを確定する。
4. iOS/MAUI の証跡・Simulator RID を Scenario と tasks に具体化する。
5. L 級へ再分類し、修正版を再レビューする。
6. README ブロック数、input validation、副作用検査を修正する。

**件数**: Critical 2 / Major 7 / Minor 3 / Suggestion 0

## 総合判定

**NEEDS_DISCUSSION**


## 突き合わせ結果 (2026-09-02)

ホスト側の自己レビュー (2 周) は整合性チェックリストを通過していたが、相方の技術指摘はいずれも該当箇所と実害シナリオを伴い、ホスト側の見逃しとして採用した。

| 指摘 | 採否 | 反映先 |
|---|---|---|
| Android content filter は排他でない | 採用 | spec「dry-run の参照先」(排他)、tasks 1.3 / 2.2 (`exclusiveContent`) |
| NuGet global cache が mapping を迂回、取得元は assets.json で不明 | 採用 | spec 同上 (空の展開先・パッケージ単位の取得元)、tasks 1.2 / 3.1 / 3.2 |
| 単一 version と platform 別開発版 | 採用 | spec「モードと version の指定」(dry-run 未指定は platform 既定、smoke は必須) |
| publish 成果物の受け渡し契約なし | 採用 | verification-ci spec「再利用契約」に `artifact` 入力、tasks 4.1 / 5.7 |
| workflow 1 本は ADR-0025 と緊張 | 採用 | platform 別 3 本 (`verify-consumer-*.yml`)。agenda 決定 5 に追記 |
| iOS dry-run で `Package.resolved` が残らない | 採用 | spec「解決結果の証跡」、tasks 1.1 / 3.3 |
| `main` 保護の食い違い・check 名未確定 | 採用 | spec「マージ保護」に main 未作成時の扱い、check 名を固定、proposal Non-Goals、tasks 6.1 |
| smoke 正ケースを今回検証できない | 採用 | spec「smoke の参照先」を 2 Scenario に分け、正ケースは phase-8 で実証と明記 |
| MAUI iOS の Simulator RID 未規定 | 採用 | spec「成立条件」、tasks 2.3 |
| M → L 再分類 | 未解決 | オーナー判断へ |
| README は 4 コードブロック | 採用 | proposal / spec / tasks 3.4 |
| input の型・不正値 | 採用 | spec「不正な入力は早期に失敗する」、tasks 4.1 / 5.2 |
| 「tag 数 0」は継続利用不可 | 採用 | spec「配信先へ副作用を残さない」(前後比較 + 権限なし)、tasks 5.1 / 5.6 |

採用 12 / 降格 0 / 未解決 1 (級)。
