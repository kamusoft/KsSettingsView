# 検証結果: add-scroll-control (003 回目)

**日付**: 2026-09-30
**判定**: VALID

**対象**: verify-002 (VALID) の後に、オーナー指示でこの change に入った追加スコープ 5 件の実装を再検証する。追加スコープは specs に無く、deviation.md の「スコープの拡張 (オーナー指示)」の行と、それに続く (1)〜(5) の実装・付随修正・オーナー合意・蒸留送りの行を合意済みの差分として扱う。確かめたのは次の 3 点である。

1. 追加スコープの修正で、既存の 97 件の Scenario の対応 (実装・テスト・証跡) が崩れていないか
2. 追加スコープで変わったファイルが、すべて deviation.md の行に箇所として現れているか
3. 足場が書き換えられていないか。tasks.md の差分がチェックの更新だけか

## サマリー

- 97 件の Scenario の対応は崩れていない。各 Scenario に対応していたテスト関数は、名前・行・アサーションともに残っている。追加スコープの修正は、Scenario の実装箇所の意味を変えていない。
- 追加スコープで変わったファイル 36 本は、すべて deviation.md の行に箇所として現れている。未記録の乖離は無い。
- 足場の逆流は無い。tasks.md の差分はチェックの更新だけである。
- テストは全件成功している。

不一致は 0 件で、VALID と判定した。

## 事前条件の検査

| 検査 | 結果 | 根拠 |
|---|---|---|
| 逆流 (足場の書き換え) | ✅ なし | `proposal.md`・`design.md`・`specs/` に HEAD (38a6c74) からの差分は無い (`git status`) |
| tasks.md | ✅ | 差分は 40 行で、すべて `[ ]` → `[x]` だけ (`--word-diff`)。全 40 件にチェックが付いている。追加スコープは tasks.md のタスクに足されていない。オーナー指示の差分として deviation.md で扱われている |
| 未記録乖離 | ✅ なし | 下記「変わったファイルと deviation.md の突き合わせ」 |
| テストの全件成功 | ✅ | 下記「テストの実行」 |

## テストの実行

| platform | 実行 | 結果 |
|---|---|---|
| Android | 本検証で `./gradlew test --rerun-tasks` を実行した。件数は test-results の XML を集計した | kssettingsview 1394 × 2、kssettingsview-bridge 193 × 2 の計 **3174 tests / 0 failures / 0 errors**。ホスト側の報告と一致する |
| MAUI | 本検証で `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` を実行した | **624 tests / 0 failures** |
| iOS | 制約 (Simulator を起動しない) のため、テストは実行していない。`xcodebuild build-for-testing -scheme KsSettingsView -destination 'generic/platform=iOS Simulator'` で、テストを含むパッケージ全体のビルドを確かめた | `** TEST BUILD SUCCEEDED **`、error 0。実行結果は、ホスト側の最新の報告 **1201 tests / 0 failures** (Bridge 189 + Core 88 + SwiftUI 131 + TestSupport 7 + UI 786) を事実として扱った |
| Swift 6 言語モード・Sample の警告・MAUI のフルビルド | 手順にコードの一時的な書き換えや Sample・検証アプリのビルドが要るため、本検証では行っていない | ホスト側の報告を事実として扱った: Swift 6 言語モードの error 0、iOS Sample の concurrency 警告 0、MAUI の Sample / IntegrationHost / 消費者検証アプリの両 OS のフルビルド成功 |

## 1. 既存の Scenario の対応への影響

verify-002.md の後に更新されたファイルを、作業ツリーの変更ファイルの更新時刻で洗い出した。そのうち、Scenario の実装箇所かテストに当たるものを読んだ。

| 変更 | Scenario の実装・テストへの影響 | 結果 |
|---|---|---|
| iOS `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`・`KsSettingsViewController+ScrollControl.swift`・`KsScrollTarget.swift` (追加スコープ (2)) | 足された処理は次の 2 つである。<br>・`viewDidLayoutSubviews` の中で待ち行列を実行したときに限り、そのレイアウトの後 (`main.async`) で祖先を再レイアウトする (`scheduleAncestorRelayout`、`KsSettingsViewController+ScrollControl.swift:198`)<br>・再レイアウトの後に位置を合わせ直す。最後に位置を決めたのが命令なら、同じ行き先・同じ合わせ位置を新しい表示範囲で解き直す (`lastSettledScroll`、`KsSettingsViewController.swift:193`)。控えの戻しなら、上端の要素とずれを保つ (`KsScrollTarget.isAnchor`)<br>いずれも各 Scenario の位置の定義 (start / center / end、両端、控えた要素とずれ) を新しい表示範囲で保つ方向の補正で、定義そのものは変えていない。新しい命令が届いている・アニメーションが進行中のときは合わせ直さないので、「命令は呼ばれた順に処理し、最終位置は最後の命令のもの」も崩れていない。<br>verify-001 の対応表のテスト関数 (`ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift` の :140〜:909) は、同じ名前・同じ行で残っている。戻しの 4 件 (:866・:880・:895・:909) は、控えを取る補助関数 `captureAnchorAtCell` の上で同じアサーションのままである。追加スコープの回帰テスト (:1209〜:1328 の 8 件) は、これらとは別に足されている | ✅ 崩れていない |
| 同上の行番号 | `KsSettingsViewController+ScrollControl.swift` の 190 行以降は、`scheduleAncestorRelayout` の追加で後ろへずれた。verify-001 の対応表の `iSC:189` 以降の行番号は、次のシンボルで読み替える: `executeScrollEntry` :233、`resolveCellID` :252、`resolveSectionID` :270、`warnUnknownScrollTarget` :279、`performScroll` :301、`settleScroll` :332、`stepScrollAnimation` :353、`targetOffsetY` :406、`offsetY` :434、`sectionRangeFrame` :462、`anchorFrame` :505。`KsSettingsViewController.swift` の `disconnectStore` は :368 (`scrollController = nil` は :369)、`viewDidLayoutSubviews` は :519 である。行番号の移動だけで、対応の中身は変わらない | ✅ (行番号の読み替えのみ) |
| 宣言の差分 `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt`・`ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift` (追加スコープ (3) と付随修正) | 変わったのは Store への移動の出し方 (最長増加部分列を動かさない) と、Section の移動の位置だけである。差分の種類と順序、宣言ツリーの最終 ID は変えていない (deviation.md に記録)。DSL 方式の Scenario (明示 ID・key での指し方、状態の変更と同じ処理の命令) が使う引き直しの受け口 (`DSLScrollCommandResolver`) は変わっていない。対応するテスト (`ios/Tests/KsSettingsViewSwiftUITests/DSLScrollControlTests.swift` の :269〜:479、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollControlTest.kt` の :141〜:243 ほか 11 件) は、同じ名前・同じ行で残り、全件成功している | ✅ 崩れていない |
| Compose `KsSettingsView(...)` の引数の追加 `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt` (追加スコープ (4)) | Store 方式・DSL 方式の両方で `rootHeaderText` / `rootFooterText` が `scrollController` の前に足された (Store 方式 :49-51、DSL 方式 :99-101)。`scrollController: KsScrollController? = null` と、Host・引き直しの受け口への接続 (:60-64、:126-155) は変わっておらず、spec の「引数 `scrollController` (既定 `null`)」を満たす。`scrollController` はこの change で新しく足した引数なので、位置の移動で既存の呼び出しが壊れることは無い。Compose の Scenario のテストは全件成功している | ✅ 崩れていない |
| Sample (`samples/android/.../ScrollControlDemoScreen.kt`・`samples/ios/KsSettingsViewSample/ScrollControlDemoView.swift`) | Android は Root Footer を `rootFooterText = "ここが内容の末尾です（Root Footer）"` に置き換えた。これで、verify-001 の時点で deviation 記録済みだった「Compose の `Text` で出す platform 差」が解消した。文言は 3 platform で一致したまま。iOS は通知の閉包の書き方を `MainActor.assumeIsolated` にそろえただけで、操作と命令は変わっていない。証跡: `evidence/android-native-root-footer-text-after.png` で、末尾へ送った状態の最下部に同じ文言の Root Footer が見えることを確かめた。6.3 の到着・着地の証跡は、これらの書き換えの前に撮ったものである。それでも有効とした理由は 2 つある。<br>・書き換えは Root Footer の出し方と閉包の書き方だけで、命令・対象・位置は変わらない<br>・追加スコープ (2) の補正は `viewDidLayoutSubviews` の中で実行した命令に限られ、デモの操作 (表示の後のタップ) はそこを通らない | ✅ 崩れていない |
| iOS の各 Cell と `KsCell.swift` (付随修正、doc コメントのみ) | 型と挙動は変えていない | 影響なし |
| MAUI の版 (`maui/Directory.Packages.props` ほか、追加スコープ (1)) | facade・Bridge のコードは変えていない。MAUI のテスト 624 件は全件成功している。maui-core・maui-bridge の Scenario の対応は変わらない | 影響なし |

## 2. 変わったファイルと deviation.md の突き合わせ

verify-002.md より後に更新された変更ファイルは 36 本である。すべて deviation.md の行に箇所として現れている。

| ファイル | deviation.md の行 |
|---|---|
| `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt`、`ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift` | スコープの拡張 (3) の実装、[付随修正] Section の移動 |
| `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculatorTest.kt`、`ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift`、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeRootAccessoryTextTest.kt` | [付随修正] スコープの拡張 (3)(4) のテスト、[付随修正] `ComposeRootAccessoryTextTest.kt` の待ち方 |
| `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt` | スコープの拡張 (4)・(4) の実装判断 (Compose の `KsSettingsView(...)` の Root Header / Footer の文字列。ファイル名ではなく API 名で書かれているが、指す箇所はこのファイルだけである) |
| `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/ScrollControlDemoScreen.kt` | スコープの拡張 (4) の「Sample の Root Footer もそれに置き換える」 |
| `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、`KsSettingsViewController+ScrollControl.swift`、`KsScrollTarget.swift`、`ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift` | スコープの拡張 (2) の実装、(2) の見直し (レビュー指摘) |
| `ios/Sources/KsSettingsViewCore/KsCell.swift` と `ios/Sources/KsSettingsViewUI/` の 12 本 (`ButtonCell`・`CheckboxCell`・`CommandCell`・`CustomCell`・`DatePickerCell`・`EntryCell`・`NumberPickerCell`・`PickerCell`・`RadioCell`・`SimpleCheckCell`・`SwitchCell`・`TimePickerCell`) | [付随修正] iOS の各 Cell の通知の閉包の公開 doc (12 本の名前が git の変更ファイルと一致) |
| `samples/ios/KsSettingsViewSample/` の 8 本 (`BasicCellsDemoView`・`CustomCellDemoView`・`InputCellsDemoView`・`SampleSliderCell`・`SectionDecorationDemoView`・`SectionDecorationSpikeView`・`UnifyCellCommonFieldsDemoView`・`VisibilityDemoView`) と `ScrollControlDemoView.swift` | スコープの拡張 (5) の実装 (8 本の名前が git の変更ファイルと一致)。`samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj` の差分は、当初スコープの `ScrollControlDemoView.swift` の追加 4 行だけで、「Xcode プロジェクトの設定は変えていない」と一致する |
| `maui/Directory.Packages.props`、`samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj`、`verification/maui/VerificationApp.csproj` | スコープの拡張 (1) の実装・利用者への影響 (オーナー合意済み) |

追加スコープの実機の証跡は、deviation.md が名指しするものがそろっている。

- (1): `evidence/maui-android-a11y-crash-before-after.log`、`evidence/maui-android-a11y-crash-{before,after}-*.png`
- (2): `evidence/maui-ios-reconnect-band-before-after.log`、`evidence/maui-ios-reconnect-band-{before,after}-*.png`、`evidence/maui-ios-reconnect-band-after-recheck-*.png`
- (4): `evidence/android-native-root-footer-text-{before,after}.png`

このうち `maui-ios-reconnect-band-after-recheck-1-B-after-reconnect.png` は、再接続の後に一覧が画面の下端まで届き、白い帯が無いことを確かめた。追加スコープは specs に Scenario を持たないので、これらは対応表の対象外として、証跡の存在だけを確認した。

## 対応表

Scenario 97 件の状態は verify-002.md から変わらない。✅ または ⚠️ (deviation 記録済み) が 97 件、❌ は 0 件である。変わった点は次の 2 つで、どちらも状態は変わらない。

- **settings-view-ios-ui のうち iOS Host の実装箇所の行番号**: 上の「1.」の表の読み替えに従う。
- **samples-android「各操作で対象の位置へ移る」**: ⚠️ の根拠だった Root Footer の platform 差の deviation 行 (sample-parity) は、追加スコープ (4) で実装側が解消した。この行は ✅ のままとする。証跡に `evidence/android-native-root-footer-text-after.png` を加える。

verify-001.md の注 1〜3 は引き継ぐ。

## 不一致

なし。

## 申し送り (判定に含めない)

- 長命層 (decisions / handbook / concepts) と利用者向け文書 (README・skills/) への反映は、deviation.md の「蒸留時に反映」の行による申し送りで、不一致としていない。追加スコープの分も同じ扱いである: Compose の文字列の Root Header / Footer、宣言の差分の移動の最小化、MAUI の要求版 10.0.71、祖先の再レイアウトの後の位置の合わせ直し、iOS の通知の閉包のスレッド
- deviation.md の「sample-parity (本体公開 API の platform 差)」の行は、追加スコープ (4) で実態が解消した。蒸留時は、この行を統一課題として扱わないよう申し送る
- iOS のテスト実行・Swift 6 言語モード・Sample の警告数・MAUI のフルビルドは、制約によりホスト側の報告に依っている
