# Verify 002: implement-modern-style (verify-001 ❌-1 の再検証)

- 検証日: 2026-08-20
- 対象: 未コミット working tree (HEAD `81bf2c4` + 未追跡新規ファイル)
- 前回: [verify-001.md](verify-001.md) — 判定 INVALID (❌ 1件)
- 判定: **VALID**

本書は verify-001 の ❌-1 のみを対象とした差分検証。verify-001 で ✅ / ⚠️ と判定した全 Requirement / Scenario の対応表は **verify-001.md をそのまま引き継ぐ** (下記「回帰の不在」で引き継ぎの前提が崩れていないことを確認済み)。

---

## 1. ❌-1 の解消判定

### 対象 Scenario

`specs/settings-view-android-ui/spec.md:45-48`

```
#### Scenario: 構造変更後も箱が Cell 範囲に追従する
- GIVEN Modern で表示中の Section
- WHEN `SettingsRootDiff` で Cell を末尾に挿入する
- THEN 箱は挿入後の末尾 Cell までを覆う
```

### 追加されたテスト

いずれも `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ModernSectionDecorationTest.kt`。

| # | テスト | 行 | Scenario 対応 |
|---|---|---|---|
| 1 | `末尾に Cell を挿入すると箱が挿入後の末尾 Cell まで伸びる` | `:240-270` | Scenario 本文そのもの (WHEN = 末尾挿入) |
| 2 | `末尾の Cell を削除すると箱が削除後の末尾 Cell まで縮む` | `:273-292` | 逆方向の追従 (Scenario の要求を超える追加保証) |

### GIVEN / WHEN / THEN の対応

| 節 | テスト 1 の実装 |
|---|---|
| GIVEN Modern で表示中の Section | `:241` `host(...)` — `KsSettingsView` に Store を bind し `style = Modern` で実レイアウトまで実行 (既存 `host` ヘルパ、`:95` で Modern を設定) |
| WHEN `SettingsRootDiff` で Cell を末尾に挿入 | `:245-251` `view.applyDiff(SettingsRootDiff.InsertCell(sectionId = "s1", index = 2, cell = LabelCell(id = "s1-added", …)))` — 2 Cell の Section へ index 2 = **末尾挿入**。spec 文言の `SettingsRootDiff` を実際に使っている |
| THEN 箱は挿入後の末尾 Cell までを覆う | `:262` `assertEquals("箱の下端は挿入行の下端まで伸びる", addedRow.bottom, after.bottom, 0.5f)` |

補助アサーション: `:261`(箱の上端は挿入行より上)、`:263-266`(箱が挿入前より縦に伸びた)、`:267-268`(Footer は箱の下外側のまま)、`:269`(中間 separator が 2 本へ増える)。

### トートロジー検査 — 成立しない (テストは判別力を持つ)

観測値と期待値が**独立した経路**から来ているかを確認した。

- **観測値** `after` = `record(activity, over = false).roundRects.first()` — `ModernSectionDecoration.onDraw` が実際に発行した `drawRoundRect` を `DecorationCanvasRecorder` が捕捉したもの。実装の内部変数ではなく描画呼び出しの実引数
- **期待値** `addedRow.bottom` = `RecyclerView` の child View の layout 座標。しかも **`rows(activity)` から「末尾の child」ではなく `LabelCell.title == "s1-Added"` で行を特定している** (`:256-259`)。「最後の子を取る」実装と同じ規則で期待値を作っていないため、実装が挿入行を取りこぼしても期待値は動かない
- **判別力の確認**: 仮に `collectSectionBoxes` が挿入行を集計しない (古い snapshot を読む / 挿入前の件数で打ち切る) 実装だった場合、`after.bottom` は旧末尾 Cell の下端に留まり `addedRow.bottom` は厳密に大きくなるため、`:262` と `:263-266` の双方が失敗する。逆に箱が過剰に伸びた場合も `:262` が失敗する
- **独立した二重確認**: `:269` の separator 本数 (1本→2本) は `onDrawOver` という別の描画経路を通した構造変化の確認であり、`onDraw` の箱とは別系統

テスト 2 も対称の構造 (`RemoveCell` → 残存 Cell を `CellListItem.CellRow` で列挙して末尾を取り、`<` の収縮アサーション付き) で、同じくトートロジーではない。

### 差分反映の待機が正しいか

`:252` / `:279` は既存の共有テスト支援 `KsSettingsViewTestSupport.kt` の `awaitDifferCommit` / `committedTexts` を使い、`AsyncListDiffer` のコミット完了 (挿入行が確定リストに現れる / 削除行が消える) を待ってから `layout(activity)` で再レイアウトしている。プロジェクト内の既存 diff テスト群 (`ContentUpdatePayloadTest` / `FullUpdateContentSyncTest` 等) と同じ待機規約であり、`KsSettingsViewTestSupport.kt` 自体には変更がない (git 上 unmodified)。

### 判定

**❌-1 は解消**。実装ではなくテストの追加で閉じられており、deviation.md への追記は不要かつ実際に行われていない (deviation.md は 12:27 のまま無変更) — verify-001 の見立て「テストを追加するのが妥当」に沿った対応。

---

## 2. 実装コードに変更が入っていないこと

verify-001 時点と現在で、実装コードの diff が完全に一致することを確認した。

| ファイル | numstat (追加/削除) | verify-001 時点との比較 |
|---|---|---|
| `android/.../ui/ClassicSectionDecoration.kt` | 30 / 5 | 一致 (計 35) |
| `android/.../ui/ModernSectionDecoration.kt` | 272 / 81 | 一致 (計 353)。ファイル行数も 340 行で一致 |
| `android/.../ui/SectionBoxMetrics.kt` | 119 / 0 | 一致 |
| `android/.../ui/SectionUnitMargin.kt` | 58 / 0 | 一致 |
| `android/.../ui/Theme.kt` | 12 / 0 | 一致 |
| `ios/Sources/KsSettingsViewUI/*` (6ファイル) | 8/0・35/0・9/0・316/22・5/4・33/1 | 全て一致 (計 8・35・9・338・9・34) |

- **注記**: `ModernSectionDecoration.kt` の mtime だけが 14:36:39 (テストファイル 14:35:54 より後) に更新されている。ただし diff の追加/削除行数・総行数がいずれも verify-001 時点と一致し、Scenario の中核である `collectSectionBoxes`(`:245-316`) を再読して verify-001 で読んだ内容と同一であることを確認した。**内容変化を伴わない保存 (no-op save) と判断する**
- 未追跡の iOS 実装ファイル 5件 (`SectionBox*.swift`) の mtime は 11:08〜13:39 で、verify-001 のテスト実行 (14:28) より前。変更なし
- 新規ファイルの追加も削除もない (`git status --porcelain` のエントリ構成は verify-001 時点と同一)

**実装コードの変更なし** ✅

## 3. テストが実際に通ること (実行して確認)

| 対象 | コマンド | 結果 |
|---|---|---|
| Android ks-settingsview-ui | `./gradlew :ks-settingsview-ui:testDebugUnitTest` | **907 passed / 0 failed / 0 skipped** (verify-001 の 905 から **+2**。増分は今回の 2 件と一致) |
| Android ks-settingsview-compose | `./gradlew :ks-settingsview-compose:testDebugUnitTest` | **111 passed / 0 failed / 0 skipped** (変化なし) |
| 新規 2 件の個別結果 | `TEST-…ModernSectionDecorationTest.xml` | `末尾に Cell を挿入すると箱が挿入後の末尾 Cell まで伸びる` (0.049s) / `末尾の Cell を削除すると箱が削除後の末尾 Cell まで縮む` (0.068s) — いずれも failure 要素なしで **passed** |
| iOS | 再実行せず | iOS の実装・テストファイルはいずれも verify-001 のテスト実行 (14:28、**885 passed / 0 failed**) 以降 mtime・diff とも無変化のため、前回結果を有効として引き継ぐ |

**テスト全件成功** ✅

## 4. 回帰の不在 (verify-001 の引き継ぎ前提)

- **逆流なし**: `specs/` 4ファイル・`proposal.md`・`design.md` はいずれも mtime 10:20:05 (提案化コミット `81bf2c4` のまま) で diff も空。`tasks.md`(13:11) と `deviation.md`(12:27) も verify-001 時点から無変更
- **虚偽チェックなし**: tasks.md は無変更。今回の追加はタスク 5.2「Android: …のテストを追加する」の範囲内であり、新たなタスク追加・チェック操作は発生していない
- **他の Scenario への影響なし**: 変更されたのはテストファイル 1 本 (`ModernSectionDecorationTest.kt`、636 → 692 行の +56) のみで、既存テストの削除・書き換えはない (既存 20 件はテスト名・件数とも維持され、全件 passed)

---

## 5. 総合判定

**VALID**

- verify-001 の ❌-1 は解消。追加された 2 件は spec Scenario (`settings-view-android-ui` の「構造変更後も箱が Cell 範囲に追従する」) を `SettingsRootDiff.InsertCell` の実経路で検証しており、期待値を実装と独立な経路 (child View の layout 座標 + title による行特定) から取っているためトートロジーではない
- 実装コードに変更はない
- テストは iOS 885 / Android 1018 (ui 907 + compose 111) すべて成功
- 逆流なし・虚偽チェックなし
- 全 Requirement / Scenario が ✅ 一致 または ⚠️ deviation.md 記録済み (内訳は verify-001.md の対応表を参照)

### 残る前提 (判定外・オーナー判断待ち)

`ui/brief.md` が「オーナーの最終承認は未取得 (証跡を提出済み)」と明記しており、**承認済みモックとの視覚照合の最終クローズはオーナー判断待ち**。ksn-verify の一致検証としては VALID だが、蒸留 (ksn-distill) に進む前にこのゲートの扱いを確認すること。
