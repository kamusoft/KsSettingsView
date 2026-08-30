# Batch D 統合結果

## 統合方針

確定済み Batch A〜C の Core、Cell、iOS、Android 文書と、`monorepo-foundation`、`samples-ios`、`samples-android` の code-first 抽出を、platform をまたぐ共通原則へ統合した。

旧 cross-platform concepts をそのまま復元せず、現行 iOS / Android の両方で確認できる責務だけを共通契約とした。platform ごとに異なる公開 API、最低行高、押下対象、Native list の表現は共通化せず、確定済み `platforms/` 文書へ参照を戻した。

| 統合ドラフト | 主な材料 | 判断 |
|---|---|---|
| `architecture/native-host-boundary.md` | iOS / Android Host、visible projection、lifecycle | Core・Store・Native list・Registry の責務境界を統合 |
| `architecture/store-and-update-streams.md` | 両 Store の現在状態、構造通知、Theme 通知 | 復元可能な状態と一過性通知を分離 |
| `architecture/display-state-synchronization.md` | snapshot / flat list、内容更新、visibility、Theme | 構造・内容・可視性・Theme の4経路を統合 |
| `architecture/cell-renderer-registry.md` | iOS Renderer / Android ViewHolder factory | Cell model と Native 描画型の拡張境界を統合 |
| `architecture/declarative-ui-bridge.md` | SwiftUI / Compose の DSL・Store 二方式 | 同じ Store / Native Host への収束を統合 |
| `architecture/declarative-tree-identity.md` | 動的 key、明示 ID、位置 fallback | 両実装で安全な identity 利用契約を統合 |
| `architecture/repository-boundaries.md` | monorepo、build roots、両 Sample | 横断変更、独立 build、consumer-shaped Sample の境界を統合 |
| `styling/style-resolution.md` | Theme / CellStyle / EffectiveStyle | UI 層の所有と実効値の解決順を統合 |
| `styling/cell-row-layout.md` | 両 platform の共通行、最低高、可変高 | 共通の視覚文法と platform 別寸法を統合 |
| `styling/cell-visual-states.md` | selection / ripple、disabled、意味色 | 共通優先関係と platform 差を統合 |
| `styling/list-appearance.md` | Classic / Modern、Root / Section H/F | 外観 mode と補助領域の所有を統合 |
| `conventions/public-identifiers.md` | product、namespace、Sample ID、開発用 GAV | ecosystem 別命名と Maven 配布前の未解消 drift を分離 |

## Sample の扱い

`samples-ios` と `samples-android` から独立した Sample concept は作らない。Sample は公開契約の一次所有者ではなく、確定済み platform / cells と今回の architecture / styling を利用者 application の境界から実行する reference である。

長命層へ残したのは次の3点だけである。

- library 本体と別 build の consumer 境界
- Local Swift Package / Gradle composite build による platform-local source reference
- 目視確認と、本体 code / test が所有する自動回帰契約の分離

デモ画面数、navigation、表示文字列、デモデータ、MAUI 比較用 Theme 値、Minimal Diffable 診断画面は Sample code / README に残す。

## monorepo 候補の選別

`monorepo-foundation` の「対応 platform と build 検証入口」は独立 concept にしない。deployment / min SDK は build metadata、tool version と個別 command は各 build root から直接確認でき、独立文書へ複製すると腐りやすい。独立 build root、現在の公開単位、MAUI が placeholder であることだけを `repository-boundaries.md` へ残した。

2026-07-19 の抽出では iOS library test、iOS Sample Simulator build、Android library test、Android Sample assemble、空の MAUI solution の読込が成功した。この実行結果は抽出証跡であり、継続的な CI 保証ではない。

## ADR 候補のトリアージ

| 候補 | 推奨 |
|---|---|
| monorepo と platform 別 build root | ADR-0001 に包含。新規なし |
| ecosystem ごとの公開識別子 | ADR-0002 に包含。ただし Maven `groupId` の実装 drift は未解消 |
| Native Host、Registry、Store、DSL / Store 収束 | ADR-0004 / 0006 / 0007 に包含。新規なし |
| 宣言ツリーの安定 identity | ADR-0008 に包含。iOS / Android 実装と ADR の併用優先順位が一致しないため、安全な利用契約だけを concept に残す |
| UI 層で platform native style 型を所有 | ADR-0009 に包含。新規なし |
| 構造・内容・可視性の同期分離 | ADR-0010 に包含。新規なし |
| 共通 Cell 行と薄い Cell 抽象 | ADR-0011 / 0013 に包含。新規なし |
| 最低 OS / toolchain baseline | 変更コストは高いが、現行 code で未強制の MAUI 方針を含む。既存 design をこの移行で新規 ADR へ写さず、実装を伴う後続変更で再評価 |
| Android Host の Material3 派生 XML Theme | Batch C からの新規 ADR 候補を維持。ただし採用理由・代替案を備えた design 出典がないため、本文を創作しない |

## drift 所見

解消方向は決めず、候補ファイルの詳細を保持したうえで、横断判断に影響する所見を集約する。

1. ADR-0002、旧 spec、archive design は Maven Central `groupId = jp.kamusoft` を定めるが、現行 Android 3 module と Sample の開発用 GAV は `jp.kamusoft.kssettingsview` を使う。`maven-publish` 設定はなく、配布開始前の判断が必要である。
2. README / docs は Core を platform 型非依存と説明するが、現行 `KsAnyView` は iOS で UIKit / SwiftUI、Android で Android View / Compose に依存する。層の依存方向は維持されるが、「platform 非依存」は現状と一致しない。
3. README / docs は frozen `openspec/specs/` を仕様の SSoT と説明するが、現行 harness は Kasane を使い `openspec/` を歴史資料として凍結している。
4. iOS / Android Sample の frozen spec は起動画面を Store デモとする一方、現行は menu から遷移する。Sample README のデモ一覧も現行より少ない。
5. 両 Sample spec は旧 Theme field `viewBackgroundColor` / `titleColor` と旧 `RadioCell.isSelected` を使う。現行は `backgroundColor` / `cellTitleColor` と `value` / `selectedValue` である。
6. Sample spec は separator、Ripple、行高など library 本体の描画契約を Sample capability に混在させる。Sample は公開 Cell を配置して観察するだけで、契約と回帰検証は Host / styling / Cell の code と test が所有する。
7. iOS Sample README の separator inset は icon あり52ptとするが、現行 code / test は icon 有無にかかわらず中間 Cell 16ptを正とする。
8. iOS platform guide は基本 Cell に `Binding<T>` initializer があると案内するが、現行基本7種は値 + callback、`Binding<T>` は入力 Cell 5種が提供する。
9. Android platform guide は利用者定義 Cell の実装先として Sample を案内するが、現行 Sample に独自 Cell / ViewHolder / Registry 登録例はない。
10. ADR-0008 は動的 key を明示 ID より優先するが、現行 iOS / Android の併用時優先順位はそれぞれ異なる。統合 concept は併用禁止を安全な利用契約とした。
11. Android の handler を持たない enabled 行にも ripple がある一方、iOS は操作可能な Cell だけが選択 feedback を持つ。横断概念で一方へ一般化しない。
12. Android Host の Material widget は Material3 派生 XML Theme を必要とするが、旧 theme bridge spec はこの host 条件を説明しない。

## 見送った情報

- Theme / CellStyle の全 field、constraint、hash、Diffable / RecyclerView の内部 item 型は code から再導出しやすいため移行しない。
- toolchain の全 version と build command は timestamp 付き抽出証跡に留め、長命 concept へ複製しない。
- Sample の画面一覧、個別 route、色値、デモデータ、診断画面は製品契約にしない。
- frozen spec / docs の廃止済み API 例は現行 contract へ持ち込まず、drift 所見として保持する。
- Maven 公開 metadata と MAUI namespace / package ID は実装がないため、現在利用可能な契約として確定しない。

## 初見可読性レビュー

`batch-d-readability-review.md` の初回必須3件・推奨8件を反映した。

- Core の Cell 抽象、UI 層の Cell model、platform の Native cell / ViewHolder の3層を定義した。
- Store 初期値の構築 API と再評価される画面 DSL を platform 別に区別し、`resolved tree → Store 操作 → Host` の流れを追加した。
- iOS / Android の利用者定義 Cell 登録例、独立 Registry、`viewType`、strict mode、再利用時の解放地点を追加した。
- architecture の読む順序、専門語の言い換え、identity 対照例、handler なし ripple の意味、行高、Theme property、Maven 規範を明確にした。

軽微な残件だった `preflight` と Android `Decoration` も日本語の役割名へ置き換えた。最終再レビューは **PASS** で、残存する必須・推奨の可読性問題はない。最終配置 `concepts/{architecture,styling,conventions}/` 基準の文書間リンクも構造上整合している。

## オーナーレビュー

2026-07-19 に Batch D の確定承認を得た。統合ドラフト12件を `concepts/{architecture,styling,conventions}/` へ配置し、index・log・tasks を更新した。

Sample は独立 concept を作らず、consumer 境界と platform-local source reference を `architecture/repository-boundaries.md` へ統合した。Maven `groupId` は ADR-0002 の `jp.kamusoft` を公開規範として維持し、現行 `jp.kamusoft.kssettingsview` は未解消の実装 drift として記録した。新規 ADR は作成せず、Material3 派生 XML Theme の ADR 候補と未実装の配布・MAUI 識別子は後続変更で再評価する。
