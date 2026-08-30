# Exploration: perf-android-customcell-composition-reuse

調査日: 2026-08-15 / 出発点: 「Native の CollectionView / RecyclerView の View 再利用はちゃんと機能しているか」(オーナー起点の性能調査)

## 課題 / 動機

- View リサイクルは本ライブラリの重要なパフォーマンス指標だが、差分検出系と違い検証の形跡がなかった。
- 調査の結果、両 OS とも **ViewHolder / Cell の器レベルのリサイクルは成立** (単一 viewType 120 / 単一 reuseIdentifier "CustomCellView" を全 CustomCell が共有。懸念だった「インスタンス毎ユニークキーでプール分断」は不在)。
- 一方 **CustomCell の宣言 UI content はリサイクル毎に作り直されている**:
  - Android: `ComposeCellViewHolder` の `ViewCompositionStrategy.DisposeOnDetachedFromWindow` 明示指定により、スクロールアウトで Composition (slot table + LayoutNode ツリー) が丸ごと破棄される ([ComposeCellViewHolder.kt:36](../../../android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ComposeCellViewHolder.kt))
  - iOS: `prepareForReuse` の `contentConfiguration = nil` により SwiftUI ホスト階層が破棄される ([CustomCellView.swift:106](../../../ios/Sources/KsSettingsViewUI/CustomCellView.swift))
- オリジナル AiForms も「型キーのリサイクルは効くが中身は MauiView から作り直し」という同型の積み残しを抱えていた。ただし本ライブラリの **MAUI Bridge 経路は両 OS とも platform view 本体を保持済み** (検証テストあり)。残るのは宣言 UI の殻の再構築コストのみ。

## 検討した選択肢 (却下案と理由を含む)

### Android

- **案A (採用): `DisposeOnDetachedFromWindow` 明示指定の撤廃** — Compose 1.2+ の既定戦略 `DisposeOnDetachedFromWindowOrReleasedFromPool` は pool-aware (プール滞在中は Composition 生存、プール破棄時に dispose)。BOM 2024.10.01 (compose-ui 1.7.5) / RecyclerView 1.3.2 で要件充足を実ソースで確認。公式ガイダンスも recycle 時 dispose の削除を明示的に推奨。
- **案B (却下→将来の追加最適化): setContent 一回化 + state 経由更新** — `ComposeView.setContent` は内部的に MutableState 書き込みのみで Composition を再インストールしない (compose-ui 1.7.5 実ソースで確認)。再構築の原因は 100% 破棄戦略側であり、**単独では効果ゼロ**。案A 適用後の invalidation 粒度最適化 (5 値の個別 state 化) としてのみ意味がある。
- 経緯: 明示指定はリーク修正の産物ではなく初期設計 (`add-settings-view-android-ui` design Decision 5) の予防的決定。当時のレビュー (review-result_001.md:168) が既に「重い経路」と指摘していたが未対応のまま残った。

### iOS

- **案A (計測ゲート待ち): `prepareForReuse` の `contentConfiguration = nil` 撤廃** — UIKit の同型 configuration 再設定 = in-place 更新は公式仕様からの推論止まり (UIHostingConfiguration 内部は非公開)。Apple 自身は「configuration は軽量、毎回作ってよい」寄りの言及のみで、**効果の定量根拠が公開情報に存在しない**。加えて (1) SwiftUI identity 持ち越し対策に `.id(安定ID)` が必要だが render が `KsCellID` を受け取っていない、(2) MAUI Bridge の退役順序レース (実機フリック再現もの) の再検証が必須、(3) iOS 18 で in-place 前提が崩れる未解決報告あり。→ **計装 (makeContentView 呼び出し回数 / フレーム時間) で効果を実測してから投資判断**。
- **案B (却下): 自前 UIHostingController 保持** — Apple が「UIHostingConfiguration が唯一の公式サポート経路。cell 内 UIHostingController 埋め込みは非サポート」と明言。deployment target iOS 16 で採用動機なし。

### 進め方

- **両 OS 同時 1 change (却下)** — iOS の不確度が Android の確実な改善を人質に取る。
- **ロードマップ化 (却下)** — 各 change は独立で順序調整不要。大げさ。
- **採用: 2 段構え** — ① Android M 級で先行、② iOS は計測スパイクで効果確定後に判断。

## 決定事項

1. **2 段構えで進める** (2026-08-15 オーナー承認): Android 案A を M 級 change として先行。iOS は計測スパイク後に判断 (効果が小さければ「やらない」も正解)。
2. Android 案A の必須セット: ADR 改訂 (core/ADR-0015 の Android lifecycle 条項の置換) + 明示指定撤廃 + `key(cell.id)` による content subtree 隔離 + 「detach→プール→再 attach 実経路」の回帰テスト新設。
3. `reset()` のリーク防止保証 (前 content への参照切断) は維持する。手段は `setContent {}` のままでも state null 化でも等価。
4. MAUI Bridge の再親付け設計は「スクロールアウトで必ず破棄」前提で検証済みのため、**Bridge の既存リサイクルテスト (KsBridgeCustomCellTest) を回帰ゲートに含める**。

## ADR 候補

- **作成済み (proposed)**: [android/ADR-0015](../../decisions/android/0015-customcell-pool-aware-composition-disposal.md) — CustomCell の Composition 破棄を pool-aware 既定戦略に任せる
- 未起票: iOS 側は計測結果が出て採否が決まった時点で起票 (現段階では決定が存在しない)

## 未決の論点

1. **Section/Root Accessory の対称性** — `SectionAccessoryViewHolders.kt:413` も同戦略を明示指定。CustomCell だけ変えると 2 経路で戦略が食い違う。同時撤廃か、非対称の理由記録か (propose で決める)。
2. **id 安定性の穴** — id 未指定 CustomCell の既定 id は `UUID.randomUUID()` で構築毎に変わる。DSL 経路は安定 ID 振り直しで保護済みだが、native Host 直接利用時の挙動は未確認。`key(cell.id)` 隔離の効き方に影響 (id が毎回変わる使い方では subtree 毎回作り直し = 現状と同じで、悪化はしない)。
3. **iOS 計測スパイクの設計** — makeContentView 呼び出し回数の計装方法、スクロールフレーム計測の手順 (第2弾で扱う)。
4. **効果の定量値** — Android も Composition 再構築コストの一次情報は定性止まり。必要なら Macrobenchmark (FrameTimingMetric) で実測。

## UI 素材

なし (挙動・性能のみの変更。見た目の変更なし)

## 変更級の推奨: M (オーナー合意済み)

- コード変更自体は小さい (実質 1 行 + key 隔離) が、**決定層の改訂 (ADR) を伴い、状態持ち越しという新リスク経路への回帰テスト新設が本体**。
- 公開 API 変更なし。可逆 (戦略指定を戻せば復元)。
- 触る範囲: ks-settingsview-ui (ComposeCellViewHolder / CustomCellViewHolder / CustomCellRow)、テスト、decisions/。Bridge はテストゲートのみ。

## 追記 (2026-08-15, second-opinion 後の設計転換)

second-opinion (spec-001) の Major 1 により、当初設計 (戦略撤廃 + `key(cell.id)` discard 隔離) では `reset()` の空 content 化が content subtree を除去するため、**目的 (CustomCell の中身のリサイクル) に届かない**ことが判明した。オーナーの指示「目的は CustomCell で Recycle を効かせること。実現不可能かを改めて報告せよ」を受けて再調査し、以下に転換した:

- **実現可能**。使う道具は discard 型の `key` ではなく、LazyColumn の item リサイクルと同じ公式機構 **`ReusableContent(cellId)` / `ReusableContentHost(active)`** (deactivate+reuse 型)。cellId 変更で remember / DisposableEffect は破棄しつつ LayoutNode の構造一致部分を再利用する。
- compose-runtime / compose-ui 1.7.5 実ソースでの裏取り結果: **条件付き成立** (プレーン ComposeView 直下で機能 / onReset なし AndroidView は非 reusable で強制置換のため Bridge の再親付け前提は不変 / deactivate で状態破棄・ノード保持を確認)。詳細な根拠 file:line は調査ログ参照。
- reset は「active=false + content state 切り離し」に変更。builder 参照の reset 時切断は維持できる見込みで、当初懸念した保証緩和は不要。
- 一度「効果ゼロ」と却下した setContent 一回化 (旧案B) は、ReusableContentHost の土台として必須になり復活。
- ADR 構造はオーナー判断で supersede 方式に確定: core/ADR-0022 (proposed) が core/ADR-0015 を supersede し lifecycle 管理を platform ADR へ委譲、android/ADR-0015 を ReusableContent 方式で書き直し。
- 新たな負担: deactivated ノードのプール滞在中メモリ (有界)、`AndroidViewHolder.onDeactivate` 新経路の実機再検証 (高速フリック)、compose-runtime 実装詳細への依存増。

### 追記 2 (2026-08-15, second-opinion spec-002 の反映)

書き直し版への 2 回目の相方レビュー (Major 5 / Minor 1、全採用) を反映:

- **itemViewCache 滞在中は active を維持する契約に決定** (オーナー承認・案a)。cache 経由の bind なし再表示では状態が維持され effect が継続する (有界)。プール経路の検証は cache 無効化、cache 経路の検証は既定設定で行う
- reset の参照切断契約を「ViewHolder 直接参照 (content state・listener) の切断 + Composition 破棄後の GC 可能性」に限定。保持ノード経由の旧 content 参照は再利用/破棄まで残ることを許容として明文化
- ノード再利用の保証範囲を「同一 builder (同一 composable 呼び出し構造) 間の reusable ノード」に限定 (Compose の reuse 条件は call-site の group 構造互換のため)
- Bridge の保全契約を specs/maui-bridge/ にデルタ化し、deactivate 経路を明示的に通す専用回帰テストを追加
- core/ADR-0022 を「適用除外の決定を本文へ完全引き継ぎ + Android 限定の暫定委譲 (iOS は当面 core 規定のまま)」へ書き直し
- proposal の domain を cross へ変更 (core ADR に触るため。実装スキル解決は android overlay)

### 蒸留への申し送り

- `kasane/concepts/maui/architecture/view-materialization.md` の「onReset を渡していない = 非 reusable」節に、根拠として compose-runtime の非 reusable ノード強制置換 (Composer の forceReplace 分岐) を追記すると、ReusableContent 導入後も前提が追跡可能になる
- `kasane/concepts/core/cells/custom-cell.md` の残留防止の担保手段記述の追随

## 根拠パス (主要)

- Android 現状: `android/ks-settingsview-ui/.../ComposeCellViewHolder.kt` L18-39 / `CustomCellViewHolder.kt` L53-66, L98-106 / `KsCellRegistryCustomCell.kt` L23-31
- 経緯: `openspec/changes/archive/2026-05-10-add-settings-view-android-ui/design.md:99` (Decision 5) / 同 `review-result_001.md:168`
- 規約ゲート: `kasane/decisions/core/0015-customcell-exemption-from-shared-row-layout.md:18`
- iOS 現状: `ios/Sources/KsSettingsViewUI/CustomCellView.swift` L77-78, L106-110 / `CustomCellTests.swift` L372-390
- MAUI Bridge (保持済みの証跡): iOS `KsBridgeCustomCellTests.swift` L319-374 / Android `KsBridgeCustomCellTest.kt` L421-463、`kasane/concepts/maui/architecture/view-materialization.md:62-78`
