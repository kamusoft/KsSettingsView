# レビュー結果: maui-appearance-change-tracking (003 回目)

**日付**: 2026-09-15
**判定**: APPROVED

## サマリー

対象は [deviation.md](deviation.md) の `[付随修正]` 記録が、review-002 の NEEDS_DISCUSSION 3 論点 (効果の主張・矛盾した説明・証跡の位置づけ) を残していないかの静的確認のみ。オーナー裁定 (選択肢 1: 機構整備として受け入れ、効果の主張は取り下げ、症状は残存として受容) は記録に反映されており、3 論点のうち 2 つ (効果の主張・矛盾した説明) は解消、1 つ (証跡の位置づけ) は言明としては入ったがファイル単位の対応づけが無く Minor として残る。コード 4 ファイルは前回から不変 (更新時刻はいずれも review-002 出力前) で、再レビューは行っていない。

## 照合した規約

- `cross/comment-policy.md` (always) — 本レビューの成果物と deviation の記述に作業文書パス・変更 ID・通番の混入が無いことのみ確認
- `cross/runtime-behavior-verification.md` (適用のきっかけ: 不具合修正の完了判定) — 下記「確認した観点」参照
- `kasane/lessons/process.md` (L-009 事後判定)、`kasane/lessons/code-review.md` (L-001: 争点は実測で。今回は実測不要の静的確認と指示されたため、証跡画像の実内容の目視で代替)

## 指摘事項

### [🟡 Minor] 証跡ファイルの before / after が実際の写り方と逆の組を含み、対応づけが記録に無い

**該当箇所**: [deviation.md](deviation.md) の `[付随修正]` 配下 1 つ目の箇条書き (証跡の位置づけの行) / `evidence/ios-sample-chrome-*.png`

**問題点**: 4 枚を開いて確認したところ、写っている状態はファイル名の before / after が示唆する A/B と逆の組を含む。

| ファイル | 実際に写っている状態 |
|---|---|
| `ios-sample-chrome-a-before-dark.png` | **症状あり** (戻るボタンが明るい円、大タイトルとステータスバーがライト色のため黒背景上で不可視。行と本文は dark) |
| `ios-sample-chrome-a-after-dark.png` | 追随 (chrome が dark) |
| `ios-sample-chrome-b-before-dark.png` | 追随 (chrome が dark) |
| `ios-sample-chrome-b-after-dark.png` | **症状あり** (a-before と同じ見え方) |

deviation は「修正前後の A/B としては成立していない」と断っているので、名前が A/B を示唆すること自体は裁定と矛盾しない。ただし「どのファイルが症状の記録で、どれが追随した状態か」は記録のどこにも無く、名前から推測すると `b-before` (症状のはずが追随) と `b-after` (追随のはずが症状) で読み違える。この記録は蒸留後も残り、再探索が症状の見え方を参照する一次資料になるため、名前だけを頼りに開いた読み手が逆に読む余地を残している。

**推奨修正**: 証跡の位置づけの行に 1 行足して、症状が写っているファイル (`a-before-dark` / `b-after-dark`) と追随した状態のファイル (`a-after-*` / `b-before-dark`) を名指しで対応づける。ファイル名の改名までは不要 (名前を A/B として読まない旨は既に書かれている)。

### [🔵 Suggestion] 残存症状の行に計測条件が無い (review-002 Suggestion の残り)

**該当箇所**: [deviation.md](deviation.md) の `[付随修正]` 配下「症状の記録 (残存)」

**問題点**: 機種・OS 版・試行回数が症状の記録そのものには無い。ただし直前の箇条書きが `review-001` / `review-002` を名指ししており、両ファイルには機種 (iPhone 17 Pro)・OS 版・回数・切替方法が残る。同じ change 配下でアーカイブされるため、参照による到達は可能で実害は小さい。

**推奨修正**: 症状の記録の行末に「iPhone 17 Pro / iOS 26.x、`simctl ui appearance` で切替、間欠」程度を添えるか、現状のまま review-001 / 002 への参照で足りると判断するか。裁定済みで受容した症状なので発生率の数値までは不要。

## review-002 の 3 論点の対処状況

| review-002 の論点 | 状態 | 根拠 |
|---|---|---|
| 🟠 Major 効果の主張に証拠が無い | **解消** | 「症状への効果は主張しない (オーナー裁定)」の見出し付き箇条書きが入り、「解消済み」の断定が消えた。`[付随修正]` 行の「理由 (最終)」も機構整備 (MAUI と同じ window override へ揃え、iOS Native 単体を対照に使えるようにする) に書き換わっており、発端 (症状・オーナー指示「今対応」) と理由が分離されている |
| 🟡 Minor 機構説明が手順 A の主張と噛み合わない | **解消** | 「window override へ移して解消した」という記述が削除され、残る機構説明 (window に明示の `.dark` を置いても `UINavigationBar` と `NavigationStackHostingController` の解決値が変わらない) と矛盾する文が無くなった。手順 A / B の区別も 1 つの症状記録に統合され、「修正の有無と独立した間欠症状」と明示された |
| 🔵 Suggestion 証跡に計測条件が残っていない | **一部** | 証跡の位置づけ (A/B として成立していない旨) は入った。ファイル対応づけ (上記 Minor) と計測条件 (上記 Suggestion) が残る |

## 確認した観点

- **裁定との整合**: 裁定 (残存症状として受容して閉じる、`NavigationStack` 作り直しは検証目的を壊すため不採用、別 change へも切り出さない) と回避策 (再起動) が記録されている。裁定の選択肢 1 が指示した 3 点 (修正を残す / 効果の主張を取り下げる / Minor・Suggestion を同じ書き換えに含める) のうち前 2 点は満たし、3 点目が上記 2 件の残りに当たる
- **`cross/runtime-behavior-verification.md` との関係**: 完了判定 (再現 → 同一手順で解消確認 → 証跡) は「不具合修正の完了」を主張する場合の規約であり、本記録はその主張自体を取り下げ症状を残存として受容している。規約 3 の「証跡を change 配下に残す」は満たしており、規約 1 / 2 を満たさないまま完了と称する状態にはなっていない。原因分析の規律 (コード読解だけで真因と断定しない) にも反しない — 失敗時の解決値の実測結果が根拠として残っている
- **証跡と症状記述の一致**: 記録の「行 (ライブラリ) と本文は追随するのに大タイトル・ステータスバー・戻るボタンがライトのまま残る」は、`a-before-dark.png` / `b-after-dark.png` の写りと一致する
- **コードの不変**: `samples/ios/KsSettingsViewSample/` の 4 ファイル (`ContentView.swift` / `SampleAppearance.swift` / `SampleAppearanceWindowStyle.swift` / `KsSettingsViewSample.xcodeproj/project.pbxproj`) の更新時刻はすべて review-002 出力より前。HEAD との差分の規模も review-002 の記述と同一。実装の妥当性・最小性・pbxproj 登録・comment-policy・sample-parity は review-001 / review-002 / second-opinion-code-001 の判定を引き継ぐ
- **L-009 の事後判定**: 作業ツリーに現れる 4 ファイルがすべて `[付随修正]` 行の「箇所:」に役割付きで現れている
- **足場の非改変**: `specs/` / `proposal.md` / `design.md` / `exploration.md` に差分なし。`tasks.md` は 18:08 のままで今回の書き換えに巻き込まれていない
- **パス表記**: deviation.md にローカル絶対パスの混入なし

## アクションプラン

1. (Minor) 証跡の位置づけの行に、症状が写っているファイルと追随した状態のファイルの対応づけを 1 行足す
2. (Suggestion) 症状の記録に計測条件を添えるか、review-001 / 002 への参照で足りるとして据え置くかを決める

いずれも 1〜2 行の追記で、蒸留前に同じ書き換えでまとめて処理できる。判定を保留する性質ではないため APPROVED とする。
