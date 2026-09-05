---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - add-sample-dark-mode-toggle (samples-android spec の Scenario「ダークを選ぶとライブラリ既定色の画面が暗く描画される」の THEN が、concepts `core/styling/style-resolution.md` の「Android のライト / ダークは同梱 DayNight テーマのため端末の夜間モードと uiMode 制御で決まる」を前提に書かれた。実装後の Emulator 確認で、Android の Theme 既定色 [`android/kssettingsview/.../ui/Theme.kt` の DEFAULT_BACKGROUND_COLOR / cellBackgroundColor / DEFAULT_SEPARATOR_COLOR / DEFAULT_CELL_DESCRIPTION_COLOR] が固定ライト値で、`EffectiveStyle.kt` の title 既定だけが `textColorPrimary` から解決される = 夜間では白地に淡色 title になることが露呈。探索・提案・spec-review [自己 + 相方 second-opinion-spec-001] のいずれも既定色の実体を読んでいない。exploration.md は「iOS のライブラリ既定色はシステム色で自動追随する」と書いていたが、iOS 面の検証で `ios/Sources/KsSettingsViewUI/Theme.swift` の `defaultBackgroundColor` / `cellBackgroundColor` 既定 / `defaultSeparatorColor` / `defaultHeaderTextColor` も固定 RGB で、dynamic なのは title `.label` と description `.secondaryLabel` だけと判明 = iOS も白地に白文字。オーケストレーターも当初「iOS は成立する」と誤って前提化し、Android 限定の裁定を先に取ってしまった)
---

## ルール文

Scenario の THEN が「ライブラリ既定の見た目が外観 (夜間モード等) に追随する」のように**本体の既定挙動**を観測するとき、concepts / ADR の機構の記述 (「同梱テーマは DayNight」「ダークは夜間モードで決まる」) をそのまま前提にせず、Scenario が観測する具体の値 (既定の背景色・Cell 背景・separator・文字色の各既定定数と解決関数) を本体ソースで 1 つずつ読み、機構が実際にその値に届いているかを確認してから spec に載せる。cross 変更で Scenario 文言を platform 間で共用するときは、platform ごとに同じ確認を行う (片方の platform で成立する文言が他方で成立するとは限らない)。機構の記述が値に届いていない場合は、spec に載せる前に concepts の記述の乖離として起票する。

## 経緯

- 2026-09-05 add-sample-dark-mode-toggle: 実装ワーカー (Android 面) の検証 5.2 で isVisible デモと Section 装飾デモの箱がダークでも白のままと判明し、停止報告としてオーナー裁定に上がった。concepts の記述は「テーマ (chrome・選択面・title 既定) が夜間に従う」点では正しいが、Theme の既定色定数には届いておらず、記述の主語と Scenario の対象がずれていた。同日、iOS 面の検証で iOS も同症状と判明 (探索時の「iOS はシステム色で追随」も既定定数を読まずに書かれていた)。platform を跨いで同じ前提を使うときは platform ごとに実体を読む、が本ルールの追加根拠。
