# Deviation: add-cell-types-custom

実装中に合意された、足場アーティファクト (specs / design) からの乖離の記録。
記録済みの乖離は「合意済みの差分」であり、レビュー・verify は違反として扱わない。

## spec からの乖離 (オーナー指示)

- **Requirement「行タップ」— 無効時の視覚表現**: spec では「無効時の見た目の描き分けは利用者責務とする」(テキスト色の disabled 置換等は任意ビューに適用できないため) → **オーナー指示により、両プラットフォームとも `isEnabled = false` のとき content 全体を淡色化する (alpha 0.38)**。

  背景と経緯: 当初 iOS は `.disabled(!isEnabled)` のみで、SwiftUI が**環境値を読む標準コントロールだけ**を自動淡色化していた。Android の `consumePointerInput` はポインタを消費するだけで淡色化しないため、同じ `isEnabled = false` の Slider が iOS では薄く Android では通常色になっていた (2026-08-03 オーナー指示で Android に `Modifier.alpha(0.38f)` を追加)。

  ところが Compose の `Modifier.alpha` は subtree 全体に掛かるのに対し SwiftUI の `.disabled` は標準コントロールにしか効かないため、今度は **`Text` / `Image` が Android だけ薄く iOS は素の色**という逆向きの非対称が生じた (review-002 Major)。**オーナー判断により iOS 側にも content 全体へ opacity 0.38 を適用**し、「content 全体が薄くなる」振る舞いに揃えた。

  副作用として、iOS の SwiftUI 標準コントロールは `.disabled` による淡色化と opacity が重なり**二重に薄くなる**。両プラットフォームで「無効に見える」ことを優先した合意済みの差分。利用者による追加の描き分けが可能である点は変わらない。(2026-08-04)

## design.md からの乖離 (実装上の必然、オーケストレーター合意)

- **Decision 4 (iOS の Renderer 基底)**: design では `UICollectionViewCell` → 実装は `UICollectionViewListCell`。理由: 罫線 (`UIListSeparatorConfiguration`) と `KsCellViewSupport` の各 API (`setRenderState` / `applyEffectiveHeight` / `adjustedLayoutAttributes`) がいずれも `UICollectionViewListCell` を引数に取るため、標準 Cell と同一経路を共有するには list cell が必要。`UICollectionViewListCell` は `UICollectionViewCell` のサブクラスであり Registry の型制約は満たす。`KsListCellBase` は継承していない (Decision 4 代替案 A の不採用は維持)。(2026-08-03)

- **Decision 5 (Android の高さ適用)**: design には記載がないが、実装は行高さを Compose ツリー内 (`Modifier.heightIn(min = )` / `Modifier.height()`) でも解決している。理由: `applyEffectiveHeight` が設定する `View.minimumHeight` は `ComposeView` では効かない (`AbstractComposeView.onMeasure` が内部 composition の測定結果をそのまま `setMeasuredDimension` するため)。実測で「cellHeight=100dp・content 40dp → 40dp」となり高さ解決契約を満たせなかったため追加した。`applyEffectiveHeight` は RecyclerView 側の LayoutParams のために併用している。(2026-08-03)

## アクセシビリティの扱い (オーナー判断)

- **無効時、Android の content は TalkBack の読み上げ対象から外れる**: `isEnabled = false` の操作抑止を `Modifier.clearAndSetSemantics { disabled() }` で実現しているため、content の semantics subtree が置換され、内容が読み上げられなくなる。

  経緯: `semantics(mergeDescendants = true)` + 子孫 action の no-op 上書きでは遮断できないことを実測で確認している (Compose の merged tree は「自身も畳み込みノードである子孫」を独立ノードとして残す仕様で、`clickable` / `Slider` がこれに該当するため親の no-op が子に効かず `OnClick` が発火した)。任意の Compose ツリーに対して「操作だけ無効化して読み上げは残す」機構が Compose に存在しないため、spec の「content 内部の操作も抑止される SHALL」と `cell-visual-states.md` の無効状態契約 (読み上げは残る前提) が二者択一になった。

  **オーナー判断により操作抑止を優先** — 無効なはずのコントロールが accessibility service 経由で動作する誤操作を防ぐことを重視した。iOS の `.disabled(true)` は VoiceOver の読み上げを残すため、**この点だけはプラットフォーム間で非対称のまま残る** (他の既定挙動は本 change ですべて揃えた)。(2026-08-04)

## concepts との緊張関係 (記録)

- **無効時の淡色化手段 (Android)**: `concepts/core/styling/cell-visual-states.md` は無効状態を「text の意味色を disabled 色へ置き換える」と規定し、「行全体の alpha だけで disabled 状態を表現しない」としている。しかし CustomCell の content は任意の Compose ツリーであり、ライブラリ側からテキスト要素を特定して色を置換することはできない。そのため **content に `Modifier.alpha(0.38f)` を適用**する形で淡色化を実現した (行背景と Disclosure Indicator は対象外)。0.38 は `Theme.disabledTextColor` (#999999) が白背景上で持つ濃度に合わせた値。標準 Cell の無効表現とは手段が異なるが、見た目の濃度は揃う。(2026-08-04)

## spec 未規定事項の実装判断

- **content が行の高さに収まらないときの縦位置**: spec・design のいずれにも規定がなく、mock も静止状態しか表さない。実装は **「収まるときは縦中央、収まらないときは上端揃え」** とした (iOS: `CustomCellRowPlacement` の独自 `Layout`)。

  理由: `UIHostingConfiguration` のホスト View は「行の高さを提案 → content がそれより大きい高さを返す → **その高さで組み直して行の中央に置く**」という配置仕様を持つ。行の self-sizing は 1 レイアウトパス遅れるため、既定の中央配置のままだと展開時に content が上下へ均等にはみ出し、行が伸びるにつれ中心が下がる = 「上に飛び出してから落ちてくる」不自然な遷移になる (オーナーが実機で指摘、2026-08-03)。`HStack(alignment: .top)` や `.frame(maxHeight: .infinity, alignment: .top)` では解決しないことを実測で確認済み (`frame` の高さは子の高さより小さくできないため、提案された行高さに切り詰められない)。(2026-08-04)

## 検証範囲の限界 (解消済み)

- **iOS 実機・deployment target 下限 (iOS 16) での動的高さ**: iOS 26.5 シミュレータでは検証済みだが、実機 iOS 16.6.1 (pixie4) での `UIHostingConfiguration` self-sizing 伝播はオーナー目視に委ねた (2026-08-03 オーナー判断)。実機自動操作には `mcp__mobile__*` のオンデバイスエージェント追加インストールが必要で、ユーザー所有端末への追加インストールを避けたため。
  → **2026-08-03 オーナー目視で動作を確認**。下限 OS でも行高さの追従は正しく機能する。ただし遷移アニメーションの不自然さ (折りたたみ時に content が下から現れる乱れ) を同時に発見 → 本 change の外で修正済み (直接コミット 9513f95、2026-08-04 develop マージ)。
