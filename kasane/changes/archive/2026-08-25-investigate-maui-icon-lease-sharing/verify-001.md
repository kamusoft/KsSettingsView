# 一致検証: investigate-maui-icon-lease-sharing (001 回目)

**日付**: 2026-08-25
**判定**: VALID

デルタスペック `specs/maui-cells/spec.md` の ADDED Requirement 1 件 (本文 5 条件 + Scenario 6 件) を実装・テストと突き合わせた。❌ は 0 件。deviation.md は存在せず、記録を要する乖離も検出しなかった。

## 前提ゲートの成立

| ゲート | 内容 | 状態 |
|---|---|---|
| spec 前文 / tasks 1.1-1.2 | probe で iOS の画像インスタンス共有が再現した場合にのみ本デルタスペックが有効 | ✅ 再現 (`exploration.md`「probe 実測: iOS の同一 UIImage 共有」— 環境・asset 種別 3 種・反復 5 回・`ReferenceEquals` と native handle の双方を記録)。撤回条項は発動せず |

## Requirement: 共有 platform 画像の後片付け安全性 (ADDED)

### 本文の条件

| 条件 (spec 本文) | 実装 | テスト | 状態 |
|---|---|---|---|
| いずれかの解決結果の破棄が、表示中の他の解決結果の icon 表示を壊さない | `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:75-111`、iOS 配線 `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:47` | `maui/KsSettingsView.Maui.Tests/IconSharingTests.cs:102` / 実機証跡 `evidence/ios-wiring-before-after.txt` | ✅ 一致 |
| 保護は解決口 (resolver) の世代交代をまたいで働く | `KsSharedImageRegistry.cs:28` (プロセス全域 static)、`KsImageResolver.cs:47` が `Shared` を参照 | `IconSharingTests.cs:72` (`renewImages: true` で resolver 差し替えを確認) | ✅ 一致 |
| 保護は異なる SettingsView の間でも働く | 同上 (所有が controller / resolver ではなく static) | `IconSharingTests.cs:181` (2 つの `SettingsView` / 2 つの controller で同一表を共有) | ✅ 一致 |
| 同一画像を包むリースが残っている間、後片付けは一切実行されない (0 回) | `KsSharedImageRegistry.cs:83-87` | `maui/KsSettingsView.Maui.Tests/SharedImageRegistryTests.cs:34` / `IconSharingTests.cs:102` | ✅ 一致 |
| 最後のリースの破棄で全ての後片付け口が各 1 回実行され、保持が残らない | `KsSharedImageRegistry.cs:88-111` (`_entries.Remove` を先に実施) | `SharedImageRegistryTests.cs:34,70` / `IconSharingTests.cs:130,295` (`TrackedImageCount` が 0) | ✅ 一致 |
| 各リースの破棄は冪等 (多重実行・カウント underflow を起こさない) | `KsImageLease.cs` の `_disposed` + `KsSharedImageRegistry.cs:126-141` (`Handle._released`) | `SharedImageRegistryTests.cs:87,119` | ✅ 一致 |
| 共有されていない画像の後片付けはリース破棄時に直ちに実行される | `KsSharedImageRegistry.cs:83-105` (カウント 1 → 0 で即実行) | `SharedImageRegistryTests.cs:17` / `IconSharingTests.cs:158` | ✅ 一致 |
| 同一 Cell が再解決で表示中と同一画像を受けた場合、配信は行わず旧リースをその場で解放する | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1611-1615` | `IconSharingTests.cs:24` (`ReplaceCell` が 0 件) / `IconSharingTests.cs:52` (flush 前にカウントが 1) | ✅ 一致 |

### Scenario

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一 Cell の再解決で新旧リースが同一画像を包む | `KsSettingsController.cs:1611-1615` | `IconSharingTests.cs:24` `ReresolvingSameCellToSameImageKeepsIconAlive` / `:52` `…ReleasesPreviousLeaseImmediately` | ✅ 一致 |
| 解決口の世代交代をまたいだ共有 | `KsSharedImageRegistry.cs:28` / `KsImageResolver.cs:47` | `IconSharingTests.cs:72` `SharingAcrossResolverGenerationsKeepsIconAlive` (`ReleaseHost` → 別 resolver インスタンス) | ✅ 一致 |
| 2 つの Cell が同一画像を包む | `KsSharedImageRegistry.cs:83-87` | `IconSharingTests.cs:102` `RemovingOneOfTwoCellsSharingImageKeepsTheOtherAlive` / 配信後であることは `:323` `SharedImageCleanupStillHappensAfterNativeDelivery` | ✅ 一致 |
| 控えられない解決結果の即時破棄でも共有画像は守られる | `KsSettingsController.cs` の `CompleteIcon` (世代不一致・通番不一致) と `StoreIcon` 冒頭 (未登録 Cell) — いずれも無変更のまま registry が防御 | `IconSharingTests.cs:213` (追い抜かれた解決) / `:239` (旧世代の解決) / `:266` (登録解除済み Cell への解決) | ✅ 一致 |
| 最後のリースの破棄で後片付けが実行される | `KsSharedImageRegistry.cs:88-105` | `SharedImageRegistryTests.cs:34` / `IconSharingTests.cs:130` `RemovingTheLastSharingCellRunsEveryCleanupOnce` (各 `DisposeCount` が 1) / `:295` `RebuildingRootReleasesEverySharedLeaseExactlyOnce` | ✅ 一致 |
| 非共有画像は従来どおり直ちに後片付けされる | `KsSharedImageRegistry.cs:83-105` / Android `KsImageResolver` は無変更 (表を通らない) | `SharedImageRegistryTests.cs:17` / `IconSharingTests.cs:158` `UnsharedImageIsCleanedUpOnItsOwnRemoval` | ✅ 一致 |

補足 (乖離ではない): spec 本文の不変条件のうち「後片付け口の例外時もカウント整合を壊さない」(tasks 2.1) は `SharedImageRegistryTests.cs:204,226,249` が伝播方針込みで固定している。

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 / 虚偽チェックの有無 | ✅ 1.1〜4.1 の全 8 タスクが `[x]`。いずれも対応する成果物を確認した (1.1/1.2 → `exploration.md` の probe 節、2.1 → `KsSharedImageRegistry.cs`、2.2 → `KsImageResolver.cs:47` + `evidence/ios-wiring-before-after.txt`、2.3 → `KsSettingsController.cs:1611`、3.1 → `Fakes/FakeImageResolver.cs` の `CompleteShared` と `Fakes/GatewayScope.cs` の `Reconnect(renewImages)`、3.2/3.3 → 新規テスト 2 ファイル、4.1 → `exploration.md` の付随確認節)。**虚偽チェックなし** |
| 逆流検査 (足場の書き換え) | ✅ `proposal.md` / `specs/maui-cells/spec.md` に差分なし。`exploration.md` は追記のみ (`git diff` の削除行 0)、`tasks.md` は `[ ]`→`[x]` のみで本文改変なし。どちらも tasks 1.1 / 1.2 / 4.1 が明示的に求めた記録であり逆流にあたらない |
| 未記録乖離 | ✅ なし (❌ 0 件)。diff 中に Scenario へ対応しない変更もない (テスト足場の 2 ファイルは tasks 3.1 に対応) |
| deviation.md | 不在。記録を要する乖離を検出しなかったため妥当 |
| UI 変更 | 対象外 (`ui/` なし、公開 API 変更なし) |
| テスト全件成功 | ✅ `maui/KsSettingsView.Maui.Tests` **462 件成功 / 0 失敗 / 0 スキップ** (レビュー側で実行)。加えて net10.0-ios / net10.0-android のビルドが 0 エラー 0 警告 |

## 判定

**VALID** — 全 Requirement 条件と全 6 Scenario が ✅ 一致。虚偽チェック・逆流・未記録乖離・テスト失敗のいずれも検出しなかった。

なお `review-001.md` の Major 指摘 (共有表が預かる後片付け口の無制限な蓄積) は、デルタスペック文言との一致という観点では乖離にあたらないため本検証では ❌ としていない。品質面の判断は `review-001.md` を参照のこと。
