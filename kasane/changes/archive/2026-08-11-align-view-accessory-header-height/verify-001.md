# 一致検証結果: align-view-accessory-header-height (001 回目)

**日付**: 2026-08-11
**判定**: VALID

デルタスペック `specs/settings-view-android-ui/spec.md` の 2 Requirement / 11 Scenario すべてに実装とテストの対応があり、❌ は 0 件。tasks.md の虚偽チェックなし、足場アーティファクトへの逆流なし、Android / iOS ともテスト全件 green。

## 対応表

### Requirement: Section Header の固定高さは accessory 種別に依らず適用される

共通実装: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:311-335` (`applySectionHeaderHeight` — 優先順位 `Section.headerHeight > 0` → `Theme.headerHeight > 0` → `WRAP_CONTENT`、dp→px 換算、Footer は常に自動)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| view accessory + Section.headerHeight 正値で固定高さになる | `SectionAccessoryViewHolders.kt:146-154` (`bind` → `applyHeaderHeight`) / `KsSettingsListAdapter.kt:167-173` (Header × View へ `item.headerHeight` + `theme` を伝搬) | `ViewAccessoryHeaderHeightTest.kt:94` (固定高さ px)、`:199` (内容 300dp > 指定 40dp でも measuredHeight は指定値)、`ui/verification/android-header-height-states.png` 状態C (48dp = 96px 内で clip)、iOS 対称: `SectionAccessoryRenderingTests.swift:849` | ✅ 一致 |
| view accessory + Theme.headerHeight フォールバック | `SectionAccessoryViewHolders.kt:319-321` | `ViewAccessoryHeaderHeightTest.kt:107`、iOS: `SectionAccessoryRenderingTests.swift:872` | ✅ 一致 |
| view accessory + Section と Theme が両方正値なら Section が勝つ | `SectionAccessoryViewHolders.kt:317-322` (when の評価順) | `ViewAccessoryHeaderHeightTest.kt:121`、iOS: `SectionAccessoryRenderingTests.swift:893` | ✅ 一致 |
| view accessory + 高さ未指定は自動高さのまま | `SectionAccessoryViewHolders.kt:323-327` (`WRAP_CONTENT`) | `ViewAccessoryHeaderHeightTest.kt:135`、iOS: `SectionAccessoryRenderingTests.swift:915` | ✅ 一致 |
| 固定高さの Header と自動高さの Header が混在しても互いに影響しない | `SectionAccessoryViewHolders.kt:328-334` (未指定時に `WRAP_CONTENT` へ戻す = ViewHolder 再利用時の引きずり防止) | `ViewAccessoryHeaderHeightTest.kt:148` (同一 ViewHolder へ 固定→自動→固定 の順で bind)、`ui/verification/android-header-height-scrolled.png` (スクロール後も B=96px / C=96px)、iOS: `SectionAccessoryRenderingTests.swift:934` (`invalidateLayout` を挟んでも不変) | ✅ 一致 |
| Footer の view accessory は高さ指定の対象外 | `KsSettingsListAdapter.kt:177-181` (Footer × View は `isHeader = false`、`headerHeight` を渡さない) / `SectionAccessoryViewHolders.kt:318` (`!isHeader -> -1.0`) | `ViewAccessoryHeaderHeightTest.kt:177`、iOS: `SectionAccessoryRenderingTests.swift:967` | ✅ 一致 |
| text accessory の高さ解決は変更されない | `SectionAccessoryViewHolders.kt:75-80` (Text 側も同じ `applySectionHeaderHeight` を通す。抽出前後で解決規則は同一) | 既存テスト `SectionAccessoryRenderingTest.kt:227` (正値) / `:248` (-1 → WRAP_CONTENT) / `:267` (Theme フォールバック) / `:289` (Section > Theme) が無改変で green | ✅ 一致 (下記「補足」参照) |

### Requirement: 表示済み Header の headerHeight 変更は hosted view を維持したまま反映される

共通実装:
- 差分検出: `KsSettingsListAdapter.kt:416-419` (`isSameHeaderHeight` が accessory 種別を問わず高さ差を内容差として扱う)
- payload 決定: `KsSettingsListAdapter.kt:369-378` (View accessory かつ中身同一 → `PAYLOAD_HEADER_HEIGHT`、それ以外 → `PAYLOAD_CONTENT`)
- 反映経路: `KsSettingsListAdapter.kt:204-216` (3 引数版 `onBindViewHolder` が高さ専用 payload のみを `applyHeaderHeight` へ振り分け、他は `super` でフル bind) / `SectionAccessoryViewHolders.kt:166-173` (`applyHeaderHeight` — `bindKsAnyView` を呼ばない)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 自動高さ → 固定高さへの動的変更 | 上記 3 点 | `ViewAccessoryHeaderHeightTest.kt:232` (Store → `replaceSection` → RecyclerView の実経路) | ✅ 一致 |
| 固定高さ → 自動高さへの動的変更 | 同上 | `ViewAccessoryHeaderHeightTest.kt:242` | ✅ 一致 |
| 固定高さの値変更 | 同上 | `ViewAccessoryHeaderHeightTest.kt:255` (48dp → 96dp) | ✅ 一致 |
| 高さのみの変更で view の内部状態が維持される | `KsSettingsListAdapter.kt:211-214` (`bindKsAnyView` を経由せず高さのみ更新) | `ViewAccessoryHeaderHeightTest.kt:264` (EditText の同一インスタンス + 入力テキスト保持を `assertSame` で確認)、`FullUpdateContentSyncTest.kt:312` (実発行 payload を渡して子 View の同一性を確認)、payload 発行側: `FullUpdateContentSyncTest.kt:272` / `ListAdapterDiffTest.kt:146` | ✅ 一致 |

### MODIFIED 相当 (旧契約の除去)

デルタスペックは ADDED のみだが、proposal が「旧契約 (View accessory の高さ差を無視) を固定していた既存テストは新契約へ置換する」と宣言しているため併せて確認した。

| 対象 | 状態 |
|---|---|
| `ListAdapterDiffTest` の「View accessory は headerHeight が違っても areContents で等価」 | ✅ 新契約テストへ置換済み (`ListAdapterDiffTest.kt:146`)。旧アサーションの残骸なし |
| `FullUpdateContentSyncTest` の「headerHeight の差で変更通知を発行しない」 | ✅ 新契約テストへ置換済み (`FullUpdateContentSyncTest.kt:272`) + payload 反映テストを新設 (`:312`) |
| `isSameHeaderHeight` の旧前提コメント / `CellListItem.SectionHeader` の「Text だけが適用」記述 | ✅ 新契約へ更新済み (`KsSettingsListAdapter.kt:409-419`, `CellListItem.kt:26-28`) |
| 「View accessory は高さ差を扱わない」旨の早期 return (`if (oldItem.accessory !is SectionAccessory.Text) return true`) | ✅ 削除済み (残骸なし。`grep` で当該分岐の再出現なし) |

## 追加検査

### tasks.md の完了状況

全 11 タスクが `[x]`。対応表と突き合わせた結果、虚偽チェックは 0 件。

- 1.1 / 1.2 / 1.3 / 1.4 / 1.5 → 上記対応表の実装欄で確認
- 2.1 / 2.4 → `ViewAccessoryHeaderHeightTest.kt` (新規, 11 テスト)
- 2.2 → **新規テストの追加はなく、既存 4 テストの green 維持で確認**。タスク文言が「回帰確認」であり実装側の解決規則が抽出のみで不変なため、虚偽チェックとは判定しない (補足参照)
- 2.3 → `SectionAccessoryRenderingTests.swift:849-982` (iOS 6 テスト新設、プロダクションコードは無変更)
- 3.1 / 3.2 / 3.3 → 下記のとおり再実行して確認

### テスト実行 (本検証で再実行)

`concepts/cross/conventions/test-execution.md` の規約に従い、件数まで確認した。

- **Android**: `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`build/test-results/testDebugUnitTest` = 1107 tests / 0 failures / 0 errors / 0 skipped、`testReleaseUnitTest` = 1107 tests / 0 failures / 0 errors / 0 skipped (合計 2214 件)
- **iOS**: `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<ios-simulator-udid>…' (iPhone 17 Pro / iOS 26.0)` → `** TEST SUCCEEDED **`、`Executed 457 tests, with 0 failures`
- **コメント規約 lint**: `python3 scripts/comment-policy-lint.py --summary` → 禁止 0 件 (検査対象 572 ファイル)

### 逆流検査 (足場アーティファクトの書き換え)

`git diff` で確認した `kasane/` 配下の変更は 2 ファイルのみ。

| ファイル | 変更内容 | 判定 |
|---|---|---|
| `tasks.md` | 全 11 行のチェックボックス `[ ]` → `[x]` のみ。タスク文言の改変なし | ✅ 正常 (進捗記録) |
| `ui/brief.md` | 末尾へ「視覚照合結果 (実装後検証)」節を追記。既存節の書き換えなし | ✅ 正常 (ksn-ui が求める照合記録) |

`proposal.md` / `specs/settings-view-android-ui/spec.md` / `exploration.md` / `second-opinion-spec-001.md` / `ui/mock/` は無変更。**逆流なし**。

### UI 変更の検査

- 承認モックの記録: `ui/brief.md` の「承認モック」節に `mock/height-states.html` 採用・`approved.png`・2026-08-11 ユーザー承認と記録あり ✅
- 実装後の視覚照合記録: `ui/brief.md` の「視覚照合結果」節に記録あり。合意済み妥協は「0 件」と記載 ⚠️
  - `ui/verification/android-header-height-states.png` の画素解析では、状態 B (固定 48dp・内容が収まる) の Header 行 96px のうち accessory が占めるのは 64px で、下 32px は未描画。承認モックの状態 B は領域が一様に塗られた矩形として描かれており、この差は照合記録に反映されていない
  - ただし**行の高さ (96px = 48dp) と clip の成立というデルタスペックの規範部分は一致**しており、Scenario 単位では ❌ に当たらない。占有範囲はデルタスペックの未規定範囲であるため、`review-001.md` の Major 指摘として扱う
- `ui/verification/android-header-height-rebound.png` は `-states.png` と SHA-256 一致 (再表示後に表示が変わらないことと矛盾はしないが、独立取得の証跡としては弱い)。該当 Scenario はユニットテストと `-scrolled.png` で担保されているため ❌ とはしない

### deviation

`deviation.md` は存在しない。対応表に ❌ が 0 件のため、未記録乖離もなし。

## 補足: 「text accessory の高さ解決は変更されない」の扱いについて

このタスク (2.2) では新規テストが追加されず、既存の `SectionAccessoryRenderingTest.kt:227/248/267/289` の green 維持で回帰確認としている。実装側は解決ロジックを `applySectionHeaderHeight` へ抽出しただけで、次の 1 点を除き規則は同一である。

- 抽出前: `layoutParams` が null かつ目標が `WRAP_CONTENT` の場合、`layoutParams` を代入しない (null のまま)
- 抽出後: `layoutParams` が null なら `ViewGroup.LayoutParams(MATCH_PARENT, targetHeight)` を必ず代入する

Text 側の itemView は `createSectionTextView` が生成時に `layoutParams` を設定するため、この分岐は実際には到達せず挙動差は生じない。したがって Scenario は満たされていると判定する (設計上の指摘としては `review-001.md` の Suggestion に記載)。
