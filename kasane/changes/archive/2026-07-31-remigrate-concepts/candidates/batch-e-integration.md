# Batch E 統合結果

## 対象と方法

Batch A〜D で確定した21 concepts を基準に、`docs/README.md`、`overview.md`、`architecture.md`、`core-model.md`、`cells.md`、`styling-and-theming.md`、`platform-guide-ios.md`、`platform-guide-android.md` の8文書を残差走査する。

各群は現行 concepts とコード・テストを先に読み、担当 docs は最後に照合する。docs の単なる再掲、廃止済み API、再導出しやすい実装一覧は移行せず、既存 concepts に未回収で価値 lint を通る知識だけを候補化する。`docs/legacy-aiforms-reference.md` は proposal の8文書に含まれない歴史参照であり、本 sweep の対象外とする。

## docs 残差スイープ

3群へ分けて code / test first で走査し、担当 docs を最後に照合した。結果は次の通り。

| 担当 docs | 既存 concept への合流候補 | 新規 concept | ADR 候補 | drift（重複を含む生件数） |
|---|---:|---:|---:|---:|
| README / overview / architecture | 0 | 0 | 0 | 12 |
| core-model / cells / styling-and-theming | 4 | 0 | 0 | 8 |
| platform-guide-ios / platform-guide-android | 1 | 0 | 0 | 10 |
| 合計 | 5 | 0 | 0 | 30 |

5件はすべて既存 concept の責務内に収まり、次のように統合した。

| 回収した残差 | 合流先 |
|---|---|
| Android composite build は source substitution を行っても build root と SDK / toolchain 解決を統合しない | `architecture/repository-boundaries.md` |
| Section / Root Accessory へ SwiftUI / UIKit / Compose / Android View の任意 View を渡す公開入口 | `core-model/settings-tree.md` |
| ButtonCell の補助フィールド有無と `titleAlignment` の適用範囲 | `cells/basic-cells.md` |
| Theme / CellStyle の特殊な fallback、font size、icon の解決規則 | `styling/style-resolution.md` |
| Section Header / Footer の寄せ方と高さ、Classic separator の geometry | `styling/list-appearance.md` |

新しい概念文書は追加しない。drift 30件は、OpenSpec SSoT、Core の platform 非依存、基本 Cell の Binding、declarative identity、Store / DSL の更新 stream、古い API / Sample 案内などの重複を含む。重複を原因単位でまとめた結果を「前回所見の引き継ぎ一覧」へ統合した。

## 最終 concepts 候補

`candidates/batch-e/concepts/` に21 concepts と `index.md` の最終候補を置く。Batch D 確定時に検出した Batch A 文書の concepts root 基準リンク24件を実配置基準へ修正し、`index.md` の「再移行中」注記を除去した。上記5件を既存文書へ合流し、docs 走査で再確認した iOS Theme 再適用境界も、表示済み Header / Footer の即時再評価を保証しない表現へ揃えた。

## 前回所見の引き継ぎ一覧

ここでは解消方向を決めず、移行完了後の探索・変更で扱う論点を重複原因ごとに集約する。詳細な個別所見は Batch A〜D の integration report を正とする。

### 決定・実装の drift

| 論点 | 現在地 | 出典 | 後続で必要な判断 |
|---|---|---|---|
| Maven `groupId` | ADR-0002 の公開規範は `jp.kamusoft`、現行 Gradle と Sample の開発用 GAV は `jp.kamusoft.kssettingsview` | `batch-d-integration.md` #1 | 公開導入前に Gradle を ADR へ合わせるか、ADR を supersede する変更を起こす |
| declarative identity の key / 明示 ID 優先順位 | ADR-0008 と現行 iOS / Android 実装が一致しない。concepts は併用禁止を安全な利用契約とする | `batch-b-integration.md` #6 / `batch-c-integration.md` #9 / `batch-d-integration.md` #10 | 実装を ADR へ合わせるか、ADR を supersede する変更を起こす |

### 実装不具合候補

| 論点 | 観察された状態 | 出典 |
|---|---|---|
| iOS 初回表示前の部分 Diff | Controller の view load 前に発行された更新が初期 snapshot へ取り込まれない可能性 | `batch-b-integration.md` #1 |
| missing Section への Accessory 更新 | iOS / Android の `updateAccessory` は他の missing-target 操作と異なり通知を発行する | `batch-b-integration.md` #2 / `batch-c-integration.md` #5 |
| iOS Section modifier と可視性 | `sectionHeader` / `sectionFooter` の copy が `isVisible` を保持しない | `batch-b-integration.md` #8 |
| 一 item から複数 DSL 要素 | iOS / Android の collection helper は同じ hint を複数結果へ付け、ID 衝突を起こし得る | `batch-b-integration.md` #9 / platform concepts |
| `disabled` modifier | iOS / Android とも公開 modifier が no-op | `batch-b-integration.md` #7 / `batch-c-integration.md` #15 |
| iOS Theme 公開値の描画反映 | `separatorColor`、`scrollIndicatorVisible`、Header / Footer 背景、表示中 Accessory への Theme 再適用が未反映または未確立 | `batch-b-integration.md` #11〜14 |
| Android View の再 attach | detach 時に Adapter を外すが、同じ View instance の再 attach で復元する経路が未確立 | `batch-c-integration.md` #6 |
| Android font 変換 | `TextStyle.fontFamily`、italic 等が `Typeface` 変換へ到達しない | `batch-c-integration.md` #18 |
| Android icon style | `cellIconSize` / `cellIconRadius` と個別 `iconSize` / `iconRadius` が共通行の24dp icon 描画へ反映されない | `batch-c-integration.md` #19 |
| Android scroll indicator | `Theme.scrollIndicatorVisible` が RecyclerView へ適用されない | `batch-c-integration.md` #20 |
| Android Button 共通行 | ButtonCell の描画経路が共通行から外れるという前回所見 | `concepts/log.md` 2026-07-18 deferred |
| Radio の再 tap 通知 | platform 間で同一選択肢の再操作時通知に差があるという前回所見 | `concepts/log.md` 2026-07-18 deferred |

### docs・旧 spec・コメントの drift

| 論点群 | 代表例 | 出典 |
|---|---|---|
| Core 境界 | docs は Core を platform 型非依存とし、`AnyCell` や古い alignment case を案内する | `batch-a-integration.md` #1〜3 / `batch-d-integration.md` #2 |
| 基本 Cell の TwoWay API | docs は基本 Cell 全般の `Binding` / `MutableState` overload を案内するが現行 API と一致しない | `batch-a-integration.md` #6〜7 / `batch-d-integration.md` #8 |
| iOS Host / SwiftUI | internal `KsListCellBase`、古い Representable / builder / style API、機能しない `disabled` を案内する | `batch-b-integration.md` #3〜7, #16, #19 |
| Android Host / Compose | 予約 `viewType`、古い `index` 引数・DSL scope・Theme / style 型、機能しない modifier を案内する | `batch-c-integration.md` #2〜4, #7〜17 |
| Sample | 起動画面・デモ一覧・旧 Theme 名・旧 Radio API・利用者定義 Cell の所在が現行 Sample と一致しない | `batch-d-integration.md` #4〜9 |
| harness の SSoT | README / docs は frozen OpenSpec を正と案内するが、現行運用は Kasane、コードとテストが一次情報 | `batch-d-integration.md` #3 |
| コードコメント | Android icon capability、`font` modifier、Classic separator 単位など一部コメントが実装と一致しない | `batch-c-integration.md` #16, #23, #27 |

### 検証鮮度

| 論点 | 観察された状態 | 出典 |
|---|---|---|
| iOS SwiftUI Theme bridge | `.theme(_:)` の Store / DSL make・update 経路を直接固定するテストが見当たらない | `batch-b-integration.md` |
| Android modifier | `backgroundColor` / `icon` / 常時 no-op の `disabled` を公開 API 単位で直接固定するテストが見当たらない | `batch-c-integration.md` #29 |

### deferred

| 論点 | 現在の扱い | 出典 |
|---|---|---|
| Material3 派生 XML Theme の ADR 化 | platform 利用契約として保持。採用理由・代替案を備えた出典がないため ADR 本文を創作しない | Batch C / D integration |
| Android Root Header / Footer の再 bind | detach / reattach と合わせて復元契約が未確立 | `concepts/log.md` 2026-07-18 deferred |
| 未消費 Style 値と platform fallback 差 | concepts へ保証として含めず、実装変更時に再評価 | `concepts/log.md` 2026-07-18 deferred |
| Maven publication / MAUI 識別子 | 実装がないため現在利用可能な公開契約にしない | Batch D integration |
| toolchain baseline concept | build metadata から再導出でき、独立文書は腐りやすいため見送り | Batch D integration |
| Sample README / platform guide 更新 | concepts 移行とは別変更で行う | `concepts/log.md` 2026-07-18 deferred |
| docs のスタブ化 | concepts 確定後の別変更として提案し、本 change では docs を編集しない | `proposal.md` Non-Goals |

## ADR 候補のトリアージ

新規 ADR 候補はない。

- monorepo と platform 別 build root は ADR-0001、Native Host の再利用は ADR-0004、Accessory の所有境界は ADR-0005、Store / DSL / identity / Native style / 表示状態は ADR-0006〜0010、共通 Cell 行は ADR-0011 に包含される。
- Android composite build の SDK 解決、Header / Footer の垂直配置、separator geometry、ButtonCell の layout 分岐は利用・実装上の具体契約であり、既存 concept に置く。
- Android XML Theme の Material3 前提は既存 concept に保持する。採用理由・比較した代替案・却下理由を再構成できないため、ADR 本文を創作せず deferred を維持する。
- 未実装 MAUI / KMP の目的記述から新しい決定を推測しない。

## 見送った情報

- Xcode、Swift tools、Gradle、AGP、Kotlin、JDK、compileSdk 等の個別 version と build command: build metadata から再導出でき、concept へ複製すると腐りやすい。
- Theme / CellStyle / Cell initializer / Store method の全フィールド一覧: API 定義から再導出しやすく、概念文書を reference dump にしない。複数箇所へ分散していた特殊解決規則だけを回収した。
- Native View hierarchy、class 継承、px / pt / dp、生の色値: 長命な責務・保証へ変換できるものを除き高腐食である。
- Sample の画面数、menu 名、表示文字列、デモ一覧: Sample は検証用 consumer であり製品契約の SSoT にしない。
- KMP 展開、利用可能な MAUI package / binding: 現行成果物と accepted ADR の裏付けがなく、構想や placeholder を公開契約にしない。
- `docs/legacy-aiforms-reference.md`: proposal で定めた8文書の対象外で、歴史参照として残す。

## docs スタブ化の別変更提案

本 change では frozen docs を編集しない。移行確定後に別 change を起こし、対象8文書を `kasane/concepts/index.md` と対応カテゴリへの短い入口へ置き換えることを提案する。その際は OpenSpec を SSoT とする案内、古い API 例、現行 Sample と異なる一覧を除去し、`legacy-aiforms-reference.md` は歴史資料として明示的に分離する。

## 全体初見可読性レビュー

docs 残差を統合した21 concepts と最終 `index.md` だけを新鮮な文脈の reviewer へ渡した。初回は次の2件で `NEEDS_FIX` だった。

- `core-model/settings-tree.md` に「styling バッチ」という移行作業中の文脈が残っていた。
- `cells/basic-cells.md` の「移植元」が未定義で、単独では理由を理解できなかった。

前者を `styling/style-resolution.md` への具体リンクへ置換し、後者は検証できない理由を除いて現行契約だけを残した。再レビューは **PASS**。index の21件と本文21件は一致し、最終配置基準の相対リンク切れ0件、「再移行中」または候補バッチ依存の注記0件を確認した。

## オーナーレビュー

状態: **2026-07-19 オーナー承認済み**。

最終候補は既存21 conceptsを維持し、新規文書・新規 ADR は追加しない。変更候補は次の10文書と `index.md` である。

- 残差統合と Theme 境界の明確化: `architecture/display-state-synchronization.md`、`native-host-boundary.md`、`repository-boundaries.md`、`core-model/settings-tree.md`、`cells/basic-cells.md`、`styling/list-appearance.md`、`style-resolution.md`
- Batch A 相対リンク修正: 上記 `settings-tree.md` / `basic-cells.md` に加え、`core-model/structural-changes.md`、`cells/input-cells.md`、`cells/ks-image.md`
- 仕上げ: `index.md` から「再移行中」注記を除去

候補を `kasane/concepts/` へ反映し、`concepts/log.md` と本 tasks を確定状態へ更新した。検証完了後、この change を archive する。
