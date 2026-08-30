# セカンドオピニオン: rollout-user-skills (code-001)
**相方**: codex / **label**: so-code-rollout-user-skills / **日付**: 2026-08-26 / **対象**: skills/ 一式 (新規 8 部 + 索引 2 枚) + aiforms-spec-summary 移送 + ルート README 導線 + manifest-draft.json
---
## 判定: CHANGES_REQUESTED

Critical 0 / Major 4 / Minor 1 / Suggestion 0

### Major

1. `skills/README.md:16` / `skills/README_ja.md:16` — コピー手順が利用者プロジェクトでは成立しない

問題点: `cp -R skills/<lang>/<skill-name> .agents/skills/` は、KsSettingsView リポジトリ内で実行すると同リポジトリへコピーし、利用者プロジェクト内で実行するとコピー元の `skills/` が存在しない。説明されている「自分のプロジェクトへコピー」を実現できない。

推奨修正: コピー元リポジトリとコピー先プロジェクトを明示する。

```bash
cp -R <kssettingsview-repo>/skills/<lang>/<skill-name> \
  <consumer-project>/.agents/skills/
```

取得から行える手順を案内する場合は、clone/download を含む一貫した手順にする。

2. `skills/ja/kssettingsview-ios/SKILL.md:28` ほか全言語・全プラットフォームの `SKILL.md:28` — 単体コピー後に索引リンクが壊れる

問題点: `../../README_ja.md` / `../../README.md` は、Skill を `.agents/skills/<skill-name>/` へコピーすると `.agents/README*.md` を指し、存在しない。さらに移行 Skill の `skills/ja/kssettingsview-aiforms-migration/SKILL.md:24` は、単体コピーでは存在しない兄弟 Skill を参照している。`skills/README_ja.md:30` の「自己完結」と矛盾する。

推奨修正: 索引へのリンクを安定した公開リポジトリ URL にするか、必要な導入情報を各 Skill 内へ収容する。移行 Skill の兄弟参照も公開 URL にするか、MAUI Skill も必要であることをコピー手順へ明記する。単一 Skill だけを一時配置した状態でリンク解決を検査する。

3. `skills/{en,ja}/kssettingsview-aiforms-migration/references/api-mapping.md:183,193,201` — 移植元 API の型が誤っており、公開メンバーも欠落している

問題点:

- `DatePickerCell.Date` は旧実装では `DateTime?` だが、表では `DateTime` と記載されている。
- `NumberPickerCell.Number` は旧実装では `int?`、新実装では `int` だが、nullability の非互換が説明されていない。
- `TextPickerCell.SelectedItem` は旧実装では `object` だが `string` と記載されている。
- 旧公開 API の `EntryCell.Completed`、`CellBase.Tapped`、`PickerCell.UsePickToClose` / `Padding` / `ShowCommand` が対応表にない。

移植元の根拠はそれぞれ `../AiForms.Maui.SettingsView/SettingsView/Cells/DatePickerCell.cs:13`、`NumberPickerCell.cs:14`、`TextPickerCell.cs:79`、`EntryCell.cs:170`、`CellBase.cs:12`、`PickerCell.cs:308,329,414`。凍結された `aiforms-spec-summary.md` にも古い型が残っているが、デルタスペックは実装コードによる最終確認と drift 報告を要求している。

推奨修正: 移植元の全公開宣言からメンバー一覧を再作成し、en/ja 両方の型・nullability・既定値・代替手段を修正する。凍結 concept は独断で変更せず、相違を drift として記録する。

4. `skills/{en,ja}/kssettingsview-android/references/cells.md:3,46,143` — 「完動コード」の前提 import が不足している

問題点: リード文では最小コードの import と Cell 関数の import だけを前提としているが、例は `Color.Red`、`InputType`、`LocalDate`、`LocalTime`、`KsImage` など追加 import を必要とする。`references/custom-cells.md:3` も Compose の `Row`、`Text`、`Slider`、`Modifier`、`Alignment`、`dp`、delegate 演算子などを使用するが、記載された前提ではコンパイルできない。デルタスペックの「完動コード」「利用者がコピーして動くこと」を満たさない。

推奨修正: 各レシピへ必要な import を含めるか、ファイル冒頭に完全な共通 import 一覧を示す。可能ならコードブロックを抽出してコンパイルする検査を追加する。

### Minor

5. `skills/{en,ja}/kssettingsview-maui/references/updates.md:7-21` — TwoWay プロパティ数と「それ以外は OneWay」が誤っている

問題点: `PickerCell.SelectedItemProperty` も `maui/KsSettingsView.Maui/PickerCell.cs:64-69` で `BindingMode.TwoWay` と宣言されている。移行 Skill の `api-mapping.md:152` では正しく TwoWay と記載されており、Skill 間でも不一致になっている。

推奨修正: `SelectedItem` を一覧へ追加する。「Native 操作から直接書き戻される10項目」と「`SelectedIndex` から導出される TwoWay の `SelectedItem`」を分けて説明する。

ホスト実施済みの機械検査10種は通過済みとして扱い、再実行していません。en/ja の節構成・コード同一性、frontmatter、ja description の英語キーワード、manifest 草案については追加の指摘はありません。


## 突き合わせ結果 (ホスト側 review-001-{ios,android,maui,aiforms-migration,common}.md との対照)

| # | 相方の指摘 | 採否 | 対応するホスト側指摘 |
|---|---|---|---|
| 1 | 索引のコピー手順が利用者プロジェクトで成立しない | **確定** (Major) | review-001-common.md Major 1 と一致 |
| 2 | 単体コピー後に SKILL.md の索引リンク・移行 Skill の兄弟リンクが壊れる | **確定** (Major) | review-001-common.md Major 2 / Minor (兄弟リンク) と一致 |
| 3 | api-mapping の旧 API 側の型誤り (DateTime? / int? / object) と公開メンバー欠落 (EntryCell.Completed, CellBase.Tapped, PickerCell.UsePickToClose/Padding/ShowCommand) | **確定** (Major) | review-001-aiforms-migration.md Major 2/4/5 と一致 (ホスト側はさらに Critical: RadioCell.GroupProperty の実在しない名前を検出) |
| 4 | android cells.md / custom-cells.md のリード文の import 前提ではコンパイル不能 | **確定** (Major) | review-001-android.md Major 2 と一致 (ホスト側は実コンパイルで再現) |
| 5 | maui updates.md の TwoWay 一覧に PickerCell.SelectedItem が欠落 (「それ以外は OneWay」が誤り) | **採用** (Minor) | 相方のみの指摘。オーケストレーターが maui/KsSettingsView.Maui/PickerCell.cs の SelectedItemProperty (defaultBindingMode: BindingMode.TwoWay) を直接確認し根拠強と判定 |

- 未解決・矛盾: なし。採用 5 / 降格 0
- 採用分はホスト側指摘と同格として修正サイクルへ含める
