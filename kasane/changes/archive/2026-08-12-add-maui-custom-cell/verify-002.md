# Verify 002: add-maui-custom-cell (再判定)

**判定**: VALID

検証日: 2026-08-12 / 対象: `verify-001.md` と同一 (maui-cells / maui-bridge / samples-maui の 17 Requirement / 37 Scenario)。

本ファイルは `verify-001.md` (判定 INVALID) の**再判定**である。オーナーが未記録乖離 1 件について「deviation 記録で合意」を選択し、`deviation.md` に 4 件目が追記されたことを受けて、その 1 点を検査し直した。

---

## 1. 再検査の範囲

`verify-001.md` の対応表 (37 Scenario) を引き継いでよいかを、まず引き継ぎ条件の側から確認した。

| 確認項目 | 結果 |
|---|---|
| 足場の逆流 (proposal / design / specs) | **なし**。3 者とも working tree でクリーン (`git status` に出ない)。`proposal.md` / `design.md` の mtime は 12:13 / 12:14 で起案時のまま |
| コード・テストの変更 | **なし**。`maui/` `ios/` `android/` `samples/` の変更ファイル数は 51 で verify-001 時点と同一。最新 mtime は 21:51 で、verify-001 のテスト実行 (それ以降) より前 |
| `tasks.md` | 変更なし (mtime 20:11) |
| 変わったもの | `deviation.md` のみ (mtime 22:50、3 件 → 4 件) |

実装・テスト・足場がいずれも動いていないため、`verify-001.md` の対応表と 1 節のテスト実行結果 (MAUI 400 / iOS 476 / Android 2320、いずれも 0 failures) はそのまま有効である。したがって再検査は追記された deviation 1 件が該当 Scenario を覆うかどうかに絞った。

---

## 2. 追記された deviation の検査

`deviation.md:5` (4 件目):

> samples-maui / Requirement「パリティ画面 CustomCellDemo を native と同一構成で提供する」Scenario「インライン構成の live 更新が動作する」: spec では「Section ① インライン構成の content 内の操作でバインド値を変更する」→ 指示により WHEN の実行場所を Section ④ (行タップカウンタのピル → Command → 同一行の即時更新) に読み替えて THEN (再設定なしの live 反映) を担保。理由: Section ① は native (iOS / Android) と同一の静的 2 行で、操作要素を足すと同 Requirement のパリティ SHALL (native と構成・文言一致) に違反するため。証跡: ios-final2-parity-05 / android-tapfix-03 (2026-08-12)

`verify-001.md` 6 節で挙げた ❌ の内容と照合する:

| verify-001 が指摘した点 | deviation #4 の記載 | 一致 |
|---|---|---|
| 対象 Scenario の特定 | samples-maui / 当該 Requirement / Scenario 名まで明記 | ✅ |
| 乖離の中身 (WHEN を Section ① で実行できない) | 「WHEN の実行場所を Section ④ へ読み替え」 | ✅ |
| THEN の担保先 (Section ④ 行タップカウンタのピル → Command → 同一行更新) | 同一の経路を明記 | ✅ |
| 理由 (Section ① への操作要素追加はパリティ SHALL に反する) | 同一の理由を明記 | ✅ |
| 証跡 | `ios-final2-parity-05` / `android-tapfix-03` — verify-001 が挙げた 2 枚と同一 | ✅ |

指摘した乖離を過不足なく覆っている。ksn-core `references/delta-spec.md` の「deviation.md に記録済みの乖離は『合意済みの差分』。レビュー・検証は違反として扱わない」に従い、当該 Scenario の状態を **❌ → ⚠️ deviation 記録済み** に改める。

なお記録は合意内容の追認であって、実装は 1 行も変わっていない。Section ④ の行タップカウンタ (`samples/maui/KsSettingsView.Sample.Maui/Pages/CustomCellDemoPage.xaml:90`-`100`) が THEN を満たすことは verify-001 の時点で確認済みで、証跡もそのときのものと同じである。

---

## 3. 最終集計 (37 Scenario)

| 状態 | 件数 | 内訳 |
|---|---|---|
| ✅ 一致 | 34 | verify-001 2〜4 節のとおり (変更なし) |
| ⚠️ deviation 記録済み | 3 | maui-cells「共有 Style の適用が例外にならない」(#1) / maui-bridge「スクロールによるリサイクルで表示が壊れない」(#3) / **samples-maui「インライン構成の live 更新が動作する」(#4 — 本再判定で追加)** |
| ❌ 未記録の欠落・乖離 | **0** | — |

deviation #2 (ReleaseHost 時の空世代再発行) は Scenario ではなく Requirement 内の design 外追加のため上表には数えていない。対応する実装・テストは `CustomCellContentTests.cs:780` で確認済み。

---

## 4. 判定の根拠

ksn-verify の VALID 条件をすべて満たす:

- [x] 全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」— ❌ は 0 件
- [x] 虚偽チェックなし — tasks.md 全 26 タスクの `[x]` に実体を確認済み (verify-001 5 節)
- [x] 逆流なし — proposal / design / specs は起案コミット `cbaab09` 以降、コミットも working tree 変更もなし (本再判定で再確認)
- [x] テスト全件成功 — MAUI 400 / iOS 476 / Android 2320、いずれも 0 failures (verify-001 1 節。以降コード変更なしのため有効)

**判定: VALID** — デルタスペックと実装の一致は成立しており、蒸留 (ksn-distill) へ進める状態にある。
