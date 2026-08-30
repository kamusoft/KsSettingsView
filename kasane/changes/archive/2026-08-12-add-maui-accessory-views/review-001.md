# レビュー結果: add-maui-accessory-views (001 回目)

**日付**: 2026-08-12
**判定**: CHANGES_REQUESTED

## サマリー

L 級の実装として質は高い。materializer seam・自己計測 wrapper・accessory slot の所有状態機械 (退役順序 = Store 更新 → native 配信 → 旧実体破棄) はいずれも design.md の Decision 1 / 3 / 5 に忠実で、既存の icon lease / text accessory のイディオムを正しく踏襲している。deviation.md 記載の 2 件 (案A の native 再計算口・iOS の superview 剥がし) も記載どおりの形で実装されており、無断の仕様逸脱・tasks.md の虚偽チェック・足場アーティファクトの書き換えは見つからなかった。全 platform のテストは green (iOS 476 / Android 2280 / MAUI 322、いずれも failures 0)。comment-policy lint も 0 件。

一方で、**本 change で新規に追加されたテストの中に検出力ゼロのものが 2 件**ある (アサーション皆無の 1 件、トートロジーの 1 件)。いずれも実測で確認済みで、修正コストは各 1〜2 行。lessons/code-review.md の重点観点 L-001 が正面から扱う型のため、Minor ながら優先度は高いと判断し CHANGES_REQUESTED とする。仕様充足・堅牢性・設計品質の側に Critical / Major は無い。

## 指摘事項

### [🟡 Minor (優先度: 高)] アサーションを 1 つも持たないテストが恒久テストスイートに入っている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/AccessoryViewSwapProbeTest.kt:203-211`

**問題点**:
`detach なし factory で別の親に付いた view を設定すると何が起きるか` は `runCatching { ... }` の結果を `println` するだけで、`assert*` を 1 つも持たない。何が起きても必ず pass するため回帰検出力がゼロで、テストスイートの実行時間と読み手の注意だけを消費する。実測でも同ファイル内で唯一アサーション数 0 のテストであることを確認した (他 4 件は 1〜3 件)。

さらに、このテストが本来押さえるべき事実 — 「detach を入れないと `IllegalStateException` になる」 — は deviation.md に「対策なしだと `IllegalStateException` を実証」として明記されており、`KsBridgeAccessoryView.anyView` の detach 実装の存在理由そのものである。つまり検証すべき対象は確定しているのに、それを固定していない。テスト名が疑問形 (「〜すると何が起きるか」) であることも、調査用プローブがそのまま残った状態を示している。

**推奨修正**:
`assertThrows<IllegalStateException>` 相当で例外型を固定し、テスト名を事実を述べる形 (例: `detach なし factory で別の親に付いた view を設定すると IllegalStateException になる`) に改める。Robolectric 上で例外型が安定しない等の理由で固定できないなら、このテストは削除して deviation.md の記録に委ねる。

### [🟡 Minor] リークテストの `Handler` アサーションがトートロジー

**該当箇所**: `maui/KsSettingsView.Maui.Tests/LeakTests.cs:127`

**問題点**:
`Assert.That(accessory.Handler, Is.Null);` は net10.0 テストでは常に成立する。`FakeViewMaterializer.Materialize` は `ToPlatform` / `ToHandler` を一切呼ばず `FakePlatformView` を返すだけなので、accessory の `Handler` はテストの全期間を通じて null のままである。

実測で確認済み: `retire(view, section, scope)` の**直前**に同じアサーションを差し込んで `LeakTests` を実行したところ 9 件すべて pass した (確認後、`shasum` 一致でファイルを原状復帰済み)。したがってこのアサーションは退役操作の前後を区別できず、直前のコメント「退役の対象は実体だけであり、facade が持つ View 自体は生きたままであることを確かめる」が主張する内容を実際には確かめていない (「View が生きている」ことを確かめているのは `GC.KeepAlive` だが、これはアサーションではない)。

なお、同テストの主眼である `GcProbe.AssertCollected(host, ...)` には検出力がある (fake lease の `PlatformView` が controller から手放されたことを実測する) ため、tasks 6.2 の「旧 wrapper の platform 実体が回収されること」は満たされている。満たされていないのは **Handler の切断**の部分であり、これは fake seam の構造上 net10.0 では検証できない (実体は E2E とスクリーンショットで担保されている)。

**推奨修正**:
このアサーションを削除し、コメントを実際に確かめている内容へ書き直す。Handler 切断が net10.0 では検証範囲外であることを明示したいなら、その旨をコメントに書く (`IKsViewLease` 実装側の責務であり platform テストの担当、という位置づけ)。

### [🔵 Suggestion] プローブテストのデバッグ出力が恒久テストに残っている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/AccessoryViewSwapProbeTest.kt:148,161,175,178,193,209,210` / `AccessoryViewLiveResizeProbeTest.kt:94,103`

**問題点**: tasks 1.1 / 1.2 の調査時に入れたと思われる `println` が 9 箇所残っている。両ファイルとも (上記 1 件を除き) アサーション自体は適切に書かれており、事実を固定する特性化テストとしての価値はあるが、`println` は Gradle のテスト出力を汚すだけで検証には寄与しない。アサーションのメッセージ側に期待値が既に書かれているため情報の重複でもある。

**推奨修正**: `println` を削除する。値の可視化が必要なら assert のメッセージへ寄せる。

### [🔵 Suggestion] `AttachViews` の doc comment が実際の実体化タイミングと食い違う

**該当箇所**: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:91-94` (`AttachViews` の `<remarks>`)

**問題点**:
「実体化そのものは Host が view 階層へ取り付けられた後 (`ApplyAccessories`) に行う — 取り付け前に作ると Android で表示に届かないため」と書かれているが、実装では seam が差し込まれた後に `SetAccessoryView` が呼ばれれば `Materialize` はその場で走り、`DeliverAccessory` も即座に配信する。`ApplyAccessories` まで待つのは「seam がまだ差し込まれていない間 (= `Connect` 中の `RebuildRoot`)」に限られる。

実害はない (取り付け前に配信されても `ApplyAccessories` が同じ slot を再配信するため表示は収束する) が、このファイルだけを読む人が「取り付け前に実体化されることはない」と誤読する。

**推奨修正**: 「実体化の口が差し込まれるのは Host 生成後であり、それ以前 (設定ツリーの初回構築時) に置かれた View は `ApplyAccessories` まで実体化されない」といった、実装が実際に保証している内容へ書き直す。

### [🔵 Suggestion] 蒸留への申し送り: concepts の陳腐化箇所

**該当箇所**: `kasane/concepts/maui/api/maui-facade.md:30` および `:74`

**問題点**: L30 の「`RootHeaderView` / `RootFooterView` は将来フェーズの予約名で、現時点で公開 API は存在しない」と L74 の「Root / Section Header・Footer の任意 View は未提供」は本 change で成立しなくなる。concepts の追随は蒸留 (ksn-distill) の責務なので本 change の実装欠陥ではないが、追随漏れが起きやすい箇所として記録しておく。あわせて、design.md Decision 6 の注記が予告している sample-parity.md への例外適用拡張の明文化と、deviation.md 第 2 項が保留している「iOS の superview 剥がしを maui/ADR-0016 と ADR-0017 のどちらへ反映するか」も蒸留時の宿題として残っている。

**推奨修正**: 蒸留時に上記 4 点 (maui-facade.md L30 / L74、sample-parity.md、ADR 反映先) を確認対象に含める。

## 確認した観点 (指摘に至らなかったもの)

- **ビルド・テスト**: iOS `xcodebuild test -scheme KsSettingsView-Package` = 476 tests / 0 failures、Android `./gradlew test` = 2280 tests / 0 failures、MAUI `dotnet test` = 322 tests / 0 failures。`python3 scripts/comment-policy-lint.py` = 禁止 0 件
- **足場の非改変**: `kasane/changes/` 配下の変更は tasks.md のチェックボックスのみ (specs / design / proposal は無改変)。deviation.md の 2 件はいずれも合意済み差分として扱った
- **spec 充足**: maui-core の 7 Requirement すべてに対応実装とテストがある。4 対象マトリクス (Root/Section × Header/Footer) は `AccessoryViewTests` が `TestCaseSource` で全 Scenario に張られている。maui-bridge の 3 Requirement は両 OS の Bridge テストで網羅 (リサイクル再バインド・未知 sectionID no-op・setRoot / replaceSection 輸送を含む)。samples-maui の 2 Requirement はサンプルページと verification-screenshots.md の目視記録で満たされている
- **退役順序 (design Decision 5)**: 「Store 更新 → native 配信 → 旧実体破棄」が `SetAccessoryView` / `RetireAccessoryView` + `DisposeRetired` / `ReleaseAccessoryViews` の全経路で守られている。`PreviousViewIsDisposedAfterTheNewOneIsDelivered` / `...AfterTheClearIsDelivered` が破棄時点の配信件数を観測しており、この順序には検出力のあるテストが付いている
- **Section 差し替え経路**: `ReplaceSectionKeepingCellIds` は Section を再登録しないため、`HeaderHeight` / `IsVisible` 変更で wrapper が作り直されない。`FillAccessoryViews` が現行 lease の platform view を DTO へ載せ直すので view accessory も落ちない (E2E スクリーンショット `e2e-*-03-headerheight-fixed.png` で確認済み)
- **切断中の変更と復元**: `ReleaseHost` → `ReleaseAccessoryViews` (Section 系は text 書き戻し) → 再接続で `ConnectGateway` が seam を差し直し → `OnHostAttached` → `ApplyAccessories` で Root / Section とも再実体化・再発行。切断中に置いた View・外した View のいずれも再接続後の表示に収束することをコードとテストの両方で追跡した
- **iOS の再計測口の堅牢性**: `accessoryElementPath` が Root 対象で常に `IndexPath(item:0, section:0)` を使う点について、Section が 0 件の状態で `invalidateAccessoryMeasurement(target: .rootHeader)` を投げる一時プローブテストを追加して実測した。クラッシュせず高さも正しく追従した (`numberOfSections=0` で 60 → 130)。プローブは確認後に削除済み。指摘には至らない
- **Android の measure 契約**: `KsAccessoryHostView.OnMeasure` の `Exactly` のみ制約を採用する分岐は、tasks 3.3 が参照する MAUI 本体 `ItemContentView` と同型であり逸脱ではない
- **多重配置検出**: `_placedViews` による slot 単位の検出、Root accessory を先に数えてから木を走査する `EnsureTreeHasNoDuplicates`、追加経路の `EnsureAccessoryViewIsFree` の 3 経路が揃っており、null 解除後の再利用も許容されている。例外送出がプロパティ確定後になる点は既存の Section / CellBase 多重配置検出と同じ契約で、spec もそれを明示している
- **sample-parity**: `MauiSpecific` 区分の新設は sample-parity.md L37 の既存例外 (MAUI にしか対応概念がないデモ) の範囲内。既存デモ項目の文言・構成は無改変

## アクションプラン

1. `AccessoryViewSwapProbeTest.kt:203` のアサーション皆無テストを、例外型を固定する形へ修正するか削除する (優先度: 高)
2. `LeakTests.cs:127` のトートロジーなアサーションを削除し、コメントを実際の検証内容へ合わせる (優先度: 高)
3. プローブテストの `println` 9 箇所を削除する (優先度: 低)
4. `KsSettingsController.AttachViews` の `<remarks>` を実装の保証内容へ書き直す (優先度: 低)
5. 蒸留時に concepts / sample-parity / ADR 反映先の宿題 4 点を確認対象に含める (本 change の修正対象外)
