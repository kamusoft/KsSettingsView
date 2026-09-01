# 検証結果: fix-ios-tapnotifyingrenderer-actor-isolation (001 回目)

**実施**: 相方 (codex) の ksn-verifier / **label**: verify-fix-ios-tapnotifyingrenderer-actor-isolation / **日付**: 2026-09-01
**対象**: HEAD からの working tree 差分と kasane/changes/fix-ios-tapnotifyingrenderer-actor-isolation/ 一式

---


## 対応表

| Requirement / Scenario | 実装の対応箇所 | テスト・証跡の対応箇所 | 判定 |
|---|---|---|---|
| Swift 6 言語モードでのビルド適合 / Swift 6 言語モードでのビルド試行がエラーゼロで成功する | `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:79`（nonisolated handler から Sendable な Bool を分離）、`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:85`（MainActor 隔離）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2423`（プロトコルの MainActor 隔離） | `evidence/swift6-build.txt:3`（パッケージ全体スキーム）、`evidence/swift6-build.txt:4`（Swift 6 一時設定と復元）、`evidence/swift6-build.txt:7`（`-swift-version 6`）、`evidence/swift6-build.txt:10`（error 0件）、`evidence/swift6-build.txt:11`（BUILD SUCCEEDED） | ✅ 一致 |
| 行タップ通知とタッチフィードバックの挙動維持 / 準拠11種すべてが行タップ通知の解決対象であり続ける | `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2423`（プロトコル）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2427`（Command～SimpleCheck）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2433`（Picker～DatePicker）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2438`（Entry）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2442`（Custom） | `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift:17`（11種を生成）、`ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift:32`（全要素を走査）、`ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift:34`（プロトコル解決をassert） | ✅ 一致 |
| 行タップ通知とタッチフィードバックの挙動維持 / 行タップで該当 CellView のハンドラが発火する | `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2406`（didSelectItemAt）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2411`（対象Cell取得）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2412`（tapHandler解決）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2413`（handler呼び出し） | `ios/Tests/KsSettingsViewUITests/CustomCellTests.swift:397`（実経路テスト）、`ios/Tests/KsSettingsViewUITests/CustomCellTests.swift:411`（行タップ実行）、`ios/Tests/KsSettingsViewUITests/CustomCellTests.swift:412`（1回発火をassert）、`evidence/runtime-check.md:13`、`evidence/runtime-check.md:14`（Command／Pickerの実操作） | ✅ 一致 |
| 行タップ通知とタッチフィードバックの挙動維持 / 押下中はハイライト色になり離すと平常時の背景に戻る | `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:78`（handler設置）、`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:82`（押下状態判定）、`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:89`（有効かつ押下中）、`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:90`（選択色）、`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:92`（平常時の実効背景色） | `ios/Tests/KsSettingsViewUITests/KsCellViewSupportTests.swift:93`（往復テスト）、`ios/Tests/KsSettingsViewUITests/KsCellViewSupportTests.swift:107`（押下）、`ios/Tests/KsSettingsViewUITests/KsCellViewSupportTests.swift:108`（選択色を確認）、`ios/Tests/KsSettingsViewUITests/KsCellViewSupportTests.swift:110`（解除）、`ios/Tests/KsSettingsViewUITests/KsCellViewSupportTests.swift:111`（平常色を確認）、`evidence/runtime-check.md:17`、`evidence/runtime-check.md:18`、`evidence/mutation-check.md:15` | ✅ 一致 |
| Controller 解放の維持 / 参照を手放すと Controller が解放される | `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:213`から`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:218`（旧deinitが削除され、初期化部から次の責務へ連続している） | `ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:15`（直接生成経路）、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:18`（autoreleasepool）、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:23`（weak参照）、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:31`（nilをassert） | ✅ 一致 |
| Controller 解放の維持 / Store 経由でも Controller が解放され Store は使い続けられる | `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:125`（Cancellableの自動解除）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:137`（Storeをweak保持）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:274`（Store接続）、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:277`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:283`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:289`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:297`（各購読のweak self） | `ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:34`（Store経路テスト）、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:42`（Controller生成）、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:46`（解放前のDiff配信）、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:56`（Controller解放をassert）、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:59`（解放後のStore操作）、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:60`（操作結果をassert） | ✅ 一致 |

## 追加検査

- tasks.md: 4区分・全タスクが完了済み。対応表および証跡と突き合わせ、未実装なのにチェック済みの項目はない。
- tasks 3.2: 想定したコメント位置と実装の差は `deviation.md:5` に記録済み。代替として購読プロパティ3件のコメントが `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:125`、`:128`、`:131` で更新されているため、⚠️ 合意済み差分として扱う。
- 足場逆流: `proposal.md` と `specs/ios-host/spec.md` に HEAD からの working tree 差分はない。両ファイルの履歴も提案一式を作成した初期コミットのみであり、実装期間中の逆流は認められない。
- 未記録乖離: なし。ソース・テストの変更はすべて6 Scenarioまたは記録済み deviation に対応する。
- 付随修正: `KsCellViewSupport.swift` の無意味な条件downcast除去は tasks 2.2 に記録されており、本務と同じファイル・経路内の局所修正である。
- Swift 6 一時設定: `ios/Package.swift` の差分は0件で、復元済み。
- Swift 6ビルド: error 0件、BUILD SUCCEEDED。残存warningはあるが、本 Requirement の契約は「concurrencyエラーゼロでビルド成功」であり、契約違反ではない。
- 全件テスト: `evidence/ios-test-all.txt:7` に645件・失敗0件、`:8` にTEST SUCCEEDEDを確認。
- 実行時証跡: `runtime-check.md` の記載と6枚のスクリーンショットを照合し、行タップ、Picker通知、押下／解除、画面pop後の表示を確認。
- ビルド・テストは依頼どおり再実行していない。提示されたホスト側結果と保存済み証跡を事実として使用した。

## 判定

**VALID**

全3 Requirement・全6 Scenarioが実装およびテスト／証跡と一致している。❌ は0件。出力先想定は `verify-001.md`。

