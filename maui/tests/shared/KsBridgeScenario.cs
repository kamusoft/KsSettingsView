using KsSettingsView.Bridge;

namespace KsSettingsView.IntegrationHost;

/// <summary>
/// 検証ホストが実行する Bridge 操作シナリオ。iOS / Android の両ホストから同じソースを参照する。
/// </summary>
/// <remarks>
/// Bridge の公開 API (Builder・setRoot・更新 API 10 種・setTheme・破棄) を一通り呼び、
/// 呼び出し後に残る表示内容を目視で確認できる状態にする。Native Host の生成と解放後の付け替えは
/// platform ごとに異なるため、ホスト側に残してある。
///
/// シナリオは <see cref="Apply"/> と <see cref="ApplyRootAccessory"/> の 2 段に分かれる。
/// Host は view の構築 / 取り付けの時点で Store の現在状態から表示を復元するため、
/// <see cref="Apply"/> は Host を取り付ける前に呼んでよい (core/ADR-0019)。一方 Root の
/// header / footer は Store が現在状態として保持せず復元の対象外なので (core/ADR-0005)、
/// <see cref="ApplyRootAccessory"/> は所有者が Host の view を構築した後に呼ぶ。
///
/// Bridge の API は UI スレッドから呼ぶ契約 (maui/ADR-0005) のため、このシナリオも UI スレッドから
/// 実行する。
/// </remarks>
public static class KsBridgeScenario
{
    /// <summary>
    /// 設定ツリーを構築して <paramref name="bridge"/> へ反映し、続けて全更新 API を適用する。
    /// </summary>
    /// <remarks>
    /// Root の header / footer は含まない。それらは <see cref="ApplyRootAccessory"/> が受け持つ。
    /// </remarks>
    /// <param name="bridge">操作対象の Bridge</param>
    /// <returns>解放後の更新シナリオが対象にする Cell の ID</returns>
    public static KsBridgeScenarioHandles Apply(KsSettingsBridge bridge)
    {
        ArgumentNullException.ThrowIfNull(bridge);

        // Builder で Section 2 個と LabelCell を構築して root を確定する
        var builder = new KsBridgeRootBuilder();

        var general = builder.AddSection("一般", "アプリ全体の設定");
        var themeCell = MakeCell("テーマ", valueText: "ライト");
        var languageCell = MakeCell("言語", valueText: "日本語");
        builder.AddLabelCell(themeCell, general.SectionID);
        builder.AddLabelCell(languageCell, general.SectionID);

        var storage = new KsBridgeSection("ストレージ", null);
        var cacheCell = MakeCell("キャッシュ", description: "一時ファイルの使用量", valueText: "12.3 MB");
        storage.AddCell(cacheCell);
        builder.AddSection(storage);

        bridge.SetRoot(builder);

        // insertSection: 「通知」を先頭と「ストレージ」の間へ挿入する
        var notification = new KsBridgeSection("通知", null);
        notification.AddCell(MakeCell("プッシュ通知", valueText: "オン"));
        bridge.InsertSection(notification, 1);

        // insertCell: 「ストレージ」の末尾へ 1 件追加する
        var versionCell = MakeCell("バージョン", valueText: "0.1.0");
        bridge.InsertCell(versionCell, storage.SectionID, 1);

        // moveCell: 追加した「バージョン」を Section 内の先頭へ移す
        bridge.MoveCell(versionCell.CellID, 0);

        // replaceCell: 同じ cellID のまま内容だけ差し替える (行の identity は維持される)
        bridge.ReplaceCell(themeCell.CellID, MakeCell("テーマ", valueText: "ダーク"));

        // replaceCells: 2 件の内容更新を 1 バッチで反映する
        bridge.ReplaceCells(
        [
            new KsBridgeCellUpdate(languageCell.CellID, MakeCell("言語", valueText: "English")),
            new KsBridgeCellUpdate(
                cacheCell.CellID,
                MakeCell("キャッシュ", description: "一時ファイルの使用量", valueText: "0 MB")),
        ]);

        // removeCell: 一時的に挿入した Cell を削除して元の並びへ戻す
        var scratchCell = MakeCell("一時項目");
        var scratchCellID = bridge.InsertCell(scratchCell, general.SectionID, 0);
        if (scratchCellID is not null)
        {
            bridge.RemoveCell(scratchCellID);
        }

        // moveSection / removeSection: 一時 Section を末尾へ動かしてから削除する
        var scratchSection = new KsBridgeSection("一時セクション", null);
        bridge.InsertSection(scratchSection, 0);
        bridge.MoveSection(0, 3);
        bridge.RemoveSection(scratchSection.SectionID);

        // replaceSection: 同じ sectionID のまま header / footer と Cell 構成を差し替える
        var replacedStorage = new KsBridgeSection(
            "ストレージ",
            "端末内に保存されたデータ",
            [
                MakeCell("バージョン", valueText: "0.1.0"),
                MakeCell("キャッシュ", description: "一時ファイルの使用量", valueText: "0 MB"),
                MakeCell("同期", valueText: "無効", isEnabled: false),
            ]);
        bridge.ReplaceSection(storage.SectionID, replacedStorage);

        // updateAccessory: Section header の text を更新し、footer を消す
        bridge.UpdateAccessory(SectionHeader, notification.SectionID, "通知設定");
        bridge.UpdateAccessory(SectionFooter, notification.SectionID, null);

        // setTheme: 輸送 DTO の Theme を適用する
        bridge.SetTheme(MakeTheme());

        // 「一般」の 2 Cell は ReplaceSection の対象外なので、ここで採番された ID が
        // 解放後の更新でもそのまま使える (ReplaceSection / ReplaceCell は identity を維持する)。
        return new KsBridgeScenarioHandles(
            themeCell.CellID,
            languageCell.CellID,
            notification.SectionID);
    }

    /// <summary>
    /// Host を解放している間に Store を更新する。再生成した Host にこの更新が復元されることを
    /// 目視で確認するための後半シナリオ (maui/ADR-0007)。
    /// </summary>
    /// <remarks>
    /// Host の解放と再生成・view 階層への付け替えはホスト側の責務であり、このメソッドは
    /// 解放中の Store 操作だけを担う。Cell 内容 (replaceCell)・accessory (updateAccessory)・
    /// Theme (setTheme) の 3 系統を更新し、どれが復元されているかを表示で切り分けられるようにする。
    ///
    /// root header / footer だけは再生成した Host に復元されない。root の accessory は Store の
    /// 状態 (設定ツリー) に含まれない UI 層のプロパティで、更新通知だけで表示へ届く作りになっており、
    /// 新しい Host が接続時に取り込む「Store の現在状態」にそもそも入っていないためである。
    /// Section の accessory は Section の状態に含まれるため復元される。
    /// </remarks>
    /// <param name="bridge">操作対象の Bridge</param>
    /// <param name="handles">前半シナリオが返した Section / Cell の ID</param>
    public static void ApplyWhileReleased(KsSettingsBridge bridge, KsBridgeScenarioHandles handles)
    {
        ArgumentNullException.ThrowIfNull(bridge);
        ArgumentNullException.ThrowIfNull(handles);

        // replaceCell: 解放中に 2 件の Cell 内容を差し替える
        bridge.ReplaceCell(handles.ThemeCellID, MakeCell("テーマ", valueText: "解放中に更新"));
        bridge.ReplaceCell(
            handles.LanguageCellID,
            MakeCell("言語", description: "解放中に説明を追加", valueText: "Français"));

        // updateAccessory: Section header を差し替える (復元される) のと、root header / footer を
        // 差し替える (復元されない) のを並べ、両者の違いが表示で分かるようにする
        bridge.UpdateAccessory(SectionHeader, handles.NotificationSectionID, "通知設定 (解放中に更新)");
        bridge.UpdateAccessory(RootHeader, null, "root header は再生成後に復元されない");
        bridge.UpdateAccessory(RootFooter, null, "root footer は再生成後に復元されない");

        // setTheme: Section header の色を変え、Theme も再生成時に復元されることを見えるようにする
        bridge.SetTheme(MakeReleasedTheme());
    }

    /// <summary>
    /// Root の header / footer を <paramref name="bridge"/> へ適用する。
    /// </summary>
    /// <remarks>
    /// Root の header / footer は Store の現在状態に含まれず、Host の復元対象外である
    /// (core/ADR-0005)。反映は所有者の責務であり、反映先は Host の view が構築された後にしか
    /// 存在しないため、取り付けと view の構築を済ませてから呼ぶ。
    /// </remarks>
    /// <param name="bridge">操作対象の Bridge</param>
    public static void ApplyRootAccessory(KsSettingsBridge bridge)
    {
        ArgumentNullException.ThrowIfNull(bridge);

        bridge.UpdateAccessory(RootHeader, null, "KsSettingsView Bridge");
        bridge.UpdateAccessory(RootFooter, null, "C# から Native Bridge を操作しています");
    }

    /// <summary>Bridge を破棄する。破棄後の操作 API は no-op になる。</summary>
    /// <param name="bridge">操作対象の Bridge</param>
    public static void Shutdown(KsSettingsBridge bridge)
    {
        ArgumentNullException.ThrowIfNull(bridge);
        bridge.DisposeBridge();
    }

    // Accessory の更新対象。Android では Java の列挙が静的プロパティとして束縛され nullable に
    // なるため、両 platform で同じ書き方ができるようここで非 null の値へ解決しておく。
    private static KsBridgeAccessoryTarget RootHeader => KsBridgeAccessoryTarget.RootHeader!;

    private static KsBridgeAccessoryTarget RootFooter => KsBridgeAccessoryTarget.RootFooter!;

    private static KsBridgeAccessoryTarget SectionHeader => KsBridgeAccessoryTarget.SectionHeader!;

    private static KsBridgeAccessoryTarget SectionFooter => KsBridgeAccessoryTarget.SectionFooter!;

    /// <summary>LabelCell の輸送 DTO を全フィールド指定で生成する。</summary>
    private static KsBridgeLabelCell MakeCell(
        string title,
        string? description = null,
        string? valueText = null,
        string? hint = null,
        bool isEnabled = true,
        bool isVisible = true)
        => new(title, description, valueText, hint, isEnabled, isVisible);

    /// <summary>前半シナリオの Theme。Section header は緑になる。</summary>
    private static KsBridgeTheme MakeTheme() => MakeTheme(unchecked((int)0xFF1B5E20));

    /// <summary>
    /// 解放中に適用する Theme。Section header をオレンジにして、前半シナリオの Theme と
    /// 見分けが付くようにする。
    /// </summary>
    private static KsBridgeTheme MakeReleasedTheme() => MakeTheme(unchecked((int)0xFFE65100));

    /// <summary>
    /// 輸送 DTO の Theme を生成する。色は ARGB を詰めた 32bit 整数で、寸法とフォントは数値と
    /// フォント記述子で表す (maui/ADR-0004)。数値の箱型が platform で異なるためここだけ分岐する。
    /// </summary>
    /// <param name="headerTextArgb">Section header のテキスト色 (ARGB)</param>
    private static KsBridgeTheme MakeTheme(int headerTextArgb)
    {
        const int SeparatorArgb = unchecked((int)0xFFC7C7CC);
        const double HeaderFontSize = 14.0;

#if IOS
        return new KsBridgeTheme
        {
            SeparatorColor = Foundation.NSNumber.FromInt32(SeparatorArgb),
            HeaderTextColor = Foundation.NSNumber.FromInt32(headerTextArgb),
            HeaderFontSize = Foundation.NSNumber.FromDouble(HeaderFontSize),
            HeaderFont = new KsBridgeFont(null, HeaderFontSize, true, false),
        };
#elif ANDROID
        return new KsBridgeTheme
        {
            SeparatorColor = Java.Lang.Integer.ValueOf(SeparatorArgb),
            HeaderTextColor = Java.Lang.Integer.ValueOf(headerTextArgb),
            HeaderFontSize = Java.Lang.Double.ValueOf(HeaderFontSize),
            HeaderFont = new KsBridgeFont(null, HeaderFontSize, true, false),
        };
#else
        return new KsBridgeTheme();
#endif
    }
}
