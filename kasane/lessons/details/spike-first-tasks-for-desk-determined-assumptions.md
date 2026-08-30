# spike-first-tasks-for-desk-determined-assumptions (process L-004 の経緯)

inbox パターンとして success 4 件 + 反例 (pain 側) 1 件で閾値到達し、2026-08-16 にオーナー承認で `process.md` L-004 へ昇格した。

## 観測 (5 change)

- 2026-08-02 add-maui-custom-cell: design Decision 5 が「wrapper の計測無効化だけで両 OS の行高さが追従するか」を机上確定せず probe 分岐として明示し、tasks 冒頭 1.1/1.2 の実測スパイク (負の対照付き) で本実装前に確定。native への再計測口追加という Non-Goals 例外分岐を使わずに済むことが実装前に判明した。
- 2026-08-02 timepickercell-color-adjust: tasks 冒頭のスパイク 4 タスクが、ADR-0006 の机上確定 3 点のうち 2 点の誤り — キャレットの着色機構・pre-draw 毎フレーム適用の 60fps ループ — を本実装前に検出し、方式自体は維持のまま安価に補正できた。
- 2026-08-02 datepickercell-color-adjust: tasks 冒頭の実機スパイクが、ヘッダ重なりの机上仮説「CJK 大フォント」を棄却し真因「AppCompat 名前空間属性が解釈されない」を本実装前に確定。誤仮説前提の補正実装を回避できた。
- 2026-08-12 datepickercell-today-shortcut: tasks 1.1 の Robolectric スパイクが ADR-0010 の机上未確定点「scrollToPosition 後の bind 待ち合わせ」を post 1 回で成立と実測確定し、performItemClick の駆動と月 position の stable ID 照合まで実装前に裏取り。本実装は待ち合わせ起因の手戻りゼロで完了した。
- 2026-08-16 perf-android-customcell-composition-reuse (**反例**): 「`ReusableContentHost` はプレーン ComposeView 直下で機能する」を compose-runtime / compose-ui 1.7.5 実ソース読解の机上確定のみで前提にし、spike なしで本実装・テスト新設まで完了。tasks 終盤の実機検証 (3.1) で初めて `measure is called on a deactivated node` の FATAL が露呈した — `ReusableContentHost` は本来 `SubcomposeLayout` の「非活性 slot を measure しない」measure policy とセットで成立する機構であり、プレーン ComposeView 直下では同等の責務を利用側が持つ必要があった。measure policy の追加設計・回帰テスト追加・レビュー 1 周回の手戻り (review-002)。冒頭の実機スパイクなら本実装前に露呈していた。

## 共通構造

外部ライブラリの内部実装をソース読解で「成立」と確定しても、実行環境の駆動条件 (Choreographer・RecyclerView の同期 measure・IME 等) との相互作用は机上では網羅できない。spike-first の 4 件はいずれも本実装前の安価な補正で済み、spike を欠いた 1 件だけが実装後の手戻りになった — 対照が揃った形。
