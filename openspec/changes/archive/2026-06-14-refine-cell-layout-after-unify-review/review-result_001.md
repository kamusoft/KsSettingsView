# レビュー結果 - refine-cell-layout-after-unify-review

**レビュー日時**: 2026年06月13日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-cell-layout-after-unify-review

---

## サマリー

本変更提案は、直前 change `unify-cell-common-fields-via-shared-row-layout` のオーナーレビュー後に明らかになった、iOS / Android の共通行レイアウトに関する 6 つの見た目側の指摘事項を 1 つにまとめた「最終調整 change」である。

実装内容を確認した結果、以下のとおり高品質な実装となっており、仕様の MUST / MUST NOT を完全に満たしている。

- **iOS**: `hintLabel.trailingAnchor` を `cell.contentView.trailingAnchor` → `cell.trailingAnchor` に変更し、オリジナル `AiForms.Maui.SettingsView` の `_HintLabel.RightAnchor=this.RightAnchor-10` と完全に整合。`Theme.hasUnevenRows` のデフォルトを `true` に変更し「Auto 高さ + 下限保証」既定挙動を確立した。
- **Android**: `iconMarginEnd` を 8dp → 16dp に拡大、`titleView` / `descriptionView` を vertical chain (`CHAIN_PACKED`, `verticalBias = 0.5f`) で本体行 packed 配置に変更し、`accessoryHolder`（縦中央）と整合した。`Theme.hasUnevenRows` も同様に `true` 既定化した。
- **サンプル**: Android 側のアイコンを `android.R.drawable.*` から Material Symbols Outlined の vector drawable に置換、`RadioCell` に `hintText = "推奨"` を追加した。

**テスト結果**:
- Android (`./gradlew :ks-settingsview-ui:test --rerun-tasks`): 290 件全件成功 (`successful` / failures = 0)
- iOS (`xcodebuild test -scheme KsSettingsView-Package`): 237 件全件成功 (failures = 0)
- `openspec validate refine-cell-layout-after-unify-review --strict`: `Change 'refine-cell-layout-after-unify-review' is valid`

**特筆事項**:
- Phase 1〜2（iOS）の AutoLayout 制約変更が `test_hintLabelのmaxXは全Cell種別で_cellRight_minus10と一致する` で accessory あり (`SwitchCell`) / なし (`ButtonCell` 相当) の両方で実測検証されている点が秀逸。仕様の Scenario「hintLabel が accessory のある cell でも cell 右端基準で配置される」を厳密にカバーしている。
- Phase 4（Android）の vertical chain 配置が、`description が GONE のとき titleView は縦中央付近に配置される` / `description が VISIBLE のとき title と description は本体行縦中央付近に配置される` / `valueText は title のベースラインに揃って同じ縦位置に配置される` の 3 件で Robolectric measure 経由の実測テストとなっており、`CHAIN_PACKED` + GONE chain member スペース 0 の挙動が厳密に保証されている。
- `Theme.hasUnevenRows` のデフォルト値変更に伴う既存テストの更新も網羅的に行われており、固定高さを期待する箇所では明示的に `Theme(hasUnevenRows: false)` を渡す形に修正されている。

**判定**: **APPROVED** — マージ可能。

---

## 指摘事項

### Critical

なし。

### Major

なし。

### Minor

なし。

### Suggestion

#### 🔵 Suggestion 1: Material Symbols drawable は 11 個に絞ったがタスクのリストは 14 個

**該当箇所**: `openspec/changes/refine-cell-layout-after-unify-review/tasks.md:58-60` / `samples/android/app/src/main/res/drawable/ic_*.xml`

**問題点**:
tasks.md 6.1 の「最低限のリスト」は `notifications` / `wifi` / `description` / `light_mode` / `dark_mode` / `brightness_auto` / `email` / `calendar_today` / `send` / `logout` / `account_circle` / `settings` / `lock` / `notifications_off` の 14 個。実装では実際に `UnifyCellCommonFieldsDemoScreen` / `BasicCellsDemoScreen` で使われる 11 個（上記 14 個から `logout` / `settings` / `lock` / `notifications_off` を除き、`storage` を追加）のみが drawable として追加されている。

**評価**:
tasks.md には「最低限のリスト」とあり、サンプルで使用しないアイコンを追加しても無意味（apk サイズだけが増える）ため、実装の判断は合理的。design.md の Risks セクションでも「1 個あたり ~1KB を 10〜15 個追加」と幅を持たせており、11 個は妥当な範囲内。修正不要だが、tasks.md チェックリストの 6.1 を完了 (`[x]`) にしていることは、「最低限の必要分を網羅した」という解釈で受容可能。

**推奨修正**:
（任意）将来のメンテナンス時に「使われないアイコンを追加した」と勘違いされないよう、tasks.md の 6.1 末尾に「※ サンプルで実際に使用するアイコンに限定する」旨を追記しても良い。本 change の archive はそのままで問題ない。

---

## アクションプラン

優先度順に対応すべき項目は **なし**。

参考程度の対応として:
1. （任意）tasks.md 6.1 の補足コメントを追加（Suggestion 1）

---

## レビュー観点別チェック結果

### 正確性・機能性
- [x] openspec の仕様・要件を正しく満たしている（`openspec validate --strict` で VALID 確認済み）
- [x] proposal.md / design.md / tasks.md / spec delta を勝手に書き換えていない
- [x] tasks.md の 45 項目すべて完了 `[x]`
- [x] 未実装にも関わらず誤って完了になっている項目なし
- [x] iOS `hintLabel` の Trailing 制約が `cell.trailingAnchor` 基準に変更され、accessory あり/なし両方で `frame.maxX == cell.bounds.maxX - 10` が実測テストで保証されている
- [x] Android vertical chain (CHAIN_PACKED, bias 0.5) が description GONE / VISIBLE 両ケースで Robolectric テスト経由で保証されている
- [x] エラーハンドリングは既存 `applyCellBaseLayout` の null 分岐をそのまま継承しており適切

### テスト容易性
- [x] iOS は `@MainActor` + `TestableKsListCell` でテスト用 subclass を用意し、`prepareForReuse` / `hintLabel` 取得をテスト可能にしている
- [x] Android は Robolectric + `ContextThemeWrapper(Theme_Material3_Light_NoActionBar)` でテーマ依存の MaterialSwitch 初期化を解決
- [x] 時刻ソースの直接参照なし（変更スコープ的に時刻 API は無関係）

### セキュリティ
- [x] 入力値バリデーション: 文字列フィールドのみで N/A
- [x] 認証・認可: N/A
- [x] 機密情報のハードコードなし

### パフォーマンス
- [x] iOS `hintLabel` は lazy 生成され、prepareForReuse で text/isHidden のみリセット（subview は保持）でリサイクル時の再生成コストを削減
- [x] Android `applyEffectiveHeight` は変更検知して `requestLayout()` 抑制
- [x] Android `hintTextView` の Z 順は `addView` 順序で静的保証し、毎 bind の `bringToFront()` を避けてパフォーマンスを維持（コメントで明示）

### 可読性・保守性
- [x] 命名は仕様文言と一致（`hasUnevenRows` / `verticalBias` / `iconMarginEnd` 等）
- [x] `CellBaseLayout.kt` / `KsListCellBase.swift` の doc コメントが本 change の意図（オリジナル AiForms 踏襲、accessory 影響を受けない右上 float）を明示

### 一貫性
- [x] 既存の `CellStyle → Theme → 既定` 解決順序を維持
- [x] vertical chain への変更後も `valueTextView` は `titleView.BASELINE` 紐付けで title 行右寄せという既存設計を踏襲

### 多言語対応
- [x] 文字列リソースの追加なし（spec 変更が論理スタイルのみ）

### テスト
- [x] 全テスト成功（Android 290 / iOS 237）
- [x] spec の Scenario に対応するテストが実装されている:
  - iOS: `test_hintLabelのmaxXは全Cell種別で_cellRight_minus10と一致する`
  - Android: `description が GONE のとき titleView は縦中央付近に配置される` / `description が VISIBLE のとき title と description は本体行縦中央付近に配置される` / `valueText は title のベースラインに揃って同じ縦位置に配置される`
- [x] スタブ未使用（MockK / Robolectric の正規 API のみ使用）
- [x] テストカバレッジは仕様 MUST に対して十分

---

## 判定結果

**ステータス**: ✅ **APPROVED**

Critical / Major 指摘なし。Minor 指摘もなし。Suggestion 1 件は任意の助言で本 change のマージを阻害しない。

すべての仕様要件を満たし、テストが網羅的で全件成功、`openspec validate --strict` も VALID。`unify change` の見た目側の最終調整 change として完成度が高く、後続 change（`add-cell-types-input` / `add-cell-types-custom` 等）の安全な前提基盤として archive 可能。
