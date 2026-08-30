---
scope: test
kind: pain
severity: normal
count: 1
first-seen: 2026-08-11
last-seen: 2026-08-11
evidence:
  - align-view-accessory-header-height (実装ワーカーが自己検出。素の View を probe にした「内容なり高さ」テストが fill 無効化ミューテーションでも落ちず検出力ゼロだった)
---

## ルール文

Android で「内容なり (WRAP_CONTENT) の高さになる」ことを検証するテストの probe に素の `View` を使わない — `View.getDefaultSize` は AT_MOST 制約で上限いっぱいを返すため、fill 系の欠陥を検出できない。最小高さを尊重するレイアウト (`FrameLayout` 等) か自前の onMeasure を持つ view を probe にし、検出力はミューテーションで実測する。

## 経緯

- 2026-08-11 align-view-accessory-header-height: hosted view の占有テストで `View(minimumHeight=20dp)` を probe にしたところ、`applyHostedViewFill` を無効化するミューテーションでもテストが通過 (getDefaultSize が AT_MOST 上限を返すため)。FrameLayout に差し替えて検出力を回復した (code-review L-001 のミューテーション実測が穴を露見させた)
