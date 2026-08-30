# レビュー結果: skills-api-coverage (002 回目)

**日付**: 2026-08-29
**判定**: APPROVED

## サマリー

review-001 の Major 1 件・Minor 2 件はいずれも適切に修正されており、修正によって新たな不整合は生じていない。en/ja 見出し階層一致・コードブロック byte 一致・manifest ハッシュ/網羅性・標準 lint はすべて再検査で通過した。未対応とした Suggestion 3 件の判断も、1 件を除いて妥当と考える (下記「Suggestion 未対応の判断について」)。いずれも承認を保留する性質のものではない。

## 前回指摘の修正確認

### [🟠 Major] iOS Store 公開操作表の全称宣言 → 解消

**確認箇所**: `skills/ja/kssettingsview-ios/references/updates.md:41,46` / `skills/en/kssettingsview-ios/references/updates.md:41,46`

導入文が「`SettingsRootStore` の**主な**公開操作は次のとおり」/ "These are the **main** public operations of `SettingsRootStore`." になり、Header / Footer 行へ `invalidateAccessoryMeasurement(target:)` が追加された。

- 全称表現が消えたことで内容規約⑧ に適合
- 表に載った `invalidateAccessoryMeasurement(target:)` は `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:322` に public 実在。同 Skill の `references/styling.md:224` の使用例との矛盾も解消
- 表の 12 操作すべてが実装のシグネチャと一致することを再確認 (`replaceAll` / `insertSection` / `removeSection` / `moveSection` / `replaceSection` / `insertCell` / `removeCell` / `replaceCell` / `replaceCells` / `moveCell` / `updateAccessory` / `invalidateAccessoryMeasurement` / `applyTheme`)
- 「すべて」を残さなかった理由として挙げられた `SettingsRootStore.preview` (`SettingsRootStore.swift:342` の public static) の存在は事実であり、判断は正しい。掲載除外指定の API を表に載せずに全称も主張しない、という解き方は exploration の決定事項と規約⑧ の両方を満たす最善手
- 後続の「残りも同じ形で呼べる」/ "the rest follow the same pattern" は表内の未レシピ化操作を指す文として読めるため、`preview` を含意する誤読は生じない

### [🟡 Minor] Android DSL scope 記述の所有関係 → 解消

**確認箇所**: `skills/ja/kssettingsview-android/references/updates.md:63` / `skills/en/kssettingsview-android/references/updates.md:63`

「その `section` と、**section ブロック内の** `cell` は返り値を持たない」/ "its `section` — along with the `cell` calls inside a section block — returns nothing" となり、`cell` を `SettingsRootScope` のメンバと読ませる記述が解消した。`SettingsRootScope.kt:29` (`section` のみ) / `:127` (`SectionScope.cell`) の実態と一致する。除外型名 `SectionScope` を出さずに正確さを回復しており、制約と正確性の両立ができている。

### [🟡 Minor] docs-refresh のモード分岐が 3e に未追従 → 解消

**確認箇所**: `.agents/skills/docs-refresh/SKILL.md:57` / `:763`

両行とも「網羅検査 (3c) と API 名網羅検査 (3e)」に更新済み。`--readme-only` 側 (`:125`) と 3e 本文 (`:194`) を含め、4 箇所のモード分岐記述が矛盾なく揃った。`:763` は「未配置 concept の配置判断**と API 名の未掲載候補**を Step 4 に載せ」と理由まで拡張されており、なぜ `--all` でも回すのかが読めるようになっている。

## Suggestion 未対応の判断について (求められた見解)

### 3e のノイズ削減 → 将来改善で妥当

同意する。これは新設した検査の**実用性**の話であり、正しさの話ではない。3e は「報告のみ」で Step 4 の承認を必ず挟む設計なので、ノイズが多くても誤った書き換えは起きない。実運用の負荷が実際に問題になってから、実データを見て畳み方を決めるほうが良い設計になる。先回りでフィルタを入れると、本来拾うべき core concepts 由来の真の漏れまで落とすリスクがある。

### 3e の ja 側限定検査 → 妥当だが、簡易起票を推奨

判断としては受け入れられる (ブロッカーにしない) が、他の 2 件より一段リスクが高いという見解を残す。

- これは実用性ではなく**検出漏れ**の話で、「en 側だけ API 名が落ちる」型の取りこぼしを 3e 自身が構造的に見逃す。3e が塞ごうとした穴 (翻訳時の系統的取りこぼし) と同型の穴が、片言語分だけ残っている
- 6-② (見出し階層) と 6-③ (コードブロック byte 一致) は散文を見ないため、この穴を代わりに埋める検査は現状どこにも無い
- 修正コストは `hay` に `skills/en/{rel}` を連結する数行

とはいえ、en/ja は同一サブエージェントが同一文脈で同時生成する運用 (Step 5 の Skill 単位委譲) であり、片言語だけ落ちる確率は構造的に低い。本 change の S 級スコープ (concepts への名前追記 + docs-refresh 反映) から見て、新設機構のさらなる改良を同梱しない判断は理解できる。

**推奨**: 忘れないよう `kasane/changes/docs-refresh-3e-en-coverage/exploration.md` 相当の簡易起票を 1 本立てておく (「3e の en 側検査」と「ノイズ削減」を同じ change にまとめてよい)。起票さえあれば本 change での未対応は問題ない。

### deviation 記録不要の判断 → 概ね妥当

`exploration.md` の凍結対象は「決定事項」節であり、件数「10個」の扱いは「実装の段取り」節にあるガイダンス。段取りからの逸脱で、かつ本 change が同時に新設した内容規約⑦ に沿った上位互換の解き方なので、記録なしで進めても足場凍結の違反にはあたらない。

一点だけ留保: 段取りの文言は「→12」であり、結果は 10 でも 12 でもない (数値そのものの削除)。蒸留時に段取りと成果物を突き合わせると一瞬引っかかる。ただし diff を見れば規約⑦ 由来だと即座に分かり、`skills/*/kssettingsview-maui/references/updates.md:7` の現行文面自体が説明になっているため、実害は小さい。記録するなら 1 行で足りる、という程度の話であり、しないという判断を覆すほどではない。

## 再検査した機械チェック (すべて通過)

| 検査 | 結果 |
|---|---|
| 6-② en/ja 見出し階層一致 (`targets` 全ペア + `readmes` 言語ペア) | OK |
| 6-③ en/ja コードブロック byte 一致 | OK |
| manifest `concepts` ハッシュ vs 現行ファイル sha256 | 全一致 (stale 0) |
| 3c 網羅検査 (`UNCOVERED` / `DELETED`) | 0 件 |
| `scripts/local-path-lint.py` | 指摘なし |
| `scripts/identity-lint.py` | 指摘なし |
| 追加行の全称表現スキャン (すべて / 常に / 必ず / all / always / every / いずれも) | 新規の未検証全称なし |

最後の項目の内訳: 今回の diff で追加された全称表現は MAUI styling.md の「いずれも bindable property で、対応する `FooProperty` を持つ」のみで、これは `CellBase.cs` 26/26・`SettingsView.cs` 51/51 で全数確認済み。他 2 件 (`every other way of closing discards the change`、`いずれも既定が TwoWay`) は本 change 以前からの記述で、行が `+` 表示になっているのは同じ行に別の文を追記したため。

## 指摘事項

なし (Critical 0 / Major 0 / Minor 0 / Suggestion 0)。

## アクションプラン

1. (任意・本 change 外) 3e の en 側検査とノイズ削減を簡易起票しておく
2. 蒸留 (ksn-distill) へ進んでよい

## 蒸留への申し送り

- `kasane/lessons/inbox/api-enumeration-written-as-inline-list.md` (count 1) は本 change 由来。今回の成果物は全て表形式で書かれており、ルール文の妥当性は成果物側で裏が取れている
- concepts 3 ファイル (`kasane/concepts/{ios,android}/api/*-native-host.md`、`kasane/concepts/maui/api/maui-facade.md`) の追記は review-001 で実装との全数一致を確認済み。timestamp も 3 件とも 2026-08-29
- `kasane/changes/ios-effectivestyle-visibility/exploration.md` は本 change の副産物として独立起票済み (未探索スタブ)
