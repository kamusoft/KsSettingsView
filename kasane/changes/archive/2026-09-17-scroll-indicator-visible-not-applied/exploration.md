# Exploration: scroll-indicator-visible-not-applied

## 課題 / 動機

`SettingsView.ScrollIndicatorVisible` は**宣言され bridge で輸送されているが、どちらのプラットフォームでも適用されていない**。Skill には利用可能なプロパティ (既定 `true`) として載っているため、利用者は「効くもの」として書く。

Android は View の既定がスクロールバー非表示のため、**設定リストにスクロールバーが出ない**という症状で顕在化する。iOS は UIKit の既定が表示なので偶然それらしく見えているだけで、`false` を指定しても消えないはず (未検証)。

`../ColorAnalyzer` の移行作業 (`../ColorAnalyzer/kasane/changes/migrate-settingsview-to-kssettingsview`) で、Android のスクロールバーが出ないという退行として発見された。移行前の `AiForms.Maui.SettingsView` では表示されていた (実機で本番ストア版と比較して確認済み)。

### 調査済みの内容 (同じ調査を繰り返さないための記録)

- **宣言と輸送はある**: `maui/KsSettingsView.Maui/SettingsView.cs:185-187`, `:620-623` に `ScrollIndicatorVisible` (`bool?`) があり、`KsThemeSnapshot` を経て `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:484` / `Platforms/Android/KsBridgeGateway.cs:489` まで届いている
- **Android は受け取るだけ**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/core/Theme.kt:95` が `scrollIndicatorVisible: Boolean = true` を宣言しているが、RecyclerView へ適用する箇所が無い。`android/` 全体で `scrollbars` / `isVerticalScrollBarEnabled` / `scrollBarStyle` は**ヒット 0 件**、スクロールバーを持つ layout XML も無い (`res/` は drawable / themes / ids のみ)
- **RecyclerView は素のコンストラクタ**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:76` は `RecyclerView(context).apply { layoutParams / layoutManager / supportsChangeAnimations }` のみ
- **iOS も受け取るだけ**: `Theme.swift` の宣言と bridge の受け渡しのみ。`showsVerticalScrollIndicator` を設定する箇所は**ヒット 0 件**で、UIKit 既定 (表示) に乗っているだけ
- **Skill には載っている**: `skills/*/kssettingsview-maui/references/styling.md:24`, `:102` / `skills/*/kssettingsview-android/references/styling.md:81` に既定 `true` として記載がある
- **実機での症状確認**: Android 実機の設定リストで低速ドラッグ中にスクリーンショットを取得したところ、内容はスクロールしているがスクロールバーは描かれていない

### 参考: 移行元 (AiForms) の実装

`AiForms.Maui.SettingsView` は aar に `settingsViewScrollBars` スタイル (`android:scrollbars="vertical|horizontal"`) を持ち、`AiRecyclerView` のコンストラクタが `ContextThemeWrapper` 経由でそれを当てていた。aar の `attrs.xml` に「CollectionView/CarouselView renderer に scrollbars スタイルを足すため」という注記があり、**Android で明示的に有効化するのは意図的な実装**だった。

## 検討した選択肢 (却下案と理由を含む)

- **Android の適用方法**
  - 採用: 同梱テーマ (`Theme.KsSettingsView.Internal`) に `recyclerViewStyle` (`android:scrollbars="vertical"` を持つ style) を定義し、RecyclerView を `context.ksThemedContext()` から生成したうえで、`isVerticalScrollBarEnabled` を Theme の値で切り替える。AiForms の `settingsViewScrollBars` + `ContextThemeWrapper` と同じ手
  - 却下: `isVerticalScrollBarEnabled = true` の設定だけ — 描画されない。`View(Context)` は `android:scrollbars != none` のときしか `initializeScrollbarsInternal` を呼ばず、`setVerticalScrollBarEnabled` は `mViewFlags` を反転するだけで `ScrollabilityCache` / `ScrollBarDrawable` を作らない。`onDrawScrollBars` は `mScrollCache == null` で即 return する (AOSP `View.java` で確認)
  - 却下: API 29+ の `verticalScrollbarThumbDrawable` 単独 — state が OFF のままで不十分。`isScrollbarFadingEnabled` との組み合わせと呼び出し順に依存し、誤ると `onDrawScrollBars` で NPE になり得る経路がある (コード読解ベース、実機未検証)
  - 保留 (実装者の裁量): XML から inflate する方法。同梱テーマ経由で足りなければ代替にできる
- **Android の既定値**: 「Android だけ既定を非表示に変える」案は却下。宣言・concepts・skills がすべて既定 `true` で、AiForms からの移行者が明示指定なしでは退行したままになり、OS 間で既定が割れるため

## 決定事項

- 2026-09-17 変更級 S / cross で直接実装に進む (ユーザー確定)。提案書は作らず、tasks は実装フェーズで持つ
- 2026-09-17 既定は表示 (`true`) のままとし、不具合修正として扱う (ユーザー確定)。何も指定していない既存利用者の Android 画面にスクロールバーが出るようになる点はリリースノートに明記する
- 対象は縦方向のみ。設定リストは縦にしかスクロールしないため、AiForms の `vertical|horizontal` は引き継がない
- iOS は `showsVerticalScrollIndicator` を、背景色と同じ二段 (`KsSettingsViewController.loadView()` の初期設定と、`applyTheme` から呼ぶ適用処理) で当てる。SwiftUI 版は同じ controller を包むだけなので 1 箇所で両方式に効く
- 棚卸しで見つかった iOS の `Theme.headerBackgroundColor` / `footerBackgroundColor` の未適用を本 change で併せて直す (下記「棚卸しの結果」)
- テストは既存の形に合わせる: iOS は `HostViewLoadRestoreTests` と同じく実 window に載せて collection view の属性を assert、Android は `internalRecyclerView()` (`KsSettingsView.kt:1139`) から取り出して Robolectric で assert (例: `SectionAccessoryThemeRefreshTest`)。Theme の差し替えに追従することも検証する
- maui-support ロードマップの phase-8-scroll-control は ScrollTo 系の命令 API が主題で、本件 (インジケータ表示) とは重ならない

### スコープ変更 (2026-09-17、実装フェーズ中のオーナー指示)

- iOS の Header / Footer 背景色を反映したうえで、**`headerBackgroundColor` / `footerBackgroundColor` の既定値を両 OS とも透明に変える** (ユーザー確定)。S 級で着手した後の指示で、公開契約の既定値変更を含むため変更級を上げて進め直す
- 経緯: 既定値は透明ではなく light #F2F2F7 / dark #000000 (`kasane/concepts/core/styling/style-resolution.md` の既定色の表) で、list 下地の既定は light #FFFFFF。iOS に反映すると、何も指定していない iOS 画面 (light) に薄いグレーの帯が新たに出る。オーナーの意向は「既定が透明なら問題ない」。移植元 AiForms の既定は Transparent (`kasane/concepts/cross/reference/aiforms-spec-summary.md`)
- 帰結: iOS は既定の見た目が今のまま (帯なし) で、指定した色は効くようになる。Android は今出ている既定の帯が消える (既定の見た目が変わる)。リリースノートに明記する
- 探索時の見落としの訂正: iOS が背景色を適用しないことは `kasane/concepts/core/styling/list-appearance.md` に「既知の platform 非対称として、背景色の共通反映は保証しない」と明記されていた (理由を定めた ADR は無い)。棚卸しでは「単なる未適用」と扱ったが、既知として記録された現状だった。蒸留時にこの記述と既定色の表を書き換える
- 2026-09-17 追加のオーナー指示: 回転ホイールにはスクロールバーを出さないが、PickerCell の候補リストには出す。候補リストのバーも `scrollIndicatorVisible` に従わせる (既定で出る、`false` なら設定リストと一緒に消える)。iOS の候補リスト (現状は OS 既定で常に表示) にも反映処理を足す
- 2026-09-17 `ui/` (brief・承認モック) は省略する (ハーネス規約からの合意済みの逸脱。選ぶ見た目が無く実機証跡で確認できるため。相方 spec レビューの指摘をオーナー判断で降格)
- 提案化で詰める点: 公開定数 (`Theme.defaultHeaderBackgroundColor` / `defaultFooterBackgroundColor`) の扱い、Android の `Color.Unspecified` からの解決先、Modern スタイルでの Header / Footer 領域の見え方、dark の既定、MAUI facade (既定 null の素通し) と concepts / skills の記述への影響
- 実装フェーズの現況 (未 commit): スクロールバーの反映 (両 OS)、利用者所有コンテンツの Context 維持 (Android)、iOS の背景色反映 (text 形式のみ、既定値は未変更) とテストが作業ツリーにある。review-001 (CHANGES_REQUESTED: スクロールバー有効化が選択シート・ホイールへ波及) の修正サイクル中

### 棚卸しの結果 (2026-09-17、ksn-scout)

「宣言・輸送されているが適用されていない」プロパティを Theme 全項目 / CellStyle 14 項目 / MAUI `SettingsView` の BindableProperty 51 個について洗った。

- 未適用は 2 組のみ: `scrollIndicatorVisible` (両 OS) と、**iOS の `headerBackgroundColor` / `footerBackgroundColor`**
- iOS の Header / Footer 背景色: 出現は宣言 (`ios/Sources/KsSettingsViewUI/Theme.swift`)・bridge の写し (`ios/Sources/KsSettingsViewBridge/KsBridgeTheme.swift:131,136`)・Theme の等価性テストのみ。accessory 生成 (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1155-1210`, `:2174-2200`) は `textColor` と `font` しか渡していない。Android は適用済み (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:60,65,221,224`)
- `kasane/concepts/ios/api/ios-native-host.md:147` は iOS でも効く前提の記述。実装を直せば記述が正になるため concepts の修正は不要の見込み (蒸留時に確認)
- CellStyle 14 項目は両 OS とも全件適用済み。MAUI facade から native に届かない BindableProperty は無し
- 判定は grep と View 側の消費箇所を 1 段追う方法による。全プロパティの描画を実機で確認したわけではない

### Android の現状 (実装の前提)

- RecyclerView は `KsSettingsView.kt:76` でホストの Context そのままから生成されており、同梱テーマのラップ (`ui/KsThemedContext.kt` の `ksThemedContext()`) を通っていない。テーマに style を足すだけでは効かず、生成側の Context 変更が要る
- `res/values/themes.xml` の `Theme.KsSettingsView.Internal` は中身が空。minSdk は 29

## ADR 候補 (作成済み: なし / 未起票: なし)

該当なし (既存の公開契約どおりに実装を合わせる修正で、覆すコストの高い判断を含まない)。

## 未決の論点

- 探索済み (2026-09-17)
- RecyclerView を同梱テーマの Context から生成することで、一覧自身の他の見た目 (overscroll の色、edge effect 等) が変わらないかは実装時に実機で確認する
- iOS の Header / Footer 背景色は、任意 View を置いた Header / Footer (accessory view) にも当てるか、テキストの Header / Footer だけかを Android の挙動に合わせて実装時に確認する

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: M / domain: cross (2026-09-17 ユーザー確定。当初の S から引き上げ)

- 当初は S / cross で着手した (スクロールバーの反映は、公開・文書化済みの契約どおりに動かす不具合修正で設計判断が無いため)
- 実装フェーズ中のスコープ変更 (Header / Footer 背景色の既定値を透明にする) で公開契約の既定値変更が入ったため引き上げ。複数能力 (スタイルの共通契約・iOS・Android) にまたがるので L を推奨したが、既定色 2 つの変更で大掛かりではないとのオーナー判断で M に確定。design.md は作らず、設計判断は proposal と ADR に寄せる
- domain は cross (iOS / Android 両方の実装スキルを解決させる)
- `ui/` は省略の見込み (OS 標準のスクロールバーと色の適用のみ。提案化で最終判断)
- 完了判定は `kasane/handbook/cross/runtime-behavior-verification.md` に従い実機で確認する
