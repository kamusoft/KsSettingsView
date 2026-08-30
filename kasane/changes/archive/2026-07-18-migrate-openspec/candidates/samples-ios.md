# Candidate: samples-ios

## 概念候補

独立した概念候補なし。

iOS Sample の低腐食な責務境界は、「ライブラリ本体の配布物ではなく利用側として分離し、プラットフォーム標準のローカル参照を通じて本体を検証する」という `monorepo-foundation` の「クロスプラットフォーム・リポジトリ境界」候補へ合流できる。Sample が公開 API の利用例と目視確認の場を提供し、挙動契約そのものは本体コードとテストが担うという位置づけも、この境界の補足として扱うのが適切である。

デモ画面の種類、表示文字列、ファイル名、操作手順、具体的な API 呼び出しは Sample コードと README から再導出でき、変更頻度も高いため、独立した長命概念にはしない。

出典: `samples/ios/KsSettingsViewSample/`、`samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj`、`ios/Tests/`、`samples/ios/README.md`、`docs/platform-guide-ios.md`、`docs/overview.md`、`kasane/changes/migrate-openspec/candidates/monorepo-foundation.md`

## ADR 候補

- 新規候補なし。Local Swift Package による本体参照や Sample ターゲットの構成は現行ビルド設定から再導出でき、Sample 固有の選択としては可逆かつ局所的である。`UIColor` を直接用いる制約も、Sample 独自の決定ではなく、Native 型を直接保持する iOS Theme API の既存境界に従った結果である。

## drift 所見

- spec の Purpose は archive 時のプレースホルダーのままであり、Sample が「公開 API の利用例」と「実機・シミュレータでの目視確認」を担うという現行の位置づけを説明していない (`openspec/specs/samples-ios/spec.md` Purpose / `samples/ios/README.md`「概要」)。
- spec は「起動直後の画面」が Store 経路の設定画面であるかのように規定する一方、現行アプリはトップメニューを起動直後に表示し、Store デモへは利用者が遷移する。後続 Scenario は現行挙動と一致しており、Requirement 本文と Scenario の間にも不整合がある (`openspec/specs/samples-ios/spec.md`「基本 Cell を含むデモ画面」 / `samples/ios/KsSettingsViewSample/ContentView.swift`)。
- spec は Store デモの初期 root を `SettingsRoot { Section { ... } }` 形式の Root/Section 両 builder で構築すると記述するが、現行コードは Root builder 内で明示的な Section 初期化と Cell 配列を用いており、Section builder は使用していない (`openspec/specs/samples-ios/spec.md`「SettingsRootStore + SwiftUI ラッパの使用」 / `samples/ios/KsSettingsViewSample/ContentView.swift`)。
- spec の MAUI 互換 Theme 欄は `viewBackgroundColor` と `titleColor` を現行フィールド名のように列挙するが、現行 API と Sample はそれぞれ `backgroundColor` と `cellTitleColor` を使用する。補助 README は `viewBackgroundColor` を旧名と明記しており、spec の用語だけが追随していない (`openspec/specs/samples-ios/spec.md`「MAUI 互換 Theme の明示渡し」 / `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` / `samples/ios/README.md`「基本 Cell 7 種デモ画面」)。
- 補助資料のデモ一覧とディレクトリ構成が現行コードに追随していない。README はトップメニューを3画面として説明し、ソース一覧から入力 Cell、共通フィールド、可視性、Minimal Diffable の各デモを省略している。iOS platform guide の一覧も Store と入力 Cell のデモを省略しているが、現行トップメニューには7つの遷移先がある (`samples/ios/README.md`「概要」「ディレクトリ構成」 / `docs/platform-guide-ios.md`「Sample アプリ」 / `samples/ios/KsSettingsViewSample/ContentView.swift`)。

## 用語

- Sample アプリ: ライブラリ本体から分離された利用側アプリ。公開 API の利用例、統合状態、視覚・操作結果を実行環境で確認するために用い、挙動契約の正本にはしない。
- Store 方式: 利用側が保持する設定ツリーへ命令的な更新を適用し、その差分を表示へ反映する利用経路。Sample は更新操作の目視確認に用いるが、経路自体の契約は iOS host capability が所有する。
- DSL 方式: 宣言的に記述した設定ツリーを状態再評価から差分へ変換する利用経路。Sample は利用例を示すが、経路自体の契約は iOS SwiftUI capability が所有する。

## 抽出メモ

概念候補数は0件。Sample 固有の長命知識として残せるのは責務境界だけだが、これは `monorepo-foundation` 候補の Sample 境界と重複するため、統合時には同候補への補足を提案する。Store 方式と DSL 方式の責務は、それぞれ `settings-view-ios-host` と `settings-view-ios-swiftui` が所有し、Sample 側では再定義しない。

Sample 専用のテストターゲットは Xcode プロジェクトに存在しない。Sample の各デモが利用する基本 Cell、入力 Cell、共通フィールド、可視性、差分更新、DSL 同一性の挙動契約は `ios/Tests/` の本体テストで確認されている。Sample ターゲットが担う機械的な検証は、全デモソースを3つの iOS モジュールとともにコンパイルできることまでで、画面上の統合結果は目視確認に依存する。

spec が列挙する画面構成、Theme 値、表示文字列、具体的な操作は現行コードから再導出できるため概念候補から除外した。`UIColor` 直接利用、iOS/Android の表記揃え、Sticky Footer 不採用などの横断的な原則を残す場合も、Sample 固有概念ではなく styling または conventions の既存候補へ統合するのが適切である。Android 側の実装は今回の入力範囲外のため、プラットフォーム間の完全一致は本 candidate では検証していない。
