---
scope: ui-impl
kind: success
severity: normal
count: 2
first-seen: 2026-08-02
last-seen: 2026-08-04
evidence:
  - fix-android-chevron-vertical-centering (includeFontPadding 仮説を実装前に画素測定で棄却し、真因の chevron drawable 非対称を発見)
  - fix-ios-separator-color-not-applied (貼られたスクショのピクセル実測でセパレータが UIKit 既定グレー #C6C6C8 のままであることを確定してからコード調査へ進み、修正後も同一手法で #E6DAB9 一致を定量確認)
---

## ルール文

UI の位置ズレ・整列不良・配色の不具合修正では、原因仮説を実装する前に実機スクリーンショットの画素測定 (要素ごとの ink 範囲と中心座標、色は RGB 実値) で裏取りする。もっともらしい定番仮説でも測定で棄却されることがあり、測定は複合原因 (複数要素が逆方向にズレる等) の切り分けにも効く。修正後も同一手法で before/after を定量比較し、効果を証跡化する。

## 経緯

- 2026-08-04 fix-ios-separator-color-not-applied: ユーザーが貼った iOS サンプルのスクショを目視で「グレーに見える」で済ませず、トランスクリプトから抽出してセパレータ行を走査 → RGB(198,198,200) = UIKit 既定色と実測してから ksn-scout のコード調査に進んだ。調査は「消費コード自体が無い」という Android と異なる原因型を特定。修正後もシミュレータ撮影のピクセル走査で全セパレータが RGB(230,218,185) = `#E6DAB9` ぴったりであることを確認し、文字アンチエイリアスの誤検出も水平連続性で除外した。色系の不具合にも同パターンが有効だった2例目。
- 2026-08-02 fix-android-chevron-vertical-centering: 「Android の日本語テキストが沈む」という定番の includeFontPadding 仮説を立てたが、実装前に Pixel 6a でスクショの画素測定を行ったところグリフ位置は 1px も動かず棄却。同じ測定で「テキスト +4px 沈み + chevron -2px 浮き (drawable パスの viewport 非対称)」の複合原因を特定し、chevron パス補正 + contentRow -1dp の最小修正で相対ズレ 6px → 0.5px を達成。修正ごとに同一手法で before/after を定量比較し、効果を証跡化した。
