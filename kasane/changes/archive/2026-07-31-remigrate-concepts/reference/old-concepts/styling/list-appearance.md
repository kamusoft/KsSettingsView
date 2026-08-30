---
type: concept
title: 設定リストの外観と補助領域
description: Classic・Modernの視覚モードとSection・Root Header／Footerの配置原則
tags: [styling, list, section, accessory]
timestamp: 2026-07-18
---

## 視覚モード

設定リストは、フラットな罫線を中心とするClassicと、角丸のまとまりでSectionを示すModernを提供する。

視覚モードの切替はSectionの装飾と区切り方だけを変更し、設定モデル、安定ID、Cell Rendererの登録を変えない。同じ表示内容を維持したまま外観を切り替える。

## HeaderとFooter

SectionとRootのHeader / Footerはリスト内容と共にスクロールし、画面端へ固定しない。空の補助領域は生成しない。

Headerは後続Cell側、Footerは先行Cell側へ近付け、補助情報とCell群の意味的なまとまりを示す。Root装飾とSection装飾のモデル境界は[設定ツリーと装飾](core-model/settings-tree.md)に従う。

## iOS互換規則

iOS Footerの既定文字色はAiForms互換の固定グレーを維持し、system appearanceへ自動追従しない。この互換規則は、descriptionなどsystem appearanceへ追従する意味色とは別に扱う。

