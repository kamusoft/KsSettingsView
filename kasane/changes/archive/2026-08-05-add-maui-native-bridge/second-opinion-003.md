# セカンドオピニオン: add-maui-native-bridge (code-review / ホスト側 review-001 対応)
**相方**: codex / **日付**: 2026-08-05 / **対象**: 未コミット実装 diff 全体 (untracked 含む)
**注**: second-opinion-001/002 は提案段階 (spec-review) の証跡。本ファイルは実装レビュー並走分
---
# レビュー結果: add-maui-native-bridge

**日付**: 2026-08-05  
**判定**: **CHANGES_REQUESTED**

## サマリー

Bridge の公開操作、ID・lifecycle 契約、iOS のバッチ更新、Native Host、両OSの契約テストは概ねアーティファクトと整合しています。一方、本番 Binding プロジェクトが確定済みの方式から無記録で逸脱しており、完了チェックとも矛盾するため承認できません。

指摘件数: **Critical 0 / Major 1 / Minor 2 / Suggestion 0**

## 指摘事項

### [🟠 Major] 本番 Binding が合意済みのプロジェクト参照形式を使用していない

**該当箇所**:

- [proposal.md](kasane/changes/add-maui-native-bridge/proposal.md:11)
- [tasks.md](kasane/changes/add-maui-native-bridge/tasks.md:34)
- [KsSettingsView.Binding.iOS.csproj](maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj:41)
- [KsSettingsView.Binding.Android.csproj](maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj:30)

**問題点**: Proposal と tasks 5.1/5.2 は、本番 Binding csproj を `XcodeProject`／`AndroidGradleProject` 形式で新設すると明記しています。しかし実装は、iOS がスクリプト実行と `NativeReference`、Android が Gradle の直接実行と生成済み AAR の `AndroidLibrary` 登録です。両タスクは完了扱いですが、記載された成果物は存在しません。

spike が指定形式を使用していても、それはタスク1の成功ゲートを満たすものであり、本番プロジェクトを要求するタスク5.1/5.2の代替にはなりません。`deviation.md` もないため、現状は未合意の逸脱です。ビルドおよび実機疎通の成功によって、このアーティファクト不整合は解消されません。

**推奨修正**: 次のいずれかをオーナー判断のもとで解決してください。

1. 本番 Binding を要求どおり `XcodeProject`／`AndroidGradleProject` 参照へ変更する。
2. SDK上の制約から現在の方式が必要なら、その理由・影響・採用方式について合意を取り、`deviation.md` に記録する。

足場アーティファクト自体を実装都合で書き換えるべきではありません。

### [🟡 Minor] 削除処理に禁止されている `rm` が残っている

**該当箇所**: [build-xcframework.sh](ios/binding/build-xcframework.sh:55)

**問題点**: `trash` が失敗すると `rm -rf` へフォールバックします。これはプロジェクトの「削除コマンドは `rm` ではなく `trash` を使用する」という明示規約に反します。

**推奨修正**: `rm -rf` のフォールバックを削除してください。`trash` が利用できない場合は、対象パスを示して明示的に失敗させるのが安全です。

### [🟡 Minor] DerivedData に関する README の制約が検証結果と矛盾している

**該当箇所**: [maui/README.md](maui/README.md:74)

**問題点**: README は DerivedData を残すとデバイス archive が失敗すると断定しています。一方、提示された検証結果では、同じスクリプトがDerivedDataを保持したまま10回連続で成功しています。現状の記述は利用者に不要な削除作業を要求し、現在のツールチェーン挙動とも一致しません。

**推奨修正**: 再現条件が残っているなら条件を限定して記載し、再現しないなら制約を削除してください。README の変更はプロジェクト規約に従い、ユーザーから明示的に依頼された `docs-refresh` 経由で行う必要があります。

## アクションプラン

1. Binding 方式の逸脱を解消するか、オーナー合意済み deviation として記録する。
2. `rm -rf` のフォールバックを除去する。
3. DerivedData の記述を現行の検証結果に合わせて更新する。

指定されたビルド・テスト・実行時疎通結果は成功として受け入れ、再実行していません。既知の申し送り5件は重複指摘していません。また、制約に従い `review-001.md` を含むファイルへの書き込みは行っていません。

---

## 突き合わせ結果 (ksn-orchestrator, 2026-08-05)

ホスト側 review-001.md (CHANGES_REQUESTED: Major 1 / Minor 4 / Suggestion 3) との突き合わせ。両者の Major は互いに別件で矛盾なし。

| 相方の指摘 | 採否 | 根拠 |
|---|---|---|
| Major: Binding csproj が proposal/tasks の XcodeProject / AndroidGradleProject 形式から無記録で逸脱 | **採用** (ホスト側の見逃し) | proposal.md:11 / tasks.md 5.1-5.2 の明記と実装の不一致は事実。deviation.md 未記録の未合意乖離。解決方法 (実装修正 or deviation 記録) はオーナー判断へ |
| Minor: build-xcframework.sh の `rm -rf` フォールバックが trash 規約違反 | **採用** | プロジェクト明示規約 (CLAUDE.md / AGENTS.md) への違反で該当箇所特定済み |
| Minor: README の DerivedData 制約記述が検証結果 (10連続成功) と矛盾 | **採用** | scheme 修正前の記述が残存した実態不一致。ホスト側 Suggestion (build-xcframework.sh の -derivedDataPath 化) と同根で、あわせて解消する |

- 確定 (双方一致): 0件 (Major が別件のため)
- 採用 (相方のみ+根拠強): 3件 — 以後ホスト側指摘と同格に修正サイクルへ
- 降格: 0件 / 未解決 (矛盾): 0件

### 追記: オーナー裁定 (2026-08-05)

- **`rm -rf` フォールバック指摘は的外れとして却下** — trash 規約はエージェントのツール操作の規律であり、リポジトリ組み込みスクリプトには及ばない。trash は環境依存で CI で実行できないため、スクリプトはむしろ trash 側を除去して rm に統一する (逆方向の修正を実施)。ksn-lesson 捕捉済み: `lessons/inbox/trash-rule-scope-is-agent-tools-not-scripts.md`
- 採用は **2件** に訂正 (Binding 方式の乖離 / README の DerivedData 記述矛盾)
- Binding 方式の乖離は「SDK 制約」という実装側の主張自体をオーナーが疑義とし、ksn-dual-research による徹底調査 (公式ドキュメント裏取り) を指示。よほどの根拠がない限り標準方式へ倒す方針
