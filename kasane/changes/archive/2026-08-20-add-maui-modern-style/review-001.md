# レビュー結果: add-maui-modern-style (001 回目)

**日付**: 2026-08-20
**判定**: CHANGES_REQUESTED

## サマリー

facade 公開 API・輸送層・両 OS Bridge・native 堅牢化・サンプルまで、デルタスペックの Requirement / Scenario をほぼ網羅的に実装しており、命名・XML doc・コメント規約・sample-parity の水準は高い。ビルドとテストはレビュー側で再実行して全件成功を確認した (facade 439 / iOS 560 / Android 2518、いずれも 0 failures。net10.0-ios / net10.0-android の `dotnet build` も 0 警告)。

ただし Android の Bridge に、負値・非有限値の `SectionMargin` が **`Theme` へ届く前に `IllegalArgumentException` で落ちる** 経路が残っている。これは maui-core の「facade は検証せず素通しし、正規化は Native の描画時に委譲する」という Requirement を Android 側で成立させておらず、公開 API のセッターから到達できるクラッシュであり、かつ iOS との挙動乖離を生む。本変更が新設した経路上の欠陥のため CHANGES_REQUESTED とする。

## 指摘事項

### [🔴 Critical] Android Bridge で負値・非有限の SectionMargin が例外になり、Native の描画時正規化まで到達しない

**該当箇所**: `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeTheme.kt:186-192` (`resolveSectionMargin()`)

**問題点**:

`resolveSectionMargin()` は Compose 標準の `PaddingValues(...)` ファクトリで方向対応型を組み立てているが、この
ファクトリは `PaddingValuesImpl` の `init` で全成分に「0 以上」を要求する。負値と NaN はここで
`IllegalArgumentException` になる。この事実はこのリポジトリ自身のテストヘルパが明記している
(`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RawPaddingValues.kt` の KDoc —
「Compose 標準の `PaddingValues(...)` ファクトリは「0 以上」を構築時に要求し、負の成分と NaN を拒否する」)。

レビュー中に一時プローブテストで実測して確定した (プローブは実行後に削除済み、作業ツリーは元の状態):

```
PROBE-NEGATIVE:   java.lang.IllegalArgumentException: Start padding must be non-negative
PROBE-NONFINITE:  java.lang.IllegalArgumentException: Top padding must be non-negative
```

到達経路は公開 API から一本道で塞がっていない:

`SettingsView.SectionMargin = new Thickness(-8, -4, -2, -1)`
→ `ApplyTheme()` → `KsBridgeGateway.SetTheme` (Android)
→ `KsSettingsBridge.setTheme` → `KsBridgeTheme.resolve()` → `resolveSectionMargin()` → **throw**

結果として次の Requirement / Scenario が Android の実経路で成立していない:

- maui-core「Theme の Section 装飾4属性の公開」— 「facade は platform 既定値の定数を持たず、値の検証
  (負値の正規化・radius の clamp・例外送出) を行わない — 正規化は Native の描画時正規化に委譲する (SHALL)」。
  委譲先へ届く前に落ちるため、委譲が成立していない。net10.0 ユニットテストの 2 Scenario
  (`範囲外の値でも例外を投げず素通しする` / `非有限数も例外を投げず素通しする`) は fake gateway 上で通るが、
  実 gateway では同じ入力が例外になる。
- settings-view-android-ui「Section 装飾値の非有限数正規化」— 本変更で入れた `SectionBoxMetrics.px()` の
  非有限ガード (task 2.6) は、MAUI 経路からは**到達不能**。非有限成分を持つ `PaddingValues` を作れるのは
  テスト専用の `RawPaddingValues` だけになっている。
- 加えて iOS は `NSDirectionalEdgeInsets` が負値・非有限を受けるため素通しし描画時に 0 へ落ちる。
  同じ利用者コードが iOS では描画・Android ではクラッシュとなり、プラットフォーム間の挙動統一という
  製品目的に反する。

公開 XML doc も現状と食い違う: `maui/KsSettingsView.Maui/SettingsView.cs:847` は
「負の成分は Native の描画時に 0 として扱われる」と約束しているが、Android では例外になる。

**推奨修正** (実装側で選択。いずれも facade は無検証のままで済む):

1. Bridge で非検証の `PaddingValues` 実装 (`RawPaddingValues` 相当を production 側へ置く) を使って生値を
   そのまま `Theme` へ渡し、正規化は既存の `SectionBoxMetrics.px()` に委ねる。デルタスペックの
   「正規化は描画時のみ」「Theme 構築時には拒否しない」に最も忠実。
2. `resolveSectionMargin()` で 4 成分を非有限 → 0 / 負値 → 0 に正規化してから `PaddingValues` を組む。
   単純だが、正規化の位置が「描画時」から「Theme 構築時」へ前倒しになるため、
   settings-view-android-ui の「Theme 構築時には拒否しない既存契約は維持する (正規化は描画時のみ)」との
   関係を deviation.md に記録する必要がある。

どちらを採るにせよ、`SettingsView.cs:847` の doc 記述と実挙動が一致することを確認すること。

### [🟠 Major] Bridge 層に負値・非有限 margin の回帰テストがなく、上記の穴が検出できていない

**該当箇所**:
- `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeThemeTest.kt:75-133`
- `ios/Tests/KsSettingsViewBridgeTests/KsBridgeThemeTests.swift:87-138`

**問題点**:

Bridge の `resolve()` に対するテストは正値・全 null・部分 null の 3 ケースだけで、負値・非有限値の
入力が 1 件もない。一方 UI 層の非有限テスト
(`SectionBoxMetricsTest.非有限の寸法は 0 として扱う` / `ModernSectionDecorationTest.非有限の寸法を持つ Theme でも…`)
は `RawPaddingValues` で `Theme` を直接組み立てており、**Bridge を迂回している**。
その結果「MAUI から非有限値を入れたときに何が起きるか」を通しで確かめるテストが存在せず、
Critical の欠陥が全 2518 件のテストを通過してしまっている。tasks.md 3.6 の
「native ui: 非有限 … が例外なく 0 として描画される (両 OS)」は UI 層単体としては満たされているが、
本変更が新設した MAUI → native の経路については未検証。

**推奨修正**: `KsBridgeThemeTest` / `KsBridgeThemeTests` に「負値および非有限の 4 成分を持つ
`KsBridgeTheme` を `setTheme` しても例外にならず、描画時に 0 へ解決される」ケースを両 OS 対称に追加する。
Critical の修正が回帰しないことを担保する最短の装置になる。

### [🟡 Minor] サンプル ViewModel の `Styles` が未使用

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/ViewModels/SectionDecorationDemoViewModel.cs:18-23`

**問題点**: `Styles` は XAML から参照されていない (style 選択は `RadioButton` を 2 個直書きしており、
`ItemsSource` を持たない)。リポジトリ全体を検索しても参照は宣言箇所のみ。デッドコードであり、
「選択肢は `Styles` が正」と誤読させる。

**推奨修正**: `Styles` を削除する (RadioButton 直書きを正とする)。あるいは `Styles` を
`BindableLayout` / `ItemsSource` の供給元として実際に使う。前者を推奨 — Android サンプルも
操作部側で `listOf(Classic, Modern)` をローカルに持っており、ViewModel には置いていない。

## 確認した観点 (指摘なし)

- **仕様充足**: maui-core / maui-bridge / samples-maui / settings-view-ios-ui の Requirement と Scenario を
  実装・テストへ突き合わせ、上記 Critical を除き対応を確認した。序数輸送 (0/1)・定義域外の Classic 正規化・
  部分 null margin の全体未指定化・borderColor の platform 色変換・Host 生成前設定の適用・
  `releaseHost()` 後の style 維持・gateway 初回接続時の配信は、いずれも両 OS 対称のテストがある。
- **足場アーティファクト**: proposal / specs は未変更 (`git diff HEAD` は tasks.md のチェックと
  screenshots/ の追加のみ)。逆流修正なし。deviation.md は存在せず、記録漏れに該当する乖離は
  上記 Critical のみ (これは合意済み差分ではなく欠陥として扱う)。
- **tasks.md の虚偽チェックなし**: 1.1〜5.1 の各項目に対応する実装・テスト・証跡を確認した。
  5.1 の視覚照合証跡は `screenshots/` に maui-*/native-* の 10 対が揃っており、
  iOS modern-standard の対を目視して Section 構成・文言・箱の配置・separator 規則の一致を確認した。
- **ビルド / テスト (レビュー側で再実行)**:
  `dotnet test` 439 tests / 0 failures、
  `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` 560 tests / 0 failures、
  `./gradlew test` 2518 tests / 0 failures (test-results XML 集計)、
  `dotnet build -f net10.0-ios` / `-f net10.0-android` いずれも成功・0 警告。
- **規約適合**: `python3 scripts/comment-policy-lint.py` は 657 ファイル / 禁止 0 件、`--selftest` も全件 OK。
  コメント中の外部参照は `maui/ADR-0023` / `maui/ADR-0024` の許容形式のみ。
- **sample-parity (cross/ADR-0016)**: メニュー文言「Section 装飾デモ（style 切替）」、Section 構成 4 件、
  Cell 文言 (機内モード / Wi-Fi / demoAP-0a1b2c-5 / Bluetooth / オン / バッテリー / 外観モード / 自動 /
  テキストサイズを変更 / True Tone / ボーダー指定時の例)、Header・Footer 本文、preset 名 3 種
  (既定 / 余白広め・角丸小 / ボーダーあり) と preset 値 (margin 32/32/32/0・radius 8・border 2 + #C7C7CC)、
  初期 style (Modern)、下地 Theme の 6 項目が native 側と一致していることを突き合わせた。
  操作部の chrome 差 (RadioButton + Picker vs SegmentedButton + DropdownMenu) とバッジ画像の字形差は、
  規約が許容する platform 差の範囲。
- **設計品質**: `ListStyle` を Theme snapshot に載せず独立経路にした点、`KsThemeSnapshot` の record 値等価が
  保たれる点 (同値 Theme 再適用スキップに影響なし)、Bridge が style を Host 外で保持する対称性、
  `Number(int?)` オーバーロードによる borderColor の整数輸送、既存 BindableProperty 宣言スタイル
  (`typeof(Color)` + `default(Color)`) との一貫性を確認した。`@get:JvmSynthetic` は当該ファイルの既存慣行に沿う。

## アクションプラン

1. **[Critical]** `KsBridgeTheme.resolveSectionMargin()` (Android) の非検証化または正規化。方式 2 を採る場合は
   deviation.md に「非有限・負値の正規化位置を描画時から Bridge の Theme 構築時へ前倒しした」旨を記録する。
2. **[Major]** 両 OS の `KsBridgeTheme` テストに負値・非有限の通し回帰ケースを追加する。
3. **[Minor]** `SectionDecorationDemoViewModel.Styles` を削除する。
4. 1 の修正後、`SettingsView.cs:847` の XML doc の記述と実挙動の一致を確認する。
