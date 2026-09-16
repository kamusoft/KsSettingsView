# リリースノート素材: maui-cell-appthemebinding-logical-child

次の prerelease のリリースノートへ転記する素材。対象パッケージは MAUI facade (`KsSettingsView.Maui`)。

## Breaking change (1 件)

### 日本語

**同じ `Section` / `CellBase` / accessory View のインスタンスを複数の `SettingsView` で共有する配置が例外になりました。**

`Section` / `Cell` は所属先 (`SettingsView` / `Section`) の論理子になりました。そのため、すでに他所が所有している `Section` / `Cell` を別の `Root` / `Cells` へ追加すると、**追加したその時点で** `InvalidOperationException` が送出されます。別の `SettingsView` が所有している場合も、`Root` から外した `Section` が所有したままの場合も同じです。accessory View (Header / Footer の View) と `CustomCell.Content` にも同じ判定が加わりました。ただし View の場合、例外になる時点は配置の経路で異なります — 所有者 (Section / CustomCell) が既に `SettingsView` の表示へ繋がっていれば設定したその時点、まだ繋がっていなければ所有者が表示へ繋がる時点 (Native Host の接続時を含む) です。

これまでは、同じ `SettingsView` の中での重複だけが表示へ変換する時点で例外になり、別の `SettingsView` をまたぐ共有は黙って通っていました。同じインスタンスを複数箇所へ置くことは以前から禁止しており、今回その検出を実装に揃えたものです。

移行: 共有していたインスタンスは、置く場所ごとに別インスタンスを作ってください。置き直したい場合は、先に元のコレクションから除去してから追加すれば新しい所属先の論理子になります。ただしこの手順が除去した時点で効くのは、増減を通知するコレクション (`SettingsRoot` / `ObservableCollection<T>`) を使っている場合です。素の `List<T>` を `Root` / `Cells` に置いている場合は除去が所有者へ届かないため、除去した後に `Root` / `Cells` へ新しいコレクションを代入し直して所属の解除を確定させてから、新しい場所へ追加してください。

### English

**Sharing the same `Section` / `CellBase` / accessory `View` instance across multiple `SettingsView` instances now throws.**

`Section` and `Cell` are now logical children of their owner (`SettingsView` / `Section`). Adding a `Section` / `Cell` that is still owned elsewhere to another `Root` / `Cells` throws `InvalidOperationException` **at the moment of the add** — whether the current owner is another `SettingsView` or a `Section` that has been removed from one. The same check now also applies to accessory views (Header / Footer views) and `CustomCell.Content`. For views, however, the moment of the throw depends on the placement path: it is the moment of the assignment when the owner (`Section` / `CustomCell`) is already part of a `SettingsView`'s display, and the moment the owner joins that display (including when a native host connects) otherwise.

Previously only duplicates within a single `SettingsView` threw, and that happened later, when the tree was converted for display; sharing across `SettingsView` instances passed silently. Placing one instance in more than one location was already disallowed — this release makes the implementation match that contract.

Migration: create a separate instance per placement. To move an instance, remove it from its current collection first; it then becomes a logical child of the new owner. The removal takes effect at that moment only for collections that raise change notifications (`SettingsRoot` / `ObservableCollection<T>`). When `Root` / `Cells` holds a plain `List<T>`, the removal never reaches the owner, so assign a new collection to `Root` / `Cells` after removing — that finalizes the release — and then add the instance to its new place.

## Breaking ではない変化 (記載は任意)

`Section` / `Cell` のプロパティに書いた `AppThemeBinding` / `DynamicResource` が、外観の変更とページ / アプリの `Resources` の差し替えで再評価されるようになりました。これまで効かなかった XAML が効くようになる方向の拡張で、`Application.RequestedThemeChanged` を購読して値を入れ直す既存のコードはそのまま動きます。
