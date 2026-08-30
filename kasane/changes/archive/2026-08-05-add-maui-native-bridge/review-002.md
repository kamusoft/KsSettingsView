# レビュー結果: add-maui-native-bridge (002 回目)

**日付**: 2026-08-05
**判定**: CHANGES_REQUESTED

## サマリー

前回サイクルの指摘は、実測を伴わないと結論が出ない 2 件 (replace 系の ID 契約 / Metadata.xml の `remove-node`) を含めて、いずれも妥当に解決されている。とりわけ review-001 Minor-1 は**指摘側が誤っており実装側の判断が正しい**ことをミューテーションで確認した (下記「修正の妥当性確認」参照)。テストは iOS 587 件 / Android 1940 件とも 0 failures、`maui/KsSettingsView.slnx` は 0 警告 0 エラーで、回帰は見られない。

一方で、今回の修正で**新設された** iOS binding の増分ビルド補正ターゲット `_AdjustKsBridgeXcodeProjectInputs` に、実際には機能していない除外指定がある。csproj のコメントと `maui/README.md` の 2 か所がこの除外を事実として記述しているため、前サイクルが潰そうとした「記述と実態の食い違い」を新しい足場で再生産している。あわせて `build-xcframework.sh` の冒頭コメントが、並走調査で反証されたはずの因果 (署名を無効化しないと framework が install されない) を今も断定しており、同一変更内の `maui/README.md` と正面から矛盾する。いずれも修正は 1〜2 行だが、事実誤認を恒久ファイルへ残す性質のため CHANGES_REQUESTED とした。

### 実行した検証

| 対象 | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<ios-simulator-udid>…'` | **587 tests / 0 failures** (Bridge 28 / Core 83 / SwiftUI 68 / UI 408) / TEST SUCCEEDED |
| Android | `./gradlew test --rerun-tasks` (件数は `build/test-results/*/TEST-*.xml` 集計) | **1940 tests / 0 failures** (bridge 58 / compose 184 / core 176 / ui 1522) |
| MAUI | `DEVELOPER_DIR=… dotnet build maui/KsSettingsView.slnx` | **成功 / 0 警告 0 エラー** (binding 2 本 + 検証ホスト 2 本) |
| Android binding | `dotnet build KsSettingsView.Binding.Android.csproj -t:Rebuild` | 成功 / 6 警告 (BG8605 / BG8606 / BG8A00 の各 2 重出力)。いずれも `maui/README.md` に記載済み |
| コメント規約 | `python3 scripts/comment-policy-lint.py` | 本変更の新規ファイルからの検出 **0 件** (既存債務 790 件は本変更と無関係) |
| 足場凍結 | `git status kasane/changes/add-maui-native-bridge/` | proposal.md / design.md / specs/ は未変更。`tasks.md` はチェックボックスのみ |

---

## 修正の妥当性確認 (前サイクル指摘への対応)

### review-001 Major (replace 系 DTO の ID 無効化) — 解決

`String?` 戻り値化は 3 層 (Swift / Kotlin / `ApiDefinition.cs`) で揃っており、`insertSection` / `insertCell` と形が一致した。両 OS のテスト (`test_replace系が返すIDで後続操作ができる` / `` `replace 系が返す ID で後続操作ができる` ``) は、**戻り値 ID を次の `insertCell` の挿入先に使い、実描画された行テキストで結果を確認する**構成で、DTO 自身の ID とは異なることも `XCTAssertNotEqual` / `assertNotEquals` で押さえている。対象不在時に `null` を返すケースも「Bridge が採番していない canonical UUID」と「非 canonical 文字列」の 2 系統で両 OS 対称に検証済み。`maui/README.md` の「既知の制約」にも記載があり、API 形状・doc comment・利用者向け記述の 3 方向で塞がっている。

実装は Store へ渡す前に存在確認 (`store.root.sections.contains…` / `store.state.value.sections.none…`) を挟む形になり、デルタスペックの「Store 公開操作へ素通し」という表現からはわずかに離れる。ただし判定条件は Store 側の照合と同一 (`id` 一致) で、観察可能な挙動 (未知 ID は no-op) は変わらず、むしろ spec の ID 契約「呼び出し側は返された ID だけを更新 API に渡す」を API 形状で担保する方向の変更であるため、逸脱とは見なさない。

### review-001 Minor-1 (Metadata.xml の `remove-node` 削除) — **指摘側が誤り。現在の解決が正しい**

採られなかったこの指摘について、実測で検証した。

1. **`remove-node` は一致している。** 変換前後の成果物を比較すると、`obj/Debug/net10.0-android/api.xml` には `WhenMappings` が残るが、`api.xml.fixed` には**存在しない**。review-001 は `api.xml` (fixup 前) を見て「除去されずに残っている」と判断しており、前提が誤っていた。
2. **entry を消すと公開表面に出る (ミューテーションで実証)。** `remove-node` を一時的にコメントアウトして `-t:Rebuild` したところ、
   - 警告は 6 → 4 に減り BG8A00 は消える (指摘の「BG8A00 が消える」部分は正しい)
   - しかし `obj/Debug/net10.0-android/generated/src/KsSettingsView.Bridge.KsSettingsBridge.cs` に `public sealed partial class WhenMappings : global::Java.Lang.Object` が生成され、`Java.Interop.__TypeRegistrations.cs` にも登録される
   - 復元後、`shasum` 一致を確認し BG8A00 の復帰も確認済み

   つまり「生成 C# に `WhenMappings` が出ないのは generator が元から出さないため」という review-001 の説明は成り立たず、**推奨どおり削除していれば内部ヘルパ型が公開 binding 表面へ露出していた**。
3. 現在の `Transforms/Metadata.xml:19-26` のコメントと `maui/README.md` の BG8A00 の説明は、上記の 2 段適用 (fixup 段で成立 → generator 段で対象なし) をそのまま述べており、実態と一致している。

`maui/spike/README.md` の逆結論との食い違いについても、README 側で本番 binding の判断理由が自己完結して書かれているため、追加対応は不要と判断した。

### その他 (review-001 Minor-2〜4 / Suggestion 1〜3、second-opinion-003) — 解決

- Minor-2: Android `KsSettingsBridge` の doc comment が「フィールドとしては保持しないが、生成した Host が `Context` を保持するため Bridge の寿命は Host を超えてはならない」と寿命依存まで書けている
- Minor-3 / Minor-4: `KsBridgeFont` の platform 差、Bridge 破棄と Host の寿命契約とも `maui/README.md` の「既知の制約」に記載。Host 単独解放は `kasane/changes/release-host-without-bridge-dispose/` として起票済み
- Suggestion 1: 契約表のラベルが `replaceSection: 既知 ID (header text は不変)` になり、直前に回避理由のコメントが付いた (両 OS)
- Suggestion 2: `build-xcframework.sh` は `-derivedDataPath "${BUILD_DIR}/DerivedData"` を渡し、中間生成物が `build/` に閉じた
- Suggestion 3: `maui/README.md` に「SDK 更新時に再検証する箇所」の表が追加された
- second-opinion-003 Major: iOS は標準 `XcodeProject` アイテム + `CreateNativeReference=false` + 手動 `NativeReference` 構成へ戻っており、SDK 経路の archive (`obj/…/archives/KsSettingsViewBridgeiOS.xcarchive`) に framework が install され 2 スライスの xcframework が生成されていることを確認した。Android は `deviation.md` に実測根拠つきで記録済みのため、方式の乖離としては指摘しない
- `trash` / `rm` はオーナー裁定済みのため指摘しない

---

## 指摘事項

### [🟡 Minor] `_XcbInputs` の除外指定が一致せず、`ios/binding/build` / `DerivedData` が実際には増分ビルド入力から外れていない

**該当箇所**: `maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj:68-69` (`_AdjustKsBridgeXcodeProjectInputs` の 2 つの `Remove`)、`maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj:57-64` (コメント)、`maui/README.md`「Native artifact の生成」節および「SDK 更新時に再検証する箇所」の表

**問題点**:

`ios/binding/build/probe/probe.swift` と `ios/binding/DerivedData/probe/probe.h` を置いて `_XcbInputs` の実体を取得したところ、**両方とも除外されずに残っていた**。

```
$ dotnet build …/KsSettingsView.Binding.iOS.csproj -t:_GetBuildXcodeProjectsInputs -getItem:_XcbInputs
   1 ios/binding/DerivedData      ← 除外されるはずが残る
   1 ios/binding/KsSettingsViewBridge.xcodeproj
   1 ios/binding/build            ← 除外されるはずが残る
  91 maui/macios/KsSettingsView.Binding.iOS   (= ios/Sources の Swift + Package.swift + csproj)
```

原因はパス表記の不一致である。SDK 側の `_AllXcbFiles` は `%(XcodeProject.RootDir)%(XcodeProject.Directory)/**/*` から**正規化済みの絶対パス** (`/…/ios/binding/build/probe/probe.swift`) で item を作る一方、`Remove` に渡している `$(KsBridgeXcodeDir)` は `$(MSBuildThisFileDirectory)../../../ios/binding/` という `..` を含む文字列のままで、glob のディレクトリ接頭辞が一致しない。

`Remove` を `$([MSBuild]::NormalizeDirectory('$(KsBridgeXcodeDir)build'))**/*` の形へ変えて同じ計測を行うと、両 probe が消えて意図どおりになることを確認した (確認後に原状復帰済み、`shasum` 一致)。

実害は、`build-xcframework.sh` を単体実行した後に binding をビルドすると xcframework が 1 回無駄に作り直される点で、正しさは損なわれない。ただし csproj のコメント (「生成物が入力として拾われて無意味な再ビルドを招くので除外する」) と `maui/README.md` の 2 か所が、この除外が働いていることを事実として記述しており、**記述と実態が食い違っている**。同じ食い違いを潰すのが前サイクルの主題だったため、新設ターゲットで再生産しない方がよい。

**推奨修正**: `Remove` の 2 行を正規化済みパスにする (`$([MSBuild]::NormalizeDirectory('$(KsBridgeXcodeDir)build'))**/*` / 同 `DerivedData`)。`_XcbInputs` を実際に取得して両ディレクトリが消えることを確認した上でコメントを残す。除外が不要という判断を採るなら、`Remove` の 2 行と csproj コメント・README 該当記述をあわせて落とす。

### [🟡 Minor] `build-xcframework.sh` の冒頭コメントが、同一変更内の `maui/README.md` で反証済みの因果を断定している

**該当箇所**: `ios/binding/build-xcframework.sh:13-14`

```
#   - 静的ライブラリなので署名は不要。デバイススライスの archive では署名を無効化しないと
#     framework が Products へ install されない。
```

**問題点**: `maui/README.md`「SDK 標準アイテムの採否」は、まさにこの因果について「同一引数での実験によりこの因果は成立しないことが確認された。当時の失敗の実体は Xcode project の target 名と SwiftPM package の target 名が衝突していたこと」と述べている。同じ変更の中で、スクリプトのコメントだけが反証前の主張を断定形で残している。

実測でも裏が取れる。SDK の `XcodeProject` 経路は `CODE_SIGNING_ALLOWED=NO` を一切渡さないが、生成された `obj/Debug/net10.0-ios/xcode/KsSettingsViewBridge-00877/archives/KsSettingsViewBridgeiOS.xcarchive` (デバイススライス) に `Products/Library/Frameworks/KsSettingsViewBridge.framework` が正しく install されている。したがって「署名を無効化しないと install されない」は成り立たない。

このコメントを読んだ人は、スクリプトから `CODE_SIGNING_ALLOWED=NO` を外せない理由を誤解し、将来 SDK 経路と挙動が食い違ったときに誤った方向へ調査する。

**推奨修正**: 当該 2 行を「静的ライブラリのため署名は不要で、archive では署名関連の設定を無効化して余計な要件を持ち込まない」程度の、install の成否と因果を結ばない記述へ書き換える。フラグ自体は残してよい。

### [🔵 Suggestion] 増分ビルド入力に framework へ入らない `KsSettingsViewSwiftUI` の Swift も含まれている

**該当箇所**: `maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj:70`

`_XcbInputs Include="$(KsBridgeSwiftRoot)Sources/**/*.swift"` は `ios/Sources` 全体を拾うため、xcframework の target が依存しない `KsSettingsViewSwiftUI` を触っただけでも native の再ビルドが走る。取りこぼしより過剰の方が安全な選択であり実害は再ビルド時間だけだが、`Sources/KsSettingsViewBridge` / `Sources/KsSettingsViewCore` / `Sources/KsSettingsViewUI` の 3 つに絞れば意図がコードから読める。絞る場合は、依存 target が増えたときに追随が必要になる点をコメントに残しておくとよい。

### [🔵 Suggestion] `ios/binding/KsSettingsViewBridge/` が空ディレクトリとして残っている

**該当箇所**: `ios/binding/KsSettingsViewBridge/`

`project.pbxproj` の `PBXFileSystemSynchronizedRootGroup` は `../Sources/KsSettingsViewBridge` を指しており、`ios/binding/KsSettingsViewBridge/` は参照されていない。git は空ディレクトリを追跡しないため clone 側には現れないが、手元では Xcode の同期グループがここを指していると誤解させる。削除しておくとよい。

---

## 確認したが問題なしと判断した観点

- **足場の凍結**: `proposal.md` / `design.md` / `specs/` は未変更。`tasks.md` はチェックボックスのみで、全項目に対応する成果物が実在する。5.1 (iOS 標準 `XcodeProject` 参照) は実装が追いついて記述と一致し、5.2 (Android) は `deviation.md` に実測根拠つきで記録された合意済み差分
- **deviation.md の内容**: Android の Gradle 複数モジュール構成で SDK の init script が `rootProject.allprojects` の buildDirectory を束ねる件は `exploration.md` の並走調査結果と整合し、pack 経路が公式アイテム (`AndroidLibrary`) 経由で標準と共通である点も記載済み。iOS 側を「実装修正で解消」と明記しており、記録の範囲が実装と一致している
- **replace 系の存在確認の照合条件**: 両 OS とも Store 側の照合 (`id` 一致) と同一。`Section.cells` は hidden を含む model 配列であり、hidden な Cell を対象にした `replaceCell` も Store と同じく成立する
- **戻り値の canonical 表現**: iOS は `UUID.uuidString` へ、Android は入力文字列をそのまま返すため大小文字の扱いが厳密には異なるが、各 platform 内では Bridge 採番の ID と往復して一致し、`insertSection` 等の既存 API と同じ規則。観察可能な差は生じない
- **`KsBridgeSection` / `KsBridgeRootBuilder`**: iOS / Android で ID 採番・`addCell` / `addSection` / `addLabelCell` の戻り値・Builder 内 Section 未存在時の `null` まで対称
- **iOS `KsSettingsViewController.applyContentUpdateBatch`**: `connectedStore` を `weak` にして Store ↔ Controller の循環を作らず、`deinit` 経路の購読解除にも `contentUpdateSubscription` が追加されている。snapshot に載らない hidden Cell を自然に除外し、`reconfigureItems` (iOS 15+) で構造変更を伴わない再構成になっている。`rebuildModelIndexes` の切り出しで `applyFullSnapshot` と index 構築規則が 1 か所に集約された
- **契約表の回避コメント**: header text を変えない理由を「未修正の再描画不具合」として自己完結の日本語で書いており、コメント規約が禁じる変更 ID / レビュー通番の裸参照を含まない (lint でも新規検出 0 件)
- **`maui/README.md` の位置づけ**: `docs-refresh` スキルが追従対象として列挙するのはルート `README.md` / `android/README.md` / `samples/{ios,android}/README.md` と `docs/` であり、`maui/` ビルドルートの開発者向け README は対象外。本変更で新規作成することは規約違反ではない
- **`.gitignore`**: `build/` / `DerivedData/` の両パターンが `ios/binding/` 配下にも及ぶため、単体スクリプトの生成物が追跡対象にならない

---

## アクションプラン

1. **Minor-1**: `_AdjustKsBridgeXcodeProjectInputs` の `Remove` を正規化済みパスにし、`_XcbInputs` を実測して除外が効くことを確認する (効かせない判断なら 2 行とコメント・README 記述をあわせて削除)
2. **Minor-2**: `build-xcframework.sh:13-14` の「署名を無効化しないと framework が install されない」を、install の成否と因果を結ばない記述へ書き換える
3. **Suggestion 2 件**: 増分ビルド入力の対象 target 絞り込み / `ios/binding/KsSettingsViewBridge/` 空ディレクトリの削除
