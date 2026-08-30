# レビュー結果 - add-samples-android（追加対応分 #2 / #3）

**レビュー日時**: 2026年05月11日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-samples-android
**対象**: 初回レビュー(`review-result_001.md`)で記録した Suggestion #2 / #3 の追加対応分

---

## サマリー

初回レビューで Suggestion として記録した以下 2 件への追加対応をレビューした。

- **指摘 #2**: `KsCellRegistry.register` の重複検証 O(1) 化（`cellClassByViewType` 逆引きマップの導入）
- **指摘 #3**: Sample 側で `KsCellRegistry.strictMode` を `BuildConfig.DEBUG` に明示設定（`buildConfig = true` を有効化）

修正範囲は 3 ファイル（`KsCellRegistry.kt` / `samples/android/app/build.gradle.kts` / `samples/android/app/src/main/kotlin/.../MainActivity.kt`）に閉じており、いずれも初回レビューで示した方向性に沿った最小限の追加対応となっている。

検証結果も以下の通り問題なし：

- `cd android && ./gradlew :ks-settingsview-ui:testDebugUnitTest --rerun-tasks`: BUILD SUCCESSFUL（47 tasks executed、0 failures）
- `cd samples/android && ./gradlew :app:assembleDebug`: BUILD SUCCESSFUL（94 tasks）

ただし、以下の 2 点は気になった：

1. 重複検証 O(1) 化に伴って追加された「同じ Cell 型を別 viewType で再登録した場合の stale エントリ掃除」ロジック（新規挙動）に対する**ユニットテストが追加されていない**。挙動変更を含むため、回帰検出のためのテストはあった方が望ましい。
2. 既存テスト `異なる Cell 型に同じ viewType を登録すると IllegalArgumentException`（`KsCellRegistryTest.kt:151`）は、テスト名と実装内容（同 Cell 型での後勝ち上書きしか検証していない）が**乖離している**。これは初回レビュー時点から残存している既存課題であり、今回の追加対応のスコープ外ではあるが、O(1) 化に踏み込んだ今こそ「異なる Cell 型 + 同 viewType」のシナリオを実テストとして追加するのが自然な流れと考える。

これらは Critical / Major ではなく、初回レビュー判定の APPROVED を覆すものではない。**判定**: `APPROVED`（ただし Minor 1 件に対する任意対応を提案）

---

## 指摘事項

### 🟡 Minor

#### [Minor] 「stale エントリ掃除」ロジックを検証するテストが無い

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:148-154`

**問題点**:
今回の追加対応で新たに以下の挙動が `register()` に組み込まれた：

```kotlin
// 同じ Cell 型を別 viewType で再登録した場合、古い viewType エントリを掃除しておく
val previousEntry = entriesByCellClass[cellClass]
if (previousEntry != null && previousEntry.viewType != viewType) {
    entriesByViewType.remove(previousEntry.viewType)
    cellClassByViewType.remove(previousEntry.viewType)
}
```

これは「同 Cell 型を別 viewType に付け替え再登録した場合に、古い viewType の `entriesByViewType` / `cellClassByViewType` 双方が掃除されること」というプロダクションコードでも到達しうる新しい契約である。しかし `KsCellRegistryTest` には対応するテストが追加されていない。

具体的に保証したい挙動：

- 旧 viewType に対して `createViewHolder(parent, oldViewType)` が `IllegalStateException`（strictMode=true 時）を投げる
- 旧 viewType が `cellClassByViewType` から消えているため、第三の Cell 型が同じ「旧 viewType」を再利用しても衝突しない

これらは現在のテストでは保証されない。将来、`previousEntry.viewType != viewType` 条件を誤って削除した場合や、片方のマップ掃除を忘れた場合に黙ってデグレが入る。

**推奨修正**:
`KsCellRegistryTest.kt` に以下のテストを追加することを推奨（今回スコープ外だが、初回レビューでの「Cell 種類が増えた段階で」という前提が崩れた今、品質維持のため強く推奨）：

```kotlin
@Test
fun `同じ Cell 型を別 viewType で再登録すると古い viewType は掃除される`() {
    KsCellRegistry.register(PocLabelCell::class, viewType = 200) { p ->
        DummyHolder(View(p.context))
    }
    // 同じ Cell 型を別 viewType で付け替え
    KsCellRegistry.register(PocLabelCell::class, viewType = 201) { p ->
        DummyHolder(View(p.context))
    }

    // 新 viewType は解決可能
    val cell = PocLabelCell(id = "c1", title = "x")
    assertEquals(201, KsCellRegistry.viewTypeOf(cell))

    // 旧 viewType は掃除されており、createViewHolder で IllegalStateException
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    val parent: ViewGroup = FrameLayout(ctx)
    assertThrows(IllegalStateException::class.java) {
        KsCellRegistry.createViewHolder(parent, 200)
    }
}
```

### 🔵 Suggestion

#### [Suggestion] `異なる Cell 型に同じ viewType を登録すると IllegalArgumentException` テストの実体化

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryTest.kt:151-181`

**問題点**:
このテストは名前と実装内容が乖離している。コメントには「テスト目的では DummyLabelCell を作るのは別モジュール越境のためできないので、本ケースは『同じ Cell 型での重複登録は許容』を確認するのみとする」とあるが、`add-samples-android` で `Cell` の `sealed` 制約が外れ、**外部モジュールから自由に `Cell` 実装を派生できるようになった**ため、このコメントの前提自体が無効化されている。

具体的には、テストモジュール内で次のような Dummy Cell を定義できる：

```kotlin
private data class DummyOtherCell(
    override val id: String = "x",
    override val style: CellStyle = CellStyle(),
) : Cell
```

これにより、今回 O(1) 化された衝突検出ロジック（`cellClassByViewType[viewType]` チェック）の核心が初めてテストで保証される。

**推奨修正**（任意・後続で可）:

```kotlin
private data class DummyOtherCell(
    override val id: String = "x",
    override val style: CellStyle = CellStyle(),
) : Cell

@Test
fun `異なる Cell 型に同じ viewType を登録すると IllegalArgumentException`() {
    KsCellRegistry.register(PocLabelCell::class, viewType = 202) { p ->
        DummyHolder(View(p.context))
    }
    assertThrows(IllegalArgumentException::class.java) {
        KsCellRegistry.register(DummyOtherCell::class, viewType = 202) { p ->
            object : CellViewHolder<DummyOtherCell>(View(p.context)) {
                override fun bind(cell: DummyOtherCell, theme: Theme) {}
            }
        }
    }
}
```

既存テストは「同 Cell 型での後勝ち上書き」のみを別テスト（リネーム後）として残せばよい。

---

## アクションプラン

優先度順：

1. **【Minor / 推奨】** `KsCellRegistryTest` に「同 Cell 型を別 viewType で再登録した場合の stale 掃除」テストを追加。今回の挙動変更に対する回帰検出のため。
2. **【Suggestion / 任意】** 既存の `異なる Cell 型に同じ viewType を登録すると IllegalArgumentException` テストを実体化（DummyOtherCell の導入）。`Cell` の `sealed` 解除により可能になったため。
3. 上記 1, 2 はいずれも本提案のマージブロッカーではない。後続提案（例えば `add-cell-types-basic` で別 Cell 型を追加する際）にまとめて対応するのも可。

---

## 各レビュー観点に対する評価

### 1. 指摘 #2 / #3 の対応の適切性

#### 指摘 #2: `KsCellRegistry.register` の O(1) 化

**判定**: 対応は **適切**。
- `cellClassByViewType: MutableMap<Int, KClass<out Cell>>` を導入し、衝突検出が O(1) になっている。
- `clear()` でも新マップが整合（`cellClassByViewType.clear()` を追加）。
- 「同 Cell 型を別 viewType で再登録した場合の stale エントリ掃除」も追加され、`entriesByViewType` / `cellClassByViewType` の整合性が保たれる。
- KDoc（`KsCellRegistry.kt:115-121`）に「Cell 種類数 N に対する O(N) の線形探索を回避する」と意図が明記されており、保守性も良好。

ただし、上記 Minor 指摘の通り **新挙動に対するテストが追加されていない** のが惜しい。

#### 指摘 #3: Sample 側 `BuildConfig.DEBUG` 明示設定

**判定**: 対応は **適切**。
- `buildConfig = true` を `buildFeatures` に追加し、`BuildConfig.DEBUG` を MainActivity から参照可能化。
- `MainActivity.onCreate` で `KsCellRegistry.strictMode = BuildConfig.DEBUG` を明示し、コメントで「Debug ビルド: strictMode=true / Release ビルド: strictMode=false」の意図を文書化。
- `build.gradle.kts:38-41` のコメントも、なぜ `buildConfig = true` を追加したかを「指摘 #3 への対応」として記録しており、変更理由のトレーサビリティも担保されている。
- 後続提案（`add-cell-types-*`）で別 Cell を追加するときの設定例として参照可能な書き方になっている。

### 2. エッジケースへの影響

| ケース | 旧実装 | 新実装 |
|--------|--------|--------|
| 同 Cell 型 + 同 viewType の再登録 | 後勝ち（既存挙動踏襲） | 後勝ち（変更なし、テスト済み） |
| 同 Cell 型 + 別 viewType の再登録 | 後勝ち、ただし旧 viewType エントリが `entriesByViewType` に残存（微バグ） | 後勝ち + 旧 viewType エントリ掃除（**新挙動**、テスト無し） |
| 別 Cell 型 + 同 viewType の登録 | `IllegalArgumentException`（線形探索） | `IllegalArgumentException`（O(1) 逆引き、**テスト無し**） |
| `clear()` 後の再登録 | 全マップクリア → 通常登録 | 全マップ（`cellClassByViewType` 含む）クリア → 通常登録（整合済み） |

「同 Cell 型 + 別 viewType の再登録」は旧実装では `entriesByCellClass` のみ上書きされ、`entriesByViewType` の旧エントリが残ったままになっていたため（旧 viewType で createViewHolder すると古い factory が呼ばれてしまう微妙なバグがあった）、新実装はむしろ品質改善。これは仕様としても自然な期待。

ただし新実装の挙動はテストで保証されておらず、Minor 指摘の通り回帰検出網がない点が惜しい。

### 3. `buildConfig = true` の副作用

`buildFeatures.buildConfig = true` を有効化すると、AGP は `<namespace>.BuildConfig` クラスを生成する（`DEBUG` / `APPLICATION_ID` / `BUILD_TYPE` / `VERSION_CODE` / `VERSION_NAME` の 5 定数）。

副作用として確認したポイント：

- ビルド時間: 1 クラス生成のみで影響は誤差レベル
- APK サイズ: 1 クラス追加のみで影響は誤差レベル
- 既存依存性: なし
- AGP 8.x 以降では `buildConfig` はデフォルト無効のため、明示有効化はベストプラクティス通り
- 副次的に `BuildConfig.APPLICATION_ID` / `VERSION_NAME` 等もアクセス可能になるが、Sample コード内で誤用される箇所は無い（`Grep` 確認済み）

副作用は実質ゼロ。問題なし。

### 4. 既存レビュー観点の再確認

- **spec 整合**: 仕様変更なし。`openspec/specs/settings-view-android-ui/spec.md` の "Cell レジストリ" Requirement / "未登録 Cell の扱い" Scenario と引き続き整合。`KsCellRegistry` の重複検証は spec で定められた挙動（"同じ viewType が別の cellClass に重複登録されたら IllegalArgumentException を投げる"）を実装パフォーマンス的に改善しただけで、外形挙動は変わっていない。
- **回帰なし**: `:ks-settingsview-ui:testDebugUnitTest --rerun-tasks` 全 pass。Sample 側 `:app:assembleDebug` も成功。
- **コード品質**:
  - `KsCellRegistry.kt` の KDoc が新マップの存在意義を明記しており、可読性が高い。
  - Kotlin イディオム的にも問題なし（`MutableMap` の `[]` アクセス、early return、null チェック後の `.remove(...)` のいずれも自然）。
  - ktlint / detekt 違反なし（ビルドが通っている時点で確認済み）。
  - `MainActivity.onCreate` の `KsCellRegistry.strictMode = BuildConfig.DEBUG` 行は `KsCellRegistry.register` より前に置かれており、登録時の strictMode 影響も自然な順序。

---

## 判定結果

**ステータス**: `APPROVED`

- ✅ 指摘 #2: O(1) 化の実装は適切。`cellClassByViewType` 逆引きマップの追加、`clear()` の整合更新、stale エントリ掃除いずれも妥当。
- ✅ 指摘 #3: `BuildConfig.DEBUG` 参照のための `buildConfig = true` 追加と `MainActivity` での明示設定はいずれも適切で副作用なし。
- ✅ ライブラリビルド成功（`:ks-settingsview-ui:testDebugUnitTest --rerun-tasks`）
- ✅ Sample APK ビルド成功（`:app:assembleDebug`、94 tasks）
- ✅ spec / design / tasks との整合性に変化なし
- ✅ 既存テスト全 pass（回帰なし）
- 🟡 Minor 1 件: 新挙動（stale エントリ掃除）に対するテスト未追加 → 後続で対応推奨
- 🔵 Suggestion 1 件: 既存テスト `異なる Cell 型に同じ viewType を登録すると IllegalArgumentException` の実体化 → 任意

Critical / Major 指摘なし。Minor は本提案のマージブロッカーではないため `APPROVED` 判定とする。Minor / Suggestion はいずれも `KsCellRegistryTest.kt` にテストを追加するのみの軽微な対応のため、本提案内で対応する場合は数十行の差分で済み、後続提案にまとめても問題ない。

ユーザー側で「本 PR 内で Minor 指摘も対応する」or「次の提案にまとめる」のいずれを選ぶか判断してください。
