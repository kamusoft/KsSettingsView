# セカンドオピニオン: add-consumer-verification (code-001)
**相方**: codex / **label**: so-code-add-consumer-verification / **日付**: 2026-09-02 / **対象**: HEAD に対する作業ツリー差分 (`.github/workflows/ci.yml`, `verify-consumer-{ios,android,maui}.yml`, `android/build.gradle.kts`, `android/kssettingsview/build.gradle.kts`, `kasane/config.yaml`, `scripts/readme-example-lint.py`, `verification/` 配下)
---
# レビュー結果: add-consumer-verification

**日付**: 2026-09-02  
**判定**: **NEEDS_DISCUSSION**

## サマリー

3 platform の参照先隔離、version 伝播、MAUI の取得元検査、README 一致検査は概ね仕様に沿っています。提示された正負ケースの結果も整合しています。

ただし `smoke + artifact` の組み合わせについて、2つの Requirement を同時に満たせず、artifact が黙って無視されます。実装だけで解決方針を決めるべきではないため `NEEDS_DISCUSSION` とします。

静的レビューのみ実施し、ビルド・テストは再実行していません。deviation 記録済みの2件と既知の iOS 警告は違反として扱っていません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/test-execution.md`（検証結果の報告）
- `kasane/handbook/cross/public-identifiers.md`（Gradle / csproj・配布座標）
- cross/ADR-0018、0019、0020、0023、0025、0026
- android/ADR-0016、0022
- maui/ADR-0025
- Kotlin / SwiftUI / C# / MAUI / GitHub Actions の実装・レビュー観点

## 指摘事項

### [🟠 Major] `smoke + artifact` では artifact がダウンロード後に無視される

**該当箇所**: `specs/verification-ci/spec.md:6`、`specs/consumer-verification/spec.md:55`、`.github/workflows/verify-consumer-ios.yml:92`、`.github/workflows/verify-consumer-android.yml:95`、`.github/workflows/verify-consumer-maui.yml:117`、`verification/ios/build-consumer.sh:42`、`verification/android/build-consumer.sh:40`、`verification/maui/build-consumer.sh:40`

**問題点**: workflow は artifact が指定されれば常にダウンロードして `--reference` を渡しますが、3つの build script は `smoke` のとき `KSV_REFERENCE` を使わず、公開レジストリを参照します。入力検査もこの組み合わせを許可しています。

したがって、呼び出し元が誤って `mode: smoke` と `artifact` を同時指定すると、artifact を検証したように見えながら、実際には公開済みパッケージだけを検証して成功し得ます。

一方、仕様には次の両方があり、組み合わせの扱いが確定していません。

- artifact 指定時は、その配布物を参照する
- smoke はローカル参照先を含めず、公開レジストリを参照する

**推奨修正**: artifact を dry-run 専用とするか、smoke でも artifact を使うかをオーナー判断してください。前者を推奨します。その場合は `smoke + artifact` を checkout 前に明示的に拒否し、負ケースを追加してください。足場は凍結済みなので、確定内容は Kasane の deviation 経路で扱う必要があります。

---

### [🟡 Minor] Android が Maven Local の実際の場所を使っていない

**該当箇所**: `verification/android/prepare-feed.sh:41`

**問題点**: コメントでは Maven 設定に従うとしていますが、実装は `${HOME}/.m2/repository` に固定されています。Gradle の `mavenLocal()` は `~/.m2/settings.xml` の `localRepository` などを考慮するため、カスタム設定環境では発行先と存在確認先がずれ、引数なし dry-run が失敗します。[Gradle公式の Maven Local 仕様](https://docs.gradle.org/current/userguide/supported_repository_types.html)

**推奨修正**: `--work` 配下の明示的な Maven Local を `-Dmaven.repo.local=...` で発行・消費の両方へ渡すか、Gradle が解決した実際の Maven Local パスを取得してください。カスタム `localRepository` の検査も追加すると確実です。

---

### [🟡 Minor] README lint が同じ見出し・言語の重複を黙って無視する

**該当箇所**: `scripts/readme-example-lint.py:96`

**問題点**: `resolve()` は最初に一致したブロックだけを消費し、同じ `(見出し, fence 言語)` の重複が `remaining` に残っても成功します。README に2つ目の Swift/Kotlin/C# 例が追加された場合、その例が消費者ビルド対象でなくても lint が緑になり得ます。

**推奨修正**: 対応表の各組について一致数がちょうど1件であることを要求し、重複時は候補数と対象を報告して失敗させてください。`--selftest` に重複ブロックの負ケースも追加してください。

---

### [🟡 Minor] version 注入のエラー案内が旧運用のまま

**該当箇所**: `android/kssettingsview/build.gradle.kts:173`

**問題点**: SNAPSHOT で Central 向けタスクを実行した際のメッセージが「バージョンカタログを変更する」と案内しますが、今回の変更と cross/ADR-0020 ではリリース版は `-Pversion=` で注入します。案内どおりカタログを変更すると、採用済み運用に反します。

**推奨修正**: `-Pversion=<release-version>` を指定する案内へ更新してください。

---

### [🔵 Suggestion] 「secrets を受け取らない」は呼び出し側でも保証する

**該当箇所**: `.github/workflows/verify-consumer-ios.yml:11`、`.github/workflows/verify-consumer-android.yml:11`、`.github/workflows/verify-consumer-maui.yml:11`

**問題点**: 現在の `ci.yml` は secrets を渡していないため安全です。ただし `workflow_call.secrets` を宣言していなくても、将来の release workflow は `secrets: inherit` で全 secrets を渡せます。[GitHub公式ドキュメント](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows)

**推奨修正**: phase-8 の呼び出し側で `secrets: inherit` を使わないことを必須確認事項にし、「宣言がないこと」だけを認証情報不在の根拠にしないでください。

## アクションプラン

1. `smoke + artifact` の契約をオーナー判断する。推奨は artifact を dry-run 専用として早期拒否。
2. Android の Maven Local パスを発行・消費で同一の明示パスにする。
3. README lint の重複ブロック検出を追加する。
4. Gradle の version 注入案内を修正する。
5. 修正後、未実施の tasks 5.6 / 5.7 / 6.1 をオーナー側で完了する。

**件数**: Critical 0 / Major 1 / Minor 3 / Suggestion 1  
**最終判定**: **NEEDS_DISCUSSION**

## 突き合わせ結果

ホスト側 review-001.md (CHANGES_REQUESTED: Major 1 / Minor 3 / Suggestion 4) との突き合わせ (2026-09-02):

| 相方の指摘 | ホスト側 | 採否 |
|---|---|---|
| Major: smoke + artifact で artifact が無視される | 指摘なし | **採用** (実物で確認: 3 platform の build-consumer.sh は dry-run のときだけ `--reference` を使い、workflow は mode に関係なく download して渡す。spec は組み合わせを定義していないためオーナー判断へ) |
| Minor: mavenLocal の位置が `~/.m2/repository` 固定 | 同じ指摘 (Minor) | **確定** (Minor) |
| Minor: README lint が同一見出し・言語の重複を黙って無視 | 指摘なし | **採用** (根拠強: resolve() の消費規則を確認。selftest に負ケースを追加して直す) |
| Minor: SNAPSHOT 時の Central 発行エラー案内がカタログ変更を案内 | 指摘なし | **採用** (本 change で触ったファイル内の数行) |
| Suggestion: `secrets: inherit` で呼ばれ得るので宣言不在を根拠にしない | 指摘なし | **採用** (phase-8 の申し送りとして agenda に追記。コード変更なし) |

ホスト側のみの指摘 (Major: `ANDROID_HOME` 必須 / Minor: 消費者 app のコメント不一致・署名任意化の CI 未担保 / Suggestion 4 件) は review-001.md の判定に従って処理する。未解決 (両者矛盾) はなし。
