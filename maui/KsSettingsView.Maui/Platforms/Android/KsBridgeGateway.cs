using System;
using System.Collections.Generic;
using Android.Content;
using KsSettingsView.Bridge;
using AndroidView = Android.Views.View;
using Drawable = Android.Graphics.Drawables.Drawable;
using JavaBoolean = Java.Lang.Boolean;
using JavaDouble = Java.Lang.Double;
using JavaInteger = Java.Lang.Integer;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// Android の Native Bridge を操作する gateway。
/// </summary>
/// <remarks>
/// Bridge が返す Native Host は Android の View であり、view 階層へ追加して表示する。
/// <c>Context</c> は Host 生成時にだけ渡し、Bridge は保持しない。全メソッドを UI スレッドから呼ぶ。
/// </remarks>
internal sealed class KsBridgeGateway : IKsSettingsGateway
{
    private readonly KsSettingsBridge _bridge = new();

    /// <summary>Bridge へ渡した通知チャネルの実体。</summary>
    private InteractionRelay? _relay;

    /// <summary>Cell の icon として載せる platform 画像の引き当て先。</summary>
    private IKsIconStore? _icons;

    /// <summary>輸送 DTO へ載せる platform view の引き当て先。</summary>
    private IKsPlatformViewStore? _platformViews;

    /// <summary>
    /// 内部 Store に接続済みの Native Host を返す。生きている Host があれば同じものを返し、
    /// 解放済みなら現在状態から表示を復元した新しい Host を返す。
    /// </summary>
    /// <param name="context">Host の生成に使う Context</param>
    public AndroidView? MakeHost(Context context) => _bridge.MakeHostView(context);

    /// <inheritdoc/>
    public IReadOnlyList<KsSectionIdentity> SetRoot(IReadOnlyList<Section> sections)
    {
        ArgumentNullException.ThrowIfNull(sections);

        KsBridgeRootBuilder builder = new();
        List<KsSectionIdentity> identities = new(sections.Count);
        foreach (Section section in sections)
        {
            KsBridgeSection dto = builder.AddSection(section.HeaderText, section.FooterText);
            dto.Visible = section.IsVisible;
            dto.HeaderVisible = section.IsHeaderVisible;
            dto.FooterVisible = section.IsFooterVisible;
            dto.HeaderHeight = Number(section.HeaderHeight);
            FillAccessoryViews(dto, section);
            identities.Add(new KsSectionIdentity(dto.SectionID, AddCells(dto, section.Cells)));
        }

        _bridge.SetRoot(builder);
        return identities;
    }

    /// <inheritdoc/>
    public KsSectionIdentity? InsertSection(Section section, int index)
    {
        ArgumentNullException.ThrowIfNull(section);

        KsBridgeSection dto = new(section.HeaderText, section.FooterText)
        {
            Visible = section.IsVisible,
            HeaderVisible = section.IsHeaderVisible,
            FooterVisible = section.IsFooterVisible,
            HeaderHeight = Number(section.HeaderHeight),
        };
        FillAccessoryViews(dto, section);
        IReadOnlyList<string> cellIds = AddCells(dto, section.Cells);
        string? sectionId = _bridge.InsertSection(dto, index);
        return sectionId is null ? null : new KsSectionIdentity(sectionId, cellIds);
    }

    /// <inheritdoc/>
    public void RemoveSection(string sectionId) => _bridge.RemoveSection(sectionId);

    /// <inheritdoc/>
    public void MoveSection(int from, int to) => _bridge.MoveSection(from, to);

    /// <inheritdoc/>
    public KsSectionIdentity? ReplaceSection(
        string sectionId,
        Section newSection,
        IReadOnlyList<string> retainedCellIds)
    {
        ArgumentNullException.ThrowIfNull(newSection);
        ArgumentNullException.ThrowIfNull(retainedCellIds);

        KsBridgeSection dto = new(newSection.HeaderText, newSection.FooterText)
        {
            Visible = newSection.IsVisible,
            HeaderVisible = newSection.IsHeaderVisible,
            FooterVisible = newSection.IsFooterVisible,
            HeaderHeight = Number(newSection.HeaderHeight),
        };
        FillAccessoryViews(dto, newSection);
        IReadOnlyList<string> cellIds = AddCells(dto, newSection.Cells, retainedCellIds);
        string? retainedId = _bridge.ReplaceSection(sectionId, dto);
        return retainedId is null ? null : new KsSectionIdentity(retainedId, cellIds);
    }

    /// <inheritdoc/>
    public string? InsertCell(CellBase cell, string sectionId, int index)
        => _bridge.InsertCell(ToDto(cell), sectionId, index);

    /// <inheritdoc/>
    public void RemoveCell(string cellId) => _bridge.RemoveCell(cellId);

    /// <inheritdoc/>
    public void MoveCell(string cellId, int index) => _bridge.MoveCell(cellId, index);

    /// <inheritdoc/>
    public string? ReplaceCell(string cellId, CellBase newCell)
        => _bridge.ReplaceCell(cellId, ToDto(newCell));

    /// <inheritdoc/>
    public void ReplaceCells(IReadOnlyList<KsCellUpdate> updates)
    {
        ArgumentNullException.ThrowIfNull(updates);

        List<KsBridgeCellUpdate> dtos = new(updates.Count);
        foreach (KsCellUpdate update in updates)
        {
            dtos.Add(new KsBridgeCellUpdate(update.CellId, ToDto(update.Cell)));
        }

        _bridge.ReplaceCells(dtos);
    }

    /// <inheritdoc/>
    public void UpdateAccessory(KsAccessoryTarget target, string? sectionId, string? text)
        => _bridge.UpdateAccessory(ToBridgeTarget(target), sectionId, text);

    /// <inheritdoc/>
    public void UpdateAccessoryView(KsAccessoryTarget target, string? sectionId, object? view)
        => _bridge.UpdateAccessoryView(ToBridgeTarget(target), sectionId, view as AndroidView);

    /// <inheritdoc/>
    public void InvalidateAccessoryMeasurement(KsAccessoryTarget target, string? sectionId)
        => _bridge.InvalidateAccessoryMeasurement(ToBridgeTarget(target), sectionId);

    /// <inheritdoc/>
    public void SetTheme(KsThemeSnapshot theme)
    {
        ArgumentNullException.ThrowIfNull(theme);

        _bridge.SetTheme(Theme(theme));
    }

    /// <inheritdoc/>
    public void SetStyle(SettingsViewStyle style) => _bridge.SetStyle(KsWireValues.ListStyle(style));

    /// <inheritdoc/>
    public void AttachIcons(IKsIconStore icons)
    {
        ArgumentNullException.ThrowIfNull(icons);

        _icons = icons;
    }

    /// <inheritdoc/>
    public void AttachPlatformViews(IKsPlatformViewStore views)
    {
        ArgumentNullException.ThrowIfNull(views);

        _platformViews = views;
    }

    /// <inheritdoc/>
    public void ReleaseHost() => _bridge.ReleaseHost();

    /// <inheritdoc/>
    public void AttachInteractions(IKsInteractionSink sink)
    {
        ArgumentNullException.ThrowIfNull(sink);

        if (_relay is not null && ReferenceEquals(_relay.Sink, sink))
        {
            return;
        }

        InteractionRelay relay = new(sink);
        _relay = relay;
        _bridge.InteractionListener = relay;
    }

    /// <inheritdoc/>
    public void DetachInteractions()
    {
        _bridge.InteractionListener = null;
        _relay = null;
    }

    /// <summary>実体化済みの header / footer の platform view を Section DTO へ載せる。</summary>
    /// <remarks>
    /// 実体化されていない位置は view なしになり、テキストの指定だけが残る。実体化の後に
    /// 明示の更新で送り直される。
    /// </remarks>
    /// <param name="dto">載せ先の Section DTO</param>
    /// <param name="section">写し元の Section</param>
    private void FillAccessoryViews(KsBridgeSection dto, Section section)
    {
        dto.HeaderView =
            _platformViews?.FindAccessoryView(section, KsAccessoryTarget.SectionHeader) as AndroidView;
        dto.FooterView =
            _platformViews?.FindAccessoryView(section, KsAccessoryTarget.SectionFooter) as AndroidView;
    }

    /// <summary>
    /// Section DTO へ Cell を順に追加し、採番された ID を配置順で返す。
    /// </summary>
    /// <remarks>
    /// <paramref name="retainedCellIds"/> に載っている位置の Cell は、採番済みの ID を引き継ぐ。
    /// 引き継げなかった位置は DTO が新しく採番した ID になる。
    /// </remarks>
    /// <param name="section">追加先の Section DTO</param>
    /// <param name="cells">追加する Cell</param>
    /// <param name="retainedCellIds">引き継ぐ ID (配置順)</param>
    private IReadOnlyList<string> AddCells(
        KsBridgeSection section,
        IList<CellBase> cells,
        IReadOnlyList<string>? retainedCellIds = null)
    {
        if (cells is null || cells.Count == 0)
        {
            return [];
        }

        List<string> cellIds = new(cells.Count);
        for (int i = 0; i < cells.Count; i++)
        {
            KsBridgeCell dto = ToDto(cells[i]);
            if (retainedCellIds is not null && i < retainedCellIds.Count)
            {
                dto.AdoptCellID(retainedCellIds[i]);
            }

            cellIds.Add(section.AddCell(dto));
        }

        return cellIds;
    }

    /// <summary>facade の Cell を、その種別に対応する輸送 DTO へ写し取る。</summary>
    /// <remarks>
    /// 写しに載らない platform 実体 (icon 画像・内容の view) は、写しの変換のあとに引き当て先から
    /// 引いて DTO へ載せる。実体化されていなければ載らず、実体化の後に送り直される。
    /// </remarks>
    private KsBridgeCell ToDto(CellBase cell)
    {
        ArgumentNullException.ThrowIfNull(cell);

        KsBridgeCell dto = FromSnapshot(cell.CreateSnapshot());
        dto.Icon = _icons?.FindIcon(cell) as Drawable;
        if (dto is KsBridgeCustomCell custom)
        {
            custom.View = _platformViews?.FindCellContentView(cell) as AndroidView;
        }

        return dto;
    }

    /// <summary>写しの種別に対応する輸送 DTO を組み立てる。</summary>
    /// <param name="cellSnapshot">写し元</param>
    private static KsBridgeCell FromSnapshot(KsCellSnapshot cellSnapshot)
    {
        return cellSnapshot switch
        {
            KsCommandCellSnapshot snapshot => Command(snapshot),
            KsCustomCellSnapshot snapshot => Custom(snapshot),
            KsButtonCellSnapshot snapshot => Button(snapshot),
            KsSwitchCellSnapshot snapshot => Switch(snapshot),
            KsCheckboxCellSnapshot snapshot => Checkbox(snapshot),
            KsSimpleCheckCellSnapshot snapshot => SimpleCheck(snapshot),
            KsRadioCellSnapshot snapshot => Radio(snapshot),
            KsEntryCellSnapshot snapshot => Entry(snapshot),
            KsPickerCellSnapshot snapshot => Picker(snapshot),
            KsNumberPickerCellSnapshot snapshot => NumberPicker(snapshot),
            KsTimePickerCellSnapshot snapshot => TimePicker(snapshot),
            KsDatePickerCellSnapshot snapshot => DatePicker(snapshot),
            KsLabelCellSnapshot snapshot => Label(snapshot),
            { } snapshot => Fill(new KsBridgeLabelCell(snapshot.Title), snapshot),
        };
    }

    private static KsBridgeLabelCell Label(KsLabelCellSnapshot snapshot)
    {
        KsBridgeLabelCell dto = Fill(new KsBridgeLabelCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        return dto;
    }

    private static KsBridgeCommandCell Command(KsCommandCellSnapshot snapshot)
    {
        KsBridgeCommandCell dto = Fill(new KsBridgeCommandCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.HideArrow = snapshot.HideArrow;
        return dto;
    }

    /// <summary>任意の View を内容とする Cell の写しを輸送 DTO へ組み立てる。</summary>
    /// <remarks>内容の実体は写しに載らないため、ここでは載せる世代と行の指定だけを写す。</remarks>
    /// <param name="snapshot">写し元</param>
    private static KsBridgeCustomCell Custom(KsCustomCellSnapshot snapshot)
    {
        KsBridgeCustomCell dto = Fill(new KsBridgeCustomCell(snapshot.Title), snapshot);
        dto.ContentToken = snapshot.ContentToken;
        dto.ShowArrowIndicator = snapshot.ShowArrowIndicator;
        dto.HasTapHandler = snapshot.HasTapHandler;
        return dto;
    }

    private static KsBridgeButtonCell Button(KsButtonCellSnapshot snapshot)
    {
        KsBridgeButtonCell dto = Fill(new KsBridgeButtonCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.TitleColor = Number(snapshot.Style?.TitleColor);
        dto.TitleAlignment = Number(KsWireValues.Alignment(snapshot.TitleAlignment));
        return dto;
    }

    private static KsBridgeSwitchCell Switch(KsSwitchCellSnapshot snapshot)
    {
        KsBridgeSwitchCell dto = Fill(new KsBridgeSwitchCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.On = snapshot.IsOn;
        dto.AccentColor = Number(snapshot.AccentColor);
        return dto;
    }

    private static KsBridgeCheckboxCell Checkbox(KsCheckboxCellSnapshot snapshot)
    {
        KsBridgeCheckboxCell dto = Fill(new KsBridgeCheckboxCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.Checked = snapshot.IsChecked;
        dto.AccentColor = Number(snapshot.AccentColor);
        return dto;
    }

    private static KsBridgeSimpleCheckCell SimpleCheck(KsSimpleCheckCellSnapshot snapshot)
    {
        KsBridgeSimpleCheckCell dto = Fill(new KsBridgeSimpleCheckCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.Checked = snapshot.IsChecked;
        dto.AccentColor = Number(snapshot.AccentColor);
        return dto;
    }

    private static KsBridgeRadioCell Radio(KsRadioCellSnapshot snapshot)
    {
        KsBridgeRadioCell dto = Fill(new KsBridgeRadioCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.GroupID = snapshot.GroupId;
        dto.Value = snapshot.Value;
        dto.SelectedValue = snapshot.SelectedValue;
        dto.AccentColor = Number(snapshot.AccentColor);
        return dto;
    }

    private static KsBridgeEntryCell Entry(KsEntryCellSnapshot snapshot)
    {
        KsBridgeEntryCell dto = Fill(new KsBridgeEntryCell(snapshot.Title), snapshot);
        dto.Text = snapshot.Text;
        dto.Placeholder = snapshot.Placeholder;
        dto.PlaceholderColor = Number(snapshot.PlaceholderColor);
        dto.Keyboard = (int)snapshot.Keyboard;
        dto.Password = snapshot.IsPassword;
        dto.TextAlignment = Number(KsWireValues.Alignment(snapshot.TextAlignment));
        dto.MaxLength = Number(snapshot.MaxLength);
        dto.AccentColor = Number(snapshot.AccentColor);
        return dto;
    }

    private static KsBridgePickerCell Picker(KsPickerCellSnapshot snapshot)
    {
        KsBridgePickerCell dto = Fill(new KsBridgePickerCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.Items = PickerItems(snapshot.Items);
        dto.SelectionMode = KsWireValues.SelectionMode(snapshot.SelectionMode);
        dto.SelectedIndex = Number(snapshot.SelectedIndex);
        dto.SetSelectedIndices([.. snapshot.SelectedIndices]);
        dto.MaxSelectedNumber = snapshot.MaxSelectedNumber;
        dto.PageTitle = snapshot.PageTitle;
        dto.AccentColor = Number(snapshot.AccentColor);
        return dto;
    }

    private static KsBridgeNumberPickerCell NumberPicker(KsNumberPickerCellSnapshot snapshot)
    {
        KsBridgeNumberPickerCell dto = Fill(new KsBridgeNumberPickerCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.Min = snapshot.Min;
        dto.Max = snapshot.Max;
        dto.Step = snapshot.Step;
        dto.Value = snapshot.Number;
        dto.Unit = snapshot.Unit;
        dto.PickerTitle = snapshot.PickerTitle;
        dto.AccentColor = Number(snapshot.AccentColor);
        return dto;
    }

    private static KsBridgeTimePickerCell TimePicker(KsTimePickerCellSnapshot snapshot)
    {
        KsBridgeTimePickerCell dto = Fill(new KsBridgeTimePickerCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.Time = snapshot.Time;
        dto.Format = snapshot.Format;
        // Kotlin は `is` で始まるプロパティのアクセサを `is24Hour()` / `set24Hour()` として出す。
        // 生成器はこの組をプロパティへまとめないため、時制だけは setter を直接呼ぶ。
        dto.Set24Hour(Number(snapshot.Is24Hour));
        dto.PickerTitle = snapshot.PickerTitle;
        dto.AccentColor = Number(snapshot.AccentColor);
        return dto;
    }

    private static KsBridgeDatePickerCell DatePicker(KsDatePickerCellSnapshot snapshot)
    {
        KsBridgeDatePickerCell dto = Fill(new KsBridgeDatePickerCell(snapshot.Title), snapshot);
        dto.ValueText = snapshot.ValueText;
        dto.Date = snapshot.Date;
        dto.Format = snapshot.Format;
        dto.MinDate = snapshot.MinDate;
        dto.MaxDate = snapshot.MaxDate;
        dto.PickerTitle = snapshot.PickerTitle;
        dto.TodayText = snapshot.TodayText;
        dto.UiStyle = Number(KsWireValues.UIStyle(snapshot.UIStyle));
        dto.AndroidButtonColor = Number(snapshot.AndroidButtonColor);
        dto.AccentColor = Number(snapshot.AccentColor);
        return dto;
    }

    /// <summary>共通の項目を DTO へ写す。</summary>
    /// <typeparam name="T">写し先の DTO の型</typeparam>
    /// <param name="dto">写し先の DTO</param>
    /// <param name="snapshot">写し元</param>
    private static T Fill<T>(T dto, KsCellSnapshot snapshot)
        where T : KsBridgeCell
    {
        dto.DescriptionText = snapshot.Description;
        dto.HintText = snapshot.HintText;
        dto.Enabled = snapshot.IsEnabled;
        dto.Visible = snapshot.IsVisible;
        dto.Style = Style(snapshot.Style);
        return dto;
    }

    /// <summary>Cell 個別のスタイルを輸送側の DTO へ写す。未指定なら null。</summary>
    /// <param name="style">写し取るスタイル</param>
    private static KsBridgeCellStyle? Style(KsCellStyleSnapshot? style)
    {
        if (style is null)
        {
            return null;
        }

        return new KsBridgeCellStyle
        {
            TitleColor = Number(style.TitleColor),
            TitleFont = Font(style.TitleFont),
            DescriptionColor = Number(style.DescriptionColor),
            DescriptionFont = Font(style.DescriptionFont),
            ValueTextColor = Number(style.ValueTextColor),
            ValueTextFont = Font(style.ValueTextFont),
            IconSize = Number(style.IconSize),
            IconRadius = Number(style.IconRadius),
            CellHeight = Number(style.CellHeight),
            HintTextColor = Number(style.HintTextColor),
            HintTextFont = Font(style.HintTextFont),
            BackgroundColor = Number(style.BackgroundColor),
        };
    }

    /// <summary>画面全体の既定スタイルを輸送側の DTO へ写す。</summary>
    /// <param name="theme">写し取る既定スタイル</param>
    private static KsBridgeTheme Theme(KsThemeSnapshot theme) => new()
    {
        SeparatorColor = Number(theme.SeparatorColor),
        BackgroundColor = Number(theme.BackgroundColor),
        CellBackgroundColor = Number(theme.CellBackgroundColor),
        SelectedColor = Number(theme.SelectedColor),
        CellAccentColor = Number(theme.CellAccentColor),
        DisabledTextColor = Number(theme.DisabledTextColor),
        ScrollIndicatorVisible = Number(theme.ScrollIndicatorVisible),
        RowHeight = Number(theme.RowHeight),
        HasUnevenRows = Number(theme.HasUnevenRows),
        HeaderTextColor = Number(theme.HeaderTextColor),
        HeaderBackgroundColor = Number(theme.HeaderBackgroundColor),
        HeaderFontSize = Number(theme.HeaderFontSize),
        HeaderFont = Font(theme.HeaderFont),
        HeaderHeight = Number(theme.HeaderHeight),
        FooterTextColor = Number(theme.FooterTextColor),
        FooterBackgroundColor = Number(theme.FooterBackgroundColor),
        FooterFontSize = Number(theme.FooterFontSize),
        FooterFont = Font(theme.FooterFont),
        CellTitleColor = Number(theme.CellTitleColor),
        CellTitleFont = Font(theme.CellTitleFont),
        CellTitleFontSize = Number(theme.CellTitleFontSize),
        CellValueTextColor = Number(theme.CellValueTextColor),
        CellValueTextFont = Font(theme.CellValueTextFont),
        CellDescriptionColor = Number(theme.CellDescriptionColor),
        CellDescriptionFont = Font(theme.CellDescriptionFont),
        CellHintTextColor = Number(theme.CellHintTextColor),
        CellHintFont = Font(theme.CellHintFont),
        CellPlaceholderColor = Number(theme.CellPlaceholderColor),
        CellIconSize = Number(theme.CellIconSize),
        CellIconRadius = Number(theme.CellIconRadius),
        SectionMarginTop = Number(theme.SectionMarginTop),
        SectionMarginLeading = Number(theme.SectionMarginLeading),
        SectionMarginBottom = Number(theme.SectionMarginBottom),
        SectionMarginTrailing = Number(theme.SectionMarginTrailing),
        SectionCornerRadius = Number(theme.SectionCornerRadius),
        SectionBorderWidth = Number(theme.SectionBorderWidth),
        SectionBorderColor = Number(theme.SectionBorderColor),
    };

    /// <summary>フォントの記述子を輸送側の表現へ写す。未指定なら null。</summary>
    /// <param name="font">写し取るフォント</param>
    private static KsBridgeFont? Font(KsFontSnapshot? font)
        => font is null
            ? null
            : new KsBridgeFont(font.FamilyName, font.PointSize, font.IsBold, font.IsItalic);

    /// <summary>未指定を許す実数を輸送側の表現へ写す。</summary>
    /// <param name="value">写し取る値。null で未指定</param>
    private static JavaDouble? Number(double? value)
        => value is double number ? JavaDouble.ValueOf(number) : null;

    /// <summary>未指定を許す真偽値を輸送側の表現へ写す。</summary>
    /// <param name="value">写し取る値。null で未指定</param>
    private static JavaBoolean? Number(bool? value)
        => value is bool flag ? JavaBoolean.ValueOf(flag) : null;

    /// <summary>未指定を許す整数を輸送側の表現へ写す。</summary>
    /// <param name="value">写し取る値。null で未指定</param>
    private static JavaInteger? Number(int? value)
        => value is int number ? JavaInteger.ValueOf(number) : null;

    /// <summary>選択候補の並びを輸送側の表現へ写す。</summary>
    /// <remarks>表示射影は写しの生成時に適用済みのため、主表示と副表示をそのまま載せる。</remarks>
    /// <param name="items">写し取る候補の並び</param>
    private static IList<KsBridgePickerItem> PickerItems(IReadOnlyList<KsPickerItemSnapshot> items)
    {
        List<KsBridgePickerItem> transported = new(items.Count);
        foreach (KsPickerItemSnapshot item in items)
        {
            transported.Add(new KsBridgePickerItem(item.Text, item.SubText));
        }

        return transported;
    }

    /// <summary>
    /// accessory の更新対象を輸送側の列挙へ写す。
    /// </summary>
    /// <remarks>
    /// Java の列挙は静的プロパティとして nullable に束縛されるため、ここで非 null へ解決する。
    /// </remarks>
    private static KsBridgeAccessoryTarget ToBridgeTarget(KsAccessoryTarget target) => target switch
    {
        KsAccessoryTarget.RootHeader => KsBridgeAccessoryTarget.RootHeader!,
        KsAccessoryTarget.RootFooter => KsBridgeAccessoryTarget.RootFooter!,
        KsAccessoryTarget.SectionHeader => KsBridgeAccessoryTarget.SectionHeader!,
        KsAccessoryTarget.SectionFooter => KsBridgeAccessoryTarget.SectionFooter!,
        _ => throw new ArgumentOutOfRangeException(nameof(target)),
    };

    /// <summary>Bridge の通知を facade の受け口へ中継する。</summary>
    /// <param name="sink">中継先</param>
    private sealed class InteractionRelay(IKsInteractionSink sink)
        : Java.Lang.Object, IKsBridgeInteractionListener
    {
        /// <summary>中継先。差し替えの要否を判断するために公開する。</summary>
        public IKsInteractionSink Sink { get; } = sink;

        /// <inheritdoc/>
        public void CommandCellTapped(string cellID) => Sink.CommandCellTapped(cellID);

        /// <inheritdoc/>
        public void ButtonCellTapped(string cellID) => Sink.ButtonCellTapped(cellID);

        /// <inheritdoc/>
        public void CustomCellTapped(string cellID) => Sink.CustomCellTapped(cellID);

        /// <inheritdoc/>
        public void SwitchCellChanged(string cellID, bool isOn)
            => Sink.SwitchCellChanged(cellID, isOn);

        /// <inheritdoc/>
        public void CheckboxCellChanged(string cellID, bool isChecked)
            => Sink.CheckboxCellChanged(cellID, isChecked);

        /// <inheritdoc/>
        public void SimpleCheckCellChanged(string cellID, bool isChecked)
            => Sink.SimpleCheckCellChanged(cellID, isChecked);

        /// <inheritdoc/>
        public void RadioCellSelected(string cellID, string value)
            => Sink.RadioCellSelected(cellID, value);

        /// <inheritdoc/>
        public void EntryCellTextChanged(string cellID, string text)
            => Sink.EntryCellTextChanged(cellID, text);

        /// <inheritdoc/>
        public void PickerCellSelectionChanged(string cellID, int index)
            => Sink.PickerCellSelectionChanged(cellID, index);

        /// <inheritdoc/>
        public void PickerCellMultiSelectionChanged(string cellID, int[] indices)
            => Sink.PickerCellMultiSelectionChanged(cellID, indices);

        /// <inheritdoc/>
        public void NumberPickerCellChanged(string cellID, int value)
            => Sink.NumberPickerCellChanged(cellID, value);

        /// <inheritdoc/>
        public void TimePickerCellChanged(string cellID, string time)
            => Sink.TimePickerCellChanged(cellID, time);

        /// <inheritdoc/>
        public void DatePickerCellChanged(string cellID, string date)
            => Sink.DatePickerCellChanged(cellID, date);
    }
}
