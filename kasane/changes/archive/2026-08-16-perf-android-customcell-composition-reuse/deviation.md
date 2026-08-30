# Deviation: perf-android-customcell-composition-reuse

- CustomCell Composition のプール生存と破棄境界 (settings-view-android-ui): spec では「行単位のスクロールアウトやリサイクルプール滞在では Composition を破棄しない」→ 実際は `scrollToPosition` 等の位置指定ジャンプ (全行再レイアウト) では RecyclerView が一時 detach (`detachViewFromParent` → `removeDetachedView`) を経由し `isWithinPoolingContainer` が false になるため Composition が破棄され、旧挙動 (再構築) へ縮退する。理由: 実装から制御できない RecyclerView 内部経路で、刻みスクロール/フリックでは spec どおり成立し、ジャンプ時も現状より悪化しない (2026-08-16 オーナー合意)
- content の表示タイミング (spec が規定しない領域): プール由来の再 bind では、非活性ノードの measure を回避する measure policy の帰結として、content の表示が再活性化の composition 適用まで最大 1 フレーム遅れる (行の高さは確保されレイアウト位置は保たれる。prefetch により通常は観測されない)。設計判断と帰結は android/ADR-0015 の Decision / Consequences に記録 (2026-08-16 オーナー合意)
