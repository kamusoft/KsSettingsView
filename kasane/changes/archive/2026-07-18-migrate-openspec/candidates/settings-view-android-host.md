# Candidate: settings-view-android-host

## 概念候補

### Android 設定画面ホスト境界 (提案カテゴリ: platforms/)

プラットフォーム非依存の設定ツリーを Android のネイティブなリスト表示へ橋渡しし、通常の View、宣言的ラッパ、MAUI バインディングの共通描画基盤を提供する。

責務境界:

- ホスト層は、完全な設定モデル、描画対象だけを抜き出した projection、画面全体の上下アクセサリ、Theme、購読ライフサイクルを統合する。
- ドメインモデルの定義、宣言的ツリー同士の差分算出、罫線や Section アクセサリの視覚詳細は隣接 capability の責務とする。
- 画面全体の上下アクセサリは、Section と Cell の projection とは別の表示領域として扱う。

不変条件:

- 非表示要素を含む完全モデルと、描画用の visible projection を分離して保持する。
- 非表示 Section はその配下を含めて projection から除外し、可視性契約へ参加しない拡張 Cell は安全側として表示対象に含める。
- バインド時には永続スナップショットを先に反映し、ライフサイクル所有者が未解決なら購読開始だけを attach 後まで遅延する。
- detach 時には更新購読を停止し、リスト View から Adapter への接続を切る。

出典: `openspec/specs/settings-view-android-host/spec.md` Purpose / 「KsSettingsView の公開 API」/「visible projection の flatten 規約」/「メモリリーク防止」、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTest.kt`、`FlattenVisibilityTest.kt`、`MemoryLeakTest.kt`

### 表示状態同期の三分類 (提案カテゴリ: architecture/)

設定画面の変化を、構造同期、同一項目の内容更新、可視性変化の三つに分類し、それぞれ異なる反映経路へ流す。目的は、構造アニメーションを保ちながら内容更新の行再生成とちらつきを避け、非表示要素もモデル上では失わないことである。

不変条件:

- 構造上の同一性は ID だけで決まり、表示内容の等価性を構造差分判定に混ぜない。
- 同一 ID の内容更新は既存の表示項目を部分更新し、構造上の差し替えとして扱わない。
- 複数項目が連動して変化するときは、一つの更新世代としてまとめ、対象すべての部分更新を同じコミット後に反映する。
- 可視性の切替は内容更新ではなく projection の再構築として扱う。Section 全体の置換も、内包し得る変化を推測せず projection を再構築する。
- 安定 ID は表示内容から独立し、同じ Section のヘッダとフッタは別の項目として識別する。

出典: `docs/architecture.md` §2・§5、`openspec/specs/settings-view-android-host/spec.md` 「DiffUtil 差分検出」/「部分 Diff の index 規約と hidden 対象の no-op」/「ReplaceCell / ReplaceSection の可視性切替防御」、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt`、`KsSettingsView.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ListAdapterDiffTest.kt`、`VisibilityApplyDiffTest.kt`、`SettingsRootStoreTest.kt` の Radio グループ一括更新テスト

### Android Store のスナップショットと変更通知 (提案カテゴリ: platforms/)

命令型の利用者に、現在状態をいつでも取得できる永続スナップショットと、その後の部分変更を伝える一過性通知を組み合わせて提供する。画面が通知購読前にも初期状態を復元でき、通常の変更では部分更新を選べるようにする。

責務境界:

- Store は完全な設定モデルと Theme の現在値を保持し、成功したモデル変更に対応する通知を発行する。
- Theme は構造変更通知から独立した状態として配信する。
- 画面全体の上下アクセサリは設定モデルに保持せず、表示ホストへ渡す更新通知として扱う。

不変条件:

- 対象が存在する操作では、現在状態を先に更新し、その変更に対応する通知を発行する。
- 対象 ID や移動元が存在しない操作は、状態も通知も変えない。
- 複数 Cell の連動する内容変更は、適用できた ID 群だけを一つのバッチ通知として発行する。適用件数がゼロなら通知しない。
- Theme の変更は設定モデルの構造変更通知を発行しない。
- インデックスは非表示要素を含むモデル配列を基準とし、表示 projection の位置を基準にしない。

出典: `openspec/specs/settings-view-android-host/spec.md` Purpose / 「SettingsRootStore（Android）」/「部分 Diff の index 規約と hidden 対象の no-op」、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt`

### Android Cell 描画の拡張境界 (提案カテゴリ: platforms/)

利用アプリが独自 Cell の値型と描画器を追加できる、ホスト層の公開拡張境界。Cell のドメイン表現を具体的な描画器へ解決し、開発時の登録漏れ検出と実運用時の耐障害性を両立する。

不変条件:

- 一つの描画種別値を異なる Cell 型へ同時に割り当てない。
- 同じ Cell 型の登録は後勝ちで差し替え可能とし、描画種別値を変更した場合は古い割り当てを残さない。
- 未登録型は厳格モードでは即時に失敗し、非厳格モードでは高さを持たない代替表示へ退避して画面全体のクラッシュを避ける。
- 再利用される描画器は listener、非同期処理、画像などの保持資源を解放できる契約を持つ。
- Compose を内包する描画器は Window から外れた時点で Composition を破棄する。

出典: `openspec/specs/settings-view-android-host/spec.md` Purpose / 「Cell レジストリ」/「CellViewHolder 抽象」/「ComposeView ライフサイクル管理」、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt`、`CellViewHolder.kt`、`ComposeCellViewHolder.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryTest.kt`

## ADR 候補

- Android のネイティブホストを単一の RecyclerView と三つの連結領域で構成し、中央領域では Section H/F と Cell を一つの平坦リストとして扱う — 出典: `openspec/specs/settings-view-android-host/spec.md` 「RecyclerView と Adapter 構成」、`docs/architecture.md` §1・§6。選別基準: 能力・コンポーネント境界を越えて影響する / 将来の決定を制約する
- 表示状態の変化を構造・内容・可視性の三分類に分け、構造は ID 同一性、内容は既存項目の部分更新、可視性は projection 再構築で反映する — 出典: `docs/architecture.md` §2・§5、`openspec/specs/settings-view-android-host/spec.md` 「DiffUtil 差分検出」/「ReplaceCell / ReplaceSection の可視性切替防御」。選別基準: 覆すのが高コスト / 能力・コンポーネント境界を越えて影響する / 将来の決定を制約する
- Theme を Core モデルおよび構造 Diff から分離し、UI 層の独立状態として配信する — 出典: `docs/architecture.md` §3・§5、`openspec/specs/settings-view-android-host/spec.md` 「KsSettingsView の公開 API」/「SettingsRootStore（Android）」。選別基準: 能力・コンポーネント境界を越えて影響する / 将来の決定を制約する
- Store の現在値を StateFlow、部分変更を replay なしの SharedFlow として分離し、購読開始前の状態はスナップショットから初期化する — 出典: `openspec/specs/settings-view-android-host/spec.md` Purpose / 「Store バインドでの初期化」/「SettingsRootStore（Android）」。選別基準: 能力・コンポーネント境界を越えて影響する / 将来の決定を制約する
- 外部 Cell の描画拡張を中央レジストリで受け付け、未登録型を厳格モードでは失敗、非厳格モードでは空の代替表示へ退避する — 出典: `openspec/specs/settings-view-android-host/spec.md` 「Cell レジストリ」、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt`。選別基準: 能力・コンポーネント境界を越えて影響する / 将来の決定を制約する

## drift 所見

- Theme は構造 Diff を通らないという同一 spec 内の MUST と実装に対し、「DiffUtil 差分検出」の Theme 更新 Scenario は削除済みの `SettingsRootDiff.UpdateTheme` を直接適用すると記述している (`openspec/specs/settings-view-android-host/spec.md` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`、`KsSettingsView.kt`)。
- Theme プロパティ更新 Scenario は背景色の入力を旧名 `viewBackgroundColor` と記述しているが、現行 Theme とホスト実装は `backgroundColor` を使用する (`openspec/specs/settings-view-android-host/spec.md` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`、`KsSettingsView.kt`)。
- Cell 登録 Scenario の `viewType = 1` は Section ヘッダ用の予約値と衝突する。実装は Cell 用に 100 以上を推奨する一方、登録時に予約値を拒否しないため、この例のままでは専用描画器へ到達しない (`openspec/specs/settings-view-android-host/spec.md` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt`、`KsSettingsListAdapter.kt`)。
- spec の「DEBUG / Release」シナリオはビルド種別で欠落 ID や未登録 Cell の挙動が決まるように読めるが、実装は利用アプリが明示設定する共通の `strictMode` で切り替え、既定値は常に厳格である (`openspec/specs/settings-view-android-host/spec.md` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt`、`KsSettingsView.kt`)。
- 実装には複数 Cell を一つの内容更新世代として扱う公開操作とバッチ通知があり、Radio グループの取りこぼし防止をテストしているが、Store の公開メソッド一覧と Scenario にこの契約がない (`openspec/specs/settings-view-android-host/spec.md` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt`)。
- アーキテクチャ対応表は Android の Section H/F を ConcatAdapter の先頭・末尾 Adapter としているが、実装の先頭・末尾 Adapter は Root H/F 専用で、Section H/F は中央の平坦リストに含まれる (`docs/architecture.md` §6 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt`、`CellListItem.kt`)。
- Android 利用ガイドの Store 例と主要メソッド一覧は Cell 挿入位置の名前付き引数を `index` としているが、公開実装は `at` である (`docs/platform-guide-android.md` §11 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`)。

## 用語

- ホスト層: Core の設定モデルをプラットフォーム固有の描画基盤へ接続し、更新とライフサイクルを統合する層。
- 完全モデル: 非表示の Section / Cell も保持する、状態の正本となる設定ツリー。
- visible projection: 完全モデルから現在描画すべき要素だけを抽出した派生リスト。
- 構造同期: ID の追加・削除・移動・種別変更をリスト構造へ反映する更新分類。
- 内容更新: 同一 ID の表示内容を、表示項目を再生成せずに反映する更新分類。
- 可視性変化: 完全モデルを保持したまま visible projection への出入りとして扱う更新分類。
- 安定 ID: 表示内容に依存せず、同じ論理項目へ継続して割り当てられる識別値。
- バッチ内容更新: 連動する複数 Cell の内容変更を一つの更新世代として反映する通知。
- 厳格モード: 欠落 ID や未登録描画型を即時失敗として扱い、統合漏れを早期検出する診断方針。

## 抽出メモ

- 「表示状態同期の三分類」は Android 固有ではなく iOS、Core、宣言的ラッパにもまたがるため、統合時は `settings-view-core` および各プラットフォーム host / wrapper の候補と合流させ、architecture/ の単一概念にするのが自然。
- 「Android Store のスナップショットと変更通知」は iOS Store と上位原則を共有する可能性が高い。一方、バッチ内容更新と `StateFlow` / `SharedFlow` の選択は Android 固有なので、共通概念と platforms/ の実装契約を分ける余地がある。
- 「Android 設定画面ホスト境界」と「Android Cell 描画の拡張境界」は同じ capability 由来だが、後者は利用アプリが直接依存する公開拡張面で寿命と変更影響が異なるため、別概念候補とした。
- Root H/F 更新が永続スナップショットに保持されず replay なし通知だけで伝わる点はコードで確認できるが、再 bind 時の期待挙動が spec から確定できない。概念化時は不変条件へ昇格せず、設計意図のオーナー確認が必要。
- detach 後もホスト自身は中央 Adapter のフィールドを保持し、切れるのは RecyclerView 側の参照である。「ListAdapter の参照を解放」という spec 文言が RecyclerView からの参照だけを意味するかは不明のため、drift 件数には含めず要確認事項とした。
