---
scope: ui-impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-28
last-seen: 2026-08-28
evidence:
  - restore-pickercell-object-items (samples 実装ワーカーが「MAUI 選択面の破棄ボタンが未ローカライズ (native は「キャンセル」、MAUI は「Cancel」)」をスコープ外の課題として報告し、オーケストレーターが起票推奨で完了報告に載せた。実際は日本語ロケールの Android エミュレータと英語ロケールの iOS Simulator を比較しており、オーナーの実機確認 (日本語 Pixel 6a の MAUI sample で「キャンセル」表示) で誤検出と判明。文言はライブラリ自前ではなく OS リソース (android.R.string.cancel / UIBarButtonItem systemItem) 解決で、ロケールを揃えれば差は無かった)
---

## ルール文

システム由来の表示文言 (OS リソース・system button・日付/数値書式) の platform 間・ホスト間比較で「未ローカライズ」「文言差」を所見として報告するときは、比較した両環境のロケール (端末言語設定・Simulator/エミュレータの言語) が同一であることを先に確認し、報告に両環境のロケールを明記する。ロケール未統制の比較から得た文言差は所見にしない — 文言の出どころ (自前リソースか OS 委譲か) をコードで特定してから報告する。

## 経緯

- 2026-08-28 restore-pickercell-object-items: samples 実装ワーカーが視覚照合中に MAUI sample の選択面キャンセルボタンが「Cancel」表示である点を native Android sample の「キャンセル」と対比してスコープ外課題として報告。オーケストレーターは裏取りせず完了報告で起票推奨に載せ、オーナーの「Pixel 6a では普通にキャンセルだった」で誤検出と判明した。実装は両 OS とも OS のローカライズ機構に委ねており (SheetChrome.kt の android.R.string.cancel / PickerListViewController.swift の systemItem: .cancel)、差の正体は比較環境のロケール差 (ja Android エミュレータ vs en iOS Simulator) だった。
