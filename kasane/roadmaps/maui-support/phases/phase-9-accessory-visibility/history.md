# phase-9-accessory-visibility 議論履歴

## 2026-08-19: 論点1 — IsHeaderVisible / IsFooterVisible の契約設計 (自動判定との関係)

- 前提調査: 現行の「表示しない」は accessory の中身の不在で表現 (iOS: header nil / footer nil or 空文字で supplementary 非生成、Android: flatten の null 判定)。原典 AiForms は `FooterVisible` のみ (Header 側トグルは無い)。
- 選択肢: (A) AND 合成 — 表示 = トグル && 内容あり (bool・既定 true) / (B) 三値 auto・shown・hidden / (C) true で空でも強制表示
- 採用: A。既定 true で現行挙動と完全一致 (後方互換)、「空の Header / Footer に領域を割り当てない」保証を維持、AiForms FooterVisible の意味論と一致、bool 1個で輸送・等価性が単純。
- B は「空でも領域を出す」需要が現状なく過剰 (需要が出たら enum 化は可逆)、C は既定値で保証を破るため却下。
- オーナー確認事項: 空の view accessory (例: MAUI の空 ContentView) が spacer として機能するか → view accessory は「内容あり」扱いで領域が生成されることを確認して条件クリア (高さは自己計測のため spacer 用途は HeightRequest / Section.HeaderHeight で確保)。text は案どおり空文字なら非表示で了承。
- core/ADR-0023 を proposed で起票。
- 併せて調査で見つかった自動判定の OS 非対称 2件 (Android の空文字 footer / iOS の Theme.headerHeight による header nil 領域生成) を論点4として追加。

## 2026-08-19: 論点4 — 自動判定の非対称の対称化

- 非対称 2件: ① Android は空文字 footer を隠さない (iOS は `.text("")` を非表示)、② iOS は `Theme.headerHeight > 0` のとき header nil でも supplementary を生成 (Android は生成しない)。
- 選択肢: 対称化する (「内容不在なら領域なし」へ統一) / 現状維持 (各 OS の挙動を容認)。
- 採用: 対称化。理由: 論点1の AND 合成の「内容あり」項が OS ごとに違う定義の上に載るのを防ぐ。① は concepts の保証「空の Header / Footer に表示領域を割り当てない」への Android の違反 (drift)。② は core/ADR-0021 の高さ解決は「存在する accessory の高さ」の話であり、nil header に領域を生やすのは意図した仕様と読めない。
- 定義: 「内容の不在 = nil または空 text」(header / footer 共通・両 OS 共通)。高さ解決は存在判定の後で、`Theme.headerHeight` は Header の存在を作らない。
- ② は iOS の公開挙動変更になるが、ADR-0021 で対称化のための挙動変更を受け入れた前例に倣う。両件とも本フェーズの change に含める。
- 新規 ADR は起こさず、「内容あり」の定義として core/ADR-0023 (proposed) に統合。

## 2026-08-19: 論点2 — Native への追加形状と Diff・Store への波及

- 選択肢: (A) Section フィールド追加 + 既存 `replaceSection` 経路に相乗り (専用 Diff なし) / (B) 専用 Diff 操作 (setSectionAccessoryVisibility 等) を新設 / (C) SectionAccessory 側に visibility を持たせる
- 採用: A。`Section.isVisible` / `headerHeight` という同性質の先例が既に replaceSection 相乗りで運用されており、MAUI 側も Section 属性変更を ReplaceSection バッチへ落とす仕組み (KsSettingsController) が既存。replaceSection は Cell ID 維持で diff されるため再バインドコストは小さく、表示トグルは高頻度操作でもない。
- B は Diff enum・Store・Bridge 3層×両OS へ新ケースが波及し重い (ヘビーな用途が出たら専用操作の後付けは可逆)。C は SectionAccessory が等価性に参加する「内容の値」であり表示都合の混入は責務違反のため却下。
- フィールドは iOS/Android とも `isHeaderVisible` / `isFooterVisible` (既定 true)、値等価性に参加。判定の織り込みは iOS 3箇所 (supplementaryModes / makeHeaderBoundaryItem / shouldShowFooter)・Android 1箇所 (flatten)。
- 輸送形の要点 (等価性参加・既存 Section 置換経路・専用 Diff なし) を core/ADR-0023 に統合。

## 2026-08-19: 論点3 — Bridge / MAUI への公開形

- 形は論点2からほぼ機械的に確定: Bridge は `KsBridgeSection` (iOS/Android) へ `isHeaderVisible` / `isFooterVisible` (既定 true) のフィールド追加のみで専用 bridge 操作なし。MAUI facade は `Section` に BindableProperty 2つ追加、PropertyChanged は `IsVisible` / `HeaderHeight` と同じ ReplaceSection バッチ分岐 (KsSettingsController) へ相乗り。
- 実質の争点は MAUI 公開名。選択肢: (A) `IsHeaderVisible` / `IsFooterVisible` (.NET 慣例) / (B) `HeaderVisible` / `FooterVisible` (AiForms 命名踏襲)。
- エージェントは B を推奨 (maui/ADR-0008 の「対応概念ありは AiForms 命名踏襲」+ FooterVisible は原典に実在) したが、オーナー裁定で **A を採用**。理由: 本機能は Native 起点の再設計による**新概念**であり、互換提供ではないから ADR-0008 の互換命名踏襲に縛られる必要がない (ADR-0008 自身が「現行コア契約に無い機能は互換提供せず Native から再設計」と位置付けている)。
- この解釈 (Native 起点の新設機能は AiForms 命名踏襲の対象外、.NET 慣例で命名) は後続の強化フェーズ (phase-7 / 8 / 10) にも効く前例。
- MAUI 公開形を core/ADR-0023 に統合。全論点解消。

## 2026-08-19: 提案化と spec-review (相方セカンドオピニオン) の裁定

- ksn-propose で change `add-accessory-visibility-toggle` (M 級・domain: cross) を作成。ksn-second-opinion (spec-review、相方 codex) を実施 — 採用 6 / 降格 2 (証跡: changes/add-accessory-visibility-toggle/second-opinion-spec-001.md)。
- 採用の主要件: 宣言 DSL 経路 (SwiftUI ksSection / Compose DSLScope.Section + 差分検出 + ADR-0018 対称テスト) の欠落、iOS の Section 手動再構築でのトグル保持、iOS Binding ApiDefinition.cs 更新、互換性文言の限定、独立性・保持 Scenario の追加。
- オーナー裁定①: `Section.headerHeight` 正値 + header 不在も「領域を生成しない」へ一般化 (ADR-0023 の存在判定先行を一貫適用)。iOS は逆契約を意図的にテストで固定していた (headerHeight40 + header nil で生成) が、Android は同条件で生成せず現行挙動自体が OS 非対称のため、Android へ対称化し既存テストを反転する。
- オーナー裁定②: DSL 経路のスコープ追加後も級は M 維持 (追加分も同じ bool の配管で新規の設計判断なし)。ui/ 省略も維持。
