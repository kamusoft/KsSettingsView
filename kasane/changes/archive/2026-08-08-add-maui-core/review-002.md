# レビュー結果: add-maui-core (002 回目)

**日付**: 2026-08-08
**判定**: APPROVED

## サマリー

修正サイクル1で確定した5項目 (A: iOS containment 順序 / B: 範囲 Move の順序保持 / C: 接続失敗のロールバック / D: `IKsSettingsGateway` の `IDisposable` 除去 / E: SHALL 2件のテスト追加) はいずれも正しく実装され、追加された 14 件のテストは回帰検出力を持つ (naive 実装へ戻すと落ちることを解析で確認)。修正による regression は検出しなかった — 既存 101 件のテストは仕様変更なしに全て通り、3 TFM ビルドと検証ホスト (MauiHost) のビルドも成功する。

指摘は Critical / Major なし、Minor 1 件・Suggestion 2 件。Minor は deviation.md への1行記録の話であり、コード修正は不要。

## 実行した検証

- `dotnet test KsSettingsView.Maui.Tests -f net10.0`: **115 件成功 / 0 失敗 / 0 スキップ** (前回 101 + 14)
- `dotnet build KsSettingsView.Maui.csproj` (net10.0 / net10.0-ios / net10.0-android、`DEVELOPER_DIR=/Applications/Xcode-26.1.1.app`): 成功。警告は **NU1608 のみ (8 件)** で CS 警告 0 — 修正で新たな警告は増えていない
- `dotnet build tests/KsSettingsView.MauiHost` (ios / android): 成功。Handler の API 変化 (`Containment` 追加、gateway の `Dispose` 削除) が検証ホストを壊していないことを確認
- `python3 scripts/comment-policy-lint.py`: 429 ファイル / 禁止 0 件
- 足場アーティファクトの凍結: `proposal.md` / `design.md` / `specs/` の更新時刻はいずれも review-001 出力より前であり、修正サイクル中に書き換えられていない

## 修正項目の対応確認

| 項目 | 実装 | テスト | 判定 |
|---|---|---|---|
| A: iOS containment 順序 | `Handlers/SettingsViewHandler.cs:65-73` で `CreateHost()` → `Containment.AddToParent()` → platform view 返却。`OnHostAttached()` で `ConfirmAdded()` → `ApplyRootAccessory()`。切断は `Remove()` → `ReleaseHost()` → `Containment = null`。iOS 実体は `Platforms/iOS/KsHostContainment.cs` | `HandlerTests.cs:88/105/125` の3件 (`RecordingHostContainment` で手順の並びと、その時点の platform view 有無・gateway 呼び出し件数を記録) | ✅ design Decision 4 の順序どおり |
| B: 範囲 Move の順序保持 | `Internals/KsRangeMove.cs` (方向別の分解)。適用先は `KsSettingsController.MoveSections` / `MoveCells` / `KsItemsSourceBinder.MirrorMove` の3経路 | `ConversionPathTests.cs:232-314` (Section / Cell × 前方 / 後方)、`ItemsSourceTests.cs:197-267` (Cell / Section × 前方 / 後方、生成区間に手動要素あり) の8件 | ✅ |
| C: 接続失敗のロールバック | `KsSettingsController.Connect:97-105` の try / catch → `Disconnect()` → rethrow | `ConversionPathTests.cs:440` (失敗 → ツリー修正 → 再接続 → 以後の構造変更が追跡される) | ✅ |
| D: `IDisposable` の除去 | `IKsSettingsGateway` から `IDisposable` を外し、両 gateway 実装の `Dispose()` も削除。破棄口を持たない理由を interface の remarks に明記 (`IKsSettingsGateway.cs:14-15`) | — (`grep Dispose` で maui 配下に残存なし) | ✅ phase-2 agenda 決定②「Bridge の明示 dispose API は作らない」と整合 |
| E: SHALL 2件のテスト | — | `ConversionPathTests.ReplacingCellsDropsOldCollection` / `ItemsSourceTests.TemplateCreatingAlreadyPlacedCellThrows` | ✅ |

## 確認した観点 (指摘なし)

- **B の正しさ**: `KsRangeMove.Steps` の「移動先は移動対象を除いた後の並びにおける挿入位置」という解釈が、Native Store 実装 (`android/.../SettingsRootStore.kt:105-113`、`ios/.../SettingsRootStore.swift:101-111` — いずれも `removeAt(from)` 後に `to` を post-removal サイズへ clamp して insert) と一致することを両 OS のコードまで遡って照合した。`FakeSettingsGateway.MoveSection` / `MoveCell` の clamp 境界も native と同値になる。前方 (source 固定・destination = `to+count-1`)・後方 (source `from+i`・destination `to+i`) の両分解を4要素の具体例で手計算し、期待順序と一致することを確認
- **回帰検出力**: 範囲 Move テストは旧実装 (1回だけの `MoveSection(from,to)`) では `[s1,s2,s0,s3]` となり期待値 `[s2,s3,s0,s1]` と食い違うため確実に落ちる。containment テストは `AddToParent` を `ConnectHandler` へ移すと `HandlerHadPlatformViewOnAdd` が true になって落ちる。ロールバックテストは catch を外すと `IsConnected` が true で落ちる。いずれもトートロジーではない
- **単一 Move の後退なし**: `count = 1` のとき `KsRangeMove.Steps` は前方・後方とも `(from, to)` 1件のみを返し、修正前と同じ呼び出しになる。`ItemMoveIsMirrored` / `MovingCellIssuesMoveCell` / `MovingSectionIssuesMoveSection` が引き続き通ることで裏付けられている
- **C のロールバック範囲**: `Disconnect()` は登録・購読・dirty set・flush 予約を全て解いて `_gateway` / `_dispatcher` を null に戻す一方、`_root` と root accessory の所有値は保持する (facade の状態であり接続とは独立) — 妥当。両 OS とも `ConnectGateway` が `MakeHost*` より先に走るため、接続失敗時に Native Host が作られたまま孤立する経路はない
- **リーク**: `Containment` は controller / hostView / handler を捕捉するクロージャを強く保持するが、`DisconnectHandler` で `null` 代入されるため切断後の強参照は残らない (`HandlerTests.cs:139` で assert)。既存の `LeakTests` 3件も引き続き成功
- **通知遮断の維持**: `UnregisterCell` / `UnregisterSection` への集約と `HandleSectionPropertyChanged` の生存ガードは修正で崩れていない。`MoveCells` は対応表を引けない Cell を skip するため、除去済み Cell を含む Move でも `updateAccessory` / `moveCell` が未知 ID で飛ぶことはない
- **`ItemsSource` 経路の一貫性**: `MirrorMoveOne` は `_generated` と生成先コレクションの両方を同じ手順で更新し、テストは「生成物の BindingContext 順」「モデル順 == 対応表 == gateway 順」の両方を assert している (`AssertCellOrderIsConsistent` / `AssertSectionOrderIsConsistent`)
- **`Loaded` の複数回発火**: `OnHostAttached()` は再入すると `ConfirmAdded()` (親が既にいれば `DidMoveToParentViewController` の再通知) と `ApplyRootAccessory()` を繰り返すが、どちらも冪等であり実害はない
- **既存資産**: `ios/` / `android/` / `maui/macios/` / `maui/android/` への変更なし (git status で確認)。`maui/tests/shared/KsBridgeScenario.cs` の `DisposeBridge()` は Binding 層の API であり D の変更対象外
- **MauiHost の MA002 警告** (4件): `<UseMaui>` の暗黙パッケージ参照に関する SDK の勧告で、検証ホスト側のみ・出荷物に影響しない。本修正サイクル由来ではないため指摘には挙げない

## 指摘事項

### [🟡 Minor] 親 ViewController の解決手段が design Decision 4 の記述と異なるが deviation.md に記録がない

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/SettingsViewHandler.cs:43-87`、`kasane/changes/add-maui-core/design.md:93`

**問題点**: design Decision 4 は「接続時は handler の `MauiContext` から親 ViewController を解決し」と手段を明記している。実装は `MauiContext` を使わず、①`VirtualView` の `Parent` を遡って最初の `Page` の `ViewController` を取る、②見つからなければ view 階層の responder chain を遡る、の二段構えで解決している。順序契約 (AddChild → view 追加 → DidMove) 自体は満たしており、`MauiContext` から得られるのは window の root ViewController であって Navigation 配下の Page の VC を指せないため、**実装側の解決手段の方が正しい**。ただし design が名指しした手段からの乖離が deviation.md に無記録のまま残ると、後続フェーズが design を読んで `MauiContext` 経由に「直そう」とする誤読を招く。

**推奨修正**: コード修正は不要。deviation.md へ「親 VC の解決は `MauiContext` ではなく Element ツリー + responder chain の二段で行う (理由: MauiContext からは window root VC しか得られず、Navigation 配下の Page VC を指せないため)」の1行を追記する (記録の主体はオーケストレーター)。

### [🔵 Suggestion] iOS 固有の containment 実体には自動テストがなく、フォールバック経路の降格も観測できない

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/iOS/KsHostContainment.cs:38-47`、`maui/KsSettingsView.Maui.Tests/HandlerTests.cs:88-140`

**問題点**: 追加された順序テストは Handler 共通部 (`IKsHostContainment` の呼び出し順) を net10.0 で固定するもので、`KsHostContainment` の中身 (`AddChildViewController` / `DidMoveToParentViewController` / `RemoveFromParentViewController` の実呼び出し) は対象外。この部分の証拠はシミュレータでの実測のみであり、回帰は自動検出できない。あわせて、`CreatePlatformView` 時点で親 VC が解決できなかった場合は `ConfirmAdded()` 内の再試行で登録され、修正が狙った「view 追加前の AddChild」が**黙って** view 追加後にずれる — この降格を観測する手段 (ログ・記録) がない。再接続時の順序についても自動テストはない (共通部のコードパスは接続時と同一のため、追加価値は iOS 実体側に限られる)。

**推奨修正**: 本変更のスコープでは実測 (`runtime-behavior-verification.md` の要件) で足りている。後続フェーズで iOS 側の UI テスト基盤を持つ際に、接続・切断・再接続の containment 呼び出し順を対象に含めることを検討する。降格の観測は、フォールバックで登録した場合のみ内部フラグを立ててテスト/ログから見えるようにする程度で足りる。

### [🔵 Suggestion] Move イベントの `OldItems` が null のときの件数フォールバックが Section 経路と Cell 経路で非対称

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:289` (Section)、同 `:404` (Cell)

**問題点**: Section の Move は `args.OldItems?.Count ?? 1` で「null なら1件」として index ベースで移動するのに対し、Cell の Move は `Items<CellBase>(args.OldItems)` の要素数を件数に使うため、`OldItems` が null の Move 通知では 0 件となり黙って no-op になる。`ObservableCollection` は Move で必ず `OldItems` を設定するため標準経路では起きないが、独自の `INotifyCollectionChanged` 実装 (本変更のテストが `RangeMoveCollection` で示しているように、利用者が用意し得る) が `OldItems` を省くと Section だけ動いて Cell が動かない、という非対称な壊れ方をする。

**推奨修正**: どちらかに揃える。Cell 経路も index ベースで移動できる情報 (`entry.Cells` の位置) を持っているため、`OldItems` が空なら `OldStartingIndex` から1件として扱う方向で揃えるのが自然。後続フェーズの検討で足りる。

## アクションプラン

1. **蒸留前 (オーケストレーター)**: Minor 1 — deviation.md へ親 VC 解決手段の1行を追記
2. **後続フェーズ候補**: Suggestion 2件 (iOS containment の自動検証と降格の可視化、Move の `OldItems` null 時の扱いの統一)
3. **申し送り済みで本サイクル対象外**: review-001 Minor 2 (NU1608) はパッケージングフェーズへ、review-001 Suggestion 4件は後続フェーズ / 蒸留へ (再指摘しない)
