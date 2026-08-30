# 検証結果: investigate-maui-icon-lease-sharing (002 回目)

**日付**: 2026-08-25
**判定**: VALID

修正サイクル後の working tree を対象に、デルタスペック `specs/maui-cells/spec.md` の Requirement / Scenario と実装・テストを突き合わせた。前提となる probe ゲート (tasks 1.2) は「共有あり」で通過しており、ADDED Requirement は有効。

## テスト実行

| 対象 | 結果 |
|---|---|
| `maui/KsSettingsView.Maui.Tests` (net10.0) | 464 件成功 / 0 失敗 / 0 スキップ |
| `maui/KsSettingsView.Maui` net10.0-ios (`--no-incremental`) | 0 エラー / 0 警告 |
| `maui/KsSettingsView.Maui` net10.0-android | 0 エラー / 0 警告 |

## 対応表

### Requirement: 共有 platform 画像の後片付け安全性 (ADDED)

Requirement 本文の各条項:

| 条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| いずれかの解決結果の破棄が、表示中の他の解決結果の icon 表示を壊さない | `maui/KsSettingsView.Maui/Internals/KsSharedImageRegistry.cs:107-124` / 配線 `maui/KsSettingsView.Maui/Platforms/iOS/KsImageResolver.cs:47` | `maui/KsSettingsView.Maui.Tests/IconSharingTests.cs:103` `:186` | ✅ 一致 |
| 保護は解決口の世代交代をまたいで働く | `KsSharedImageRegistry.cs:39` (プロセス全域 static) | `IconSharingTests.cs:73` `:244` | ✅ 一致 |
| 保護は異なる SettingsView の間でも働く | 同上 | `IconSharingTests.cs:186` | ✅ 一致 |
| 同一画像を包むリースが残っている間、後片付けは 0 回 | `KsSharedImageRegistry.cs:115-119` | `maui/KsSettingsView.Maui.Tests/SharedImageRegistryTests.cs:38` `:259` / `IconSharingTests.cs:103` | ✅ 一致 |
| 最後のリース破棄で、保持されていた全ての後片付け口が各 1 回だけ実行され、保持は残らない | `KsSharedImageRegistry.cs:122-123` (保持は画像 1 つにつき 1 件。`:93` の `Cleanup ??=`) | `SharedImageRegistryTests.cs:118` / `IconSharingTests.cs:134` `:300` | ✅ 一致 (注記 1) |
| 各リースの破棄は冪等で、多重破棄が多重実行や underflow を起こさない | `KsSharedImageRegistry.cs:139-154` (`Handle._released`) | `SharedImageRegistryTests.cs:162` `:188` | ✅ 一致 |
| 共有されていない画像の後片付けは、そのリースの破棄時に直ちに実行される | `KsSharedImageRegistry.cs:115-123` | `SharedImageRegistryTests.cs:17` / `IconSharingTests.cs:163` | ✅ 一致 |
| 同一画像の再解決では native への差し替え配信を行わず、旧リースはその場で解放される | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1611-1615` | `IconSharingTests.cs:25` `:53` | ✅ 一致 |

Scenario:

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一 Cell の再解決で新旧リースが同一画像を包む | `KsSettingsController.cs:1611-1615` | `IconSharingTests.cs:25` (後片付け 0 回・表示維持・`ReplaceCell` 配信なし) / `:53` (退役キューに滞留しない) | ✅ 一致 |
| 解決口の世代交代をまたいだ共有 | `KsSharedImageRegistry.cs:39` + `KsImageResolver.cs:47` | `IconSharingTests.cs:73` (`GatewayScope.Reconnect(renewImages: true)` で resolver インスタンスを差し替え) | ✅ 一致 |
| 2 つの Cell が同一画像を包む | `KsSharedImageRegistry.cs:115-119` | `IconSharingTests.cs:103` / 配信後に後片付けが走ることは `:400` | ✅ 一致 |
| 控えられない解決結果の即時破棄でも共有画像は守られる | `KsSettingsController.cs:1564-1574` (追い抜き・旧世代) / `:1590-1594` (登録解除済み Cell) | `IconSharingTests.cs:218` `:244` `:271` | ✅ 一致 |
| 最後のリースの破棄で後片付けが実行される | `KsSharedImageRegistry.cs:115-123` | `IconSharingTests.cs:134` `:300` / `SharedImageRegistryTests.cs:38` | ✅ 一致 (注記 1) |
| 非共有画像は従来どおり直ちに後片付けされる | `KsSharedImageRegistry.cs:115-123` / iOS 配線 `KsImageResolver.cs:47` | `SharedImageRegistryTests.cs:17` / `IconSharingTests.cs:163` / 実機証跡 `evidence/ios-wiring-before-after.txt` | ✅ 一致 |

**注記 1**: 実装は画像 1 つにつき後片付け口を 1 件だけ保持し、後から預かった口は保持も実行もしない。Requirement / Scenario の文言は「その画像について**保持されていた**全ての後片付け口が各 1 回だけ実行され」であり、保持する集合の大きさを規定していないため、文言との一致は成立している。判定の詳細と前提 (iOS の 4 画像解決サービスが渡す破棄 `Action` は全て `image.Dispose()` / `ImageSourceServiceResult` は固有資源を持たない) の裏取りは `review-002.md` の論点 (i) を参照。

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md 全件完了 / 虚偽チェックなし | ✅ 1.1〜4.1 の 9 件すべて `[x]`。各タスクに対応する実装・テスト・記録の実在を上表および下記で確認した。虚偽なし |
| 逆流検査 (足場の書き換え) | ✅ `proposal.md` `specs/maui-cells/spec.md` に差分なし (`git status` / `git diff HEAD`)。`tasks.md` は `[ ]`→`[x]` のみで本文改変なし。`exploration.md` の変更は tasks 1.1 / 1.2 / 4.1 が明示的に求めた記録の追記と、決着済み論点への取り消し線 (行の削除ではなく打ち消しでの上書き) |
| 未記録乖離 | ✅ 対応表に ❌ なし。diff にあって Scenario に対応しない変更もなし (下記) |
| UI 変更 | 対象外 (`ui/` を持たない変更) |
| テスト全件成功 | ✅ 464 件成功 / 0 失敗 |

**Scenario に対応しない diff の内訳** (いずれも tasks か Requirement 本文に紐づき、付随修正には当たらない):

- `KsSettingsController.cs:1632-1669` (`DisposeRetired` の全件試行 + `AggregateException` 集約) — tasks 2.1 の不変条件「後片付け口の例外時もカウント整合を壊さない (例外の伝播方針をテストで固定)」および Requirement 本文「以後その画像に関する保持は残らない (解放漏れ・エントリ残留をしない)」に対応。テストは `IconSharingTests.cs:361` (先頭 2 件を失敗させても 3 件目が破棄され `TrackedImageCount == 0`)
- `maui/KsSettingsView.Maui.Tests/Fakes/FakeImageResolver.cs` / `Fakes/GatewayScope.cs` — tasks 3.1 のテスト足場整備 (共有表を通す完了経路 `CompleteShared`、resolver インスタンスを差し替える `Reconnect(renewImages:)`)
- `KsSharedImageRegistry.RetainedCleanupCount` (`:52-67`) — 保持が積み上がらないことの観測点。Requirement 本文の「保持は残らない」に紐づく
- `evidence/ios-wiring-before-after.txt` — tasks 2.2 が求めた probe ハーネス上の配線確認
- `exploration.md` の probe 実測記録・付随確認記録 — tasks 1.1 / 4.1 の成果物

## 所見 (判定には影響しないもの)

- `proposal.md` の What Changes 2 は「各解決結果の後片付け口は共有表が保持し、カウント 0 で全てを各 1 回実行する」と書いており、実装 (代表 1 件のみ保持・実行) と字面が食い違う。デルタスペックの Requirement / Scenario には適合しているため対応表は ✅ のままだが、`deviation.md` が存在しないため、この設計変更とその根拠はどのアーティファクトにも記録されていない。**見立て**: 実装を直す必要はなく、deviation として記録して合意するのが適切 (蒸留が proposal の字面から誤った知識を長命層へ運ぶのを防ぐため)。決定は呼び出し元とオーナーの判断
