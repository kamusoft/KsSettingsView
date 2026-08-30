# レビュー結果 - add-cell-types-basic (§21 / Decision 9 追加修正・再レビュー)

**レビュー日時**: 2026年06月02日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-cell-types-basic
**スコープ**: review-result_002（NEEDS_DISCUSSION）の指摘を受けた追加修正の再レビュー。`Theme.cellAccentColor` 追加（Android core / iOS）・accent 着色リワイヤ（Android UI）・design.md 追補・前回 Suggestion 群の解消。

---

## サマリー

前回 NEEDS_DISCUSSION の本体であった「Decision 9-3 / 9-5 の accent 着色が design 記述どおりに実装できず title 色流用で着地していた」乖離が、ユーザー判断に基づく `Theme.cellAccentColor` の新設によって**正しく解消**された。Android / iOS の `Theme` に同名・同既定値（`#007AFF` 相当）でフィールドが対称に追加され、Android UI の Radio / SimpleCheck のチェック色は title 色流用を廃して `EffectiveStyle.accentColor`（`Theme.cellAccentColor` 由来）に切り替わっている。前回の Suggestion 4 件（誤コメント・KDoc・build.gradle・docs）はすべて解消済み。新たな Critical / Major の混入はなく、リグレッションも検出されなかった。

- ビルド: Android core / ui ともコンパイル成功。iOS swift build / test 成功。
- ユニットテスト:
  - Android `:ks-settingsview-core:testDebugUnitTest`（ThemeTest=6 含む全スイート） **全 PASS**（failures=0 / errors=0 / skipped=0）
  - Android `:ks-settingsview-ui:testDebugUnitTest`（BasicCellsTest=35 / EffectiveStyleTest=4 含む全スイート） **全 PASS**
  - iOS `swift test`（ThemeTests=6 含む 123 件） **全 PASS**（0 failures）
- `openspec validate add-cell-types-basic --strict`: **valid**

**判定: APPROVED**（実機目視検証タスク完了をアーカイブ条件とする前提）

理由: 前回 NEEDS_DISCUSSION の唯一の論点が設計どおりに解消され、Critical / Major はゼロ。残る指摘は Minor / Suggestion のみで優先度が低い。`settings-view-core` 仕様の Theme Requirement は必須フィールドを「最低限」と規定しており、フィールド追加は既存要件と矛盾しない（後述）。実機目視検証（21.5.3/21.5.4/21.7.2〜21.7.6）はコードレビューでは合否判定不能であり、ユーザー指定どおりアーカイブ条件として扱う。

---

## 確認観点ごとの所見

### 1. 前回 NEEDS_DISCUSSION 論点（accent 着色の design 乖離）の解消 — 解消済み

- `EffectiveStyle.kt:77` で `accentColor = theme.cellAccentColor.toColorInt()` を確定値として合成。
- `RadioCellViewHolder.kt:49` / `SimpleCheckCellViewHolder.kt:53` がともに `checkView.color = effective.accentColor` に変更され、title 色流用（`effective.titleColor`）は廃止。
- design.md にも「従来の title 色流用は廃止」（9-3 / line 188）と確定記述化されており、コードと design が一致。
- `BasicCellsTest`（line 370 / 386 / 397）に「Radio / SimpleCheck のチェックは `Theme.cellAccentColor` で着色」「accent 未指定時は既定 `cellAccentColor`」を検証する 3 テストが追加され、title 色（黒）流用でないこと（`assertNotEquals(Color.BLACK, ...)`）まで実証。

### 2. Theme への cellAccentColor 追加の対称性・spec 整合 — 妥当

- Android `Theme.kt:30`（`cellAccentColor`、既定 `DEFAULT_ACCENT_COLOR = KsColor(0.0, 0.478, 1.0, 1.0)`）と iOS `Theme.swift:27,75`（`cellAccentColor`、既定 `defaultAccentColor = KsColor(0.0, 0.478, 1.0, 1.0)`）は**フィールド名・既定値・KDoc/docコメントまで対称**。
- `openspec/specs/settings-view-core/spec.md:161` の Theme Requirement は「最低限、`separatorColor` …（既定 8 フィールド）… を含まなければならない (MUST)」と**下限規定**であり、追加フィールドを禁じていない。よって `cellAccentColor` 追加で既存要件に違反は生じず、実装者の「delta 不要・main specs 未変更」という判断は**妥当**。
- iOS Cell View（`RadioCellView.swift:28` / `SimpleCheckCellView.swift:30`）は依然 `effective.titleColor` でチェックを着色しているが、これは design.md line 149 で「iOS Cell View 側の着色リワイヤは別途 iOS 実機レビューのタスクで実施」と**明示的にスコープ外**として記述されており、本提案（Decision 9 = Android 固有のちらつき/着色是正）の責務範囲と整合する。core Theme への対称フィールド追加までは完了しており、iOS UI リワイヤの後続実施は計画どおり。

### 3. 着色リワイヤの実装妥当性 — 正しい（ただし design 文言にやや過剰表現あり / 後述 Minor）

- `CellStyle`（core）にも `RadioCell` / `SimpleCheckCell` にも accent 専用フィールドは存在しない（確認済み）。したがって着色解決は実質 `Theme.cellAccentColor` の一本道であり、`EffectiveStyle.from` が `theme.cellAccentColor` のみを参照するのは**正しい**。
- ViewHolder のコメント（`RadioCellViewHolder.kt:47-48` / `SimpleCheckCellViewHolder.kt:51-52`）も「RadioCell / CellStyle に accent 指定フィールドは無いため Theme.cellAccentColor を採用」と**実コードと一致**しており、前回のミスリードコメントは解消済み。
- なお `CheckboxCellViewHolder.kt:54-61` は別途 `CheckboxCell.accentColor`（既存フィールド）を `buttonTintList` に反映する従来挙動を維持しており、本修正の影響を受けていない（リグレッションなし）。

### 4. 前回 Suggestion 群の解消状況 — すべて解消

| 前回 Suggestion | 状態 |
|---|---|
| `RadioCellViewHolder.kt:47`「accentColor 指定があれば」誤コメント | 解消（該当文字列は全リポジトリから消失。実挙動に合致するコメントへ置換） |
| `SimpleCheckCell.kt` KDoc「左側に小さなチェック」 | 解消（`SimpleCheckCell.kt:11-12` で「accessory（右側）」に修正） |
| `build.gradle.kts` テーマ要件コメント（MaterialComponents/AppCompat） | 解消（`build.gradle.kts` で「`Theme.Material3.*` 派生が必須」へ修正、material 依存追加コメントも整備） |
| `docs/android-ui.md` テーマ要件不一致 | 解消（「テーマ要件（重要）」節を新設し Material3 必須・禁止テーマ・Compose 埋め込み時の注意まで明記） |
| SwitchCell ドラッグ操作（情報提供のみ） | 前回も「必須でない／実機 21.7.6 で UX 判断」と整理済み。挙動不変、許容範囲。 |

### 5. リグレッション・新規 Critical/Major — 検出なし

- `RadioCell` / `SimpleCheckCell` の `equals`/`hashCode` は内部状態（`selectedValue`/`isChecked`）除外・payload 局所更新（Decision 9-1）の方針を維持しており、accent 追加で破壊されていない。
- `bindStateOnly`（Radio/SimpleCheck/Checkbox）は引き続き状態のみ更新し、二重発火防止・外部 submitList 反映の整合も維持。
- ktlint/detekt 相当の compile 警告は確認範囲で増加なし（ビルド成功）。

### 6. ビルド・全ユニットテスト — PASS

検証ログは下記「検証ログ」参照。Android core/ui・iOS のすべてが failures=0。テスト失敗の見逃しに該当する事象はなし。

---

## 検証ログ

- `cd android && ./gradlew :ks-settingsview-core:testDebugUnitTest :ks-settingsview-ui:testDebugUnitTest --rerun-tasks`
  → BUILD SUCCESSFUL。
  - core: ThemeTest=6, CellStyleTest=5, SettingsRootDiffTest=20 ほか 全スイート failures=0/errors=0/skipped=0。
  - ui: BasicCellsTest=35, EffectiveStyleTest=4, ApplyDiffTest=15, KsCellRegistryTest=10, SettingsRootStoreTest=21 ほか 全スイート failures=0/errors=0/skipped=0。
- `cd ios && swift test` → All tests passed。`KsSettingsViewPackageTests` 123 件 0 failures（ThemeTests=6 に `cellAccentColor` 既定値/等価性/Set 重複の検証を含む）。
- `openspec validate add-cell-types-basic --strict` → valid。

---

## 指摘事項

#### 🟡 Minor: design.md 9-3 / 9-4 の「CellStyle の accent 指定 → なければ Theme.cellAccentColor」は実体のない優先順位を含む

**該当箇所**: `design.md:188`（9-3 着色）/ `design.md:200`（9-4 着色）

**問題点**:
両節は着色規則を「`CellStyle` の accent 指定 → なければ `Theme.cellAccentColor`」と二段優先で記述するが、`CellStyle`（core）にも `RadioCell` / `SimpleCheckCell` にも accent 専用フィールドは存在しないため、「`CellStyle` の accent 指定」という第 1 段は現状到達不能で、実体は `Theme.cellAccentColor` 一本である。同じ文中で「accent 専用フィールドが無いため EffectiveStyle.accentColor（Theme 由来）を用いる」と但し書きされているため**矛盾ではない**が、将来 `CellStyle.accentColor` を追加した読み手が「既に CellStyle accent が優先される実装になっている」と誤読しうる。

**推奨修正**:
（design 変更可能タイミングで）「現状 `CellStyle`/`RadioCell`/`SimpleCheckCell` に accent 専用フィールドは無いため、当面は `Theme.cellAccentColor` のみで着色する。`CellStyle.accentColor` を追加した場合に CellStyle 優先とするのは将来拡張」と将来形であることを明示すると誤読を防げる。実装自体は正しいため**修正は任意**。

#### 🔵 Suggestion: iOS の cellAccentColor は core 追加のみで UI 未反映（追跡導線の明確化）

**該当箇所**: `ios/Sources/KsSettingsViewUI/RadioCellView.swift:28` / `SimpleCheckCellView.swift:30`

**問題点**:
core の `Theme.cellAccentColor` は iOS にも対称追加されたが、iOS の Radio / SimpleCheck セルは依然 `effective.titleColor` で着色しており、`cellAccentColor` をまだ消費していない。design.md line 149 で「別途 iOS 実機レビューのタスクで実施」と明記されておりスコープ判断は妥当だが、core にフィールドだけ存在して UI 未使用の期間が生じる。

**推奨修正**:
必須ではない。後続の iOS 着色リワイヤを追跡できるよう、tasks.md または後続変更提案に「iOS Radio/SimpleCheck のチェック色を `Theme.cellAccentColor` 由来へリワイヤ」のタスクを明示的に起票しておくことを推奨。

---

## 良好な点（特筆）

- **クロスプラットフォーム対称性**: Android `Theme.kt` と iOS `Theme.swift` で `cellAccentColor` のフィールド名・既定値（`#007AFF` 相当 = `(0.0, 0.478, 1.0, 1.0)`）・ドキュメントコメントが 1:1 で一致。両 ThemeTest/ThemeTests も「既定値が `selectedColor`（グレー）と別物であること」まで対称に検証。
- **テスト品質**: `BasicCellsTest` の追加テストは「accent 色（緑）を指定 → チェック色に反映」「title 黒の流用でないこと」「未指定時は既定 cellAccentColor」と、リワイヤの意図を直接突くアサーション。スタブ・スキップ・言い訳コメントによる実質スキップは無し。
- **前回 Suggestion の即時解消**: 誤コメント・KDoc・build.gradle・docs の 4 点をすべて反映し、`docs/android-ui.md` には禁止テーマ（`Theme.MaterialComponents.*` / `Theme.AppCompat.*` / フレームワーク標準 `Theme.Material.*`）と症状（`materialSwitchStyle` 未解決・`SwitchCompat.makeLayout` NPE）まで具体化。MEMORY の Android テーマ要件とも整合。

---

## アクションプラン（優先度順）

1. （任意・design 変更可能時）design.md 9-3 / 9-4 の着色記述を「現状は `Theme.cellAccentColor` のみ。CellStyle accent 優先は将来拡張」と将来形で明示（Minor、誤読防止）。
2. （任意・追跡導線）後続で iOS Radio/SimpleCheck の `cellAccentColor` 着色リワイヤを tasks へ起票（Suggestion）。
3. （アーカイブ前の残作業・本レビュー対象外）21.5.3 / 21.5.4 / 21.7.2〜21.7.6 の Pixel 6a 実機目視検証（Ripple 重畳順序・selectedColor 反映・ちらつき非発生・セル全体タップ・accent 着色の視認）。コードレビューでは合否判定不能のため、実機検証完了をアーカイブ条件とする。

---

## 判定結果

**ステータス: APPROVED**

- 前回 NEEDS_DISCUSSION の唯一の論点（accent 着色の design 乖離）が `Theme.cellAccentColor` 新設により設計どおり解消。Critical / Major はゼロ。
- Android core/ui・iOS の全ユニットテスト PASS（テスト失敗の見逃しなし）、`openspec validate --strict` valid。
- `settings-view-core` Theme Requirement は下限規定のため、フィールド追加は仕様非違反。delta 不要の判断は妥当。
- 前回 Suggestion 群はすべて解消。残る指摘は Minor 1 / Suggestion 1 で、いずれも実装の正しさに影響せず優先度が低い（マージ可能）。
- ただし実機目視検証タスク（21.5.3/21.5.4/21.7.2〜21.7.6）は未了であり、これらの完了を**アーカイブ条件**とすること（ユーザー指定どおりコードレビュー範囲外）。
