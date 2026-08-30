# phase-9-accessory-visibility

Section の Header / Footer 表示トグル (IsHeaderVisible / IsFooterVisible) を Native 起点で設計して MAUI まで通す (AiForms の FooterVisible 相当の再設計)。

## 論点

(なし — 全論点解消済み)

## 決定事項

- **論点1: IsHeaderVisible / IsFooterVisible の契約 (2026-08-19)**: bool・既定 `true`・表示 = トグル && 内容あり の AND 合成。トグルは「内容があっても隠す」専用で、「空の Header / Footer に表示領域を割り当てない」保証は維持する。view accessory は常に「内容あり」(空の view でも領域は生成される。spacer 用途は `HeightRequest` / `Section.HeaderHeight` で高さを確保する)。→ [core/ADR-0023](../../../../decisions/core/0023-accessory-visibility-and-composition.md) (proposed)
- **論点2: Native への追加形状 (2026-08-19)**: iOS / Android の `Section` に `isHeaderVisible` / `isFooterVisible` (既定 `true`) をフィールド追加し、値等価性に参加させる。更新経路は新設せず `isVisible` / `headerHeight` と同じ **`replaceSection` 相乗り** (専用 Diff 操作・Store 操作は作らない)。判定の織り込み先は iOS の `supplementaryModes` / `makeHeaderBoundaryItem` / `shouldShowFooter` 3箇所と Android の `flatten` 1箇所 (論点4の対称化と同じ場所)。→ core/ADR-0023 に輸送形として統合
- **論点3: Bridge / MAUI への公開形 (2026-08-19)**: Bridge は `KsBridgeSection` (iOS/Android) へ `isHeaderVisible` / `isFooterVisible` (既定 `true`) のフィールド追加のみ (専用 bridge 操作なし)。MAUI facade は `Section` に BindableProperty 2つを追加し、PropertyChanged は `IsVisible` / `HeaderHeight` と同じ ReplaceSection バッチ分岐へ相乗り。公開名は **`IsHeaderVisible` / `IsFooterVisible`** (.NET 慣例) — Native 起点の新概念であり maui/ADR-0008 の AiForms 命名踏襲の対象外 (オーナー裁定。phase-7/8/10 にも効く前例)。→ core/ADR-0023 に統合
- **論点4: 自動判定の非対称の対称化 (2026-08-19)**: 「内容の不在 = nil または空 text」を header / footer 共通・両 OS 共通に統一 (① Android の空文字 footer を非表示へ揃える)。高さ解決は存在判定の後とし、`Section.headerHeight` / `Theme.headerHeight` は存在する Header の高さを決めるだけで存在を作らない (② iOS の header 不在 + 高さ指定の空領域生成を廃止。`Section.headerHeight` への一般化は spec-review 指摘を受けたオーナー裁定 2026-08-19 — 逆契約を固定していた既存 iOS テストは反転する)。両件とも本フェーズの change に含める。→ core/ADR-0023 に統合

## TODO

- [x] 論点の解消 (2026-08-19 全4論点確定)
- [x] core/ADR-0023 (proposed) の確定 (蒸留時) — 2026-08-19 accepted へ昇格
- [x] ksn-propose で変更提案を起こす — [changes/archive/2026-08-19-add-accessory-visibility-toggle](../../../../changes/archive/2026-08-19-add-accessory-visibility-toggle/proposal.md) (M 級)

## 実装結果 (2026-08-19 反映)

- 全層 (iOS / Android native・Bridge・MAUI facade・宣言 DSL・samples) へ実装済み。review-001 APPROVED / verify-001 VALID (Requirement 15 / Scenario 40、未記録乖離 0)。視覚証跡は 4 platform 構成 (iOS / Android / MAUI net10.0-android / net10.0-ios) の 4 組み合わせ + 対称化 3 件
- deviation 2 件 (SwiftUI `copyWith` の `isVisible` 保持を同種コピー漏れ解消として追加 / 対称化証跡のサンプル一時改変・完全 revert) — [deviation.md](../../../../changes/archive/2026-08-19-add-accessory-visibility-toggle/deviation.md)
- net10.0-ios のパッケージング不能は環境要因ではなく workloadVersion ピンの継承が真因と判明し、リポジトリ直下の `global.json` (10.0.300 / 10.0.300.3) で解消済み (lessons impl L-004 の出典)
- 申し送り (いずれも受け皿確定済み):
  - iOS の実行時未使用 layout helper (`supplementaryModes` / `makeListConfig` / `layoutModesDiffer`) の整理 (review-001 Suggestion) → 独立変更として簡易起票済み: [changes/fix-ios-root-accessory-theme-refresh](../../../../changes/fix-ios-root-accessory-theme-refresh/exploration.md) (2026-08-21 に統合)
  - Android `settingsRoot { section(...) }` ビルダーへのトグル引数追加の要否 (review-001 Suggestion、却下もあり得る) → 独立変更として簡易起票済み: [changes/harden-compose-settingsroot-dsl](../../../../changes/harden-compose-settingsroot-dsl/exploration.md) (2026-08-21 に統合)
  - 公開名の裁定 (Native 起点の新概念は AiForms 命名踏襲の対象外) は phase-7 / 8 / 10 にも効く前例として論点3の決定事項に記録済み (追加の転記不要)
