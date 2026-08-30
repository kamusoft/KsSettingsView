# Candidate: settings-view-core

## 概念候補

### Core の論理モデル境界 (提案カテゴリ: architecture/)

Core は、設定画面を構成するツリーと、そのツリーに対する変更の意味を、iOS と Android の UI ホスト層が共有できる論理モデルとして提供する。両プラットフォームで具体的な型表現は異なっても、Root、Section、Cell、装飾、更新という語彙と責務は対応させる。

Core が担うのは状態の値表現、値比較、識別子を介した参照、更新意図の受け渡しである。描画、状態の保持と適用、Theme / CellStyle、具象 Cell は UI 層が担う。Root のスタイルと Root Header / Footer も設定ツリーの値には含めず、View 側の状態として扱う。一方、装飾へ任意 View を渡すための不透明な View payload は Core の公開境界を通過するため、現実の Core は「プラットフォーム型を一切含まない純粋モデル」ではなく、「描画責務を持たない論理モデル」と捉えるのが実装に合う。

不変条件は、Core の値に UI スタイルや具象描画の責務を混入させないこと、UI 層から Core への逆依存を作らないこと、プラットフォーム固有の型表現差を許容しながら同じ論理的意味を維持することである。

出典: `ios/Sources/KsSettingsViewCore/SettingsRoot.swift`、`ios/Sources/KsSettingsViewCore/Section.swift`、`ios/Sources/KsSettingsViewCore/KsCell.swift`、`ios/Sources/KsSettingsViewCore/KsAnyView.swift`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRoot.kt`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Section.kt`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/KsAnyView.kt`、`openspec/specs/settings-view-core/spec.md` Purpose、`docs/core-model.md`、`docs/architecture.md`

### 設定ツリーと装飾の語彙 (提案カテゴリ: core-model/)

設定画面のモデルは、Section の順序付き集合を持つ Root、Cell の順序付き集合を持つ Section、識別子を持つ Cell から成る。空の Root と空の Section は有効な状態である。Cell は選択・編集・値変更の対象となる行であり、Header / Footer の装飾とは区別する。

Section の Header / Footer は Section 専用の装飾値として表し、Root の Header / Footer は Root 専用の装飾値として表す。両者は文字列または不透明な任意 View を保持できるが、将来異なる振る舞いを持てるよう別の型として維持する。Root 装飾は Root の値には含めず、UI 層の View が保持する。差分命令の内部で両装飾を共通に運ぶ場合だけ、用途を失わない判別付きの統一表現を使う。

任意 View は意味のある値等価を定義できないため、装飾値の等価性には View の内容を参加させない。同じ種類の View 装飾同士は内容によらず等価とし、実際の内容更新は描画レイヤへ委ねる。文字列装飾は文字列内容を値等価へ含める。

出典: `ios/Tests/KsSettingsViewCoreTests/SettingsRootTests.swift`、`ios/Tests/KsSettingsViewCoreTests/SectionTests.swift`、`ios/Tests/KsSettingsViewCoreTests/RootAccessoryTests.swift`、`ios/Tests/KsSettingsViewCoreTests/SectionAccessoryTests.swift`、`android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootTest.kt`、`android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/SectionTest.kt`、`android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/RootAccessoryTest.kt`、`android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/SectionAccessoryTest.kt`、`openspec/specs/settings-view-core/spec.md` Purpose・「SettingsRoot ドメインモデル」・「Section ドメインモデル」、`docs/core-model.md`、`kasane/decisions/0005-root-section-accessory-boundary.md`

### 値等価・構造同一性・表示更新の分離 (提案カテゴリ: architecture/)

モデルの値等価と、表示構造を追跡する同一性は別の契約である。値等価は、テスト、一般的な値比較、コレクション利用のためにモデルの内容を比較する。一方、Section と Cell の追加・削除・移動を判定する構造同一性には識別子だけを使い、表示内容を混ぜない。識別子の一意性はモデル利用者が保証する。

同じ識別子の Cell に対する置換は、同一行の内容を再構成する意味を持つ。識別子そのものを変える差し替えや Section 間の移動は、削除と追加による構造変更として表す。置換対象の識別子と新しい Cell の識別子が一致することも呼び出し側の不変条件である。

可視性は値等価には参加するが、内容更新とは区別する。非表示要素を含む完全な model と描画対象だけの visible projection を分け、可視性の変更は projection 上の追加・削除として扱う。これにより、通常の内容変更は同じ行を破棄せずに反映し、非表示データは model 内の位置と内容を保ったまま再表示できる。

出典: `ios/Sources/KsSettingsViewCore/KsCellID.swift`、`ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift`、`ios/Tests/KsSettingsViewCoreTests/KsCellIDTests.swift`、`ios/Tests/KsSettingsViewCoreTests/SectionVisibilityTests.swift`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiff.kt`、`android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/SectionVisibilityTest.kt`、`openspec/specs/settings-view-core/spec.md`「表示状態同期の三層分離」・「SettingsRootDiff 型」、`docs/architecture.md`、`kasane/decisions/0010-three-way-display-state-synchronization.md`

### Core の変更語彙 (提案カテゴリ: core-model/)

Core は、設定ツリー全体の差し替えに加え、Section と Cell の追加・削除・移動・置換、および Root / Section 装飾の更新を、判別可能な変更値として表す。変更値は状態を保持・適用せず、どの対象へどの種類の更新を要求するかだけを UI 層へ伝える。状態保持、対象解決、無効な操作への対処、プラットフォーム固有のアニメーションは UI 層の責務である。

Cell の置換は同一識別子の内容更新、Cell の移動は同一 Section 内の順序変更を意味する。装飾更新では対象位置と装飾種別を別々に保持し、値の不在を削除として扱う。Theme の変更はツリー構造の変更ではないため、この変更語彙には含めず UI 層の独立した経路で扱う。

出典: `ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift`、`ios/Sources/KsSettingsViewCore/AccessoryTarget.swift`、`ios/Sources/KsSettingsViewCore/SettingsAccessory.swift`、`ios/Tests/KsSettingsViewCoreTests/SettingsRootDiffTests.swift`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiff.kt`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/AccessoryTarget.kt`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsAccessory.kt`、`android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiffTest.kt`、`openspec/specs/settings-view-core/spec.md`「SettingsRootDiff 型」、`docs/core-model.md`、`kasane/decisions/0006-structural-diff-ui-store-boundary.md`

## ADR 候補

- 新規候補なし。値型中心の Core と薄い Cell 抽象は ADR-0003、Root / Section 装飾の境界は ADR-0005、変更値と UI Store の境界は ADR-0006、構造・内容・可視性の分離は ADR-0010 として accepted 済み。ADR-0003 と現行コードの矛盾は新規判断として創作せず、下記 drift 所見へ記録する。

## drift 所見

- spec と docs は Core を「プラットフォーム固有型を含まない」「UIKit / Compose 等に一切依存しない」「Android の依存は Kotlin stdlib のみ」と説明するが、現行の任意 View ラッパは iOS で SwiftUI / UIKit、Android で Android `Context` / `View` と Compose runtime を直接参照し、Android Core のビルドも Compose plugin/runtime を使用する。色・フォント・Theme を Core から隔離する境界は維持されている一方、「プラットフォーム型を一切含まない」という説明は実装より強い (`openspec/specs/settings-view-core/spec.md` Purpose / `docs/core-model.md` 冒頭 / `docs/architecture.md` §1 / `docs/overview.md`「プラットフォーム別モジュールマップ」 / `ios/Sources/KsSettingsViewCore/KsAnyView.swift` / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/KsAnyView.kt` / `android/ks-settingsview-core/build.gradle.kts`)。
- `docs/core-model.md` は iOS の異種 Cell コレクションを `[AnyCell]` とし、型消去ラッパ `AnyCell` を提供すると説明しているが、現行コードと spec は `[any KsCell]` を直接保持し、実装コメントは旧 `AnyCell` を廃止済みとしている。accepted の ADR-0003 も型消去ラッパ採用を Decision としており、現行実装と矛盾する (`docs/core-model.md` §2・§6 / `kasane/decisions/0003-value-oriented-core-model.md` / `openspec/specs/settings-view-core/spec.md`「Section ドメインモデル」 / `ios/Sources/KsSettingsViewCore/Section.swift`)。
- accepted の ADR-0003 は Kotlin の Cell 抽象を sealed interface とし、UI 層の型分岐に網羅性を持たせる Decision だが、現行コードと spec は外部モジュールからの独自 Cell 実装を許す通常の interface を要求している。どちらを長命な判断として残すか、ADR の supersede を含めた判断が必要 (`kasane/decisions/0003-value-oriented-core-model.md` / `openspec/specs/settings-view-core/spec.md`「Cell 抽象」 / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt`)。
- `docs/core-model.md` の Kotlin 例はタイトル配置の列挙値を lowercase の `start / center / end` と記載し、spec もプラットフォームを区別せず同じケース名を要求するが、現行 Kotlin 公開 API とテストは `START / CENTER / END` である (`docs/core-model.md` §7 / `openspec/specs/settings-view-core/spec.md`「CellTitleAlignment 列挙型」 / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/CellTitleAlignment.kt` / `android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/CellTitleAlignmentTest.kt`)。

## 用語

- Root: 設定画面のモデル上の最上位で、Section の順序を保持する値。Root Header / Footer と Theme は保持しない。
- Section: Cell の順序、Section 装飾、可視性をまとめる設定画面内の区画。空の Cell 集合も許容する。
- Cell: 設定値の表示・選択・編集を担う行。Core では識別子だけを共通契約とし、具象型とスタイルは UI 層に属する。
- Accessory: Root または Section の Header / Footer に配置する装飾。Cell とは異なる責務を持つ。
- 構造同一性: Section / Cell の追加・削除・移動を判断するため、内容ではなく識別子だけを比較する契約。
- 値等価: モデルの内容を比較し、テストや一般的なコレクション操作に使う契約。構造同一性とは別物である。
- model: 非表示要素を含む設定ツリーの完全な状態。
- visible projection: model から表示対象だけを射影した、UI の構造同期に使う派生状態。
- reconfigure: 同じ識別子の Cell を破棄・再生成せず、その表示内容だけを更新すること。

## 抽出メモ

独立概念は「Core の論理モデル境界」「設定ツリーと装飾の語彙」「値等価・構造同一性・表示更新の分離」「Core の変更語彙」の4件を提案する。後二者は関連が強いが、前者は複数 capability を横断する表示同期原則、後者は Core が外部へ公開する変更の意味論であり、統合時には architecture/ と core-model/ に分けると参照目的が明確になる。

「設定ツリーと装飾の語彙」は ADR-0005 の判断結果を利用者が現在形で理解する概念、「値等価・構造同一性・表示更新の分離」は ADR-0010 の判断結果を実装者が参照する概念として扱う。ADR の採否理由を概念へ重複させず、長命な責務と不変条件だけを残した。

Cell の安定 ID 再採番プロトコルは Core に配置されているが、目的は DSL 層と具象 Cell の依存逆転であり、settings-view-ios-swiftui / settings-view-android-compose capability および ADR-0008 との統合対象とする。本候補では独立概念にしない。

Section Header の高さ、タイトル配置の列挙、更新値の全ケース一覧など、コードから容易に再導出できるシグネチャや現在値は概念本文へ列挙しない。公開 API の利用リファレンスとして必要な場合は、concept ではなく docs または `type: reference` の別成果物として扱うのが適切である。
