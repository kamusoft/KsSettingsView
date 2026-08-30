# レビュー結果: add-maui-modern-style (002 回目)

**日付**: 2026-08-20
**判定**: APPROVED

## サマリー

1 周目で確定した 5 指摘 (Critical 1 / Major 1 / Minor 3) はすべて解消している。Critical の Android Bridge は
「Bridge で正規化を前倒しする」のではなく**非検証の `PaddingValues` 実装を輸送層に置いて生値を運ぶ**方式が
採られており、デルタスペックの「Theme 構築時には拒否せず、正規化は描画時のみ」という契約に最も忠実な直し方に
なっている。回帰装置も両 OS 対称に入り、以前は Bridge を迂回していた非有限値の検証が実経路を通るようになった。
新規コード (`KsBridgeSectionMargin.kt` / 両 OS の Bridge テスト / `parity-table.md` / ViewModel) を新鮮な目で
読み直したが、Critical / Major に相当する欠陥は見つからなかった。

ビルドとテストはレビュー側で全件再実行し、成功を確認した (facade 439 / iOS 560 / Android 2522、いずれも 0
failures。`dotnet build -f net10.0-ios` / `-f net10.0-android` はどちらも 0 警告 / 0 エラー)。

## 前回指摘の再確認

### 1. [🔴 Critical] Android Bridge で負値・非有限の SectionMargin が例外になる → **解消**

**修正内容**: `android/.../bridge/KsBridgeSectionMargin.kt` (新規) が `PaddingValues` インタフェースを
成分そのまま返す形で実装し、`KsBridgeTheme.kt:188` の `resolveSectionMargin()` が Compose 標準ファクトリの
代わりにこれを使う。

**判定根拠**:

- 前回の欠陥はファクトリの `init` 事前条件だったので、ファクトリを通さない実装に替えれば経路そのものが
  消える。`KsBridgeSectionMargin` は 4 メソッドの実装だけを持ち、検証を一切しない。
- 実測でも確認した。1 周目に例外を確認した入力とほぼ同じ値 (NaN / -8 / +∞ / -∞) を `setTheme` に渡す
  `KsBridgeThemeTest.kt:138` `負値と非有限の Section 装飾は素通しされる` が Android の全 2522 件の中で成功して
  おり、test-results XML にも当該メソッド名が記録されている。修正が戻れば `setTheme` の時点で例外になり、
  このテストは必ず落ちる (回帰検出力あり)。
- 委譲先まで届くことも `KsBridgeThemeTest.kt:169` / `KsBridgeThemeTests.swift:170` が Host に描画させて
  確認している。`SectionBoxMetrics` 以外に `Theme.sectionMargin` を読む経路が無いことは grep で確認した
  (Android: `SectionBoxMetrics.kt:91` のみ、iOS: `SectionBoxMetrics.swift:57` のみ)。したがって非有限値の
  防波堤は両 OS とも描画直前の 1 箇所に集約されている。
- 方式 1 が採られたため、正規化位置は「描画時」のままで spec と一致する。deviation の記録は不要 (実際に
  deviation.md も作られていない) — 正しい。
- 前回のアクション 4 (XML doc と実挙動の一致) も満たされた。`maui/KsSettingsView.Maui/SettingsView.cs:847`
  の「負の成分は Native の描画時に 0 として扱われる」は、Android でも例外を経ずに成立する。

### 2. [🟠 Major] Bridge 層に負値・非有限 margin の回帰テストがない → **解消**

`KsBridgeThemeTest.kt:138,169` と `KsBridgeThemeTests.swift:145,170` に、両 OS 対称で 2 段構えのケースが
入った (① Theme まで生値が届く ② 描画まで通して例外なく 0 になる)。②は Host を attach して Modern を
適用したうえで先頭 Cell 行の水平位置が 0 であることまで見ており、「例外が出ない」だけの弱い主張に
なっていない。UI 層の既存テストが `RawPaddingValues` で Bridge を迂回していた穴は、これで塞がった。

### 3. [🟡 Minor] Classic × Bordered の視覚証跡欠け → **解消**

`screenshots/` が 24 枚 (12 組 = 2 OS × 2 style × 3 preset) に揃った。追加された
`{maui,native}-{ios,android}-classic-bordered.png` の 4 枚を目視で突き合わせ、両 OS とも **Classic では
border が描かれず Section が全幅**であること、Section 構成・文言・separator 規則が MAUI と native で
一致することを確認した。差は操作部の chrome (RadioButton + Picker ↔ segmented / dropdown) と
Bluetooth バッジの字形だけで、いずれも sample-parity が許容する platform 差。

### 4. [🟡 Minor] sample-parity 対応表の成果物がない → **解消**

`parity-table.md` が追加された。対象ファイル・メニュー文言・操作部文言・preset 3 件の 4 属性値・
Section / Cell 構成・下地 Theme・初期状態・許容差分の 7 節構成で、task 4.1 の成果物として十分。
記載値を実コードと照合し、preset 値 (32/32/32/0・radius 8・border 2 + #C7C7CC)、初期 style (Modern)、
下地 Theme 6 項目、Section 4 件の構成が実際に一致していることを確認した。

### 5. [🟡 Minor] `SectionDecorationDemoViewModel.Styles` が未使用 → **解消**

`Styles` は削除され、XAML から実際に使われる `Presets` だけが残っている
(`ViewModels/SectionDecorationDemoViewModel.cs:19`)。style 選択は `RadioButton` 直書き + `RadioButtonGroup`
の構成が正となり、Android サンプルの持ち方 (操作部側にローカル) と揃った。

## 新規コードの指摘事項

### [🔵 Suggestion] `KsBridgeSectionMargin` と `RawPaddingValues` が同内容で二重に存在する

**該当箇所**: `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeSectionMargin.kt`
と `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RawPaddingValues.kt`

**問題点**: 本文が完全に同じ実装 (4 メソッドの start/end 写像) が 2 ファイルある。将来どちらかだけが
直されると、テストが観測している型と production が運ぶ型がずれる。

**推奨**: **現状のままで良い**と考える。統合するには ui モジュール側の実装を bridge から見える公開型に
する必要があり (Kotlin の `internal` はモジュール境界を越えない)、ライブラリの公開 API を 1 型ぶん
広げる代償のほうが大きい。両ファイルの KDoc が互いの存在理由 (「ファクトリが 0 以上を要求する」) を
説明しているので、今の重複は意図が読める。片方を直すときは他方も見る、という運用で足りる。
指摘として残すのは、将来の読み手が「片方だけ直す」ことを防ぐため。

### [🔵 Suggestion] iOS の `firstRowLeading` が取得失敗時に `-1` を返す

**該当箇所**: `ios/Tests/KsSettingsViewBridgeTests/KsBridgeStyleTests.swift:122-130`

**問題点**: layout attributes が取れなかった場合に `XCTFail` ではなく `-1` を返すため、失敗時のメッセージが
「0 と -1 が違う」になり、原因 (行が実描画されていない) が読み取りにくい。Android 側の同等ヘルパ
(`KsBridgeStyleTest.kt:143` `firstCellRowLeft`) は `error("Cell 行が実描画されていない")` で落としており、
そちらのほうが診断しやすい。

**推奨**: 失敗を `XCTFail` + `nil` 返し、または `guard ... else { XCTFail(...); return 0 }` にして
両 OS の失敗時の読み取りやすさを揃える。挙動の正しさには影響しないため優先度は低い。

## 確認した観点 (指摘なし)

- **仕様充足**: 5 capability の全 Requirement / Scenario を実装・テストへ突き合わせた (詳細は
  `verify-001.md`)。❌ は 0 件。
- **足場アーティファクト**: `git diff HEAD -- kasane/changes/add-maui-modern-style/` は tasks.md の
  チェックボックスのみ。proposal / specs は未変更で逆流なし。deviation.md は不在で、これは方式 1 を
  採った以上正しい (記録すべき乖離が無い)。
- **tasks.md の虚偽チェックなし**: 1.1〜5.1 のすべてに対応する実装・テスト・証跡を確認。4.1 は
  `parity-table.md`、5.1 は `screenshots/` 12 組。
- **堅牢性**: 非有限・負値の入力は両 OS とも Bridge を素通りして描画直前で 0 へ落ちる。定義域外の style
  序数は両 OS とも Classic へ倒れる (`Int.MAX_VALUE` / `-1` / `2` / `7` のケースあり)。`isDisposed` 後の
  `setStyle` は既存の `setTheme` と同じく無視で、挙動が揃っている。`Connect()` 内の `SetStyle` は既存の
  try/catch (失敗時 `Disconnect()` して rethrow) の内側にあり、中途半端な接続状態を作らない。
- **テストの手抜きなし**: 新規テストに「言い訳コメントによる実質スキップ」は無い。Bridge の非有限
  描画テストはヘッダ位置ではなく実際の行位置を見ており、`assertEquals` の左右が両方 null になって
  素通りする形にもなっていない (view holder 未取得なら null 比較で落ちる)。
- **設計品質**: 輸送層 (`KsBridgeSectionMargin`) が「検証しない」という契約を型として表現しており、
  正規化の責務が描画層 1 箇所に集約されている。`Theme` の値等価 (同値 Theme の再適用スキップ) は
  data class のままなので壊れていない。命名・KDoc / XML doc・コメント規約は既存に揃っている。
- **規約適合**: `python3 scripts/comment-policy-lint.py` は 657 ファイル / 禁止 0 件。コメント中の外部参照は
  `maui/ADR-0002` / `maui/ADR-0023` / `maui/ADR-0024` の許容形式のみ。
- **lessons**: `lessons/code-review.md` の重点観点 L-001 (ミューテーションで検出力を実測する) は、
  今回は Critical の修正確認が争点だったため、1 周目に実測済みの例外がテストとして固定されたことの
  確認 (test-results XML の当該メソッド成功) で代替した。「指摘しないこと」に昇格済みルールはない。
- **ビルド / テスト (レビュー側で再実行)**: `dotnet test -f net10.0` 439 / 0、
  `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'`
  560 / 0、`./gradlew test --rerun-tasks` 2522 / 0 (test-results XML 集計)、
  `dotnet build -f net10.0-ios` / `-f net10.0-android` いずれも 0 警告 / 0 エラー。

## アクションプラン

Critical / Major はなし。以下はいずれも任意で、この変更のマージを止めるものではない。

1. (任意) `KsBridgeSectionMargin` と `RawPaddingValues` の重複について、片方を直すときは他方も見る旨を
   コード側で示したいなら、双方の KDoc に相互参照を 1 行足す。
2. (任意) `KsBridgeStyleTests.firstRowLeading` の失敗時を `XCTFail` にして Android 側と診断性を揃える。
