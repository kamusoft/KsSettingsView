# セカンドオピニオン: add-maui-native-bridge (code-review round 2 / ホスト側 review-002 対応)
**相方**: codex / **日付**: 2026-08-05 / **対象**: 修正サイクル1 + iOS 標準方式差し戻し後の未コミット diff 全体
**注**: second-opinion-001/002 は提案段階 (spec-review) の証跡、-003 は実装レビュー round 1
---
# レビュー結果: add-maui-native-bridge（002 回目）

**日付**: 2026-08-05  
**判定**: **APPROVED**

## サマリー

前回の Major は適切に解消されています。`replaceSection` / `replaceCell` は両 Native 実装で有効 ID を nullable で返し、iOS binding にも正しく反映されています。後続操作・未知 ID・破棄後の戻り値を確認するテストも追加されており、新たな契約破綻は見つかりませんでした。

指摘件数: **Critical 0 / Major 0 / Minor 1 / Suggestion 0**

## 指摘事項

### [🟡 Minor] 単体 xcframework スクリプトの署名コメントが調査結果と矛盾している

**該当箇所**: [ios/binding/build-xcframework.sh](ios/binding/build-xcframework.sh:12)

**問題点**: コメントは「署名を無効化しないと framework が Products へ install されない」と断定しています。しかし [exploration.md](kasane/changes/add-maui-native-bridge/exploration.md:13) では、署名無効化フラグなしでも install・2スライス生成が成立し、当初の失敗原因は scheme 衝突だったと結論づけています。[maui/README.md](maui/README.md:40) も同じ結論へ修正済みです。

実行時の問題はありませんが、再調査済みの誤った因果関係がソースコメントに残っています。

**推奨修正**: 署名無効化が必須という説明を削除するか、「単体スクリプトでは署名環境への依存を避けるため無効化する」程度の事実に限定してください。フラグ自体を残すことは問題ありません。

## 前回指摘への対応評価

- 前回 Major: **解決済み**
  - Swift / Kotlin とも対象存在確認後に identity を維持して置換し、有効 ID または `nil` を返す。
  - [ApiDefinition.cs](maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:372) に nullable 戻り値と契約説明が反映済み。
  - 両 OS の生成 C# 表面も `string?` になっている。
  - 戻り値による後続操作、DTO 自身の ID 破棄、未知・不正 ID の `nil` をテスト済み。
- Metadata.xml: **現方式は妥当**
  - `WhenMappings` は中間 `api.xml` に存在し、`api.xml.fixed` と生成 C# からは除去されている。
  - したがって `remove-node` は実際に機能しており、削除すると公開表面へ漏れるという実測結果と整合する。
  - BG8A00 の二段適用による説明も [Metadata.xml](maui/android/KsSettingsView.Binding.Android/Transforms/Metadata.xml:19) と README で一貫している。
- Binding 方式: **解決済み**
  - iOS は標準 `XcodeProject` 方式へ復帰し、`ios/Sources` を増分入力へ追加している。
  - Android の Exec 方式は [deviation.md](kasane/changes/add-maui-native-bridge/deviation.md:3) に根拠と承認が記録されており、違反として扱わない。
- 前回 Minor 2〜4 / Suggestion 1〜3: **対応済み**
- `rm -rf`: オーナー裁定どおり、CI 用スクリプトとして問題なし。

提示されたビルド・テスト・pack・増分ビルド結果を受け入れ、今回は再実行していません。上記 Minor は承認を妨げない文書整合性の修正です。

---

## 突き合わせ結果 (ksn-orchestrator, 2026-08-05)

ホスト側 review-002.md (CHANGES_REQUESTED: Minor 2 / Suggestion 2) との突き合わせ。判定は割れたが指摘に矛盾はない。

| 指摘 | 出典 | 採否 |
|---|---|---|
| build-xcframework.sh の署名コメントが調査結論 (署名無効化は install の必要条件ではない) と矛盾 | 双方一致 | **確定** (Minor) |
| `_AdjustKsBridgeXcodeProjectInputs` の `Remove` がパス正規化差で不一致、除外が実効しておらず csproj コメント / README の記述が実態と食い違う | ホストのみ (probe 実測 + NormalizeDirectory での修正確認あり) | **確定** (Minor。相方の見逃しとして扱う) |
| 増分入力が framework 非対象の KsSettingsViewSwiftUI まで拾う / 空ディレクトリ ios/binding/KsSettingsViewBridge/ の残存 | ホストのみ | Suggestion として修正サイクルに含める |

- 前サイクル指摘への対応は両者とも妥当と評価 (Metadata.xml の remove-node 維持は review-002 がミューテーションで再実証)
- 修正サイクル2 (cycle 2/3) で上記を処理する
