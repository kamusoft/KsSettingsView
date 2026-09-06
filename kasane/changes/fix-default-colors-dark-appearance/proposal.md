# Proposal: fix-default-colors-dark-appearance

## Why

Theme を渡さずにダーク外観 (iOS のダーク / Android の夜間モード) で使うと、3 platform とも「白地に淡色の文字」になり判読できない。既定色のうち外観に追随するのは Cell title (iOS `.label` / Android 同梱テーマの `textColorPrimary`) と iOS の description だけで、list 下地・Cell 背景・separator・Header / Footer・disabled・selected はライト前提の固定 RGB のままだからである (2026-09-05 に 4 実行面で実測、[exploration.md](exploration.md))。

根本原因は、Theme の色が (iOS の `UIColor` を除き) 外観を持たない静的な値しか表せないこと。Compose の `Color` は不変の値で、外観に応じて変わる色は存在しない。この制約は既定色だけでなく、利用者が自分の色を渡すときのダーク対応にも同じ形で効く。そこで本 change は「既定色の追随」に留めず、**Theme のダーク対応の仕組み全体** (既定色 + 利用者が渡す Theme の追随手段) を対象にする (探索でのオーナー裁定)。設計の正は [core/ADR-0030](../../decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md) (proposed)。

## What Changes

- **既定色を全部、外観に追随させる。** ライト側は現行の固定値をそのまま使い (既存利用者のライト時の見た目は変えない)、ダーク側の既定値をライブラリが新たに持つ。dark の生値は iOS の dark システムパレットを不透明に近似した値を 3 platform で同じ値として置く (値の正は ui/ の承認モックの色ロール対応表)
- **未指定の色だけを描画時に現在の外観の既定へ解決する。ライブラリは明示指定された値を別の既定値へ置き換えない** (iOS で利用者が dynamic な `UIColor` を渡せばその色自身が外観に解決される。Android の固定 `Color` は不変で、利用者が外観に応じて Theme を作り直して渡せば新しい値になる。MAUI は `AppThemeBinding` が外観変更時に新しい明示値を供給する)。 Theme の型・Store の `theme` 経路・MAUI facade の契約は変えず、対の型やコールバックは作らない。利用者が両外観の色を自分で決める手段は各 platform の慣用に乗せる (iOS: dynamic な `UIColor` / Android: 構築時に `isSystemInDarkTheme()` または Configuration で選ぶ / MAUI: `AppThemeBinding`)
- **iOS**: 既定色の `public static let` を `UIColor(dynamicProvider:)` の light / dark の対にする (型・公開面は不変)。CGColor を layer に置く Modern の Section 装飾の枠線は、外観の trait 変更時に再解決する
- **Android**: Theme と CellStyle の色フィールドを `Color?` から `Color` (未指定 = `Color.Unspecified`) に揃える (色以外の nullable フィールドは対象外)。ライブラリ所有の light / dark 2 セットを `KsSettingsViewDefaults` (仮称) の factory として公開し、`DEFAULT_*` の public 定数は internal 化する。Compose 入口の `theme` 既定値式は `@Composable` な factory で外観に応じた側を選ぶ。View は Theme を受け取った時点と uiMode の Configuration 変更時に未指定を現在の外観の既定へ解決し、既存の Theme 更新経路で再適用する。Cell title の既定は同梱テーマの `textColorPrimary` の動的解決をやめ、2 セットの値にする。`values-night` の色リソースは置かない
- **MAUI**: facade・bridge DTO の wire 形式は変えない (未指定を null で native に渡す現行契約のまま native の既定に追随する)。Android bridge の DTO → Theme 変換は `Unspecified` へ写す形に追随する
- **samples**: Android サンプルの `SampleTheme` が `null` を渡している箇所を `Unspecified` に追随させる。Theme を渡さない画面 (Store / DSL / 共通フィールド統合 / isVisible / Section 装飾デモの下地) が 4 実行面でダーク描画になることを視覚照合する (起票元 change の合意済み差分の解消)
- **tests**: 3 platform の dark 既定値が同値であることを定数比較で検証する (iOS は dynamic 色を dark trait で解決して比較)。Android の `Unspecified` 解決、uiMode 変更時の再解決、iOS の枠線再解決をテストする
- 影響能力: settings-view-ios-ui / settings-view-android-ui / maui-bridge (Android 側の変換のみ) / samples-ios / samples-android / samples-maui

## Non-Goals

- **ホストの `MaterialTheme` (Dynamic Color を含む) への追随** — android/ADR-0020 (視覚隔離) の改訂を伴う別の設計判断。将来 opt-in の adapter として検討する (ADR-0030 Revisit When)
- **Android / MAUI で明示指定した色を表示中の外観変更に追随させる仕組み (対の型・コールバック)** — 探索で却下 (ADR-0030 Alternatives)。利用者は構築時の分岐 / `AppThemeBinding` で対応する
- **Theme の全部入り data class の構造見直し (Compose の grab-bag style 非推奨への対応)** — core/ADR-0009 の領分で本 change の範囲外 (並走調査の指摘として exploration.md に記録)
- **色以外の nullable フィールド (`TextStyle?` / `Dp?` / `PaddingValues?`) の `Unspecified` 化** — Compose 自身も `TextStyle` は既定値式で扱うため慣習上の不整合がなく、今回触らない
- **`skills/` (利用者向け Agent Skills) と README の追従** — `DEFAULT_*` 定数表と `?: Theme.DEFAULT_CELL_TITLE_COLOR` の例が古くなるが、追従は docs-refresh スキルの責務でユーザーの明示依頼により別途行う (AGENTS.md の運用)
- **MAUI サンプルの Android `MainActivity` が uiMode 変更で自前再生成している回避策の撤去** — MAUI 自身のテーマ状態 (`RequestedTheme`) 更新のための処置であり、本 change の View 側再解決とは目的が異なる。撤去できるかは実装後の観察で判断し、必要なら別途起票する

## Impact

- **破壊的変更 (Android)**: Theme / CellStyle の色フィールドの型が `Color?` → `Color` になる。`null` を明示的に渡す呼び出しと、値を読み出して `?:` で補っているコード (bridge の変換・samples・tests・skills の例) に追随作業が発生する。`Theme.DEFAULT_*` の色定数が internal になり、外部からの参照は `KsSettingsViewDefaults` の factory 経由になる。ライブラリは未配信で外部利用者がおらず、互換のための旧 API 凍結はしない (ADR-0030 前提)
- **等価性の意味変化 (iOS / Android 共通)**: 既定と同じ生値を固定色で明示した Theme (例: Android の `Theme(cellBackgroundColor = Color.White)`、iOS の `Theme(cellBackgroundColor: .white)`) は、これまで既定 Theme と等価だったが、本 change 後は等価でなくなり、その色はダークでも変わらない (明示指定の扱い)。既存テストの一部がこの前提で書かれているため追随する
- **iOS**: 公開型・シグネチャの変更なし。既定色の値が dynamic になるため、既定値と生の固定 RGB を `isEqual` で比べているテストは dark trait での解決を挟む形に書き換える
- **MAUI**: facade の公開面・wire 形式の変更なし。ダーク端末で Theme プロパティを設定しない画面の見た目が変わる (判読不能 → 判読可能)
- **利用者可視の変更**: ダーク外観での既定の見た目 (3 platform)。ライト外観は変わらない
- **リスク**: Android の構造更新経路 (Full diff / Section 置換 / 可視性変更) が現行では Theme を利用者側フィールドへも書き戻すため、解決済み Theme を渡すと以後の外観変更で未指定色が明示扱いになる。design Decision 6 で構造更新は利用者 Theme を変えない契約にし、回帰 Scenario を持つ
- **リスク**: Android で解決済み値を持つキャッシュ (adapter の EffectiveStyle・選択面・dialog の色) の無効化漏れがあると、uiMode 変更後に古い外観の値が混在する。tasks で消費者を列挙して 1 箇所の解決点に寄せる
- **リスク**: iOS の `UIColor` の等価性 (`isEqual`) が dynamic 色で期待どおり働くかは実装時の確認事項 (Theme の `==` は各フィールドの `isEqual` で判定している)
- **ゲート**: MAUI の `AppThemeBinding` が facade の色プロパティ経由で Theme 再適用まで届くかは未検証。tasks 0.1 の spike を実装の先頭で行い、届かなければ本提案を承認済みとして進めず探索へ戻す (「利用者 Theme のダーク対応全体」へ広げた根拠がこの経路に依存するため)
- 長命層: core/styling/style-resolution.md・list-appearance.md、android / ios の native-host、maui-styling の記述が変わる (蒸留で追随。範囲は exploration.md 論点 8)

## 級: L

複数能力横断 (iOS UI / Android UI / bridge / samples 3 面)、Android の公開 API の破壊的変更、dark 既定値が製品契約に加わる不可逆性、UI の承認ゲートあり (2026-09-05 オーナー確定)。

domain: core
