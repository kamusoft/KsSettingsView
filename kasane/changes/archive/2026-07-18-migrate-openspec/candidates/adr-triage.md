# ADR backfill トリアージ

判定日: 2026-07-17

採否: 推奨11候補をユーザーが一括承認。ADR 0001〜0011 として `status: proposed` でドラフト済み。

選別基準:

1. 覆すのが高コスト
2. 能力・コンポーネント境界を越えて影響する
3. 将来の決定を制約する

後続 change で改訂・撤回された判断は、最終形を示す出典を優先して統合した。各候補はユーザー承認後にのみ MADR-lite 全文を作成する。

## 推奨候補

| 候補 | 出典 | 基準 | 推奨 | 要旨 |
|---|---|---:|---|---|
| モノレポとプラットフォーム別ビルドルート | `2026-05-06-add-monorepo-foundation/design.md` Decision 1–2 | 1, 2, 3 | 採用 | 単一リポジトリで横断変更を同期しつつ、iOS・Android・MAUI は独立したビルドルートとして保つ。 |
| 公開識別子の名前空間 | `2026-05-06-add-monorepo-foundation/design.md` Decision 3 | 1, 3 | 採用 | 所有ドメインを根拠に Apple/Android の識別子を `jp.kamusoft.kssettingsview.*`、Maven を `jp.kamusoft`、.NET を `KsSettingsView.*` とする。 |
| 値型中心の Core モデルと薄い Cell 抽象 | `2026-05-07-add-settings-view-core/design.md` Decision 1–3、`2026-06-08-purify-core-extract-style-to-ui-layer/design.md` Decision 4 | 1, 2, 3 | 採用 | Core は値型中心とし、Cell 抽象は同一性に必要な最小契約へ絞る。Swift の型消去と Kotlin の sealed interface は各言語の制約に合わせる。 |
| Native View を描画基盤として宣言UIから再利用 | `2026-05-09-add-settings-view-ios-ui/design.md` Decision 1, 3–4、`2026-05-10-add-settings-view-android-ui/design.md` Decision 1–4 | 1, 2, 3 | 採用 | iOS は UICollectionView、Android は RecyclerView を描画基盤とし、SwiftUI/Compose はラッパとして再利用する。Cell 登録は Registry に集約する。 |
| Root と Section の装飾責務境界 | `2026-05-09-refactor-accessory-and-root-hf/design.md` Decision 1–3、`2026-05-15-add-partial-update-core/design.md` Decision 1, 4、`2026-05-15-add-partial-update-native/design.md` Decision 5–6 | 1, 2, 3 | 採用 | Section 装飾は Section モデルの型付き値として保ち、Root H/F は View 側プロパティとする。任意 View は Cell と分離し、値等価による差分対象にしない。 |
| 構造 Diff と UI Store による更新境界 | `2026-05-15-add-partial-update-core/design.md` Decision 2–7、`2026-05-15-add-partial-update-native/design.md` Decision 1–3, 7–8 | 1, 2, 3 | 採用 | Core は閉じた構造 Diff 型を定義し、UI 層の Store が状態保持と適用を担う。root 全代入や推測更新を公開経路にしない。 |
| 宣言 DSL と Store API の併存・内部収束 | `2026-05-18-add-declarative-dsl/design.md` Decision 1–2, 6 | 1, 2, 3 | 採用 | 一般利用向け DSL と高度利用向け Store を併存させ、DSL 内部も Store と Diff の同じ更新経路へ収束させる。 |
| 宣言ツリーの安定同一性 | `2026-05-18-add-declarative-dsl/design.md` Decision 3–4 | 1, 2, 3 | 採用 | 再評価ごとのランダム UUID を最終IDにせず、ForEach key・明示ID・安定した位置情報から Section/Cell ID を解決する。 |
| スタイルを UI 層に隔離し Native 型で表現 | `2026-06-08-purify-core-extract-style-to-ui-layer/design.md` Decision 1–10、`2026-06-08-port-theme-and-cellstyle-missing-fields/design.md` Decision 3 | 1, 2, 3 | 採用 | Theme/CellStyle/KsImage を UI 層へ置き、色・フォントは各プラットフォームの Native 型を使う。Theme 更新を構造 Diff から分離し、実効値は CellStyle → Theme → platform default で解決する。 |
| 表示状態同期を構造・内容・可視性に分離 | `2026-06-03-refactor-display-state-sync/design.md` Decision 1–5、`2026-06-14-add-visibility-flags-section-and-cell/design.md` Decision 1–6 | 1, 2, 3 | 採用 | 構造はID、内容は同一 View の再構成、可視性は model と visible projection の再構築として別経路で扱う。値等価を構造同一性に流用しない。 |
| Cell 共通行レイアウトをコンポジションで統一 | `2026-06-13-unify-cell-common-fields-via-shared-row-layout/design.md` Decision 1–4, 8–12 | 2, 3 | 採用 | 継承階層や Cell ごとの重複ではなく、共通行レイアウト関数と構成要素のコンポジションで全 Cell の共通フィールドを扱う。Android 外枠は View ベースを維持する。 |

## ADR にしない判断群

| 判断群 | 主な出典 | 判定 | 理由・行き先 |
|---|---|---|---|
| 最低ツールチェイン・Deployment Target | `2026-05-06-add-monorepo-foundation/design.md` Decision 4 | concepts 候補 | 現在値はビルド設定から再導出でき、時間で変わりやすい。長命な「モダンAPIを利用できる範囲を基準にする」方針だけを concepts 統合時に検討する。 |
| Sample のプロジェクト形式・画面内容・README構成 | `2026-05-11-add-samples-ios/`、`2026-05-11-add-samples-android/` | 見送り | Sample 内に閉じた可逆な判断で、コードと README から再導出できる。 |
| PoC Cell の導入・削除時期、実装順序 | 初期 UI / Sample / basic Cell 各 design | 見送り | 一時的な変更管理・移行手順であり、将来判断を制約しない。 |
| 個別 Cell の具体 API、描画値、罫線、余白、色、Widget 選択 | basic Cell / style / sample-layout / layout-review 各 design | concepts または見送り | 公開契約は code-first で concepts 候補にする。局所描画の実装詳細はコードに任せる。 |
| OpenSpec の MODIFIED/ADDED、archive 順序、review 対応 | 各 design の spec/workflow Decision | 見送り | 旧ハーネス固有であり Kasane へ移植しない。 |
| `KsColor` / `KsFont` を Core に置く初期判断 | `2026-05-07-add-settings-view-core/design.md` Decision 5 | 撤回済み | `2026-06-08-purify-core-extract-style-to-ui-layer` により削除された。最終判断のみ候補化する。 |
| Theme.AppCompat 必須化 | `2026-06-03-add-cell-types-basic/design.md` Decision 7 | 撤回済み | 同 design Decision 8 で Material3 必須へ改訂された。局所的な現在要件は code-first で照合する。 |
| 表示状態同期の二層分離 | `2026-06-03-refactor-display-state-sync/design.md` | 改訂済み | `2026-06-14-add-visibility-flags-section-and-cell` で三層分離へ進化したため、最終形だけを候補化する。 |
| `KsAccessoryReusableView` 採用 | `2026-06-07-refine-basic-cells-sample-layout/design.md` Decision 16-2, 16-3 | revert 済み | 同 change Decision 18-1 で撤回済み。 |

## 承認後の処理

- 採用候補に `0001` から連番を割り当て、出典に忠実な MADR-lite 全文を `status: proposed` で作成する。
- 複数出典を統合する候補は、後続 change による改訂を本文で明示し、撤回済み内容を現判断として記述しない。
- ユーザーレビューで修正後に `accepted` とし、`kasane/decisions/index.md` を更新する。
