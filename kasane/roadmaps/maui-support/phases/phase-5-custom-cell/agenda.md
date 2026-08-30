# phase-5-custom-cell

CustomCell の MAUI 対応。現行のカスタムセル3層契約 (content 値 + builder・等価性による再バインド制御) に適合する形で、C# View を Native CustomCell の content に接続する。

原案: `openspec/changes/add-maui-cells` の一部 (凍結・参照のみ。旧案の設計は現行契約と食い違うため全面再検討)

## 論点

(なし — 全論点解消済み)

## 決定事項

- (2026-08-12 改訂) サンプルは **①パリティ画面 `CustomCellDemoPage` (native の既存 CustomCellDemo と同一の5構成・文言一致) + ②MAUI 固有デモページ (差し替え・null 遷移 / ItemTemplate 独立動作 / 再接続復元 / サイズ変化追従、「MAUI 固有」区分)** の2画面構成とする。当初決定 (独自6項目1ページ) は spec-review で sample-parity 規約 (cross/ADR-0016、「CustomCellDemo は本フェーズで追随予定」と明記) との違反が判明したため改訂 (経緯は history 2026-08-12 後段と change の second-opinion-spec-001.md)

- (2026-08-12) CustomCell の公開 API は **`CustomCell : CellBase` + `[ContentProperty]` の `Content : View`**。挙動プロパティは `Command` / `CommandParameter` / `Tapped` (core の onTap 対応、自 facade の CommandCell と命名統一) と `ShowArrowIndicator` (既定 false、AiForms 命名踏襲。CommandCell の `HideArrow` と向きが逆なのは core 契約の既定値が逆のための意図的非対称)。`IsEnabled` / `IsVisible` / `BackgroundColor` / `Height` は CellBase 継承をそのまま使う。継承で露出する不適用プロパティ (Title / Description / IconSource / テキスト系 style) は**隠蔽せず露出のまま silent no-op** (snapshot 変換が読まない) + ドキュメント・XML doc 明記 — `new` 隠蔽は XAML (BindableProperty 経由)・Binding・基底型アクセスに効かず中途半端なため不採用。例外送出は CellBase 対象の共有 Style を壊すため不採用。AiForms の `IsSelectable` / `IsMeasureOnce` / `UseFullSize` / `LongCommand` は非提供 (maui/ADR-0008、IsMeasureOnce は maui/ADR-0016 の wrapper 無分岐規律とも衝突)。DataTemplate は生成機構としては共存 (ItemTemplate 生成の各 CustomCell が独立に実体化経路へ乗る)、再バインド機構・仮想化としては使わない

- (2026-08-12) content 等価性の MAUI 実現は **「参照が正・内容は live」+ 世代トークン方式**。MAUI CustomCell は MauiView インスタンスを持ち、内容変化は binding の live 更新 (native への再発行なし、maui/ADR-0018 と同規律)。native へは phase-6 の実体化機構 (maui/ADR-0016〜0017) で wrapper platform view を輸送し、native builder は interop で埋め込む定数返しクロージャ。native content には facade が振る一意の世代トークンを格納 — 等価性へ正しく参加し、View 差し替え再発行時のみトークンが変わる → その時だけ native が再バインド。サイズ変化は wrapper の MeasureInvalidated → 高さ再計算経路で追従。KsAnyView 直持ち (旧案) は再バインド暴発のため不採用、content 値 + DataTemplate の native 忠実再構築 (案B) は phase-10 の仮想化とセットで再考
- (2026-08-12) 利用者定義 Cell の拡張境界 (Registry) は **MAUI に公開しない** — 上記③非提供の決定に吸収。`KsCellRegistry` は native 側利用者向けの拡張境界のまま維持する (maui/ADR-0019)
- (2026-08-12) カスタムセル3層のうち MAUI は **① インライン + ② ラップ再利用のみ提供し、③ UserDefinedCell は非提供**。理由: ③は per-type 輸送 (maui/ADR-0011) に利用者型が乗らず、利用者に Swift/Kotlin Renderer + binding 拡張を強いるため C# 完結の利用者像から外れる。①の content が任意 MauiView になるため実用ニーズは①で満たせ、②は追加機構ゼロで手に入る (C# ファクトリメソッド形 / CustomCell 派生サブクラス形のどちらでも可 — 形の選択は論点2 で詰める)。③の後付け追加は公開面の追加であり可逆

## TODO

- [x] 論点の解消 (2026-08-12 全5論点解消。maui/ADR-0019〜0021 起票済み)
- [x] ksn-propose で変更提案を起こす (2026-08-12 add-maui-custom-cell、実装完了・蒸留済み)
- [x] (phase-6 からの引き継ぎ 2026-08-12) content の実体化は phase-6 で建てた共有機構を再利用する — 前提知識は [concepts/maui/architecture/view-materialization.md](../../../../concepts/maui/architecture/view-materialization.md) と maui/ADR-0016〜0018。特に: 論理所有と platform lease の寿命分離 / 退役順序 (Store 更新 → native 配信 → 破棄、同一 View 包み直し前の先行破棄) / iOS wrapper の `IntrinsicContentSize` 必須 / fake materializer の Handler 1:1 模擬 / 用途固有ポリシーを wrapper 本体へ分岐で持ち込まない

## 実装結果 (2026-08-12 反映)

change: [changes/archive/2026-08-12-add-maui-custom-cell](../../../../changes/archive/2026-08-12-add-maui-custom-cell/proposal.md) — レビュー2周 (ホスト + codex 相方) APPROVED、verify VALID、maui/ADR-0019〜0021 accepted。

- deviation 4 件 (いずれもオーナー合意済み、詳細は change の deviation.md): 共有 Style Scenario の読み替え / ReleaseHost 時の空世代再発行 / iOS 埋め込み形 (行ごとの入れ物 + 引き取り規則) / パリティ①の live 更新 WHEN の Section ④ への読み替え
- probe の結果、行高さ追従は wrapper 自身の計測無効化で完結し、native への一過性再計測通知 (Non-Goals の事前許容分岐) は不採用
- E2E で実欠陥 5 件を実装ウェーブ内で検出・解消 (Android 空描画 = バッチ配信漏れ / iOS スクロール空行 / iOS 高さ追従回帰 / Android 行タップ不発 = Compose interop / iOS stale binary の検証罠)。継ぎ目の吸収層の知識は concepts/maui/architecture/view-materialization.md へ蒸留

### 申し送りのルーティング

- ContentTemplate・content 値駆動 template 再実体化・live View 常存 (仮想化なし) の解消 → [phase-10-template-virtualization の論点](../phase-10-template-virtualization/agenda.md) に追記済み (2026-08-12)
- iOS: 表示中の行を別種 Cell へ差し替えると部分更新経路がクラッシュする既存不具合 (CustomCell 固有でない) → 独立変更として起票済み (バックグラウンドタスク「iOS: 別種 Cell への replaceCell クラッシュを修正」)
- Android: 共有 View の後片付けレース (iOS で確定した機構の同型危険、未観測) の検証 → 独立変更として起票済み (同「Android: CustomCell 共有 View の後片付けレース検証」)
- accessory (HeaderView/FooterView) の View 重複検査・値確定前ガードを content と同じ規律に揃える (review-002 Minor の逆方向ガード含む) → 独立変更として起票済み (同「accessory の View 重複検査を content と同じ規律に揃える」)
- 見送り (2026-08-12 オーナー判断): ① Android の content 上タップで行 ripple が出ない非対称 — spec 要求外で、修正には行 View の pressed 状態を継ぎ目から駆動する拡張が必要。問題化した時点で変更を起こす ② Android Slider の初回表示スタイル揺れ疑い — 再現せず (再接続後はむしろ native と一致)。観察記録のみ ③ パリティ② footer 文言「SliderCell(label:value:) 関数が…」と MAUI 実装形 (派生クラス) の齟齬 — 直すなら native 含む全 platform 一斉の文言変更のため見送り
