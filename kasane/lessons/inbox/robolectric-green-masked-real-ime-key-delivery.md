---
scope: impl
kind: success
severity: normal
count: 3
first-seen: 2026-08-01
last-seen: 2026-08-11
evidence:
  - fix-entrycell-enter-focus-crash (OnEditorActionListener 方式は Robolectric 全 green + 退行検出 A/B まで確認済みだったが、実機では IME が Enter の DOWN を消費し UP 単独配達になるため enterDown ゲートが成立せず無効。runtime-behavior-verification 規約に従った完了前の実機 A/B で捕捉し、View.OnKeyListener 方式へ転換して解消)
  - add-maui-basic-input-cells (MAUI Android の EntryCell 書き戻し経路は Robolectric/net10.0 テスト全 green + エミュレータ視覚照合まで通過したが、実機 Pixel 6a の日本語 IME で確定・BackSpace ごとの rebind フォーカス喪失が露呈し連続入力不可。tasks に置いた実機確認ゲート (7.2) が捕捉 — キー配達に限らず「IME × RecyclerView rebind の相互作用」も Robolectric の緑では証明できない。別 change へ切り出し)
  - fix-entrycell-writeback-caret-race (書き戻しレースによる文字欠落・並び替え・IME desync は既存 Robolectric 全 green の下で実機バースト注入により初めて再現 — フレーム間タイミングと Gboard の composing 挙動は Robolectric で再現しない。修正の完了条件にも tasks グループ 4 として実機 A/B を必須化し、修正前 39〜56% 破損 → 修正後 0 の有意差で解消を確認。規律が機能した成功例)
---

## ルール文

キーイベント・IME が絡む実装方式の選定では、Robolectric のキー配達 (down/up が対で届く) を実機の証明に使わず、実機では IME がイベントの一部だけを消費する非対称配達 (例: DOWN 消費・UP 単独到達) が起きる前提で方式を選び、完了前に実機 A/B で配達パターンごと検証する。

## 経緯

- 2026-08-11 fix-entrycell-writeback-caret-race: 高速連続入力のレース (書き戻し往復と打鍵の競合) は Robolectric の決定論的テストでは stale bind を明示的に挟んで初めて表現でき、自然発生の再現は実機のみ。exploration 段階から実機バースト注入 (`repro-burst-loop.sh`) で高再現率の再現を確立し、修正後も同一手順の A/B で解消を証明した。
- 2026-08-01 fix-entrycell-enter-focus-crash: `TextView.onKeyUp` の `OnEditorActionListener` 呼び出しは DOWN 時消費の印 (`enterDown`) を前提とするため、IME 表示中の UP 単独配達では呼ばれない。この配達パターンは Robolectric では再現せず (down/up 対送出のテストは全 green)、実機 A/B (IME 表示中の keyevent 66) で初めて露呈した。`View.OnKeyListener` (dispatchKeyEvent で先行呼び出し・ゲートなし) への転換で解消。`cross/conventions/runtime-behavior-verification.md` の完了 3 条件を適用していたことが捕捉の決め手 (規約が機能した成功例)。
