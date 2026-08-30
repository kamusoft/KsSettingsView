# レビュー結果: add-maui-native-bridge (001 回目)

**日付**: 2026-08-05
**判定**: CHANGES_REQUESTED

## サマリー

実装は proposal / design / デルタスペックの要求をほぼ全面的に満たしている。Bridge → Store → Native Host の縦経路が両 OS で対称に組まれ、テストは内部状態ではなく**実描画された行テキスト・header/footer・Adapter 通知 / snapshot** を観察する形で書かれており、12操作の契約表・未知 ID・index 丸め込みの境界まで両 OS で揃っている。手抜き (言い訳コメントによる実質スキップ、境界の欠落) は見当たらない。生成された C# binding 表面も確認したところ、内部型の漏れがなく 12操作 + Builder + Host 生成 + `DisposeBridge` が両 OS で揃っている。

一方で、**replace 系 API に渡した DTO の Bridge 採番 ID が黙って無効化される**問題が両 OS にあり、呼び出し側にそれを判別する手段がない。これは maui/ADR-0005 Decision 9 が「Bridge 採番ならこのケース自体が消える」として排除したはずの「見た目は正当な ID なのに全操作が無言 no-op になる」状態を、新設の公開 interop 表面に再導入している。ドキュメントのみの修正でも解消可能だが、公開 API の契約として明示が要るため Major とした。

あわせて、`Transforms/Metadata.xml` の `remove-node` が実際には何にも一致しておらず (BG8A00 が恒常的に出る)、コメントと `maui/README.md` の記述が実態と食い違っている点を確認した。

### 実行した検証

| 対象 | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | **Executed 408 tests, 0 failures** / TEST SUCCEEDED |
| Android | `./gradlew test --rerun-tasks` (件数は `build/test-results/*/TEST-*.xml` 集計) | **1936 tests / 0 failures** (bridge 54 / ui 1522 / core 176 / compose 184) |
| MAUI | `dotnet build maui/KsSettingsView.slnx` | **失敗** (下記注記) |
| MAUI | `DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer dotnet build maui/KsSettingsView.slnx` | **成功 / 0 警告 0 エラー** |
| Android binding | `dotnet build maui/android/.../KsSettingsView.Binding.Android.csproj -t:Rebuild` | 成功 / **6 警告** (BG8605 / BG8606 / BG8A00 の各2重出力) |
| コメント規約 | `python3 scripts/comment-policy-lint.py` | 本変更の新規ファイルからの検出 **0 件** (既存債務 790 件は本変更と無関係。`android/settings.gradle.kts` の3件も HEAD 由来で、追記されたコメントは規約準拠) |

注記2点 (いずれも指摘ではなく検証結果の但し書き):

- `dotnet build maui/KsSettingsView.slnx` は `DEVELOPER_DIR` 未指定だと iOS 検証ホストが `requires Xcode 26.1` で失敗する。`maui/README.md` と `maui/spike/README.md` に「必須」と明記されているため実装の欠陥ではないが、slnx のビルド手順としては README を読まないと通らない。
- ホスト側報告の「0警告0エラー」は成立するが、それは差分なしビルドの結果である。Android binding を `-t:Rebuild` すると BG8605 / BG8606 / BG8A00 が出る。BG8605 / BG8606 は申し送り済みの許容範囲だが、BG8A00 は下記 Minor-1 のとおり本変更自身の設定に起因する。

---

## 指摘事項

### [🟠 Major] replace 系 API に渡した DTO の Bridge 採番 ID が無言で無効になり、呼び出し側に判別手段がない

**該当箇所**:
- `ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:114-117` (`replaceSection`)
- `ios/Sources/KsSettingsViewBridge/KsBridgeSection.swift:68-75` (`makeSection(id:)`)
- `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsSettingsBridge.kt:135-139`
- `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeSection.kt:50-55`

**問題点**:

`KsBridgeSection` / `KsBridgeLabelCell` はインスタンス生成時に自分で canonical UUID を採番し、`sectionID` / `cellID` として公開する。`insertSection` / `insertCell` / Builder 経由で渡した場合、この ID はそのまま Store 上の identity になる (`insertSection` は `section.sectionID` を戻り値としても返す)。

ところが `replaceSection(sectionID:newSection:)` は `newSection.makeSection(id: uuid)` と、**対象の ID で作り直す**ため、`newSection.sectionID` は Store のどこにも存在しない ID になる。呼び出し側から見ると、

- `replacedSection.SectionID` は型としても値としても正当な canonical UUID 文字列であり、
- 以後 `insertCell(..., sectionID: replacedSection.SectionID, ...)` は `null` を返して無言 no-op、
- `removeSection` / `replaceSection` も無言 no-op、

となる。しかも同じ DTO の中で、**内包する Cell の `cellID` は生きている** (`makeSection` が `$0.makeCell()` を使い DTO 自身の ID を維持するため) ため、「Section の ID だけ死んでいて Cell の ID は生きている」という判別しにくい状態になる。同種の問題は `replaceCell(cellID:newCell:)` の `newCell.cellID`、`KsBridgeCellUpdate.cell.cellID` にもある (後者は `KsBridgeCellUpdate` の doc comment に「`cell` 自身が持つ `cellID` は使われず」と明記があるが、`replaceCell` / `replaceSection` 側には対応する記述がない)。

これは maui/ADR-0005 Decision 9 が「呼び出し側採番では『不正 ID で新規作成』という未定義ケースが生じる → Bridge 採番ならこのケース自体が消える」として排除した失敗様式そのものであり、`ApiDefinition.cs` / 生成される C# 表面 (`KsBridgeSection.SectionID` が読み取り専用プロパティとして常に見える) からは、その ID が生きているか死んでいるかを区別できない。デルタスペックの「呼び出し側は返された ID だけを更新 API に渡す」という規則は、DTO が構築時に ID を公開する現在の API 形状では呼び出し側から守りようがない。

**推奨修正** (いずれか):

1. `replaceSection` / `replaceCell` の戻り値を `String?` (有効な sectionID / cellID) にして、置換後に有効な ID を呼び出し側へ返す。`insertSection` / `insertCell` と形が揃い、「戻り値の ID だけを使う」という規則が API 形状で守れるようになる。
2. 最小対応として、`replaceSection` / `replaceCell` の doc comment (Swift / Kotlin / `ApiDefinition.cs` の3か所) に「渡した DTO 自身の `sectionID` / `cellID` は破棄され、以後の操作には引き続き**対象の** ID を使う」ことを `KsBridgeCellUpdate` と同じ明示度で書き、`maui/README.md` の「既知の制約」にも1行足す。

### [🟡 Minor] `Transforms/Metadata.xml` の `remove-node` が何にも一致せず、恒常的な BG8A00 とコメントの不一致を生んでいる

**該当箇所**: `maui/android/KsSettingsView.Binding.Android/Transforms/Metadata.xml:19-22`、`maui/README.md:90-91`

**問題点**: 変換後の `obj/Debug/net10.0-android/api.xml` を確認したところ、`<class ... name="KsSettingsBridge.WhenMappings">` は**除去されずに残っている**。つまりこの `remove-node` は一致せず (BG8A00)、生成 C# に `WhenMappings` が出ないのは generator 側が元から出さないためである。結果として、

- 「Bridge の API 表面ではないため束縛しない」というコメントは、この変換が仕事をしている前提の記述で実態と食い違う。
- `maui/README.md` の「実際には対象の内部ヘルパ型は生成物から除かれている」も、除去の主体を取り違えている。
- ビルドのたびに BG8A00 が出続けるため、将来 Metadata.xml を触ったときの本物の警告が埋もれる。

なお `maui/spike/README.md` には、spike で同じ手を試して「BG8401 が消える代わりに BG8A00 が出て警告総数が減らないため transform を置かない」と結論した経緯が書かれており、本番 binding の判断と逆になっている。

**推奨修正**: `remove-node` の entry を削除する (`WhenMappings` は元から生成されないため実害なし)。残す判断をするなら、`path` を実際に一致する形へ直して BG8A00 が消えることを確認した上で、コメントを「一致することを確認済み」と書ける状態にする。

### [🟡 Minor] Android の「`Context` を保持しない」という記述が、保持している Host View 経由の Context 保持を隠している

**該当箇所**: `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsSettingsBridge.kt:22, 40, 59-66`

**問題点**: doc comment は「`Context` は `makeHostView` の引数で受け取り、Bridge は保持しない」と書いているが、`hostView` フィールドが `KsSettingsView(context)` を強参照で保持するため、**Bridge は Host View を通じて Activity Context を保持し続ける**。maui/ADR-0005 の「Bridge は `Context` を保持しない」も同じ表現で、フィールドの型としては真だが、寿命の観点では成り立たない。Bridge を Activity より長命なスコープ (シングルトン・DI コンテナ等) に置いた場合は Activity リークになる。`dispose()` で解放される設計なのでフロー上は塞がれているが、その依存関係が記述から読み取れない。

**推奨修正**: doc comment を「`Context` はフィールドとして保持しないが、生成した Host が Context を保持するため、Bridge の寿命は Host (= Activity) の寿命を超えてはならない。超える場合は `dispose()` で解放する」の趣旨へ書き換える。

### [🟡 Minor] `KsBridgeFont` の「pointSize が 0 以下」の解釈が iOS / Android で異なり、どこにも記録されていない

**該当箇所**: `ios/Sources/KsSettingsViewBridge/KsBridgeFont.swift:44-45` / `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeFont.kt:32-37`

**問題点**: 同じ C# コード `new KsBridgeFont(null, 0, false, false)` が、iOS では**本文既定サイズ (`UIFont.preferredFont(forTextStyle: .body).pointSize`) の具体的なフォント**に、Android では **`TextUnit.Unspecified` (サイズ未指定)** に解決される。`familyName` が解決できない場合の扱いも iOS は system font へフォールバック、Android は `fontFamily = null` と異なる。`UIFont` が具体値を要求する以上ある程度は不可避だが、`maui/README.md` の「既知の制約」に列挙された platform 差 (プロパティ名・enum の束縛形・BG 警告) には含まれておらず、phase-2 の facade 設計時に見落とされる。

**推奨修正**: `maui/README.md` の「既知の制約」に1行追加する。あるいは Android 側も `pointSize <= 0` を「描画層の本文既定サイズ」へ寄せて挙動を揃える (どちらを採るかは phase-2 facade の責務分担次第)。

### [🟡 Minor] Bridge を破棄せずに Host だけを解放する手段がなく、phase-2 の Handler 再接続経路が塞がっている

**該当箇所**: `ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:51-66` / `android/.../KsSettingsBridge.kt:59-75`

**問題点**: 実装は「`makeHost*` は同じ Host を返す / `dispose()` 後は `nil` を返し、以後 Host は二度と生成できない」。デルタスペックの「新たな Host が必要な場合は破棄後に再生成する」は、破棄 = Bridge 破棄しか存在しない以上「新しい Bridge を作る」としか読めず、**同じ Store 内容を保ったまま Host だけ作り直すことができない**。maui/ADR-0005 は「破棄の冪等性と破棄後 no-op により MAUI 側の DisconnectHandler 経路から安全に呼べる」と述べているが、DisconnectHandler で `DisposeBridge` を呼ぶと、その後の再接続 (ページ再表示・Shell の再生成) で Store 内容ごと失われるため、facade 側が root を組み直す必要が生じる。

phase-1 の実装は accepted な ADR-0005 に忠実であり、これ自体は逸脱ではない。ただし phase-2 の Handler 設計に入る前に、(a) `releaseHost()` 相当を足す、(b) 「Bridge の寿命 = cross-platform control の寿命であり、DisconnectHandler では破棄しない」と契約を固める、のいずれかを決めておかないと phase-2 で手戻りになる。

**推奨修正**: phase-1 のコード変更は不要。phase-2 の agenda 論点として起票し、`maui/README.md` に現行の寿命契約 (Bridge を破棄したら Host も Store 内容も戻らない) を1行明記する。

### [🔵 Suggestion] 12操作の契約表が `replaceSection` の header text 変更を回避しており、カバレッジが実際より広く見える

**該当箇所**: `ios/Tests/KsSettingsViewBridgeTests/KsBridgeOperationContractTests.swift:140-148` / `android/.../KsBridgeOperationContractTest.kt:120-129`

両 OS とも `replaceSection` のケースは置換後の header を `"S1"` (置換前と同一) にしており、header text を変える経路を通っていない。`KsBridgeUpdateTests.test_replaceSection_はsectionIDのidentityを保つ` も header の変更は Store 状態でのみ確認し、実描画は見ていない。これは既知の header 再描画不具合 (`fix-replace-section-header-refresh` / `fix-android-accessory-header-refresh`) を避ける合理的な選択だが、テスト名が「全12操作が契約どおりに反映される」であるため、この回避が読み手に見えない。ケースのラベルを「`replaceSection`: 既知 ID (header text は不変)」等にするか、テーブルに1行コメントを添えると、修正後に境界を足し戻す手掛かりが残る。

### [🔵 Suggestion] `build-xcframework.sh` を DerivedData 非依存にすれば README の手動回避手順を消せる

**該当箇所**: `ios/binding/build-xcframework.sh:26-36`、`maui/README.md:74-78`

`xcodebuild archive` に `-derivedDataPath` を渡していないため共有 DerivedData を使い、README が「作り直すときは `ios/binding/DerivedData` も消す」という手動手順を要求している。この経路は `KsSettingsView.Binding.iOS.csproj` の `_BuildKsBridgeXcFramework` から自動で呼ばれるため、手動手順を知らない利用者・CI がハマる。`-derivedDataPath "${BUILD_DIR}/DerivedData"` を足して build ディレクトリ配下に閉じ込めれば、`build/` を消すだけで再現性が取れる。スクリプトが framework 未 install を自前で検出して落ちる作り (`:41-46`) は良い。

### [🔵 Suggestion] SDK 内部ターゲットへの依存を SDK 更新時の再検証項目として残す

**該当箇所**: `maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj:47-62`、`maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj:78-86`

`_DetectSdkLocations` / `_SanitizeNativeReferences` / `_CategorizeAndroidLibraries` / `_ResolveLibraryProjectImports` はいずれも SDK 側のアンダースコア付き内部ターゲットで、互換保証がない。SDK の XcodeProject / AndroidGradleProject アイテムが使えない理由はコメントに書かれており判断としては妥当だが、workload / SDK を上げたときに真っ先に壊れる箇所である。`maui/README.md` に「SDK 更新時に再検証する箇所」として2行残しておくと、次の SDK バンプで原因究明が短くなる。

---

## 確認したが問題なしと判断した観点

- デルタスペック `ios-store` の全 Scenario (バッチ配信・既知/未知混在・適用0件・空リスト・重複指定) に対応するテストが `SettingsRootStoreTests` に揃っており、配信時点で購読者が更新後状態を参照できることまで検証している。Host 反映も `ContentUpdateBatchTests` が実描画と行 identity (`firstRowBefore === ...`) で確認している
- `SettingsRootStore.replaceCells` の `root = SettingsRoot(sections:)` は `SettingsRoot` が `sections` のみを持つため情報欠落なし。`Section` の再構築も既存 `replaceCell` と同じ6項目で漏れなし
- `KsBridgeIdentifier` は Android 側で `java.util.UUID.fromString` の短縮形受理を避け、正規表現で iOS の `UUID(uuidString:)` と厳密さを揃えている。両 OS の未知 ID 判定が同一という spec 要求を満たす
- `updateAccessory` の未知 sectionID 素通しは iOS / Android の Store 双方が常に Diff を発行する (対称) ため、申し送り済みの課題どおりで新規の非対称は生じていない
- Android Store の `applyTheme` は `MutableStateFlow` の等値 conflation、iOS は明示的な等値比較で、同値 Theme 非通知が両 OS で成立している (テストでも確認済み)
- 生成された Android binding の C# 表面に internal 型・Store・`isDisposed` の漏れがないことを `obj/.../generated/src` で確認した
- Xcode binding project は `PBXFileSystemSynchronizedRootGroup` を使っており、Bridge に Swift ファイルを足しても project 編集が要らない (取りこぼしの罠がない)
- 足場アーティファクト (proposal / design / specs) は未変更。`tasks.md` はチェックボックスのみの更新で、全項目に対応する成果物が実在する
- `maui/spike/` の生成物 (`.gradle/` / `DerivedData/` / `bin/` `obj/`) はすべて `.gitignore` 済み。追跡対象になるのは spike のソースと gradle wrapper のみで、wrapper jar の同梱は `android/` `samples/android/` と同じ扱い
- Android の Robolectric テストが `Thread.sleep` を含む pump を使う点は、既存 `ks-settingsview-ui` のテスト (`AdapterReattachTest` / `ContentUpdatePayloadTest` 等) と同じ確立済みパターンで、本変更が持ち込んだものではない

---

## アクションプラン

1. **Major-1**: `replaceSection` / `replaceCell` に渡した DTO の `sectionID` / `cellID` が無効になることを、戻り値 (推奨) または doc comment 3か所 + README で明示する
2. **Minor-1**: `Transforms/Metadata.xml` の一致しない `remove-node` を削除し、コメントと `maui/README.md` の記述を実態に合わせる (BG8A00 が消えることを `-t:Rebuild` で確認)
3. **Minor-2**: Android `KsSettingsBridge` の `Context` 非保持に関する doc comment を、Host 経由の保持と `dispose()` の必要性が読める記述へ修正する
4. **Minor-3**: `KsBridgeFont` の `pointSize <= 0` / familyName 未解決時の platform 差を `maui/README.md` の「既知の制約」に追記する
5. **Minor-4**: Host 単独解放の不在を phase-2 agenda 論点として起票し、現行の寿命契約を README に1行残す (phase-1 のコード変更は不要)
6. **Suggestion 3件**: 契約表の `replaceSection` ケースのラベル明確化 / `build-xcframework.sh` の `-derivedDataPath` 追加 / SDK 内部ターゲット依存の再検証メモ
