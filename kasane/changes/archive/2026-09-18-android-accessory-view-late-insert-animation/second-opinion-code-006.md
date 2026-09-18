# セカンドオピニオン: android-accessory-view-late-insert-animation (code-006)
**相方**: codex / **label**: so-code-android-accessory-view-late-insert-animation / **日付**: 2026-09-18 / **対象**: 修正サイクル 5 後の maui/ の切断時の後片付け (範囲を 1 点に限定、ベース HEAD)
---
## 判定: APPROVED

- Critical: 0件
- Major: 0件
- Minor: 0件
- Suggestion: 0件

### 確認結果

前回の Major 1 は解消しています。

- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:406`
  - native への退役通知に失敗した lease を `deferred` に集め、`ReleaseHost` 呼び出し後の `:413-417` で破棄しています。

- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:790`
  - Section accessory は書き戻し成功時だけ通常の破棄対象になります。
  - 失敗した slot の lease だけが `:808` で延期されます。
  - Root accessory は既存契約どおり、Host 解放前に破棄されます。

- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1211`
  - CustomCell の内容なし世代の配信が失敗した場合、バッチの到達範囲を判断できないため、全対象 lease を `:1225` で延期しています。
  - 配信成功時は従来どおり、配信後・Host 解放前に破棄されます。

正常系の順序も壊れていません。accessory は「書き戻し → lease 破棄 → Host 解放」、CustomCell は「内容なし世代の配信 → lease 破棄 → Host 解放」の順で、`maui/ADR-0016` の退役順序を維持しています。

新規テストも回帰検出力があります。

- `maui/KsSettingsView.Maui.Tests/AccessoryViewTests.cs:1120`
  - 失敗したSection leaseがHost解放後、書き戻し不要のRoot leaseがHost解放前に破棄されることを、破棄時点の呼び出し記録で確認しています。
  - 2件の書き戻し失敗により、全件試行も確認できています。

- `maui/KsSettingsView.Maui.Tests/CustomCellContentTests.cs:477`
  - バッチ配信失敗時に全content leaseがHost解放後まで延期されることを確認しています。

- `maui/KsSettingsView.Maui.Tests/Fakes/FakeSettingsGateway.cs:68`
  - accessory更新と単発・バッチCell更新の両方に、対象経路を直接失敗させる seam が追加されています。

今回の限定修正自体による新しい欠陥は見つかりませんでした。提示されたMAUI 589件成功、全TFMビルド成功、lint成功は客観的事実として扱い、こちらではテストを再実行していません。

## 申し送り

新規の申し送りはありません。`DisconnectHandler` の逐次実行、native Host解放自体の失敗、その他の多重障害は、オーナー裁定どおり `kasane/changes/maui-teardown-failure-safety` の範囲として判定・指摘件数から除外しました。


## 突き合わせ結果

突き合わせ相手: review-006.md (ホスト側、APPROVED / 指摘 0)

- **確定**: 双方 APPROVED・指摘 0 件。前回の Major (書き戻し・配信に失敗した実体の破棄時機) は双方が解消と確認
- **採用 / 降格 / 未解決**: なし
- 申し送り: ホスト側の 2 件 (配信成功後の破棄ループの全件試行がテスト未固定 / リースを外すループの途中の例外で取り出し済みの実体を取りこぼす) を `kasane/changes/maui-teardown-failure-safety/exploration.md` へ追記した。相方の申し送りは無し
