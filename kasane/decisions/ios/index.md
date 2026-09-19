# ios ADR 一覧

| ID | タイトル | status | 概要 |
|---:|---|---|---|
| [0001](0001-accessory-column-outside-content-stack.md) | アクセサリは contentStack 外のアクセサリ列に置き垂直 Fill させる | accepted | Cell 級アクセサリを stackH のアクセサリ列へ分離し、description の回り込みを解消する。 |
| [0002](0002-customcell-hosting-recreation-accepted.md) | CustomCell の hosting 階層はリサイクル毎の再生成を維持する | accepted | 計測スパイク (iPhone 11/15 実機) で in-place 更新の機構成立と効果ゼロを確認し、「中身のリサイクル」最適化を見送る。 |
| [0003](0003-modern-self-drawn-section-decoration.md) | Modern は insetGrouped を廃し自前の Section 装飾で実現する | accepted | Section の余白・角丸・ボーダー4属性の制御可能性を OS 外観への自動追従より優先し、compositional layout 上の自前装飾で実現する。 |
| [0005](0005-cell-press-feedback-library-controlled-timing.md) | Cell の押下色は UIKit のタッチ遅延に頼らず、ライブラリ側の遅延・フェード・遷移連動の解除で制御する | accepted | タッチ遅延を切りスイッチ上は優先、押下色は遅延+フェードで立ち上げ、遷移中は選択を残して戻りに合わせて解除する。 |
| [0006](0006-swiftui-wrapper-ignores-container-safe-area.md) | SwiftUI ラッパは既定で container のセーフエリアを無視して全面に広がり、inset は UIKit に任せる | accepted | Representable の配置縮小が帯と iOS 26 大タイトル不全の原因。container 全辺を既定で無視し keyboard 領域は残す、opt-out の口を設ける。 |

欠番: 0004 は起票取り下げ (前提が実測で反証。出典: [kasane/changes/archive/2026-08-22-fix-ios-entrycell-writeback-race](../../changes/archive/2026-08-22-fix-ios-entrycell-writeback-race/rejected-adr-draft.md))。

採番はドメイン内で 0001 から (採番規則は [../index.md](../index.md) を参照)。
