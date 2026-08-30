# レビュー結果 - add-samples-android（追加対応分 #3 / テスト追補）

**レビュー日時**: 2026年05月11日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-samples-android
**対象**: `review-result_002.md` で残存していた Minor 1 件 / Suggestion 1 件（いずれも `KsCellRegistryTest.kt` のテスト追補）への対応分

---

## サマリー

`review-result_002.md` で残置となっていた以下 2 件のテスト指摘について、`sdd-implementer` による追加対応を確認した。

- **Minor**: 「同じ Cell 型を別 viewType で再登録すると古い viewType は掃除される」回帰テストの追加
- **Suggestion**: 「異なる Cell 型に同じ viewType を登録すると IllegalArgumentException」テストの実体化（`DummyOtherCell` / `DummyOtherHolder` 導入）+ 「同 Cell 型での後勝ち上書き挙動」を別テストに分離

修正範囲は `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryTest.kt` 1 ファイルに完全に閉じている（`KsCellRegistry.kt` / `samples/android/app/build.gradle.kts` / `MainActivity.kt` の差分は前回レビュー対象である review-result_002 時点の差分そのままで、本サイクルでの追加変更なし）。

検証結果も以下の通り問題なし：

- `cd android && ./gradlew :ks-settingsview-ui:testDebugUnitTest --rerun-tasks`: BUILD SUCCESSFUL（47 tasks）
  - `KsCellRegistryTest`: tests=10, failures=0, errors=0, skipped=0
  - 新規追加 2 テスト（実体化分・stale 掃除分）の実行も確認済み
- `cd android && ./gradlew test`: BUILD SUCCESSFUL（166 tasks、全モジュール）
- `cd samples/android && ./gradlew :app:assembleDebug`: BUILD SUCCESSFUL（94 tasks）

`review-result_002.md` で「マージブロッカーではないが品質維持のため強く推奨」とした 2 件が、いずれも推奨内容そのままに、しかも前回レビューで提案したコード例にほぼ準拠した形で正しく実装されている。

**判定**: `APPROVED`（archive 可能状態に到達）

---

## 指摘事項

### Critical / Major / Minor

なし。

### 🔵 Suggestion

#### [Suggestion] テスト 3 ステップ目のローカル変数名を統一すると微小に可読性が上がる

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryTest.kt:277`

**問題点**:
新規追加テスト「同じ Cell 型を別 viewType で再登録すると古い viewType は掃除される」内で、ファクトリ引数名が `parent`（前 2 ステップ）と `p`（3 ステップ目）で混在している。挙動には影響しないが、同一テスト関数内でのスタイル統一として `parent` に揃えると読みやすい。

```kotlin
// 現状
KsCellRegistry.register(
    cellClass = DummyOtherCell::class,
    viewType = 200,
) { p ->                              // ← ここだけ p
    DummyOtherHolder(View(p.context))
}
```

**推奨修正**（任意・Optional）:
ラムダ引数を `parent` に揃える。完全に好みの範囲のため、対応不要でも問題ない。

```kotlin
KsCellRegistry.register(
    cellClass = DummyOtherCell::class,
    viewType = 200,
) { parent ->
    DummyOtherHolder(View(parent.context))
}
```

---

## アクションプラン

優先度順：

1. **【Suggestion / 任意】** 上記 Suggestion 1 件（ラムダ引数名の統一）。スタイル統一目的。本提案内で対応しても、放置しても、後続提案でまとめて整えても問題ない。
2. 上記対応の有無に関わらず、本提案は archive 可能状態に到達している。

---

## 各レビュー観点に対する評価

### 1. `review-result_002.md` の 2 件指摘の解消状況

#### Minor: 「stale エントリ掃除」回帰テストの追加

**判定**: **完全に解消**。

新規テスト `同じ Cell 型を別 viewType で再登録すると古い viewType は掃除される`（`KsCellRegistryTest.kt:246-281`）が追加され、前回レビューで「具体的に保証したい挙動」として挙げた 3 点すべてを 1 テスト内で検証している：

1. 新 viewType で `viewTypeOf` が正しく `201` を返すこと（`assertEquals(201, KsCellRegistry.viewTypeOf(cell))`）
2. 旧 viewType (200) に対する `createViewHolder` が `IllegalStateException` を投げること
3. 旧 viewType (200) が `cellClassByViewType` から消えているため、別 Cell 型 `DummyOtherCell` で旧 viewType を再利用しても衝突せず登録でき、`viewTypeOf` で `200` を返すこと

特に 3 点目は、`KsCellRegistry.register` の `cellClassByViewType.remove(previousEntry.viewType)`（`KsCellRegistry.kt:153`）の片方マップ掃除を将来誰かが誤って削除した場合に、即座に `IllegalArgumentException`（"viewType 200 is already registered for ..."）でテストが落ちる回帰検出網になっている。前回レビュー指摘の意図を漏れなく満たしている。

KDoc コメント（`KsCellRegistryTest.kt:232-244`）も、保証対象の 3 挙動を明記しており保守性が高い。

#### Suggestion: 「異なる Cell 型に同じ viewType を登録すると IllegalArgumentException」テストの実体化

**判定**: **完全に解消**。

- `DummyOtherCell`（`KsCellRegistryTest.kt:55-58`）と `DummyOtherHolder`（同 63-65）が `private` クラスとしてテスト内に定義された。`Cell` の `sealed` 解除を活かした実装で、前回レビュー時点の推奨どおりの形になっている。
- 既存テスト `異なる Cell 型に同じ viewType を登録すると IllegalArgumentException`（同 178-195）は、`PocLabelCell` で viewType=202 を登録した後に `DummyOtherCell` で同 viewType=202 の再登録を試み、`IllegalArgumentException` を期待するという、テスト名に完全一致する内容に書き換えられた。これにより `KsCellRegistry.register` の `cellClassByViewType[viewType]` 衝突検出パスが核心レベルでテスト保証されるようになった。
- 「同 Cell 型での後勝ち上書き」挙動は、新規テスト `同じ Cell 型 + 同じ viewType の再登録は後勝ちで上書きされる`（同 204-230）に分離された。テストの名前・コメント・検証内容（`newFactoryCalled` フラグでファクトリ呼出しを確認）が一致しており、テスト責務分離の観点からも適切。

旧テストにあった『DummyLabelCell を作るのは別モジュール越境のためできない』という言い訳コメント（前 review-result_002 で問題視したもの）は完全に削除された。

### 2. テストの実装意図と検証内容の一致

| テスト名 | 検証内容 | 一致度 |
|----------|----------|--------|
| `異なる Cell 型に同じ viewType を登録すると IllegalArgumentException` | `PocLabelCell` 登録後、`DummyOtherCell` で同 viewType 再登録が `IllegalArgumentException` | ✅ 完全一致 |
| `同じ Cell 型 + 同じ viewType の再登録は後勝ちで上書きされる` | `PocLabelCell` を viewType=202 で 2 回登録し、後者の factory が呼ばれることを `newFactoryCalled` で確認 | ✅ 完全一致 |
| `同じ Cell 型を別 viewType で再登録すると古い viewType は掃除される` | 新 viewType 解決 + 旧 viewType `IllegalStateException` + 別 Cell 型による旧 viewType 再利用成功 | ✅ 完全一致（3 ステップとも保証） |

テスト名・コメント・assert 内容のすべてが整合しており、テスト名と実装の乖離は解消された。

### 3. テスト品質の観点

- **手抜き実装なし**: 旧テストにあった「言い訳コメント + 別の事象を確認」のような実質スキップは完全に解消され、テスト名通りの検証が行われている。
- **スタブ未使用**: `DummyHolder` / `DummyOtherHolder` は `bind` を `no-op` 実装する Test Double であり、レジストリ機構の挙動検証では本質的な振る舞いを変えないため、本テストの範囲では「実装すぎず・モック過多にもならない」適切な粒度の Fake である。
- **境界値・異常系**: 「同型・同 viewType 後勝ち」「異型・同 viewType IllegalArgumentException」「同型・別 viewType stale 掃除」の 3 軸で完全に組み合わせを満たしており、`KsCellRegistry.register` の分岐網羅も十分。
- **テスト隔離**: `tearDown` で `KsCellRegistry.clear()` 呼び出し済み（既存）であり、`clear()` 内では `cellClassByViewType.clear()` も含まれるため、新規追加テスト間でも状態漏れなし。

### 4. スコープ外変更の混入有無

`git diff` 結果から、本サイクル（review-result_002 から本レビュー時点まで）の変更は `KsCellRegistryTest.kt` 1 ファイルのみで、`KsCellRegistry.kt` / `samples/android/app/build.gradle.kts` / `MainActivity.kt` の差分は review-result_002 時点で確認済みのものから 1 行も増えていない。スコープ外変更の混入なし。

なお `git status` 上で 4 ファイル modified となっているが、これは前回レビュー対象（review-result_002）でレビュー済みの差分がまだ commit されていないことに由来し、本レビュー対象であるテスト追加分のみが今回の純粋な追加差分である。

### 5. spec / design / tasks との整合性

- `tasks.md` 全 35 タスクすべて `[x]` 完了で変動なし（review-result_002 時点と同一）。
- `proposal.md` / `design.md` / `specs/` の仕様変更なし（テスト追補のみのため当然）。
- `openspec/specs/settings-view-android-ui/spec.md` の "Cell レジストリ" Requirement に「同じ viewType を別 cellClass に重複登録したら `IllegalArgumentException` を投げる」と明記されており、今回追加された実体化テストはまさにこの仕様を実テストで保証するもの。仕様と実装とテストが今回 100% 揃った状態になった。

### 6. archive 可能状態への到達評価

| 観点 | 状態 |
|------|------|
| すべての tasks 完了 | ✅ 35/35 |
| ライブラリビルド成功 | ✅ `cd android && ./gradlew test` 166 tasks BUILD SUCCESSFUL |
| Sample APK ビルド成功 | ✅ `cd samples/android && ./gradlew :app:assembleDebug` 94 tasks BUILD SUCCESSFUL |
| 全テスト pass | ✅ `KsCellRegistryTest` 10/10、その他既存テスト全 pass |
| spec 整合 | ✅ 仕様変更なし、既存 spec と完全整合 |
| review-result_001 / 002 残存指摘 | ✅ Minor 1 件 / Suggestion 1 件いずれも今サイクルで完全解消 |
| 残存指摘 | 🔵 Suggestion 1 件（ラムダ引数名の統一・任意） |

archive 可能状態に到達している。

---

## 判定結果

**ステータス**: `APPROVED`

- ✅ `review-result_002.md` の Minor 1 件・Suggestion 1 件が **いずれも完全に解消**
- ✅ 追加テスト 2 件（stale 掃除回帰テスト + 異 Cell 型衝突実体化テスト）+ 同 Cell 型後勝ち分離テストの計 3 件が、前回レビュー推奨のコード例にほぼ忠実な形で正しく実装
- ✅ テスト名と実装内容が完全に一致（旧テストにあった「言い訳コメント + 別事象検証」型の手抜きは完全解消）
- ✅ `KsCellRegistry` の主要 3 経路（同型同 vt 後勝ち / 異型同 vt 拒否 / 同型異 vt stale 掃除）が漏れなくテストで保証
- ✅ ライブラリ全テスト pass（`KsCellRegistryTest` 10/10）+ 全モジュール `./gradlew test` BUILD SUCCESSFUL（166 tasks）
- ✅ Sample APK ビルド成功（94 tasks）
- ✅ スコープ外変更の混入なし（`KsCellRegistryTest.kt` 1 ファイルのみの純粋追加）
- ✅ spec / design / tasks との整合性に変化なし
- 🔵 Suggestion 1 件のみ残存（ラムダ引数名の `p` / `parent` 混在 → 統一推奨、任意）

Critical / Major / Minor 指摘なし。残存 Suggestion 1 件はテスト関数 1 か所のラムダ引数名のスタイル揺れに過ぎず、archive のブロッカーではない。

**本変更提案 `add-samples-android` は archive 可能状態に到達した**。次ステップとして `openspec archive add-samples-android` 等での archive 化を推奨する。Suggestion の対応は archive 前に対応してもよいし、後続提案（`add-cell-types-basic` 等）の中でついでに直してもよい。
