# レビュー結果 - add-settings-view-android-ui (再レビュー)

**レビュー日時**: 2026年05月10日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-settings-view-android-ui
**前回レビュー**: review-result_001.md (CHANGES_REQUESTED)

## サマリー

前回 `CHANGES_REQUESTED` とした 4 件の指摘（Major 1 + Minor 3）に対する修正をすべて確認した。総評は以下の通り。

| # | 前回指摘 | 重要度 | 対応 |
|---|----------|--------|------|
| 1 | 未登録 Cell のリリースビルド向けプレースホルダ ViewHolder フォールバック未実装 | 🟠 Major | **適切に解消** |
| 2 | `CellListItem` の用語不一致（spec の `sealed class` ↔ 実装 `sealed interface`、subtype 名 `Cell` ↔ `CellRow`） | 🟡 Minor | **spec / proposal / design / tasks を実装に合わせて統一済（実装に合わせる方針で正当）** |
| 3 | `KsColor.toColorInt` の 0.5 → 127 切り捨て（仕様例示 `0xFFFF8000` と齟齬） | 🟡 Minor | **`((v * 255) + 0.5).toInt()` の四捨五入に変更、`EffectiveStyleTest` で `0x80` を assert する形に更新** |
| 4 | `KsFont.toTypeface` の weight 4 値 → 2 値縮退 | 🟡 Minor | **`Typeface.create(base, numericWeight, italic)`（API 28+）で 4 値忠実反映に変更、`KsFontExtTest` 新設** |

ビルド・テストはすべて成功。`./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test --rerun-tasks` を実機で再走行し、`testDebugUnitTest` 配下 19 テストクラス・**95 テスト**を集計した結果は `failures=0, errors=0, skipped=0`。implementer 報告の件数（core 47 + ui 41 + compose 7）と一致する。

新規追加された `EmptyPlaceholderViewHolder` および `KsCellRegistry.strictMode` は、`KsSettingsListAdapter.onBindViewHolder` 側でも `if (holder is EmptyPlaceholderViewHolder) return` の早期 return が入り、CellRow に対する bind 経路で型キャスト失敗を起こさないよう適切に経路統合されている（リグレッション無し）。

仕様（spec / proposal / design / tasks）と実装の整合性も保たれており、用語訂正（`sealed interface CellListItem` / `CellRow`）が一貫して反映されている。下記「指摘事項」に **Suggestion 1 件** だけ残るが、いずれもブロッカーではない。

**判定**: `APPROVED`

---

## 指摘事項

### 🔵 Suggestion: KDoc コメント中の「`sealed class`」表現が 1 箇所だけ残置（実装は `sealed interface`）

**該当箇所**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellListItem.kt:10` — `「sealed class」で 3 つのサブタイプに分岐させる（Decision 1: 平坦リスト方式）。`
- `openspec/changes/add-settings-view-android-ui/design.md:166` — `「areItemsTheSame」で sealed class subtype をまず比較してから ID 比較する実装で、誤検出を防ぐ。`

**問題点**:
spec.md / proposal.md / tasks.md および `design.md:57` は「`sealed interface CellListItem`」に統一されている一方、上記 2 箇所のみ「sealed class」表記が残っている。

`CellListItem.kt:10` の KDoc は実装ファイル内のため特に違和感が出やすい（読者は次行で `internal sealed interface CellListItem` の定義を見る）。`design.md:166` は緩和策の記述で「sealed (class/interface) の subtype による比較」という Kotlin 慣用句的な表記とも読めるが、変更提案全体で実装に合わせる方針が確定した今、こちらも `sealed interface` に揃えるほうが一貫する。

**推奨修正**:

```kotlin
// CellListItem.kt:10 の修正例
- * `sealed class` で 3 つのサブタイプに分岐させる（Decision 1: 平坦リスト方式）。
+ * `sealed interface` で 3 つのサブタイプに分岐させる（Decision 1: 平坦リスト方式）。
```

```markdown
<!-- design.md:166 の修正例 -->
- - **緩和策**: `areItemsTheSame` で sealed class subtype をまず比較してから ID 比較する実装で、誤検出を防ぐ。
+ - **緩和策**: `areItemsTheSame` で sealed interface subtype をまず比較してから ID 比較する実装で、誤検出を防ぐ。
```

ブロッカーではないため、本変更提案 archive 前または別 OpenSpec change（`openspec sync` 系）で吸収してよい。

---

## 前回指摘事項の確認詳細

### 🟠 Major: 未登録 Cell のリリースビルド向けプレースホルダ ViewHolder（解消確認）

**確認内容**:
- `KsCellRegistry.strictMode: Boolean`（`@Volatile`、デフォルト `true`）が新設された。
- `VIEW_TYPE_PLACEHOLDER = 99` を新設し、`viewTypeOf` は `strictMode = false` 時に未登録 Cell に対し本値を返す。
- `createViewHolder` は `VIEW_TYPE_PLACEHOLDER` 渡し時 / 未登録 viewType + `strictMode = false` 時に `EmptyPlaceholderViewHolder.create(parent)` を返す。
- `KsSettingsListAdapter.onBindViewHolder` の `CellRow` 分岐に `if (holder is EmptyPlaceholderViewHolder) return` を追加（プレースホルダ ViewHolder の bind スキップ）。
- `KsCellRegistryTest` に **3 件**（`strictMode false 時の viewTypeOf` / `strictMode false 時の createViewHolder` / `VIEW_TYPE_PLACEHOLDER 直接渡し`）の新規テストが追加され、すべて成功（19 テストクラス内、KsCellRegistryTest = 8 件、failures=0）。

**評価**: 仕様 Scenario「未登録 Cell の扱い」のデバッグ／リリース両ビルドを完全に充足する実装。`@Volatile` で並行性も担保。`strictMode` のデフォルトを `true`（安全側に倒したデバッグ検出優先）とし、利用側（アプリ `Application#onCreate` 等）から `BuildConfig.DEBUG` に応じて切り替える方針も KDoc に明記されており、AAR 配布時の `BuildConfig` 分断問題を正しく回避している。**指摘解消**。

### 🟡 Minor: `CellListItem` の用語訂正（解消確認）

**確認内容**:
- `proposal.md:10`、`design.md:41/57`、`tasks.md:17`、`spec.md:21/27` が一貫して `sealed interface CellListItem` / subtype 名 `SectionHeader` / `CellRow` / `SectionFooter` に統一された。
- 実装 `CellListItem.kt:18` は `internal sealed interface CellListItem` で、subtype 名は `SectionHeader` / `CellRow` / `SectionFooter`。
- 命名理由（"`CellRow` は Core 側の `Cell` 型との衝突回避"）も spec / proposal / tasks の各箇所に明記。

**評価**: 用語訂正は概ね一貫しているが、`CellListItem.kt:10` の KDoc コメントと `design.md:166` の緩和策記述に「sealed class」表記が **2 箇所だけ** 残置されている（上記 Suggestion 参照）。これは実装そのものではなくドキュメンテーション層の残骸であり、機能・テストには影響しない。**Major→Minor→Suggestion へ降格**。

### 🟡 Minor: `KsColor.toColorInt` の四捨五入化（解消確認）

**確認内容**:
- `KsColorExt.kt:21-25` が `((v.coerceIn(0.0, 1.0) * 255.0) + 0.5).toInt()` の四捨五入丸めに変更された。
- `EffectiveStyleTest.kt:38` の assertion が `assertEquals(0x80, Color.green(argb))` に更新され、`assertEquals(0xFFFF8000.toInt(), argb)` で全体一致も確認している。
- 仕様 Scenario「KsColor から ColorInt」例示 `0xFFFF8000` と完全に一致する。

**評価**: 仕様例示と完全一致。Color 値域は非負（0.0–1.0）のため `+ 0.5` 方式の四捨五入で問題なし。**指摘解消**。

### 🟡 Minor: `KsFont.toTypeface` の weight 4 値忠実反映（解消確認）

**確認内容**:
- `KsFontExt.kt:29-42` が `Typeface.create(baseTypeface, numericWeight, false)`（API 28+ オーバーロード）を使うように変更された。
- `KsFontWeight.toCssNumericWeight()` 拡張関数が新設され、`REGULAR=400 / MEDIUM=500 / SEMIBOLD=600 / BOLD=700` の CSS 数値ウェイトに対応付ける。
- `KsFontExtTest.kt`（新設）が 3 件のテストでマッピング検証・各 weight の Typeface 生成・family 切替を検証。Robolectric では `Typeface.weight` getter が `0` を返す簡易実装なので値検証は省略しているが、API 呼び出しエラーゼロ＋4 値が distinct（`assertEquals(4, values.toSet().size)`）であることは保証されている。
- `minSdk = 29` のため API 28+ オーバーロードは常に利用可能（条件分岐不要）。

**評価**: 4 値縮退の解消。API 互換性も `minSdk = 29` 制約下で問題なし。Robolectric 環境制約は KDoc にも明記済みで、実機検証は `add-samples-android` の責務として分離されている。**指摘解消**。

---

## 新たな問題（リグレッション）の有無

`EmptyPlaceholderViewHolder` 追加に伴う `KsSettingsListAdapter` 側の経路変更（`is EmptyPlaceholderViewHolder` での早期 return）について以下を確認した。

- **CellRow 経路の型キャスト安全性**: `holder is EmptyPlaceholderViewHolder` を先に判定することで、`holder as CellViewHolder<Cell>` がプレースホルダ時に `ClassCastException` を起こさない。安全。
- **`onViewRecycled` 経路**: `EmptyPlaceholderViewHolder` は `CellViewHolder<*>` でも `SectionAnyViewAccessoryViewHolder` でもないため、`reset()` 呼び出し対象から自然と外れる。空 View なのでリセット対象も無く問題なし。
- **stable IDs**: `EmptyPlaceholderViewHolder` 経路は `getItemId` のロジックを変えないため、ID 衝突は発生しない（`CELL_ID_OFFSET = 100L` 以上、Root H/F 予約値 `1L` / `2L` と非衝突）。
- **テスト副作用**: `KsCellRegistryTest` の新規 3 ケースは、いずれも `try / finally` で `KsCellRegistry.strictMode = true` に戻しており、他テストへの副作用なし。`@After tearDown` の `clear()` も `strictMode` をデフォルト `true` にリセットする実装に更新されている。

**結論**: リグレッションなし。

## アクションプラン

優先度順：

1. **【任意・Suggestion】用語残骸（`sealed class`）の解消** — `CellListItem.kt:10` KDoc と `design.md:166` の 2 箇所だけ「sealed interface」に揃える。本変更提案 archive 前 / 後どちらでも問題ない。

## 判定結果

**ステータス**: `APPROVED`

**理由**:
- 前回指摘 Major 1 件・Minor 3 件すべてが適切に解消されており、対応するテスト追加・更新も伴っている（テスト 89 → 95 件、failures=0）。
- 修正に伴うリグレッションは見当たらず、`EmptyPlaceholderViewHolder` 経路は安全に統合されている。
- 残課題は KDoc / design.md の用語残骸 2 箇所（Suggestion）のみで、機能・仕様・テスト合格性には影響しない。
- 仕様（spec / proposal / design / tasks）と実装の整合性は概ね一貫しており、Source of Truth spec への sync を行う段階として十分な品質。

archive 段階に進めて問題ないと判断する。
