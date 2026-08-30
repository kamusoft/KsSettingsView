# セカンドオピニオン: add-maui-custom-cell (code-001)
**相方**: codex / **日付**: 2026-08-12 / **対象**: working tree の HEAD (develop) に対する全変更 (未追跡の新規ファイル含む)
---
## 指摘

### Major 1 — 公開 delegate / listener への必須メソッド追加が破壊的変更になっている

該当箇所:

- [ios/Sources/KsSettingsViewBridge/KsBridgeInteractionDelegate.swift:39](ios/Sources/KsSettingsViewBridge/KsBridgeInteractionDelegate.swift:39)
- [android/ks-settingsview-bridge/.../KsBridgeInteractionListener.kt:38](android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeInteractionListener.kt:38)
- [maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:604](maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:604)
- [kasane/changes/add-maui-custom-cell/proposal.md:23](kasane/changes/add-maui-custom-cell/proposal.md:23)

問題点:

`customCellTapped` が両OSの公開 protocol/interface に必須メソッドとして追加されています。既存利用者の delegate/listener 実装は再コンパイル時に失敗し、既存バイナリでも呼び出し時にメソッド不在となり得ます。.NET binding も `[Abstract]` のため同様です。

これは proposal の「破壊的変更なし」と一致しません。

推奨修正:

- iOS は `@objc optional` として optional chaining で呼ぶか、CustomCell 通知専用の追加 protocol に分離する。
- binding 側も非 abstract の optional メソッドとして公開する。
- Android は既定 no-op 実装、または追加のサブインターフェースに分離する。
- 新メソッドを実装しない従来形の delegate/listener が引き続き使用できる互換性テストを追加する。

### Major 2 — Cell コレクション差し替え時の View 重複検査が native 更新より後になる

該当箇所:

- [maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1393](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1393)
- [同:1419](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1419)
- [同:1953](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1953)
- [同:2054](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2054)

問題点:

`RebuildSectionCells` の事前検査は Cell インスタンスの重複だけで、複数の新規 `CustomCell.Content` が同じ View を参照しているケースを検出しません。

そのため、異なる2 Cellが同じ Viewを持つコレクションを `Section.Cells` に設定すると、次の順になります。

1. 現在の登録を解除
2. `ReplaceSection` で native を更新
3. 1件目を登録
4. 2件目の `PlaceCellContent` で初めて重複例外

さらに `RegisterCell` は Content 検証前に `_cellEntries` などを書き換えるため、例外後は native、登録表、`entry.Cells`、退役待ち lease が部分更新状態になります。`AddCells` の複数件イベントにも同型の問題があります。

推奨修正:

候補 Cell 群の Content と accessory を共通の `HashSet<View>` で、既存配置との衝突も含めて全件事前検証してください。gateway 呼び出しや登録解除は、検証完了後に限定すべきです。

以下を確認する回帰テストも必要です。

- 重複例外前後で gateway 呼び出しがない
- 旧 Cell ID・表示・lease・論理所有が維持される
- 登録表に新規 Cell が部分的に残らない

### Major 3 — `Content` の重複配置例外後に公開値と表示状態が分離する

該当箇所:

- [maui/KsSettingsView.Maui/CustomCell.cs:49](maui/KsSettingsView.Maui/CustomCell.cs:49)
- [maui/KsSettingsView.Maui/Internals/KsAccessoryViewOwnership.cs:70](maui/KsSettingsView.Maui/Internals/KsAccessoryViewOwnership.cs:70)
- [maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:749](maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:749)

問題点:

配置済み Cell の `Content` を、別 Cell が使用中の View に変更すると、BindableProperty の callback が先に旧 View を論理ツリーから外します。その後 controller が重複を検出して例外を送出しますが、BindableProperty 値と論理所有は復元されません。

結果として、例えば Cell B が旧 View B を表示中に Cell A の View A を設定して失敗すると、

- `B.Content` は View A
- native と controller の lease は旧 View B
- View B は論理親を失う

という不整合が残ります。既存テストは空の Cell への重複設定のみで、このケースを捕捉していません。

推奨修正:

配置済み Cell では、旧所有を変更する前に重複検査できる経路へ移すか、失敗時に BindableProperty・論理所有・controller 状態を確実にロールバックしてください。既存 Content を持つ Cellへの重複設定で、例外後も旧 Content、Parent、platform view、token が維持されるテストを追加してください。

### Minor 1 — バッチ配信テストが呼び出し時点の payload を保存していない

該当箇所:

- [maui/KsSettingsView.Maui.Tests/Fakes/FakeSettingsGateway.cs:202](maui/KsSettingsView.Maui.Tests/Fakes/FakeSettingsGateway.cs:202)
- [maui/KsSettingsView.Maui.Tests/Fakes/GatewayCall.cs:64](maui/KsSettingsView.Maui.Tests/Fakes/GatewayCall.cs:64)
- [maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:93](maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:93)
- [同:118](maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:118)

問題点:

単発 `ReplaceCell` は呼び出し時点の snapshot と platform view を保存しますが、`ReplaceCells` は可変な Cell 参照をそのまま記録します。新規テストも件数しか検証しておらず、Host attach 時に実体付き DTO、release 時に実体なしの新世代が実際に渡されたことを保証できません。

推奨修正:

バッチについても各更新の `CellId`、snapshot、content platform view を呼び出し時に不変値として保存し、attach/release 双方の token と view を検証してください。

### Minor 2 — iOS の最終証跡セットに再接続復元が含まれていない

該当箇所:

- [kasane/changes/add-maui-custom-cell/tasks.md:49](kasane/changes/add-maui-custom-cell/tasks.md:49)
- [screenshots/](kasane/changes/add-maui-custom-cell/screenshots)

問題点:

タスク 7.3 は完了扱いですが、指定された最終版 `ios-final2-*` には、切断中に差し替えた Content と再接続記録を示す画像がありません。旧 `ios-fix4-*` には有効な画像がありますが、最終版として指定された証跡集合だけでは確認できません。Android の `android-reverify-*` には両方あります。

推奨修正:

最終バイナリで再接続を再確認し、`ios-final2-specific-*reconnect-restored.png` と再接続記録の証跡を追加してください。

## 総合判定

**CHANGES_REQUESTED**

Critical 0 / Major 3 / Minor 2 / Suggestion 0。

指定された3件の合意済み deviation は違反として扱っていません。レビューは静的に実施し、ビルド・テスト結果は提示された全件成功を前提としました。

## 突き合わせ結果 (ホスト裁定: 2026-08-12)

ホスト側 (review-001.md): CHANGES_REQUESTED — Major 1 (Android タップ操作証跡なし) / Minor 1 (AddCells バッチの View 重複事前検査漏れ) / Suggestion 2。

| 指摘 | 採否 | 根拠 |
|---|---|---|
| 相方 Major 1 (delegate 必須メソッド追加が破壊的) | **降格** | bridge (KsSettingsViewBridge / ks-settingsview-bridge) は MAUI binding 専用の内部輸送層で、delegate/listener の実装者は同一リポジトリの relay のみ (maui/ADR-0003 の単一 delegate 集約)。過去の Cell 追加 (CommandCellTapped / ButtonCellTapped 等) も全て必須メソッドとして追加してきた確立パターンで、消費者は同一変更内で同時更新される。proposal の「破壊的変更なし」は利用者向け公開面 (facade / native UI) を指す従来解釈と整合。相方に無いプロジェクト文脈由来の指摘 (L-002 の型) |
| 相方 Major 2 (コレクション差し替え時の View 重複検査が native 更新より後) | **確定 (Major)** | ホスト Minor 1 と双方一致。相方が Section.Cells 差し替え経路と部分更新残留 (native / 登録表 / lease) まで特定しており、規則に従い高い方の重要度を採る。修正サイクル対象 |
| 相方 Major 3 (Content 重複配置例外後の公開値と表示状態の分離) | **採用 (Major)** | 相方のみだが該当箇所特定 + 実害シナリオ (B.Content=A / lease=B / B の論理親喪失) が具体的。ホスト側の見逃しとして扱う。修正サイクル対象 |
| 相方 Minor 1 (バッチ配信テストの payload 未保存) | **採用 (Minor)** | fix1 (バッチ配信) の再発防止テストの検証力に直結する具体的指摘。修正サイクル対象 |
| 相方 Minor 2 (ios-final2 に再接続証跡なし) | **採用 (Minor)** | L-003(3) の証跡範囲問題として妥当。最終バイナリでの再接続証跡を追加取得する |
| ホスト Major 1 (Android タップ操作証跡なし) | 確定 (相方は対象外の観点) | 証跡取得を実施中 |

未解決 (相方との矛盾): なし
