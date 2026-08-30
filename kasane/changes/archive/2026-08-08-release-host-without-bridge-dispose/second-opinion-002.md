# セカンドオピニオン: release-host-without-bridge-dispose (002 = review-001 対応 / code-review モード)
**相方**: codex / **日付**: 2026-08-08 / **対象**: worktree 未コミット diff 全体 (iOS / Android / maui binding + 検証ホスト)

※ second-opinion-001.md は提案段階 (spec-review) の証跡。本ファイルはホスト側 review-001.md に対応する。

---
# レビュー結果: release-host-without-bridge-dispose

**日付**: 2026-08-08  
**判定**: **CHANGES_REQUESTED**

## サマリー

Critical 0件、Major 1件、Minor 1件です。Host の切断、冪等性、再生成、旧 Host / Context の回収、Binding 公開面には対称な実装・テストがありますが、`updateAccessory` の一部が Host 再生成をまたいで失われ、デルタスペックを満たしていません。

提示されたテスト結果（iOS 633件、Android 2008件、MAUI build・E2E成功）は確認済み前提とし、追加実行はしていません。

## 指摘事項

### [🟠 Major] Root Header / Footer の更新が Host 再生成時に失われる

**該当箇所**:

- [maui/tests/shared/KsBridgeScenario.cs:118](maui/tests/shared/KsBridgeScenario.cs:118)
- [ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:270](ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:270)
- [android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:238](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:238)
- [kasane/changes/release-host-without-bridge-dispose/specs/maui-bridge/spec.md:18](kasane/changes/release-host-without-bridge-dispose/specs/maui-bridge/spec.md:18)

**問題点**:  
両 Store は Root Header / Footer を現在状態へ保存せず、一過性の Diff としてだけ配信します。Host 不在中の Diff は購読者がいないため消失し、再生成された Host へ復元されません。共有 E2E コードもこの欠落を明記しており、証跡画像でも解放前に表示されていた Root Header / Footer が再生成後に消えています。

デルタスペックは「解放中に `updateAccessory` を呼んだ後、新しい Host に更新後の状態が表示される」としており、Root target を除外していません。deviation.md もないため、未合意の仕様逸脱です。

**推奨修正**:  
Bridge または復元可能な状態層で Root Header / Footer の現在値を保持し、`makeHost*` で新 Host へ適用してください。iOS / Android 双方に次の回帰テストを追加してください。

- 解放前に設定した Root Header / Footer が再生成後も残る
- Host 不在中の更新・解除（`text = null`）が再生成後に反映される
- E2E でも Root Header / Footer の復元を合否対象にする

### [🟡 Minor] ID 契約のコメントと実際の利用方法が矛盾している

**該当箇所**:

- [maui/tests/shared/KsBridgeScenarioHandles.cs:7](maui/tests/shared/KsBridgeScenarioHandles.cs:7)
- [maui/tests/shared/KsBridgeScenario.cs:103](maui/tests/shared/KsBridgeScenario.cs:103)

**問題点**:  
コメントは「DTO 自身が公開する ID は Store に存在せず、API の戻り値だけが有効」と断定していますが、実際には `themeCell.CellID`、`languageCell.CellID`、`notification.SectionID` というDTOのプロパティを保存しています。Builder / insert で追加された DTO の ID は Store identity になります。無効になるのは主に replace に渡した新 DTO 側の ID です。

現在のコードは動作しますが、公開契約を誤って説明しており、将来の利用者に誤った実装を促します。

**推奨修正**:  
コメントを「Builder / insert で追加された DTO の ID は有効だが、replace に渡した新 DTO の ID は採用されない」と正確に直すか、追加 API の戻り値を取得・null 検証して `KsBridgeScenarioHandles` に渡してください。

## アクションプラン

1. Root Header / Footer を復元可能な状態として保持し、両OSで再生成時に適用する。
2. Root accessory の更新・解除・再生成テストとE2E確認を追加する。
3. ID 契約コメントを実装と一致させる。
4. 修正後に既存の全件テストとMAUI E2Eを再確認する。


---

## 突き合わせ結果 (ksn-orchestrator, 2026-08-08)

| # | 指摘 | 出典 | 採否 |
|---|---|---|---|
| 1 | Host 再生成で root header / footer が失われる (Major) | 双方一致 (host review-001 / codex) | **確定** — 実装ワーカーも E2E で独立に検出。severity は双方 Major で一致。spec 違反か否かの解釈は割れた (codex: 未合意の仕様逸脱 / host: THEN は「Store 現在状態」限定のため spec 違反ではなく verify VALID)。指摘内容自体は一致しており、扱いは設計判断が必要なため NEEDS_DISCUSSION としてオーナーへ |
| 2 | KsBridgeScenarioHandles の ID 契約コメントが実装と矛盾 (Minor) | codex のみ | **採用** — 該当箇所特定・矛盾内容が具体的で根拠強。ホスト側の見逃しとして扱い、コメントを実態 (insert 系で追加した DTO の ID は identity になる / replace の新 DTO の ID は不採用) に修正済み |
| 3 | concepts maui/api/native-bridge.md の乖離ほか Suggestion 4 件 | host のみ | 修正サイクル対象外 (Suggestion)。蒸留フェーズへ申し送り |

- 採用 1 / 確定 1 / 降格 0 / 未解決 0 (未解決相当の #1 はオーナー判断待ちとして完了報告に含める)
