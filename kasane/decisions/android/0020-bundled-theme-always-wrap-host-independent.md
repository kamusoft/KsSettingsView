---
id: 0020
title: ライブラリ UI は同梱 Material3 派生テーマの常時ラップで生成し、ホストの XML テーマに依存しない
status: accepted
date: 2026-08-27
---

## Context

ホストアプリの XML テーマが `Theme.Material3.*` 派生であることを利用前提にしていた。`MaterialSwitch` / `MaterialCheckBox` は非 Material3 テーマだと初期化時に例外で落ち、`MaterialColors` による色 attr 解決は attr 欠落時に無言で色がずれる (クラッシュと無言劣化の二層)。メインターゲットの Compose 専業アプリは XML テーマが最小限でこの前提を満たせず、MAUI もテンプレート既定 (`Maui.SplashTheme`) のままでは満たせないため、サンプルはテーマ置換で回避していた (オーナー却下済みの形)。

ライブラリ側の構造として、View 生成に使う Context の注入点は「ViewHolder 生成の `parent.context`」と「シート/ダイアログの `views.root.context`」の2系統に集約されている。また色の方針は「テーマ attr 直参照ではなく accent から導出する」(ADR-0017) に既に舵を切っており、ホストテーマを見た目のカスタマイズ手段として推す理由は薄くなっていた。

## Decision

ライブラリが Material3 派生のテーマを res に同梱し、View 生成・シート/ダイアログ表示に使う Context を**ホストテーマに関わらず常に** `ContextThemeWrapper` でラップする。これにより全ホストテーマで挙動を同一にする。

- ホストテーマからの色引き継ぎは行わない。現行で唯一ホストテーマ色を反映していた ButtonCell タイトル色の既定 (`colorPrimary` の動的解決) も廃止し、固定の既定色に統一する
- 見た目のカスタマイズの正はホストの XML テーマではなくライブラリの Theme API (accent 等) とする

> 訂正 (2026-08-27): 起票時の本節には「既定 accent の colorPrimary 追従を維持する」とあったが、提案設計時の調査で現行実装に accent のホスト追従は存在しない (ホスト色の反映は ButtonCell タイトル既定のみ) ことが判明した。オーナー決定により、その ButtonCell の追従も廃止して完全隔離へ統一した。accent 既定のホスト colorPrimary 追従化は将来の変更候補として見送り。

## Alternatives Considered

- **非 Material3 ホストのみ検知して補完ラップ**: ホストの種類で挙動が2系統になりテストも2倍になる。テーマ系譜の検知ロジック自体が脆い。却下。
- **必要 attr だけを定義した最小 ThemeOverlay を常時適用**: Material ウィジェットが要求する attr 群を漏れなく列挙し切る必要があり、漏れ = クラッシュ残存。列挙の保守が永続負担になる。上書きされる attr についてはホストテーマの反映がどのみち失われ、利点が中途半端。却下。
- **`Theme.Material3.*` 必須の前提を維持しドキュメントで案内**: メインターゲットの Compose 専業アプリとテンプレート既定の MAUI アプリが標準構成で使えないままになる。前提解消を必須とするオーナー判定に反し却下。

## Consequences

- 正: 非 Material3 ホストでのクラッシュ (MaterialSwitch / MaterialCheckBox の初期化例外) と色の無言劣化が構造ごと消え、Compose 専業アプリ・テンプレート既定テーマの MAUI アプリで動作する。
- 正: ライブラリ UI の見た目が全ホストで決定的になり、テーマ起因の見た目差の調査・テストのテーマ準備 (ContextThemeWrapper の儀式) が不要になる。
- 正: MAUI サンプルの Material3 テーマ置換 (却下済みの回避策) をテンプレート既定へ戻せる。
- 負: ホストが Material3 テーマ側でカスタムした色 (dynamic color を含む) はライブラリ UI へ自動反映されなくなる。ButtonCell タイトル色の既定もホスト `colorPrimary` 追従から固定既定色に変わる (利用者可視の変更)。色の調整は Theme API での指定が必要。
- 負: 同梱テーマ分の res が増え、Material Components のバージョン更新時に同梱テーマの追従確認という保守項目が加わる。

出典: kasane/changes/relax-android-host-prerequisites/exploration.md (決定事項・検討した選択肢)
