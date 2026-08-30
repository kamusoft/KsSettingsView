# Deviation: cleanup-comment-lint-debt

実装フェーズ中にオーナー判断で当初スコープと異なる扱いにした事項を記録する。

## 合意済みのスコープ拡大

- **lint が検出しない規約違反も併せて直す**: tasks.md の完了条件は「lint の禁止件数 0」だったが、同一ブロック内に lint 非検出の規約違反 (裸の change-id 参照・履歴記述・タスク通番) が混在していた。片方だけ直すと不自然な状態が残るため、**書き換え対象ブロックの周辺に限って**併せて修正する方針をオーナーが承認。ファイル全体の総点検はしない (2026-08-06)
- **コメント内の事実誤認の訂正**: 書き換え対象ブロック内でコメントの記述が現状のコードと食い違っている箇所 (例: 「core が Theme / CellStyle を提供する」— 実際は UI 層) を、現状に合わせて訂正することをオーナーが承認。機能コードは変更しない (2026-08-06)
- **lint 非検出の裸 change-id 参照 5 箇所の追加修正**: 「ついでの範囲」に入らなかったブロックに残った `本提案 add-partial-update-native で〜` 等 5 箇所 (iOS 4 / Android 1) を、この change を逃すと恒久的に残るためオーナー判断で追加修正 (2026-08-06)
- **対の片側だけ直したことで顕在化した矛盾の解消**: レビュー (review-002.md) で、修正した記述に対応するプラットフォーム / モジュール側が未修正のまま残り、**対になる公開 API ドキュメントが互いに矛盾する**状態が複数見つかった (例: `RootHeaderFooterAdapter.kt:8` は前回 Major-3 と同じ誤りでテスト側だけ修正済み、`ui/build.gradle.kts:65` は compose 側だけ `Theme` の所属を直した対)。**未変更行であっても、この change の修正によって矛盾が生じた箇所は同じ change で閉じる**方針をオーナーが承認 (2026-08-06)
  - あわせて修正サイクルの工程として、**指摘を直したら対になるプラットフォーム / モジュールの同名・同概念ファイルを必ず突き合わせる**ことを規律に加えた
- **未変更行に残る「実在しない識別子」「禁止類型」3 件の修正**: 対称性チェックの過程で見つかった以下をオーナー判断で修正 (2026-08-06)。(1)(2) は grep で到達できない型名であり、規約がリポジトリ内識別子の参照を許す前提 (「grep で到達でき、消えれば同一コミット内で壊れに気づける」) を満たさないため
  - `ios/Sources/KsSettingsViewUI/DatePickerUIStyle.swift` — 存在しない型 `DatePickerAndroidStyle` を参照 (実際の Android 側の型名は同名の `DatePickerUIStyle`)。同一ブロック内で「Android には対応する型を定義しない」と自己矛盾もしていた
  - `android/ks-settingsview-compose/.../KsIdentifiable.kt` — KDoc のサンプルコードが存在しない `SampleLabelCell` を使用 (実在する `LabelCell` に差し替え)
  - `android/ks-settingsview-ui/.../KsSimpleCheckView.kt` — 「〜の `OnDraw` ロジックを移植したもの」が規約の禁止類型 (時間軸を含む記述)

## 対象外とした事項

- **assertion メッセージ文字列内の議論通番**: `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift` の `XCTAssertEqual(..., "Phase 14.2 で \`.estimated(20)\` に縮めた既定値が維持される")` は、コメントではなくコード (文字列リテラル)。「機能コードを1文字も変えない」という本 change の絶対制約を優先し、**この change では触らない**とオーナーが判断 (2026-08-06)。テスト失敗時に表示される文言としては同種の問題を抱えているため、別途扱う
- **advisory (要確認) 2 件の残置**: 以下は lint の「履歴記述」パターンが自然な日本語の「〜だった」を拾った**誤検知**と判定し、書き換えていない。禁止件数は 0
  - `android/ks-settingsview-ui/src/test/.../VisibilityApplyDiffTest.kt:21` — 「可視性を切り替える差分だったとき」
  - `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1482` — 「削除前の Cell が hidden だった場合」

## レビューサイクルの打ち切り (オーナー判断、2026-08-06)

レビュー上限 (`orchestrator.max-review-cycles: 3`) に到達した時点で、**オーナー判断により Major 2 件のみ修正して完了**とした。残る指摘 (review-003.md の Minor 15 / Suggestion 4) と、レビュアーが提案した引用昇格パターンの機械的洗い出しは**実施しない**。残債務の別 change 起票も行わない。

オーナーの判断理由: 「キリがないし意味がない」。

打ち切り時点で満たされている条件:

- `comment-policy-lint.py` の禁止件数 **0** (765 → 0、検査対象 401 ファイル)
- **機能コードの差分 0** (レビュアーが 3 回とも言語別コメント除去後の正規化比較で 224 ファイル全件一致を機械的に証明)
- Android 1986 tests / iOS 624 tests いずれも 0 failures

修正した Major 2 件:

- `ios/Sources/KsSettingsViewUI/EntryCellView.swift` — 制約設定の記述が実装と逆だった (実際は `fieldWrapper` に Hugging 100 / CCR required)。記述どおりに直すと `secureTextEntry` のレイアウト崩れが再発する
- `android/ks-settingsview-ui/.../CheckboxCell.kt` — 存在しない書き戻し経路を断言していた

残る指摘の性質: review-003.md の分析によれば、指摘 21 件中 14 件は「アーカイブ済み spec / 履歴記述の**引用文**を、削除せずに現在形の断定へ昇格させた」同一パターン。規約の類型 1・3 はいずれも「削除」を指示しており、引用本文の昇格は想定外の操作だった。同種の未発見分がコメント文面に残っている可能性がある。

## スコープ外とし、かつ起票もしないもの (オーナー判断、2026-08-06)

- **CI ゲートの新設** (exploration.md 論点3): 当初は「別 change として起票する」としたが、**起票しない**判断。再発防止は既存の書き込み前 hook (`.claude/hooks/comment-policy-check.py`) に委ねる
- **lint 非検出の残債務** (裸 change-id 参照 30 件 / 22 ファイル、未変更行の矛盾クラスタ、`development.md` 参照): review-002 / review-003 で「別 change として起票」を推奨されたが、**起票しない**判断
- **review-003 の残る指摘** (Minor 15 / Suggestion 4) および引用昇格パターンの機械的洗い出し: 実施しない

この change をもってコメント規約債務の返済は完了扱いとする。
