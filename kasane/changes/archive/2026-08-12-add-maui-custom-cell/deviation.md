# Deviation: add-maui-custom-cell

- maui-cells / Requirement「継承プロパティのうち不適用のものは silent no-op」Scenario「共有 Style の適用が例外にならない」: spec では「CellBase 対象の共有 Style オブジェクトを Cell 群へ適用する」→ 指示により「同一のスタイル指定値を複数 Cell へ当てる」に読み替えて THEN の観測 (適用できる Cell には効き CustomCell では無視され例外なし) を担保。理由: MAUI 公開面で `Style` プロパティは `NavigableElement` のみで `CellBase : Element` に存在せず、WHEN が文字どおりには成立しないため (2026-08-12)
- maui-cells / Handler 解放経路 (design 外の追加): design では ReleaseHost 時の記述なし → 指示により「内容なしの世代で再発行してから lease 破棄」を追加維持。理由: 退役済み実体を Store が指したまま Host 再接続すると死んだ platform view を触り得るため (accessory の text 書き戻しと同型の安全側処理) (2026-08-12)
- samples-maui / Requirement「パリティ画面 CustomCellDemo を native と同一構成で提供する」Scenario「インライン構成の live 更新が動作する」: spec では「Section ① インライン構成の content 内の操作でバインド値を変更する」→ 指示により WHEN の実行場所を Section ④ (行タップカウンタのピル → Command → 同一行の即時更新) に読み替えて THEN (再設定なしの live 反映) を担保。理由: Section ① は native (iOS / Android) と同一の静的 2 行で、操作要素を足すと同 Requirement のパリティ SHALL (native と構成・文言一致) に違反するため。証跡: ios-final2-parity-05 / android-tapfix-03 (2026-08-12)
- maui-bridge / iOS の埋め込み形 (design Decision 3 の実装形からの乖離): design では「representable は accessory と同じく返す前に `removeFromSuperview()` し、常に同じインスタンスを返す」→ 実装では representable が返すのは行ごとに新しく作る入れ物 (`KsBridgeCellContentHostView`) とし、輸送された platform view はその入れ物の子として取り付ける (取り付け前の切り離しは維持)。理由: accessory と違い Cell は行の再利用で描画側が同じ内容を作り直すため、representable が共有インスタンスを直接返す形では、前の行の後片付けが後から走ったときに表示中の行から内容が外れて空行になる。

  内容の実体は 1 つきりで入れ物の間を移動するため、入れ物化だけでは足りず「どの入れ物が抱えるか」の決まりを実装側で持つ (これも design の記述にはない):
  - 行の内容として作られたばかりの入れ物 (`makeUIView`) は無条件に引き取る
  - それ以外の機会では、内容がどこにも付いていないか、抱えている相手が表に出ていないときで、かつ自分が表に出ているときだけ引き取る (表に出ている入れ物からは決して奪わないため、取り合いが振動しない)
  - 引き取りを確かめる機会は配置任せにせず、内容を外された合図 (`willRemoveSubview`) と表示への出入り (`didMoveToWindow`) で配置を予約する。抱え主が表から外れたことは待つ側に届かないため、生存中の入れ物の弱参照一覧をたどって同じ内容を待つ入れ物にも確かめ直させる

  この決まりが要るのは、行の再利用で bind される入れ物が表示に出ないまま退役し得るためで、その場合に内容がどこにも付かない状態のまま残り、表示中の行が空になる。減速の遅いドラッグでは配置の機会が来ないため空行のまま固定される (実機で再現・計測により機構を確定。fix5 の検証: 高速フリック → 遅いドラッグの組 6 セット × 3 セッションで検出 0 件)。spec の Requirement (切り離してから返す / トークン変更でのみ差し替わる / 同一トークンでインスタンス維持) はいずれも維持しており、乖離は design の実装形の記述のみ (2026-08-12)
