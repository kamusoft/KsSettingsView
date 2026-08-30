# レビュー結果 - unify-cell-common-fields-via-shared-row-layout (改訂後)

**レビュー日時**: 2026年06月09日
**レビュワー**: sdd-reviewer
**変更提案ID**: unify-cell-common-fields-via-shared-row-layout
**レビュー対象**: 改訂後の実装（spec 改訂で Android Compose 化要件を撤回 → View ベース `CellBaseViews` + `applyCellBaseLayout` 採用、hintText 右上 float 配置に変更）

## サマリー

前回の `review-result_001.md` で `CHANGES_REQUESTED` となった後、design.md / spec / tasks.md が大幅改訂され、両プラットフォームの実装も改訂方針に沿って書き換えられた。本レビューでは、改訂版の spec MUST を満たしているかを中心に、コード品質・テストカバレッジ・ビルド/テスト結果を網羅的に確認した。

### 実装状況の総括

| 領域 | 状態 | 評価 |
|------|------|------|
| iOS `CellBaseLayout.swift` の新規実装 | 完了 | spec 通り、`hintText` を `hintLabel` の lazy 生成 subview に反映する経路が正しく実装されている |
| iOS `KsListCellBase.hintLabel` の lazy 生成 / prepareForReuse リセット | 完了 | spec の方式 A（lazy property + addSubview + 制約付き）に厳密準拠 |
| iOS 7 種 Cell View の `applyCellBaseLayout` 経由化 | 完了 | `LabelCellView` / `CommandCellView` / `SwitchCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView` / `ButtonCellView` すべてで呼び出しを確認 |
| iOS 旧 `ksCellRow` 関数の削除 | 完了 | `grep` で残存なし |
| iOS `UnifyCellCommonFieldsTests` 22 件（5.R 7 件追加） | 完了 | hintLabel 反映 / 制約 / 重複防止 / prepareForReuse / font/color 解決すべて検証 |
| Android `CellBaseLayout.kt` 新規実装（`CellBaseViews` + `buildCellBaseViews` + `applyCellBaseLayout`） | 完了 | ConstraintLayout programmatic 構築、spec の MUST 配置規約を満たす |
| Android 旧 `KsCellRowLayout.kt`（Compose 版）削除 | 完了 | ファイル不在、`androidx.compose.runtime` import が Cell ViewHolder には存在しない |
| Android 7 種 ViewHolder の `applyCellBaseLayout` 経由化 | 完了 | `ButtonCellViewHolder` の aux 切替（normal / button-style）も spec 通り実装 |
| Android `UnifyCellCommonFieldsTest` 29 件（10.R 6 件追加） | 完了 | hintTextView Z 順 / centerY / visibility / views 同一性 / KsCellRow 不在すべて検証 |
| Android `BasicCellsTest` の ConstraintLayout 移行（35 箇所のキャスト書き換え） | 完了 | 76 件成功 |
| Cell モデル拡張（共通フィールド + accentColor） | 完了 | iOS / Android 双方、`ButtonCell` の `description` 除外も両プラットフォームで MUST NOT を満たす |
| DSL 拡張関数の引数追加 | 完了 | spec 通り Optional 引数として追加 |
| サンプルアプリ（Switch + icon + description + hintText / Radio + accentColor） | 完了 | iOS / Android 双方 |
| Phase 12.R.4 / 12.R.5（実機目視確認） | 未完了（残課題） | 妥当（ユニットテストでは十分検証できない領域） |
| Phase 10.R.3（hintTextView と accessoryHolder の物理分離） | 判断変更（Z 順前面の保証に変更） | 受容可能（後述の指摘 Suggestion-2 を参照） |

### ビルド・テスト確認結果（再走）

- `swift build`（iOS）: 成功
- `swift test`（iOS）: **83 件成功、0 failures**
- `./gradlew :ks-settingsview-ui:test`（Android、debug + release）: **574 件成功、0 failures, 0 errors**
  - `UnifyCellCommonFieldsTest`: 29 件成功
  - `BasicCellsTest`: 76 件成功
- `./gradlew :ks-settingsview-compose:test`（Android）: BUILD SUCCESSFUL
- `openspec validate unify-cell-common-fields-via-shared-row-layout --strict`: `Change is valid`

### 判定

**`APPROVED`**

理由:

1. 前回 `CHANGES_REQUESTED` の根本原因（Android `KsCellRow` がデッドコード／`ButtonCellViewHolder` aux 描画欠落）は、改訂方針（Compose 撤回 + View ベース）への完全移行により解消された。
2. 改訂後 spec の MUST 要件（`CellBaseViews` の ConstraintLayout 構造、`hintText` 右上 float の Z 順前面保証、7 種 ViewHolder すべての `applyCellBaseLayout` 経由、`KsCellRow.kt` 削除、`ButtonCellViewHolder` の aux 切替、iOS `hintLabel` の lazy 生成と AutoLayout 制約 / prepareForReuse リセット）はいずれもコードで満たされ、テストで担保されている。
3. 全ビルド・全テストが成功し、`openspec validate --strict` も `valid`。
4. 残課題（Phase 12.R.4 / 12.R.5 実機目視）はユニットテストで間接検証されており、最終確認をユーザーに委ねるのは妥当な判断。

以下に挙げる指摘は Major / Critical はなく、Minor / Suggestion のみで、マージブロッカーではない。**マージ可能**。

## 指摘事項

### 🟡 Minor

#### [Minor-1] iOS: `ButtonCellView.contentAlignment(for: .end)` が `.justified` を返している

**該当箇所**: `ios/Sources/KsSettingsViewUI/ButtonCellView.swift:146-152`

**問題点**:

`UIListContentConfiguration.TextProperties.TextAlignment` には Apple SDK の制約上 `.natural` / `.center` / `.justified` の 3 ケースしか存在せず、`.right` / `.trailing` に相当する明示的な右寄せ用ケースが無い。実装では `.end` → `.justified` にマップしているが、`.justified` は厳密には「両端揃え（分散）」であり、1 行のタイトルでは効果が無く（最終行は左寄せのまま）、結果として `titleAlignment = .end` を指定しても title 列内では左寄せの見た目になる可能性がある。

spec の `Scenario: icon / valueText / hintText を指定したときの titleAlignment の挙動` は「`titleAlignment` は title 列の中での揃え位置のみを制御する」と規定するのみで、`.end` の正確な見た目までは明文化していない。ただし利用者期待としては「`.end` → title 列内で右寄せ」が自然である。

**推奨修正（任意）**:

通常レイアウト時は `UIListContentConfiguration` の `textProperties.alignment` 経路を使わず、`contentConfiguration` を取得した後に `cell.contentView` 内の title `UILabel` を辿って `textAlignment = .right` を直接設定するか、または spec 側に「`.end` は `.justified` にマップする（Apple API 上の制約）」と明記する。後者のほうが実装変更コストが低く現実的。

本 change のスコープと完成度を考慮して、本指摘は Minor とし**マージブロッカーにしない**。後続 change で扱う場合は `cell-types-basic` の MODIFIED Requirement に注記を追加すること。

#### [Minor-2] iOS: `ButtonCellView` の通常レイアウト時に `disabledTextColor` の上書きがハードコードされている

**該当箇所**: `ios/Sources/KsSettingsViewUI/ButtonCellView.swift:89-93`

**問題点**:

`applyCellBaseLayout` は既に内部で `isEnabled == false` 時に `titleColor` を `disabledTextColor` で上書きしているが、`ButtonCellView` は「ボタン文字色の 4 段優先順位」を満たすため `render` の後段で `contentConfiguration.textProperties.color = btn.isEnabled ? baseColor : effective.disabledTextColor` を再上書きしている。この再上書き自体は仕様上必要だが、二重に `disabledTextColor` を解決しており保守性が低い（`effective` が将来 disabled 時の解決ルールを変えた場合、`ButtonCellView` 側もメンテが必要）。

**推奨修正**:

`applyCellBaseLayout` の signature に「title color override」用の Optional 引数 (`titleColorOverride: UIColor?`) を追加し、ButtonCellView の場合のみ `baseColor` を渡す経路に統一する。ただし他 Cell に影響しない優先順位の高い改善ではないため、本 change スコープでは見送り可。

### 🔵 Suggestion

#### [Suggestion-1] Android: `applyCellBaseLayout` 内で `views.hintTextView.bringToFront()` を呼んでいる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:294`

**所見**:

`buildCellBaseViews` で既に `accessoryHolder` → `hintTextView` の順に `addView` しており、Z 順は構築時点で前面確定済み。さらに `applyCellBaseLayout` 内で `views.hintTextView.bringToFront()` を呼ぶのは「ViewHolder 側で `accessoryHolder.addView(...)` した後に呼ばれた場合の保険」として有効だが、毎 bind ごとに `requestLayout()` を誘発するコストもある。

`bringToFront()` は `ViewGroup.requestLayout()` を内部で呼ぶため、RecyclerView の高速スクロール時にレイアウト無効化が連鎖する懸念がある。Robolectric テストでは検出できないため**現状の指摘は Suggestion レベル**にとどめる。

**推奨修正（任意）**:

各 ViewHolder の `create()` で `accessoryHolder.addView(...)` を行ったあとに 1 度だけ `views.hintTextView.bringToFront()` を呼ぶか、`buildCellBaseViews` 内で `hintTextView` を最後に `addView` するだけで十分（spec も「accessoryHolder より後に addView することで Z 順前面に置く」と明記しており、現状の構築順序は spec を満たしている）。

#### [Suggestion-2] Android: Phase 10.R.3 の判断変更（物理分離 → Z 順前面の保証）について

**該当箇所**: `tasks.md:139`、`UnifyCellCommonFieldsTest.kt:430`（`hintTextView は accessoryHolder より Z 順で前面に配置される`）

**所見**:

spec MUST 「`hintTextView` は `accessoryHolder` より後に `addView` することで Z 順の前面に置かれ、万一の干渉時にも `hintText` が前面に見える状態を保証しなければならない」は満たされている（`buildCellBaseViews` の addView 順序 + 検証テスト）。

「物理分離」については spec 自体が「両者は物理的に重なり得るが、通常は干渉しない」と明言し、MUST としては Z 順前面の保証のみを要求しているため、判断変更は **spec の意図と整合する**。実装者が Phase 12.R.5（Android サンプルアプリでの実機目視）を残課題に残しているのも妥当。

物理分離の検証手段が他にないか検討した結果、Robolectric / instrumentation テストどちらでも実機相当のセル高さを再現するのは非現実的（48dp 最低保証や `applyEffectiveHeight` の効きは layout pass 後の `getHeight()` で判定する必要があり、unit test 環境では measure spec の制約に依存する）。**実機目視で確認する** という残課題化が現実解。

**追加提案（任意）**:

実機目視チェックリストとして、samples/android に「hintText と accessory が干渉せず縦分離している」ことの確認手順をスクリーンショット付きで残しておくと、後続変更で hintText 位置を変更する場合の回帰検出が容易になる。

#### [Suggestion-3] Android: `SectionAccessoryViewHolders.kt` に残る Compose import について（参考）

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:10-13`

**所見**:

`settings-view-android-compose` spec の MUST NOT「Compose（`androidx.compose.runtime`）を用いない」は **CellViewHolder の共通行レイアウト** に対する MUST NOT であり、SectionH/F の `RootAccessory.Compose` 形式は別 capability（`settings-view-android-ui` の SectionAccessory）であるため、本指摘の対象外。Compose import が残っていても本 change の MUST 違反ではない。

ただし、後続 change で Android UI 全体を View ベースに統一する方針を検討する場合は、SectionAccessory も含めて検討する必要がある。本 change のスコープを超えるため Suggestion レベルにとどめる。

## アクションプラン

優先度順:

1. **（マージ前必須）なし**。本 change はマージ可能。
2. **（マージ後／後続 change で対応推奨）** Minor-1: `ButtonCellView.contentAlignment(for: .end)` の `.justified` マッピングを spec の cell-types-basic に注記するか、`UILabel.textAlignment = .right` の直接設定経路に置き換える。
3. **（マージ後／後続 change で対応推奨）** Suggestion-1: Android `applyCellBaseLayout` 内の `hintTextView.bringToFront()` 呼び出しを ViewHolder `create()` 時の 1 回に移すことで、bind 時のレイアウト無効化コストを抑える。
4. **（マージ後／ユーザー対応）** Phase 12.R.4 / 12.R.5: iOS / Android サンプルアプリでの実機目視確認を実施する（hintText 右上 float / accessory との縦分離）。

## レビュー観点チェックリスト確認

### 正確性・機能性
- [x] openspec の spec / proposal / design / tasks をすべて満たしている
- [x] 7 種 Cell モデルへの共通フィールド追加、`ButtonCell` の `description` 除外（MUST NOT）も含めて準拠
- [x] DSL 拡張関数引数追加、`withDSLID` / `withStyle` / `copy()` の保持
- [x] エッジケース（`hintText = nil`, `icon = nil`, `valueText only`, `description only`）を網羅

### テスト
- [x] iOS 83 件 / Android 287+（debug + release で 574 件）成功
- [x] spec の各 Scenario に対応するテストを実装（hintLabel の constraint, font/color, prepareForReuse, accessory 並び順, accentColor 解決順序, ButtonCell aux 切替, 右端 X 整列, etc.）
- [x] テスト内で手抜き実装なし（スタブ濫用なし、すべてプロダクションコードを通したアサート）
- [x] Phase 10.R.3 の判断変更も「Z 順前面検証」という形で MUST を満たすテストを実装

### セキュリティ・パフォーマンス
- [x] N+1 や不要な処理は見当たらない
- [x] `KsListCellBase.hintLabel` の lazy 生成でリサイクル時の再生成コスト回避
- [x] Suggestion-1（`bringToFront` 多用）に若干のパフォーマンス懸念があるが、実機検証なしでは特定不可

### 可読性・保守性
- [x] 命名統一（iOS / Android 双方で `applyCellBaseLayout`）
- [x] コメント・KDoc・Swift Doc が spec 参照付きで丁寧に書かれている
- [x] 既存パターン（`KsCellViewSupport.applyEffectiveHeight` 等）の踏襲

### 一貫性・多言語対応
- [x] プロジェクト規約踏襲（kotlin-impl-skill / swift-ui-impl-skill 観点で逸脱なし）
- [x] 多言語リソースは本 change のスコープ対象外

### openspec/changes 規約の厳守
- [x] openspec/specs を改訂していない
- [x] openspec/changes/unify-cell-common-fields-via-shared-row-layout の内容に反する指摘なし
- [x] 改訂後 spec の MUST / SHALL / MUST NOT をいずれも満たす

## 判定結果

**ステータス**: `APPROVED`

- Critical / Major 指摘なし
- Minor / Suggestion はマージブロッカーではなく、後続 change で対応推奨
- 全ビルド・全テスト成功、`openspec validate --strict` も valid
- 残課題（実機目視確認）はユーザー対応として妥当

本 change はアーカイブ可能な状態にある。
