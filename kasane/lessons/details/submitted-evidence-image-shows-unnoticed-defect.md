# 経緯: 提出した証跡画像に気づかない欠陥が写っている (impl L-008)

inbox パターンとして pain 3 件で閾値到達し、2026-09-06 にオーナー承認で `impl.md` L-008 へ昇格した。

## 観測 (3 change)

- android-hinttext-position (review-001 Critical「行右端の余白 16dp が消える」の実害が、実装者が提出した証跡 android-after.png の ButtonCell 行に既に写っていた。テストも実装者の目視も検出せず、レビュアーの画像照合で発覚)
- relax-android-host-prerequisites (MAUI サンプルのテーマ復帰で NavigationBar が暗色地×黒文字の低コントラストに劣化。verify-maui-01 以降の証跡に写っていたが、検証・レビューとも Cell 部分だけを見ておりオーナー指摘で発覚。ライブラリ UI 外のアプリクロームも証跡確認の対象に含めるべき観測)
- fix-default-colors-dark-appearance (Android の live 切替の証跡 `ui/verification/android-native-input-cells-live-dark-after.png` に、EntryCell の placeholder が暗い地に暗い文字 [#252220 / #2A2620、コントラスト比ほぼ 1:1] で判読できない実欠陥が写っていた。撮影・提出した側は list 下地と Cell 背景の追随だけを見ており気づかず、review-002 が画素サンプリングで Major として検出 → 行の ViewHolder が同梱テーマ属性を現在の外観から引き直す修正に至った)

## 経緯

- 2026-08-24 android-hinttext-position: root padding 廃止の副作用 (accessoryHolder GONE 行の右余白消失) が提出済み証跡 `android-after.png` に写っていたが、実装者は気づかず提出。review-001 が画像と実測プローブで Critical として検出した。
- 2026-08-28 relax-android-host-prerequisites: MAUI サンプルのテーマをテンプレート既定へ戻した副作用で NavigationBar がフォールバック配色 (暗スレート地に黒タイトル) となり判読困難に。証跡 (verify-maui-01 以降) に一貫して写っていたが、撮影ワーカー・レビューサイクルとも検証対象 (ライブラリの Cell / 選択面) だけを照合しており、誰も指摘しないままオーナーが発見した。証跡の目視確認は変更対象の外側 (アプリクローム・システムバー境界) も含める。
- 2026-09-06 fix-default-colors-dark-appearance: 外観 live 切替の証跡画像に placeholder の判読不能 (旧外観の hint 色が残る) が写っていたが、提出側は照合対象のロール (下地・Cell 背景) だけを見て提出。review-002 が画像を画素実測して Major。「証跡は最初のレビュー機会」の 3 例目で、今回も検出は照合対象外の要素 (入力欄の placeholder) にあった。

観測の共通構造: 3 例とも検出はレビュー (画像の画素実測・目視) かオーナーで、提出者は照合対象のロール・部位だけを見ていた。欠陥は毎回「照合対象の外側」(余白・アプリのクローム・入力欄の placeholder) に写る。
