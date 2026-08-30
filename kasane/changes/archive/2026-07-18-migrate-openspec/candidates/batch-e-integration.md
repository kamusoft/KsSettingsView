# バッチE 統合案 — Samples

統合日: 2026-07-18
レビュー結果: 2026-07-18 承認

対象:

- `samples-ios`
- `samples-android`
- 補助資料: `samples/ios/README.md`、`samples/android/README.md`、`docs/platform-guide-ios.md`、`docs/platform-guide-android.md`、`docs/overview.md`

抽出結果:

- 独立概念候補: 0件
- ADR候補: 2件
- drift所見: iOS 5件、Android 7件

## concepts 統合案

新規 concept は作らず、既存 2 concepts へ低腐食な責務だけを合流する。

### `architecture/repository-boundaries.md` の更新

- Sample はライブラリ本体の配布物ではなく、公開 API を利用者側の境界から実行する独立アプリである。
- 各エコシステムのローカル参照機構を通じ、開発中の本体をソース参照する。
- Sample は実行可能な利用例、統合状態、視覚・操作結果の目視確認を担う。
- 挙動契約の SSoT と自動回帰検証は本体コードとテストが担い、Sample の画面構成や表示文字列を契約にしない。

Sample の画面一覧、操作手順、ファイル構成、具体的な API 呼び出しはコードから再導出できるため移さない。

### `styling/style-resolution.md` の更新

- platform default はホストアプリが提供する Native theme の属性を含む。
- Android UI は Material3 属性を消費するため、ホストアプリは Material3 派生テーマを提供する。
- この要件は Sample 固有ではなく、Android UI を組み込むすべてのホストに適用する。

具体的な widget 名や属性名は実装から再導出できるため concept へ列挙しない。

## ADR 統合判断

### Android Sample の composite build

Sample と本体を独立ビルドルートとして保ち、プラットフォーム標準のローカル参照で接続する方針は ADR-0001 と `architecture/repository-boundaries.md` に確定済みである。Gradle composite build はその Android での具体化であり、独立 ADR を起票しない。

### Android ホストの Material3 派生テーマ

ホストテーマ要件は能力境界を越える重要な公開統合条件だが、現在採用する Native widget 群から生じる platform 要件であり、過去の ADR トリアージでも局所的な現在要件として扱っている。長命な統合条件は `styling/style-resolution.md` に残し、widget 選択と属性名はコード・テストへ委ねる。

したがって新規 ADR は起票しない。

## drift 所見の統合

| ID | 所見 | 主な根拠 | 推奨する扱い |
|---|---|---|---|
| E-1 | 両 Sample spec の Purpose がプレースホルダーで、実行可能な利用例・手動統合確認という現行責務を説明しない | spec ↔ code/README | 既存のリポジトリ境界 concept に責務だけを合流し、旧 spec は歴史資料として凍結する |
| E-2 | 両 spec は起動直後に設定一覧を表示するように読めるが、現行アプリはトップメニューから各デモへ遷移する | spec内部 / spec ↔ code | 現行コードを正とする。起動画面は可変な Sample 詳細なので concepts へ移さない |
| E-3 | iOS spec は Store デモで Root / Section の両 builder を使うと記すが、現行コードは同じ構成方式ではない | spec ↔ code | 現行コードを正とし、具体的な構築手順を concepts へ移さない |
| E-4 | 両 spec に Theme の旧フィールド名が残る | spec ↔ code/README/docs | 現行 API 名を正とする。旧名は移さず、周辺 docs の SSoT 案内更新時に現行例を確認する |
| E-5 | 両 Sample README と platform guide のデモ一覧・構成一覧が現行コードに追随していない | docs ↔ code | 網羅一覧を長命契約にしない。周辺 docs は固定一覧を簡略化し、Sample コードを実行例として案内する |
| E-6 | Android spec は Radio の旧内部 widget と ring / dot 表現を要求するが、現行は独自の単純チェック表示を使う | spec ↔ code/test | 現行コードとテストを正とする。内部 widget 名を長命概念へ移さない |
| E-7 | Android spec は旧 Gradle DSL のプロパティ存在で JVM 17 を確認するが、現行は Java compatibility と Kotlin toolchain で同じ結果を指定する | spec ↔ build code | JVM 17 という結果は現行ビルド設定を正とし、検証構文は concepts へ移さない |
| E-8 | Android guide は利用者定義 Cell の詳細実装として Sample を案内するが、Sample に独自 Cell 実装はない | docs ↔ code | 誤誘導として周辺 docs 更新時に修正し、Registry の長命原則は既存 concept を参照する |

## 推奨レビュー結果

1. 新規 concept は作らず、`repository-boundaries.md` と `style-resolution.md` の更新を承認する。
2. 新規 ADR は起票せず、ADR-0001 と既存 concepts を維持する。
3. E-1〜E-4、E-6、E-7は現行コード・テストを正とし、旧 spec は凍結する。
4. E-5、E-8は移行完了時の周辺 docs 更新へ含める。
