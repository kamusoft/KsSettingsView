# セカンドオピニオン: add-maui-modern-style (spec-001)
**相方**: codex / **日付**: 2026-08-20 / **対象**: 提案一式 (proposal.md + specs/ 3 capability + tasks.md)
---
# スペックレビュー結果: add-maui-modern-style

**日付**: 2026-08-20  
**判定**: **NEEDS_DISCUSSION**

## サマリー

style の初期適用・Handler 再接続時の復元契約が、既存の gateway／Host lifecycle と整合していません。このままでは `ListStyle="Modern"` の初回表示または再接続後が Classic になる実装が成立します。

加えて、Native サンプルとの初期状態の矛盾と、非有限数を無検証で輸送することによる Android 描画時例外の可能性があります。実装前に仕様上の選択が必要です。

指摘件数: Critical 0 / Major 3 / Minor 2 / Suggestion 1

## 指摘事項

### [🟠 Major] style の pre-host 適用・再接続時復元を実現する所有者とタイミングが未定義

**該当箇所**: [`kasane/changes/add-maui-modern-style/specs/maui-bridge/spec.md:38`](kasane/changes/add-maui-modern-style/specs/maui-bridge/spec.md:38)、[`kasane/changes/add-maui-modern-style/tasks.md:13`](kasane/changes/add-maui-modern-style/tasks.md:13)

**問題点**: spec は facade controller が style を保持し、gateway 接続時に再送するとしています。しかし既存実装では:

- `ConnectGateway()` は Host 生成より前に呼ばれるため、初回 `SetStyle` 時点では Native Host がまだ存在しません。
- Handler 再接続時も gateway は同じインスタンスを再利用し、`KsSettingsController.Connect()` は再実行されません。
- `releaseHost()` は Host を破棄し、新しい Host は既定 Classic で生成されます。
- 再接続 Scenario は「gateway が Modern 操作を受け取る」と要求しますが、既存接続モデル上は gateway の再接続自体が発生しません。

根拠は [`SettingsView.cs:810`](maui/KsSettingsView.Maui/SettingsView.cs:810)、[`Android/SettingsViewHandler.cs:21`](maui/KsSettingsView.Maui/Platforms/Android/SettingsViewHandler.cs:21)、[`HandlerTests.cs:45`](maui/KsSettingsView.Maui.Tests/HandlerTests.cs:45) です。

**推奨修正**: 次のどちらを契約として選び、Requirement・Scenario・tasks を揃えてください。

- Bridge が現在 style を Host 外で保持し、`makeHost*()` が新規 Host に適用する。この場合、再接続 Scenario は「gateway が再度操作を受ける」ではなく「再生成 Host が Modern で生成される」とする。
- facade が Host 生成ごとに明示的に再送する。どの lifecycle hook で、Host 生成の前後どちらに送るかまで定める。

あわせて「Host 生成前の `SetStyle`」「release → makeHost 後の復元」を両 OS の Bridge テスト対象にしてください。

### [🟠 Major] Native サンプルとの完全一致要求と Scenario の初期 style が矛盾している

**該当箇所**: [`kasane/changes/add-maui-modern-style/specs/samples-maui/spec.md:7`](kasane/changes/add-maui-modern-style/specs/samples-maui/spec.md:7)、[`kasane/changes/add-maui-modern-style/specs/samples-maui/spec.md:15`](kasane/changes/add-maui-modern-style/specs/samples-maui/spec.md:15)

**問題点**: Requirement は Native `SectionDecorationDemo` と画面構成・初期値を一致させることを要求していますが、Scenario はデモページの初期状態を Classic としています。実際の Native サンプルは iOS・Android ともに Modern が初期値です（[`SectionDecorationDemoView.swift:28`](samples/ios/KsSettingsViewSample/SectionDecorationDemoView.swift:28)、[`SectionDecorationDemoScreen.kt:36`](samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SectionDecorationDemoScreen.kt:36)）。

実装者は Native parity と Scenario のいずれかを破らざるを得ません。

**推奨修正**: Native 実装を正とするなら Scenario の GIVEN を Modern に変更し、WHEN を Classic 選択、THEN を Classic 表示、さらに Modern へ戻せることとしてください。Classic 初期表示を採るなら、Native サンプルも変更対象に含める必要があり、現在の Non-Goals／スコープと衝突します。

### [🟠 Major] 「無検証で素通し」が非有限数を含み、Android 描画時例外に到達し得る

**該当箇所**: [`kasane/changes/add-maui-modern-style/specs/maui-core/spec.md:29`](kasane/changes/add-maui-modern-style/specs/maui-core/spec.md:29)、[`kasane/changes/add-maui-modern-style/specs/maui-core/spec.md:48`](kasane/changes/add-maui-modern-style/specs/maui-core/spec.md:48)

**問題点**: `Thickness` と `double` は `NaN`・正負の無限大を保持できますが、spec は値検証を行わず Native へ素通しするとだけ定め、Scenario は負値と過大 radius しか扱っていません。

Android の既存正規化は `max(0, value)` であり、`NaN` は有限値になりません。その後 margin は `roundToInt()` されるため例外へ到達し得ます（[`SectionBoxMetrics.kt:94`](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionBoxMetrics.kt:94)、[`ModernSectionDecoration.kt:76`](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ModernSectionDecoration.kt:76)）。iOS でも非有限の geometry がそのまま残ります。

**推奨修正**: 非有限数の扱いを明示してください。選択肢は以下です。

- facade で有限値のみ許可し、非有限数を拒否する。
- facade または Native で 0／未指定へ正規化する。
- Native の4属性契約を拡張し、安全な正規化を両 OS に実装する。

これは「facade は検証しない」「Native 側は変更しない」という現在の前提を同時には維持できないため、設計判断が必要です。決定後、4つの寸法フィールドについて `NaN`・`±Infinity` の Scenario を追加してください。

### [🟡 Minor] margin DTO の部分的 null 状態が解決不能なまま残っている

**該当箇所**: [`kasane/changes/add-maui-modern-style/specs/maui-bridge/spec.md:5`](kasane/changes/add-maui-modern-style/specs/maui-bridge/spec.md:5)

**問題点**: facade が生成する値は all-or-none とされていますが、`KsBridgeTheme` 上では4成分が個別の nullable フィールドになります。Native の `sectionMargin` は単一の optional 値なので、「top・leading・bottom は設定、trailing だけ null」のような DTO をどう解決するか決まっていません。Scenario も「全設定」と「全 null」だけです。

**推奨修正**: 部分 null を不正状態として扱うか、1成分でも null なら margin 全体を未指定にするか、欠落成分を何らかの値で補うかを明記し、両 OS で同じ結果になる Scenario を追加してください。

### [🟡 Minor] 未知の style 序数と不正な C# enum 値の扱いが未定義

**該当箇所**: [`kasane/changes/add-maui-modern-style/specs/maui-bridge/spec.md:24`](kasane/changes/add-maui-modern-style/specs/maui-bridge/spec.md:24)

**問題点**: 0 と 1 の対応だけが定義されていますが、Bridge API には任意の `int` を渡せ、C# enum にも `(SettingsViewStyle)2` のような値を設定できます。未知値を Classic に落とす、無視する、例外にする、現在値を維持する、のいずれも実装可能です。

**推奨修正**: facade と両 Bridge の未知値ポリシーを定め、未知値を入力する Scenario を追加してください。非 nullable・既定 Classic という契約に寄せるなら、Classic へ正規化するのが一案です。

### [🔵 Suggestion] 視覚照合タスクを判定可能なチェックリストにする

**該当箇所**: [`kasane/changes/add-maui-modern-style/tasks.md:30`](kasane/changes/add-maui-modern-style/tasks.md:30)

**問題点**: 「見え方を突き合わせる」だけでは合否が担当者の印象に依存します。今回は UI mock がないため、Native 実装を正とする比較条件が特に重要です。

**推奨修正**: OS × style × preset の確認表を設け、少なくとも以下を明示してください。

- 初期選択と切替後の style
- Header／Footer が箱外であること
- 単一 Cell Section に separator がないこと
- 中間 separator が左右対称であること
- margin・radius・border preset の反映
- Classic では水平 margin が適用されないこと
- 比較用スクリーンショットの保存有無

## アクションプラン

1. style の保持場所と Host 生成前／再生成時の適用順を決定する。
2. `NaN`・無限大の契約を決定し、Non-Goals と整合させる。
3. サンプルの初期 style を Native parity と一致させる。
4. 部分 null margin と未知 style 序数のフォールバックを定義する。
5. tasks の Bridge lifecycle テストと視覚照合基準を具体化する。

依頼どおり静的レビューのみ実施し、ビルド・テストおよび `review-*.md` の作成は行っていません。

## 突き合わせ結果 (ホスト側判定: 2026-08-20)

全6件をコードで裏取りのうえ判定。採用 6 / 降格 0 / 未解決 0。

| 指摘 | 採否 | 裏取りと反映 |
|---|---|---|
| Major 1: style の pre-host 適用・再接続復元の未定義 | **採用** (ホスト側の見逃し) | ConnectGateway の「Host 解放をまたいで gateway を作り直さない」注記と、Theme のみ Store 復元される構造を確認。契約を「Bridge が style を Host 外で保持し makeHost で適用」(相方推奨の第1案) に置き換え — maui-bridge spec の Requirement を書き換え、lifecycle テスト (tasks 3.5) を追加 |
| Major 2: サンプル初期 style の parity 矛盾 | **採用** | 両 OS の native デモの初期値が Modern であることを実コードで確認。samples-maui の Scenario を「初期 Modern → Classic 切替」に修正 |
| Major 3: 非有限数 (NaN・±∞) の素通しで Android 描画時例外 | **採用** (設計判断はオーナー) | NaN → roundToInt() の例外経路を確認。オーナー判断で「Native の描画時正規化を非有限→0 へ拡張」(案 A) を採用 — settings-view-ios-ui / settings-view-android-ui のデルタを追加し、MAUI の素通し契約は維持 |
| Minor 1: margin DTO の部分 null が解決不能 | **採用** | 「1成分でも null なら margin 全体を未指定として解決 (両 OS 同一)」を明記し Scenario 追加 |
| Minor 2: 未知の style 序数・不正 enum 値が未定義 | **採用** | 「定義域外の序数は Classic へ正規化」を明記し Scenario 追加。facade は bridge の正規化に委ねる旨を追記 |
| Suggestion: 視覚照合の判定可能化 | **採用** | tasks 5.1 を OS × style × preset の確認項目リスト + スクリーンショット保存に具体化 |

相方判定 NEEDS_DISCUSSION の論点 (style lifecycle・NaN) はいずれもオーナー判断で決着済み。
