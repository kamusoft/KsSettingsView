# セカンドオピニオン: maui-appearance-change-tracking (spec-001)
**相方**: codex / **label**: so-spec-maui-appearance-change-tracking / **日付**: 2026-09-15 / **対象**: 提案一式 (proposal.md / specs/ / tasks.md)
---
# レビュー結果: maui-appearance-change-tracking

**判定**: NEEDS_DISCUSSION  
**指摘件数**: Critical 0 / Major 5 / Minor 2 / Suggestion 0

## サマリー

不具合の再現・描画値による回帰検証という方向は妥当です。しかし、変更級と domain、UIKit containment の契約、保証する色領域、状態維持の検証、spike 後のスコープ処理が未確定です。このまま実装へ進むと、異なる実装方針が同じ spec を満たした扱いになったり、凍結後の spec と実装が衝突したりするため、提案段階での再整理が必要です。

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` — 常時
- `kasane/handbook/cross/test-execution.md` — テスト計画と完了判定
- `kasane/handbook/cross/runtime-behavior-verification.md` — 実環境での修正前後確認
- `kasane/handbook/cross/sample-parity.md` — Sample の一時変更
- `kasane/handbook/cross/local-development-setup.md` — Sample・外観切替手順
- `kasane/handbook/ios/swift6-language-mode-check.md` — iOS source 変更時
- `kasane/handbook/maui/integration-host-verification.md` — MAUI facade/handler の E2E
- `core/ADR-0030`、`core/ADR-0031`
- `kasane/concepts/core/styling/style-resolution.md`
- `kasane/concepts/maui/api/maui-styling.md`
- `kasane/concepts/core/architecture/display-state-synchronization.md`
- `kasane/concepts/maui/api/maui-rendering-lifecycle.md`

## 指摘事項

### [🟠 Major] 変更級と domain が対象範囲に一致していない

**該当箇所**: `proposal.md:38`、`proposal.md:40`、`proposal.md:42`

**問題点**: 提案自身が `settings-view-ios-ui` と `maui-bridge` の複数能力を扱い、iOS Native・MAUI handler・facade・両 OS の検証ホストを変更対象にしています。それにもかかわらず M 級、`domain: core` とされています。Kasane の基準では複数能力横断または architecture 境界の変更は L 級であり、複数 domain に該当する proposal は `cross` です。

`domain: core` のままでは、`kasane/config.yaml:29-42` に定義された iOS/MAUI の実装スキルが解決されず、蒸留先やレビュー対象 index も誤ります。さらに本提案には containment と trait 観測位置という設計判断があるのに `design.md` がありません。

**推奨修正**: L 級・`domain: cross` へ変更し、実際に触る iOS と MAUI の domain overlay を結合する前提にしてください。`design.md` で少なくとも trait 観測主体、containment、再適用経路、テスト観測境界を確定してください。

### [🟠 Major] 「親 VC なしを保証する」契約と containment 修正が競合している

**該当箇所**: `specs/settings-view-ios-ui/spec.md:14`、`tasks.md:15`

**問題点**: spec は「親 view controller を持たず view だけを置く」構成を正式な保証対象にしています。一方、既存契約は iOS Host を親 Page の child VC として embed することです（`kasane/concepts/maui/api/maui-rendering-lifecycle.md:60`）。実装も `AddChildViewController` と `DidMoveToParentViewController` を前提にしています（`maui/KsSettingsView.Maui/Platforms/iOS/KsHostContainment.cs:29-45`）。

UIKit も、子 VC の root view を追加する前に containment を確立することを custom container の契約としています。[Apple UIViewController documentation](https://developer.apple.com/documentation/uikit/uiviewcontroller?changes=_8_9)

さらに task 2.1 は「正しく親付けする」と「親なしのまま trait だけ届かせる」という異なる契約を選択肢として残しています。後者では外観だけ直っても lifecycle/appearance forwarding の不正状態が残ります。現実装は `Loaded` 後に既に一度再試行しているため、単に「接続後まで遅らせる」だけでは新しい解になりません。

**推奨修正**: MAUI 経路は正しい containment の確立を必須とし、親解決の再試行契機、失敗時の扱い、購読解除、二重追加防止を design/spec に定めてください。view-only 利用まで公開保証するなら、MAUI containment 修正とは分離し、UIKit の非標準利用を正式サポートする理由と lifecycle 境界を別の設計判断として扱うべきです。

### [🟠 Major] trait 観測位置の候補が等価でなく、「各行」の保証を満たさない実装が許される

**該当箇所**: `tasks.md:11`、`specs/settings-view-ios-ui/spec.md:5`

**問題点**: task は観測先を `KsSettingsViewController` または `KsListCellBase` / `CellBaseLayout.swift` の選択にしていますが、結果は等価ではありません。

- Controller 観測は view controller の trait 伝播に依存し、親 VC なし Scenario と衝突します。
- `KsListCellBase` は標準 Cell 用です。`CustomCellView` は直接 `UICollectionViewListCell` を継承して別の背景設定経路を持ちます（`ios/Sources/KsSettingsViewUI/CustomCellView.swift:25`、同`:53-55`）。
- `CellBaseLayout.swift` は描画関数であり、trait 購読の寿命を所有できません。
- 行単位の観測だけでは list 下地、Section decoration、supplementary の再適用主体を説明できません。

このため LabelCell の先頭行テストだけを通し、CustomCell や他の表示領域が古い外観のまま残る実装が成立します。

**推奨修正**: trait の観測主体を design で一つに決め、標準 Cell・CustomCell・list 下地・Section decoration・supplementary の対応表を作ってください。行側で観測するなら `KsCellViewSupport` など両 Cell 系統が共有する地点を含め、少なくとも標準 Cellと CustomCell の描画値テストを要求してください。

### [🟠 Major] MAUI Requirement が作業・検証範囲より広く、Header/Footer 背景は現行実装でも輸送後に使われていない

**該当箇所**: `specs/maui-bridge/spec.md:5`、`tasks.md:25`、`tasks.md:27`、`tasks.md:33`

**問題点**: Requirement は行背景・list 下地・Header/Footer の背景と文字・全 Cell 文字色を保証していますが、Scenario と tasks が観測するのは行背景、title、`CellValueTextColor` の一項目だけです。

iOS の supplementary 描画は `headerTextColor` / `footerTextColor` を渡していますが（`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1163-1183`）、`applyAccessoryToListCell` は Header/Footer の背景色を設定していません（同`:2229-2284`）。つまり `Theme.headerBackgroundColor` / `footerBackgroundColor` はこの描画経路で消費されず、本提案の作業だけでは Requirement 全体を保証できません。

また MauiHost は Root/Section Header/Footer に固定色のカスタム View を配置しているため（`maui/tests/KsSettingsView.MauiHost/SettingsPage.xaml:13-37`）、未指定の text Header/Footer 背景・文字の追随を目視できません。

**推奨修正**: 今回の契約を実際の障害範囲である「行背景・行文字・list 下地」に狭めるか、Header/Footer の色適用自体を実装範囲へ加えてください。後者なら text accessory を使う専用検証面と、背景・文字それぞれの描画値テストが必要です。

### [🟠 Major] identity と表示位置を維持する SHALL に対応する検証がない

**該当箇所**: `specs/settings-view-ios-ui/spec.md:5`、同`:12`、`proposal.md:34`、`tasks.md:18-22`

**問題点**: Requirement は Section/Cell identity と表示位置の維持を要求し、proposal は押下状態とスクロール位置をテストで固定するとしています。しかし Scenario と tasks が確認するのは先頭 Cell インスタンスの同一性だけです。Section identity、スクロール位置、押下状態に対応するテストはありません。

このままでは、外観切替時に full snapshot/reload を行ってスクロール位置が動く実装でも Scenario が通ります。

**推奨修正**: 十分な行数を持つ root を途中までスクロールした状態で切り替え、同じ可視 Cell ID、同じ Cell インスタンス、同じ `contentOffset` またはアンカー位置を確認する Scenario/task を追加してください。押下状態も契約に含めるなら専用 Scenario を追加し、含めないなら proposal の保証表現から外してください。

### [🟡 Minor] ユニットテスト Scenario に条件付き免除が入り、合否が環境依存になっている

**該当箇所**: `specs/maui-bridge/spec.md:26-29`、`tasks.md:26`

**問題点**: normative な Scenario が「再現できなければ検証ホストで代替可能」としており、Scenario 自身の合否が定まりません。また GIVEN は gateway 接続しか要求せず、`SettingsView` を Application → Window/Page の element tree へ接続していません。`AppThemeBinding` の変更は Application の theme 変更から parent resource change として伝播するため、tree 外の対象では試験が空振りし得ます。`UserAppTheme` 自体は同じ theme-change 経路を発火させます。[.NET MAUI Application source](https://github.com/dotnet/maui/blob/main/src/Controls/src/Core/Application/Application.cs)

**推奨修正**: Application/Window/Page/SettingsView の最小 element tree を組み、snapshot 再配信を必須 Scenario としてください。それが net10.0 テストで成立しないなら、ユニット Scenario を spec から削除して任意の調査 task に下げ、MauiHost Scenarioだけを受け入れ基準にしてください。

### [🟡 Minor] ライト→ダークしか検証せず、双方向の Requirement になっている

**該当箇所**: `specs/settings-view-ios-ui/spec.md:5`、`specs/maui-bridge/spec.md:5`、`tasks.md:6-7`

**問題点**: Requirement はライト/ダークの外観変更一般を保証していますが、全 Scenario と完了手順はライト→ダークだけです。ダーク値への切替だけを特別処理し、ライトへの復帰に失敗する実装でも合格します。

**推奨修正**: ダーク→ライトの復帰 Scenario を追加するか、今回の保証をライト→ダークに限定してください。一般契約を維持するなら、同じ Cell identity・表示位置のまま往復させる一つの Scenarioにまとめられます。

## アクションプラン

1. 級を L、domain を `cross` に修正し、`design.md` を追加する。
2. MAUI containment を必須にするか、view-only Hosting を正式保証するかを決定する。
3. trait 観測主体と、標準 Cell／CustomCell／list／decoration／supplementary の再適用範囲を確定する。
4. MAUI Requirement の色領域を今回の実装範囲へ狭めるか、Header/Footer 実装・検証を追加する。
5. スクロール位置など identity/表示状態の受け入れ基準を具体化する。
6. spike は結果にかかわらず本 change では記録のみと確定する。Cell の `AppThemeBinding` 本実装は新しい proposal へ分離する。現状の「同梱も選べる」は、凍結された Non-Goal と `core/ADR-0031` 維持契約に反するため削除する。
7. 条件付きユニット Scenario と双方向性を整理してから、再レビューする。



## 突き合わせ結果 (2026-09-15、ホスト側自己レビュー 2 周との照合)

| 指摘 | 採否 | 根拠 |
|---|---|---|
| Major 1: 級 L・domain cross・design.md | **採用 (級はオーナー確認)** | ksn-core の判定表は「複数能力横断 = L」で、本 change は settings-view-ios-ui / maui-bridge の 2 能力にまたがる。`domain: core` では domain-skills の iOS / MAUI 実装スキルが解決されない (references/domain-axis.md)。観測主体と containment は設計判断で design.md に固定する価値がある。ホスト側は先行 change (cell-style-dark-appearance-parity) の M / core を踏襲しており、見逃し |
| Major 2: 親 VC 無し保証と containment 修正の競合 | **採用** | `KsHostContainment.cs` は `ConfirmAdded` で既に再試行しており「接続後まで遅らせる」は新解ではない。concepts (maui-rendering-lifecycle) の契約は child VC embed。親 VC 無しの Scenario を spec から外し、MAUI 経路は containment の確立を必須にした (2.1、design Decision 2)。view-only 利用の公式サポートは Non-Goal へ |
| Major 3: 観測主体の候補が非等価・CustomCell の別経路 | **採用** | `CustomCellView.swift` は `KsListCellBase` を継承せず独自に背景設定を組む。観測主体を controller の view 1 つに固定し (design Decision 1)、CustomCell の描画値 Scenario / テストを追加した |
| Major 4: Requirement が Header / Footer 背景まで広く、iOS 描画が消費していない | **採用 (スコープ拡張として)** | `ios/Sources` で `headerBackgroundColor` / `footerBackgroundColor` を消費するのは bridge の DTO 変換のみで UI 層に消費点が無い一方、Android は `SectionAccessoryViewHolders.kt` で消費している (parity の穴)。オーナーの方針 (隣接課題は同じ change で直す) に従い、MAUI Requirement は行背景・行文字・list 下地に狭めつつ、iOS の text Header / Footer への適用を新 Requirement として同梱した |
| Major 5: identity・表示位置の検証が無い | **採用** | スクロール途中で往復させ可視行の ID・インスタンス・content offset を見る Scenario とテストを追加。押下状態は契約から外した |
| Minor 1: ユニット Scenario の条件付き免除 | **採用** | spec からユニット Scenario を削除し、最小 element ツリーで試す調査タスク (4.2) に降格。受け入れ基準は検証ホスト |
| Minor 2: 片方向のみ | **採用** | iOS / MAUI とも往復の Scenario に統合 |
| アクション 6: spike の「同梱も選べる」 | **採用** | 凍結後の Non-Goal と矛盾するため「記録のみ、採用は別 change の起票」に統一 |

採用 8 / 降格 0 / 未解決 0 (級の最終確定はオーナー)。
