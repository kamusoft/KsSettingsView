# レビュー結果: release-host-without-bridge-dispose (001 回目)

**日付**: 2026-08-08
**判定**: NEEDS_DISCUSSION → **APPROVED** (2026-08-08 追記。オーナー裁定と対応を確認した結果。末尾「追記」を参照)

## サマリー

実装はデルタスペックの全 Requirement / Scenario を両 OS 対称に満たしており、テストは iOS 439 件 / Android 2008 件 (debug + release) すべて成功、E2E スクリーンショット証跡も両 OS 分揃っている。`releaseHost()` の冪等性・dispose 後 no-op・旧 handle の購読解除・旧 Host と `Context` の回収可否まで、Scenario ごとに検出力のあるテストが対応している。コード品質・コメント規約 (lint 0 件)・足場の非改変も問題ない。

一方で、実装過程で **root header / footer が Host 再生成をまたいで失われる**ことが判明しており (E2E 証跡で両 OS とも確認できる)、これは本変更のデルタスペック文言 (「Store 現在状態」) には適合するが、ADR-0007 の狙い (Handler 再接続をまたいで表示内容が保持される) には穴を残す。この穴の埋め方は Bridge 単独では決められない設計判断を含むため、NEEDS_DISCUSSION とする。それ以外の指摘は Suggestion 級のみで、実装のやり直しを要するものはない。

## 指摘事項

### [🟠 Major] Host 再生成で root header / footer が失われる (設計判断が必要)

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:270-284`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:238-242`
- `maui/tests/shared/KsBridgeScenario.cs` (`ApplyWhileReleased` の doc コメント)
- 証跡: `kasane/changes/release-host-without-bridge-dispose/evidence/ios-02-after-recreate.png` / `android-02-after-recreate.png`

**問題点**:

root header / footer は `SettingsRoot` 値型に含まれない UI 層プロパティで、`updateAccessory` は Store 状態を変えず Diff の発行だけを行う (両 OS 共通の既存設計)。したがって新しい Host が接続時に取り込む「Store 現在状態」に root H/F は含まれず、`releaseHost()` → `makeHost*` を通ると root H/F は **更新が届かないだけでなく、解放前に表示されていた内容ごと消える**。

証跡がこれを裏づけている。解放前 (`*-01`) には root header「KsSettingsView Bridge」と root footer「C# から Native Bridge を操作しています」が出ているが、再生成後 (`*-02`) には両方とも消えている。iOS / Android 双方で同じ。

ADR-0007 は MAUI Handler の DisconnectHandler → `releaseHost()` / ConnectHandler → `makeHost*` を 1:1 対応させる決定であり、その帰結として **ページを再訪するたびに root header / footer が消える**ことになる。phase-2 の facade 実装でそのまま踏むと、利用者からは「ページを戻ると SettingsView のヘッダ / フッタが消える」という不具合として現れる。

なお実装はデルタスペックの文言には反していない。Scenario「解放中の更新は再生成時に反映される」「解放後の再生成は Store 現在状態を復元する」の THEN はいずれも「Store 現在状態」に限定されており、root H/F はその外側にある。よってこれは実装の瑕疵ではなく、**spec が覆っていない範囲で本変更が新たに露出させた穴**である。

実装者はこの事実を把握しており `maui/tests/shared/KsBridgeScenario.cs` の doc コメントに理由まで書いているが、記録先が maui の検証ホストのコード内に閉じている。Bridge の公開 API doc (`releaseHost` / `makeHost*` / `updateAccessory`) にも、deviation.md にも、ADR / concepts にも残っていないため、この変更をアーカイブすると知識が失われる。

**推奨修正** (いずれも設計判断を含むため、レビュアーからは選択肢の提示に留める):

- **A. 現状を仕様として確定する** — root H/F は Host 単位のプロパティであり再生成で失われる、と Bridge 公開 API の doc コメントと ADR-0007 (または concepts の Bridge lifecycle 節) に明記し、phase-2 facade 側で「再接続後に root H/F を再適用する」責務を持たせる。本変更内では doc コメント追記＋合意事項の記録で完結する
- **B. Bridge が root H/F の最終値を保持して再生成時に再適用する** — Bridge 内に Store 外の状態を持つことになり、「Store を迂回する更新経路は作らない」(maui/ADR-0001) と正面から衝突するため、ADR での明示的な例外化が要る
- **C. root H/F を `SettingsRoot` の状態に取り込む** — 本来の筋だが core モデルの変更であり、本変更 (capability: maui-bridge) の範囲外。別変更に切り出す判断が要る

いずれを採るにせよ、**この変更のうちに合意内容を記録に残す**ことを推奨する (A なら doc コメント、B/C なら別変更・別 ADR への切り出しと、本変更側への申し送り)。

### [🔵 Suggestion] concepts の Bridge lifecycle 記述が実装と乖離する

**該当箇所**: `kasane/concepts/maui/api/native-bridge.md:51-53`

**問題点**: 「`makeHost*` の再呼び出しは同じ handle を返す」「Host だけを解放して作り直す経路は存在しない」「Bridge の寿命は Host が保持する `Context` の寿命を超えてはならない」の 3 点が、いずれも本実装で置き換わっている。

**推奨修正**: concepts の更新は蒸留 (ksn-distill) の責務なので本変更内での修正は求めない。蒸留時の必須追随項目として申し送る。

### [🔵 Suggestion] iOS の `deinit` と `disconnectStore()` が同じティアダウンを二重に持つ

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:212-217` と `:311-317`

**問題点**: 購読 3 本の cancel + nil 代入が 2 箇所に複製されている。片方だけに購読が増えると解除漏れになる形。

**推奨修正**: `deinit` の先頭で `disconnectStore()` を呼ぶ形に寄せると 1 箇所になる (`connectedStore = nil` が増えるだけで `deinit` の解放順序は保たれる)。

### [🔵 Suggestion] Android テストヘルパの `attach` 2 引数版と 3 引数版が同一実装

**該当箇所**: `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeTestHost.kt:57-93`

**問題点**: 2 引数版は 3 引数版に `controller.get()` を渡したものと処理が完全に一致する。

**推奨修正**: `attach(bridge, controller) = attach(bridge, controller, controller.get())` に委譲すると重複が消える。

### [🔵 Suggestion] 購読解除 API の platform 間非対称

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:310` / `android/.../KsSettingsView.kt:329`

**問題点**: Android は `unbind()` の対になる公開 `bind()` があり解除後に再接続できるが、iOS は `disconnectStore()` の対になる公開 API がなく再接続できない (Controller は `init(store:)` で束縛するため)。Bridge の用途では新しい Host を作るので支障はない。

**推奨修正**: 仕様上の要求ではないため対応不要。`disconnectStore()` の doc に「再接続は新しい Controller の生成で行う」と一言添えると、Android と読み比べたときの誤解が減る。

## 確認した観点 (指摘なし)

- **テスト実行**: iOS `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → 439 tests / 0 failures。Android `./gradlew test --rerun-tasks` → 2008 tests / 0 failures / 0 errors (up-to-date スキップを避けるため `--rerun-tasks` で再実行し、`build/test-results/**/TEST-*.xml` を集計)
- **新規テストの実行確認**: `KsBridgeHostReleaseTests` 7 件・`StoreDisconnectionTests` 2 件・`KsBridgeHostTest` 追加 3 件・`KsBridgeLifecycleTest` 追加 5 件・`StoreUnbindTest` 3 件がいずれも実行され pass している (件数を XML / xcodebuild 出力で確認)
- **テストの検出力**: `解放後の Store 更新は旧 handle に反映されない` は解放前の更新が届くことを対照として先に assert しており、購読が実在しないだけで通るトートロジーになっていない。回収検証は弱参照を返す別メソッドに生成〜解放を閉じ込め、スタック上の強参照が回収を妨げる誤検出を避けている
- **`dispose()` との相互作用**: `dispose()` が `disconnectStore` / `unbind` を呼ばないのは、「破棄後は Store を操作しないため保持中の Host の表示も変化しない」という maui/ADR-0005 の既存契約どおり。非対称だが意図的で、本変更で変える理由はない
- **リーク**: 解放後の Bridge は iOS `hostController` / Android `hostView` を `nil` / `null` にし、Android は `KsSettingsView.unbind()` で `pendingStore` も手放すため `Context` を含む参照が残らない。両 OS のテストで回収可否として検証済み
- **足場の非改変**: `proposal.md` / `specs/maui-bridge/spec.md` は未変更。`tasks.md` の差分はチェックボックスのみ
- **コメント規約**: `python3 scripts/comment-policy-lint.py --summary` → 禁止 0 件 (検査対象 401 ファイル)。新規コメントの外部参照は `maui/ADR-0007` のみで許容形式
- **binding**: iOS は `ApiDefinition.cs` に `[Export("releaseHost")] void ReleaseHost()` を追加、Android は自動生成で `releaseHost()V = ReleaseHost()` が採れている (生成物で確認)。`KsSettingsView.IntegrationHost.Android` はビルド成功。`KsSettingsView.IntegrationHost.iOS` は当レビュー環境の Xcode 26.5 と .NET for iOS 26.1 の版差ゲートでビルドできず、実行は証跡スクリーンショットで確認した (コード起因ではない)

## アクションプラン

1. **Major (root H/F の消失)** — 選択肢 A / B / C のいずれを採るかオーナー裁定を仰ぐ。A を採るなら本変更内で Bridge 公開 API の doc コメント追記まで行う。B / C を採るなら別変更へ切り出し、本変更には合意事項として記録を残す
2. Suggestion 群 (concepts 乖離の申し送り / iOS ティアダウン重複 / テストヘルパ重複 / doc の一言追記) — 1. の裁定と併せて、対応するかどうかを判断する。いずれも本変更のマージを妨げない

---

# 追記: 対応確認 (2026-08-08)

**判定**: **APPROVED**

オーナー裁定は**選択肢 A (現状を仕様として確定)**。対応後の成果物を独立に確認し、Major を解消済みと判断する。

## Major (root H/F の消失) — 解消

**doc 追記**: `makeHost*` / `releaseHost` / `updateAccessory` の 6 箇所 (両 OS × 3) に追記されている。内容は裁定 A を正確に表しており、実装の事実とも一致する。

- `ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:51-54` / `:66-70` / `:223-226`
- `android/.../bridge/KsSettingsBridge.kt:58-64` / `:79-87` / `:264-270`

事実確認: 「root の header / footer は Store ではなく Host が持つプロパティ」は `KsSettingsViewController.rootHeader` / `.rootFooter` (`KsSettingsViewController.swift:58` / `:73`) と `KsSettingsView.rootHeader` / `.rootFooter` (`KsSettingsView.kt:165` / `:174`) が public プロパティとして保持していること、`SettingsRootStore.updateAccessory` が root 対象で state を変えず Diff だけを流すこと (`SettingsRootStore.swift:270-284` / `SettingsRootStore.kt:238-242`) と一致する。`updateAccessory` の doc が「Section 対象は復元されるが root 対象は引き継がれない」と両者を対比させている点も正確。

**deviation.md**: 記載内容を 1 項ずつ照合し、すべて事実と一致することを確認した。

- 「spec の THEN は Store 現在状態の復元であり文言上の違反ではない (verify-001 は VALID)」— spec.md の THEN と verify-001 の判定に一致
- 「E2E で解放前の root H/F が再生成後に消える」— `evidence/*-02-after-recreate.png` で両 OS 分を確認
- 「両 OS の SettingsRootStore は root accessory を状態に保存せず Diff 発行のみ」— 上記コードで確認
- 「root H/F を保持しない利用者は Bridge だけだった / SwiftUI DSL の `.rootHeader` と同形」— SwiftUI 層は `_rootHeader` を DSL 側 state に持ち `makeUIViewController` で毎回 `controller.rootHeader = _rootHeader` と代入する (`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:158` / `:205`)。「所有者が保持し Host 生成のたびに再適用する」が Native 既存イディオムであるという記述は裏取り済み
- 「Bridge 公開 API doc に明記した」— 上記 6 箇所で確認

## その他の対応 — 問題なし

- **iOS `deinit` の複製復帰**: 妥当。`KsSettingsViewController` は `UIViewController` 由来で MainActor 隔離されており、`deinit` は nonisolated な文脈のため isolated メソッドを呼べない。残されたコメント (`KsSettingsViewController.swift:212-215`) の説明は Swift の規則と一致し、「購読を増やすときは両方に追記」の注意も要点を押さえている。**前回の Suggestion はコンパイル可能性を確認せずに出したもので、こちらの誤り**。lessons/inbox への記録 (`kasane/lessons/inbox/deinit-teardown-consolidation-breaks-isolation.md`) の内容も正確
- **Android `attach` の委譲**: `attach(bridge, controller) = attach(bridge, controller, controller.get())` になり重複が解消 (`KsBridgeTestHost.kt:66-69`)
- **iOS `disconnectStore` doc**: 「再接続は同じ Store から新しい Controller を生成して行う」が追記され、Android `unbind()` の doc (再 bind 可) と読み比べたときの誤解が解けている
- **ID 契約コメント**: 前回 APPROVED 済み。refinement (主語を「置換対象の identity」に絞る) も反映されている

## 再検証

- iOS `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → **Executed 439 tests, with 0 failures** / `** TEST SUCCEEDED **`
- Android `./gradlew test --rerun-tasks` → `BUILD SUCCESSFUL`、`build/test-results/**/TEST-*.xml` 集計で **total=2008 / failures=0 / errors=0 / skipped=0**
- `python3 scripts/comment-policy-lint.py --summary` → 禁止 0 件。未追跡ファイルは全体 lint の対象外のため、`scripts/comment_policy_rules.py` の `scan_text` を新規・改変ファイルへ個別適用して 0 件を確認 (`KsSettingsViewController.swift:1501` に advisory 1 件があるが、本変更が触れていない既存行)
- 足場の非改変: `proposal.md` / `specs/maui-bridge/spec.md` は依然未変更。`tasks.md` の差分はチェックボックス 10 行のみ (本文の書き換えなし)

## 新規指摘

### [🔵 Suggestion] Android では「再適用は Host を view 階層へ取り付けた後」でないと root H/F が落ちる

**該当箇所**: `kasane/changes/release-host-without-bridge-dispose/deviation.md` (phase-2 への申し送り)、根拠は `android/.../ui/KsSettingsView.kt:298-316` / `:350-352` / `:361-370`

**問題点**: Android の `makeHostView` 内で呼ばれる `bind(store)` は、View がまだ window に attach されていないと `findViewTreeLifecycleOwner()` が `null` を返して購読を張らない。`diffs` は replay を持たない `SharedFlow` で、`onAttachedToWindow` の `resyncFromStore` は `store.state.value` (sections) と Theme しか取り込み直さない。したがって **`makeHostView` → `updateAccessory(RootHeader)` → `addView` の順で再適用すると root H/F の Diff は誰にも届かず、黙って失われる**。正しい順序は `makeHostView` → `addView` → `updateAccessory`。iOS は `init(store:)` で即座に Combine 購読を張るため順序に依存しない。

MAUI Handler の property mapper は `CreatePlatformView` の直後・view tree への追加前に走り得るため、phase-2 facade が素直に mapper で再適用すると Android だけこの穴を踏む。検証ホストが `host.Post(() => ...)` で取り付け後に操作しているのは、まさにこの制約への既存の対処。

**推奨修正**: 本変更の実装には手を入れず、deviation.md の申し送りに「Android では取り付け後に再適用する必要がある」旨を一文足すことを推奨する (phase-2 の探索で再導出できる範囲なので必須ではない)。

## 判定根拠

Critical / Major なし。残るのは Suggestion のみで、いずれも本変更のマージを妨げない。ビルド・テストは両 OS で全件成功。裁定 A の内容は doc と deviation.md の双方に正確に記録されており、蒸留時に失われる形にはなっていない。
