using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Internals;

/// <summary>
/// facade の設定ツリーを gateway の操作へ変換する経路。
/// </summary>
/// <remarks>
/// 対応表 (Section / Cell と gateway 採番 ID の双方向) と、コレクション・プロパティの購読を
/// ここで一元所有する。gateway が接続されるまではコレクションの差し替えを覚えるだけで何も
/// 配信せず、接続時に設定ツリー全体を送って対応表を作り、以後は変更を差分で流す。
/// Native Host の解放をまたいでも gateway と購読は維持し、設定ツリーの状態は Bridge 側が保つ。
/// Native で起きたユーザー操作の受け口も兼ね、通知値を対応する Cell のプロパティへ書き戻す。
/// Cell の icon は画像の解決を挟むためここが所有し、解決できた時点で内容更新として流す
/// (設計判断: maui/ADR-0015)。
/// accessory の View と CustomCell の内容の View も実体化を挟むためここが所有し、Native Host と
/// 同じ世代で作り直す (設計判断: maui/ADR-0016)。
/// 全メソッドを UI スレッドから呼ぶ (呼び出し側契約であり、スレッド marshal は行わない)。
/// </remarks>
/// <param name="owner">この変換経路を持つ SettingsView。root accessory の論理上の所有者になる</param>
internal sealed class KsSettingsController(SettingsView owner)
    : IKsCollectionObserver,
        IKsPropertyObserver,
        IKsInteractionSink,
        IKsIconStore,
        IKsPlatformViewStore,
        IKsCellContentGuard,
        IKsAccessoryViewGuard
{
    private readonly Dictionary<Section, SectionEntry> _sectionEntries =
        new(KsReferenceComparer<Section>.Instance);

    private readonly Dictionary<string, Section> _sectionsById = [];

    private readonly Dictionary<CellBase, CellEntry> _cellEntries =
        new(KsReferenceComparer<CellBase>.Instance);

    private readonly Dictionary<string, CellBase> _cellsById = [];

    /// <summary>配信待ちの Cell を登録順に保つ並び。</summary>
    private readonly List<CellBase> _dirtyCells = [];

    private readonly HashSet<CellBase> _dirtyLookup = new(KsReferenceComparer<CellBase>.Instance);

    /// <summary>配信待ちのうち可視性が変わった Cell。単発で送るため内容バッチから分ける。</summary>
    private readonly HashSet<CellBase> _visibilityDirtyCells =
        new(KsReferenceComparer<CellBase>.Instance);

    /// <summary>
    /// 配信待ちのうち Section 単位の差し替えでしか送れない変更を持つ Section。
    /// </summary>
    /// <remarks>可視性とヘッダ高さが該当し、Section ごとの単発配信になる。</remarks>
    private readonly HashSet<Section> _replacePendingSections =
        new(KsReferenceComparer<Section>.Instance);

    /// <summary>Cell ごとの解決済み platform 画像。icon なしの Cell は載らない。</summary>
    private readonly Dictionary<CellBase, KsImageLease> _icons =
        new(KsReferenceComparer<CellBase>.Instance);

    /// <summary>
    /// Cell ごとの最新の解決要求の通番。
    /// </summary>
    /// <remarks>
    /// 画像の指定が変わるたびに新しい通番を割り当て、完了した解決が最新の指定に対するものかを
    /// 判定する。追い抜かれた古い解決の結果は捨てる。
    /// </remarks>
    private readonly Dictionary<CellBase, int> _iconGenerations =
        new(KsReferenceComparer<CellBase>.Instance);

    /// <summary>
    /// 控えから外れたが、まだ後片付けを待っている画像のリース。
    /// </summary>
    /// <remarks>
    /// 後片付けは画像の実体を使えなくし得るため、native がその画像を使わなくなってから行う必要が
    /// ある。控えの更新は gateway への配信より先に走る (内容更新は flush まで遅れる) ので、外した
    /// 時点では破棄せずここへ積み、配信を終えた時点でまとめて破棄する。
    /// </remarks>
    private readonly List<KsImageLease> _retiredIcons = [];

    /// <summary>accessory の位置ごとに置かれている View と、その実体化の状態。</summary>
    private readonly Dictionary<KsAccessorySlot, ViewPlacement> _accessories = [];

    /// <summary>accessory に置かれている View と、その置き場所。多重配置の検出に使う。</summary>
    private readonly Dictionary<View, KsAccessorySlot> _placedViews =
        new(KsReferenceComparer<View>.Instance);

    /// <summary>CustomCell ごとに内容として置かれている View と、その実体化の状態。</summary>
    private readonly Dictionary<CustomCell, ViewPlacement> _cellContents =
        new(KsReferenceComparer<CustomCell>.Instance);

    /// <summary>Cell の内容に置かれている View と、その持ち主。多重配置の検出に使う。</summary>
    private readonly Dictionary<View, CustomCell> _placedContentViews =
        new(KsReferenceComparer<View>.Instance);

    /// <summary>
    /// 置き場所から外れたが、まだ後片付けを待っている accessory の実体。
    /// </summary>
    /// <remarks>
    /// 画像のリースと同じ理由で、native がまだ実体を抱えている間は壊せない。外した時点では
    /// 破棄せずここへ積み、配信を終えた時点でまとめて破棄する。包んでいた View も一緒に覚え、
    /// 同じ View を包み直すときに先に片付けられるようにする。
    /// </remarks>
    private readonly List<RetiredView> _retiredViews = [];

    /// <summary>必要サイズが変わり、領域の測り直しを待っている accessory の位置。</summary>
    private readonly HashSet<KsAccessorySlot> _measureDirtySlots = [];

    private IKsSettingsGateway? _gateway;
    private IKsDispatcher? _dispatcher;
    private IList<Section>? _root;
    private KsWeakCollectionSubscription? _rootSubscription;
    private bool _flushScheduled;
    private string? _rootHeaderText;
    private string? _rootFooterText;
    private KsThemeSnapshot _theme = new();

    /// <summary>現在の見た目スタイル。接続時に配信するためここで控える。</summary>
    private SettingsViewStyle _style = SettingsViewStyle.Classic;

    private IKsImageResolver? _images;
    private IKsViewMaterializer? _views;

    /// <summary>画像解決の口の世代。差し替え・切断のたびに進み、古い口の結果を捨てる。</summary>
    private int _imageGeneration;

    /// <summary>
    /// 解決要求へ払い出す通番。
    /// </summary>
    /// <remarks>
    /// controller の寿命を通じて単調に増え、登録解除や再構築でも戻らない。Cell ごとに数え直すと、
    /// 同じ Cell を外して入れ直したときに古い要求と新しい要求が同じ番号になり、追い抜かれた結果を
    /// 最新と誤判定するため。
    /// </remarks>
    private int _iconRequestSequence;

    /// <summary>
    /// Cell の内容の世代へ払い出す通番。
    /// </summary>
    /// <remarks>
    /// 変換経路の寿命を通じて単調に増え、実体を作り直すたびに新しい世代を割り当てる。同じ View を
    /// 外して入れ直しても世代は戻らないため、Native 側が「変化なし」と誤判定しない。
    /// </remarks>
    private int _contentSequence;

    /// <summary>gateway が接続済みかどうか。</summary>
    public bool IsConnected => _gateway is not null;

    /// <summary>接続済みの gateway。未接続なら null。</summary>
    public IKsSettingsGateway? Gateway => _gateway;

    /// <summary>対応表に登録済みの Section の ID。未登録なら null。</summary>
    /// <param name="section">対象の Section</param>
    public string? FindSectionId(Section section)
        => _sectionEntries.TryGetValue(section, out SectionEntry? entry) ? entry.Id : null;

    /// <summary>対応表に登録済みの Cell の ID。未登録なら null。</summary>
    /// <param name="cell">対象の Cell</param>
    public string? FindCellId(CellBase cell)
        => _cellEntries.TryGetValue(cell, out CellEntry? entry) ? entry.Id : null;

    /// <summary>指定 ID に対応する Section。未知の ID なら null。</summary>
    /// <param name="sectionId">gateway が採番した Section の ID</param>
    public Section? FindSection(string sectionId)
        => _sectionsById.TryGetValue(sectionId, out Section? section) ? section : null;

    /// <summary>指定 ID に対応する Cell。未知の ID なら null。</summary>
    /// <param name="cellId">gateway が採番した Cell の ID</param>
    public CellBase? FindCell(string cellId)
        => _cellsById.TryGetValue(cellId, out CellBase? cell) ? cell : null;

    /// <summary>
    /// gateway を接続し、現在の設定ツリー全体を送って対応表を作る。
    /// </summary>
    /// <remarks>
    /// gateway は facade と同じ寿命を持つため、接続済みの状態で呼び直しても何も起こらない。
    /// 初期構築が失敗したとき (設定ツリーに同じインスタンスが重複配置されている等) は接続そのものを
    /// なかったことにして未接続へ戻す — 中途半端に gateway だけを抱えたまま購読が張られない状態で
    /// 残ると、設定ツリーを直しても以後の変更を追えなくなるため。呼び出し側は設定ツリーを直して
    /// 接続をやり直せる (その際は新しい gateway で構築し直される)。
    /// </remarks>
    /// <param name="gateway">接続する gateway</param>
    /// <param name="dispatcher">内容更新の flush を予約する実行口</param>
    public void Connect(IKsSettingsGateway gateway, IKsDispatcher dispatcher)
    {
        ArgumentNullException.ThrowIfNull(gateway);
        ArgumentNullException.ThrowIfNull(dispatcher);

        if (_gateway is not null)
        {
            return;
        }

        _gateway = gateway;
        _dispatcher = dispatcher;
        gateway.AttachIcons(this);
        gateway.AttachPlatformViews(this);

        try
        {
            gateway.SetTheme(_theme);
            gateway.SetStyle(_style);
            RebuildRoot();
        }
        catch
        {
            Disconnect();
            throw;
        }
    }

    /// <summary>接続を解いて未接続の状態へ戻す。</summary>
    /// <remarks>
    /// 登録と購読をすべて解いたうえで gateway への参照を手放す。手放した gateway は
    /// 以後どこからも参照されないため、Bridge ごと GC の対象になる。
    /// </remarks>
    private void Disconnect()
    {
        ClearRegistrations();
        _gateway = null;
        _dispatcher = null;
        _flushScheduled = false;

        // 表示先ごと手放すため、控えていた画像と実体はこの時点で後片付けしてよい。
        DisposeRetired();
    }

    /// <summary>
    /// ユーザー操作の通知を受け取り始める。
    /// </summary>
    /// <remarks>
    /// 通知は Native Host が表示されている間だけ起きるため、登録は Host の生成に合わせて行う。
    /// 未接続では何も起こらない。
    /// </remarks>
    public void AttachInteractions() => _gateway?.AttachInteractions(this);

    /// <summary>
    /// 画像の解決口を差し込み、現在の指定を解決し直す。
    /// </summary>
    /// <remarks>
    /// 解決口は Native Host と同じ寿命を持つため、Host を作り直すたびに新しい口へ差し替わる。
    /// 差し替えると世代が進み、進行中の解決が後から完了しても結果は採用されない。
    /// </remarks>
    /// <param name="images">画像を platform の表現へ解決する口</param>
    public void AttachImages(IKsImageResolver images)
    {
        ArgumentNullException.ThrowIfNull(images);

        _imageGeneration++;
        _images = images;

        foreach (CellBase cell in Registered())
        {
            ResolveIcon(cell);
        }
    }

    /// <summary>
    /// View の実体化の口を差し込む。
    /// </summary>
    /// <remarks>
    /// 実体化の口は Native Host と同じ寿命を持つため、Host を作り直すたびに新しい口へ差し替わる。
    /// 口が差し込まれるのは Host を作った後であり、それ以前 (設定ツリーの初回構築時) に置かれていた
    /// View はまだ実体化されていない。その分は Host が view 階層へ取り付けられた後の
    /// <see cref="ApplyHostViews"/> でまとめて実体化して配信する。口が差し込まれた後に置かれた
    /// View は、その場で実体化して配信する。
    /// </remarks>
    /// <param name="views">View を platform view へ実体化する口</param>
    public void AttachViews(IKsViewMaterializer views)
    {
        ArgumentNullException.ThrowIfNull(views);

        _views = views;
    }

    /// <summary>Native Host だけを解放する。設定ツリーの状態と購読は維持される。</summary>
    /// <remarks>
    /// Host のない間は操作も起きないため、あわせて通知の受け取りも止める。画像の解決口も
    /// View の実体化の口も Host と同じ寿命であり、進行中の解決は世代を進めて結果を捨てる
    /// (解決済みの画像はそのまま残る)。accessory と Cell の内容の実体は Host と一緒に退役させる。
    /// </remarks>
    public void ReleaseHost()
    {
        _imageGeneration++;
        _images = null;
        _views = null;
        ReleaseAccessoryViews();
        ReleaseCellContentViews();
        _gateway?.DetachInteractions();
        _gateway?.ReleaseHost();
    }

    /// <summary>画面全体の既定スタイルを差し替える。</summary>
    /// <param name="theme">新しい既定スタイル</param>
    public void SetTheme(KsThemeSnapshot theme)
    {
        ArgumentNullException.ThrowIfNull(theme);

        _theme = theme;
        _gateway?.SetTheme(theme);
    }

    /// <summary>見た目スタイルを差し替える。</summary>
    /// <remarks>
    /// Native 側でも見た目スタイルは Store の外にあるため、Native Host を作り直すと失われる。
    /// 現在値をここで控え、接続時に配信することで Host の世代をまたいで保つ (maui/ADR-0023)。
    /// </remarks>
    /// <param name="style">新しい見た目スタイル</param>
    public void SetStyle(SettingsViewStyle style)
    {
        _style = style;
        _gateway?.SetStyle(style);
    }

    /// <inheritdoc/>
    public object? FindIcon(CellBase cell)
        => _icons.TryGetValue(cell, out KsImageLease? lease) ? lease.Image : null;

    /// <summary>
    /// Native Host と同じ寿命を持つ表示内容を、現在の所有値で適用し直す。
    /// </summary>
    /// <remarks>
    /// root の accessory は Native Host 単位のプロパティであり、Host を作り直すと失われる。
    /// accessory と Cell の内容の View の実体も Host と同じ寿命であり、Host を作り直すと退役して
    /// いる。Host が view 階層へ取り付けられた後にここを通すことで、両 OS で同じ経路で復元する。
    /// </remarks>
    public void ApplyHostViews()
    {
        if (_gateway is null)
        {
            return;
        }

        ApplyRootSlot(KsAccessoryTarget.RootHeader);
        ApplyRootSlot(KsAccessoryTarget.RootFooter);

        foreach (Section section in Sections())
        {
            ApplySectionSlot(new KsAccessorySlot(KsAccessoryTarget.SectionHeader, section));
            ApplySectionSlot(new KsAccessorySlot(KsAccessoryTarget.SectionFooter, section));
        }

        List<CustomCell> delivered = [];
        foreach (CellBase cell in Registered())
        {
            if (cell is CustomCell custom && ApplyCellContent(custom))
            {
                delivered.Add(custom);
            }
        }

        DeliverCellContents(delivered);
    }

    /// <summary>root の header / footer のテキストを設定する。</summary>
    /// <remarks>View が置かれている間は表示に使われないため、値を控えるだけで配信しない。</remarks>
    /// <param name="target">更新対象</param>
    /// <param name="text">表示するテキスト。null で解除</param>
    public void SetRootAccessoryText(KsAccessoryTarget target, string? text)
    {
        if (target == KsAccessoryTarget.RootHeader)
        {
            _rootHeaderText = text;
        }
        else
        {
            _rootFooterText = text;
        }

        if (_accessories.ContainsKey(new KsAccessorySlot(target, null)))
        {
            return;
        }

        _gateway?.UpdateAccessory(target, null, text);
    }

    /// <summary>root の header / footer に表示する View を設定する。</summary>
    /// <param name="target">更新対象</param>
    /// <param name="view">表示する View。null で解除し、控えているテキストへ戻す</param>
    public void SetRootAccessoryView(KsAccessoryTarget target, View? view)
        => SetAccessoryView(new KsAccessorySlot(target, null), view);

    /// <inheritdoc/>
    public object? FindAccessoryView(Section section, KsAccessoryTarget target)
        => _accessories.TryGetValue(new KsAccessorySlot(target, section), out ViewPlacement? placement)
            ? placement.Lease?.PlatformView
            : null;

    /// <inheritdoc/>
    public object? FindCellContentView(CellBase cell)
        => cell is CustomCell custom && _cellContents.TryGetValue(custom, out ViewPlacement? placement)
            ? placement.Lease?.PlatformView
            : null;

    /// <inheritdoc/>
    public void EnsureContentCanBePlaced(CustomCell cell, View? content)
        => EnsureContentViewIsNotPlaced(cell, content);

    /// <inheritdoc/>
    public void EnsureAccessoryViewCanBePlaced(Section section, KsAccessoryTarget target, View? view)
        => EnsureAccessoryViewIsNotPlaced(new KsAccessorySlot(target, section), view);

    /// <summary>root の header / footer に置けない View なら例外を送出する。</summary>
    /// <remarks>
    /// root の accessory は SettingsView が持つプロパティであり、その所有者はこの変換経路を直に
    /// 持っている。<see cref="IKsAccessoryViewGuard"/> のような差し込みを介さずここを直接呼ぶ。
    /// </remarks>
    /// <param name="target">これから置く位置</param>
    /// <param name="view">置こうとしている View。null なら何もしない</param>
    public void EnsureRootAccessoryViewCanBePlaced(KsAccessoryTarget target, View? view)
        => EnsureAccessoryViewIsNotPlaced(new KsAccessorySlot(target, null), view);

    /// <summary>
    /// 設定ツリーの Section コレクションを差し替える。
    /// </summary>
    /// <remarks>
    /// 旧コレクションの購読を解除し、新コレクションの内容で表示を作り直す。
    /// 実体が <see cref="INotifyCollectionChanged"/> でない場合は以後の構造変更を追跡しない。
    /// </remarks>
    /// <param name="root">新しい Section コレクション</param>
    public void SetRootCollection(IList<Section>? root)
    {
        _root = root;
        RebuildRoot();
    }

    /// <inheritdoc/>
    public void OnObservedCollectionChanged(object? context, NotifyCollectionChangedEventArgs args)
    {
        if (_gateway is null)
        {
            return;
        }

        if (context is Section section)
        {
            HandleCellsChanged(section, args);
        }
        else
        {
            HandleSectionsChanged(args);
        }

        // 除去・差し替えが native へ届いた後なら、外れた Cell が使っていた画像を後片付けしてよい。
        DisposeRetired();
    }

    /// <inheritdoc/>
    public void OnObservedPropertyChanged(object sender, string? propertyName)
    {
        if (_gateway is null)
        {
            return;
        }

        switch (sender)
        {
            case Section section:
                HandleSectionPropertyChanged(section, propertyName);
                break;
            case CellBase cell:
                HandleCellPropertyChanged(cell, propertyName);
                break;
        }
    }

    // ---- accessory の View の所有 ----

    /// <summary>
    /// 指定の位置に置く View を差し替える。
    /// </summary>
    /// <remarks>
    /// 「多重配置の検査 → 論理上の所有の確定 → 新しい状態の用意 → native への配信 → 旧実体の破棄」
    /// の順で進める。検査を所有の確定より先に置くのは、例外になった配置が他所の正しい配置から
    /// 所有を奪わないため。所有の確定を実体化より先に置くのは、Handler が BindingContext の
    /// 定まった View に対して作られるようにするため。旧実体の破棄を配信より後にするのは、native が
    /// まだ旧実体を子として抱えている間に壊す窓を作らないため。
    /// 実体化の口が無い間 (Native Host 未生成) は置き場所と所有だけを確定させ、実体化と配信は
    /// <see cref="ApplyHostViews"/> まで待つ。
    /// </remarks>
    /// <param name="slot">置き場所</param>
    /// <param name="view">置く View。null で解除</param>
    private void SetAccessoryView(KsAccessorySlot slot, View? view)
    {
        EnsureAccessoryViewIsNotPlaced(slot, view);

        IKsViewLease? retired = null;
        _accessories.Remove(slot, out ViewPlacement? previous);
        if (previous is not null)
        {
            _placedViews.Remove(previous.View);
            _measureDirtySlots.Remove(slot);
            retired = TakeLease(previous);
        }

        KsAccessoryViewOwnership.Reassign(OwnerOf(slot), previous?.View, view);

        if (view is not null)
        {
            ViewPlacement placement = new(view);
            _accessories[slot] = placement;
            _placedViews[view] = slot;
            Materialize(slot, placement);
        }

        DeliverAccessory(slot);
        retired?.Dispose();
    }

    /// <summary>この位置の accessory を論理上所有する要素。</summary>
    /// <param name="slot">対象の位置</param>
    private Element OwnerOf(KsAccessorySlot slot) => slot.Section ?? (Element)owner;

    /// <summary>root の指定位置を、置かれている View または控えているテキストで適用する。</summary>
    /// <param name="target">適用する位置</param>
    private void ApplyRootSlot(KsAccessoryTarget target)
    {
        KsAccessorySlot slot = new(target, null);
        if (_accessories.TryGetValue(slot, out ViewPlacement? placement))
        {
            Materialize(slot, placement);
        }

        DeliverAccessory(slot);
    }

    /// <summary>Section の指定位置に View が置かれていれば、実体化し直して適用する。</summary>
    /// <remarks>
    /// View が置かれていない位置のテキストは Section の状態として保たれているため、ここでは触らない。
    /// </remarks>
    /// <param name="slot">適用する位置</param>
    private void ApplySectionSlot(KsAccessorySlot slot)
    {
        if (!_accessories.TryGetValue(slot, out ViewPlacement? placement))
        {
            return;
        }

        Materialize(slot, placement);
        DeliverAccessory(slot);
    }

    /// <summary>
    /// 置かれている View を platform view として実体化する。
    /// </summary>
    /// <remarks>
    /// 論理ツリーへの接続と BindingContext の継承は、この呼び出しより前に
    /// <see cref="KsAccessoryViewOwnership"/> で確定させてある。ここで作るのは platform 実体だけで、
    /// その寿命は Native Host と一致する。
    /// </remarks>
    /// <param name="slot">置き場所</param>
    /// <param name="placement">置かれている View の状態</param>
    private void Materialize(KsAccessorySlot slot, ViewPlacement placement)
    {
        if (_views is not { } materializer || placement.Lease is not null)
        {
            return;
        }

        DisposeRetiredViewsOf(placement.View);
        placement.Lease = materializer.Materialize(
            placement.View,
            () => OnAccessoryMeasureInvalidated(slot));
    }

    /// <summary>置き場所から実体を切り離し、後片付け待ちのリースを返す。</summary>
    /// <param name="placement">対象の状態</param>
    private static IKsViewLease? TakeLease(ViewPlacement placement)
    {
        if (placement.Lease is not { } lease)
        {
            return null;
        }

        placement.Lease = null;
        return lease;
    }

    /// <summary>
    /// 指定位置の現在の内容を native へ配信する。
    /// </summary>
    /// <remarks>
    /// View が実体化済みならその実体を、View が置かれていなければ控えているテキストを送る。
    /// View は置かれているが実体化できていない間は、取り付け後の適用まで配信を待つ。
    /// </remarks>
    /// <param name="slot">配信する位置</param>
    private void DeliverAccessory(KsAccessorySlot slot)
    {
        if (_gateway is null || !TryResolveSectionId(slot, out string? sectionId))
        {
            return;
        }

        if (_accessories.TryGetValue(slot, out ViewPlacement? placement))
        {
            if (placement.Lease is { } lease)
            {
                _gateway.UpdateAccessoryView(slot.Target, sectionId, lease.PlatformView);
            }

            return;
        }

        _gateway.UpdateAccessory(slot.Target, sectionId, AccessoryTextOf(slot));
    }

    /// <summary>
    /// Native Host と同じ寿命の実体を退役させる。
    /// </summary>
    /// <remarks>
    /// Section の accessory は状態として保たれるため、退役した実体を指したままにならないよう
    /// テキスト (無ければ解除) を書き戻してから破棄する。root の accessory は Host が持つので
    /// 書き戻しは要らない。
    /// </remarks>
    private void ReleaseAccessoryViews()
    {
        List<IKsViewLease> retired = [];
        foreach ((KsAccessorySlot slot, ViewPlacement placement) in _accessories)
        {
            if (TakeLease(placement) is not { } lease)
            {
                continue;
            }

            retired.Add(lease);

            if (slot.Section is not null
                && _gateway is not null
                && TryResolveSectionId(slot, out string? sectionId))
            {
                _gateway.UpdateAccessory(slot.Target, sectionId, AccessoryTextOf(slot));
            }
        }

        _measureDirtySlots.Clear();

        foreach (IKsViewLease lease in retired)
        {
            lease.Dispose();
        }
    }

    /// <summary>この Section の accessory を置き場所ごと外し、実体を後片付け待ちへ回す。</summary>
    /// <param name="section">対象の Section</param>
    private void RetireSectionAccessoryViews(Section section)
    {
        RetireAccessoryView(new KsAccessorySlot(KsAccessoryTarget.SectionHeader, section));
        RetireAccessoryView(new KsAccessorySlot(KsAccessoryTarget.SectionFooter, section));
    }

    private void RetireAccessoryView(KsAccessorySlot slot)
    {
        if (!_accessories.Remove(slot, out ViewPlacement? placement))
        {
            return;
        }

        _placedViews.Remove(placement.View);
        _measureDirtySlots.Remove(slot);

        if (TakeLease(placement) is { } lease)
        {
            _retiredViews.Add(new RetiredView(placement.View, lease));
        }
    }

    /// <summary>この Section に指定済みの View を置き場所へ載せ、実体化して配信する。</summary>
    /// <param name="section">対象の Section</param>
    private void PlaceSectionAccessoryViews(Section section)
    {
        if (section.HeaderView is not null)
        {
            SetAccessoryView(
                new KsAccessorySlot(KsAccessoryTarget.SectionHeader, section),
                section.HeaderView);
        }

        if (section.FooterView is not null)
        {
            SetAccessoryView(
                new KsAccessorySlot(KsAccessoryTarget.SectionFooter, section),
                section.FooterView);
        }
    }

    /// <summary>必要サイズが変わった accessory を、次の配信で測り直す対象に加える。</summary>
    /// <param name="slot">対象の位置</param>
    private void OnAccessoryMeasureInvalidated(KsAccessorySlot slot)
    {
        if (_gateway is null || !_accessories.ContainsKey(slot))
        {
            return;
        }

        _measureDirtySlots.Add(slot);
        ScheduleFlush();
    }

    /// <summary>この位置に控えているテキスト。</summary>
    /// <param name="slot">対象の位置</param>
    private string? AccessoryTextOf(KsAccessorySlot slot) => slot.Target switch
    {
        KsAccessoryTarget.RootHeader => _rootHeaderText,
        KsAccessoryTarget.RootFooter => _rootFooterText,
        KsAccessoryTarget.SectionHeader => slot.Section?.HeaderText,
        _ => slot.Section?.FooterText,
    };

    /// <summary>
    /// この位置を配信するときに渡す Section の ID を求める。
    /// </summary>
    /// <remarks>root 対象では ID を持たない。対応表にいない Section の位置は配信対象にならない。</remarks>
    /// <param name="slot">対象の位置</param>
    /// <param name="sectionId">求まった ID。root 対象では null</param>
    private bool TryResolveSectionId(KsAccessorySlot slot, out string? sectionId)
    {
        sectionId = null;
        if (slot.Section is not { } section)
        {
            return true;
        }

        if (!_sectionEntries.TryGetValue(section, out SectionEntry? entry))
        {
            return false;
        }

        sectionId = entry.Id;
        return true;
    }

    /// <summary>後片付け待ちの実体をまとめて破棄する。</summary>
    /// <remarks>
    /// 破棄の最中に新しい後片付けが積まれても取り違えないよう、待ち行列を先に空にする。
    /// 空にした後は取り出した実体が唯一の持ち主になるため、1 件の後片付けが失敗しても
    /// 残りを取りこぼさないよう、全件の破棄を試みてから失敗をまとめて投げる。
    /// </remarks>
    private void DisposeRetiredViews()
    {
        if (_retiredViews.Count == 0)
        {
            return;
        }

        List<RetiredView> retired = [.. _retiredViews];
        _retiredViews.Clear();

        List<Exception>? failures = null;

        foreach (RetiredView entry in retired)
        {
            try
            {
                entry.Lease.Dispose();
            }
            catch (Exception exception)
            {
                failures ??= [];
                failures.Add(exception);
            }
        }

        if (failures is not null)
        {
            throw new AggregateException(failures);
        }
    }

    /// <summary>
    /// この View を包んでいた実体が後片付けを待っていれば、その分だけ先に破棄する。
    /// </summary>
    /// <remarks>
    /// Handler は View と 1 対 1 であり、同じ View を包み直すと新旧の実体が同じ Handler を指す。
    /// 実体の後片付けは Handler を切るため、包み直す前に済ませないと、作ったばかりの実体が
    /// 切断済みの Handler を抱えることになる (設定ツリーを作り直しても同じ Section が残る経路)。
    /// この時点では、外した実体を表示から取り除く更新は既に native へ配信されている。
    /// </remarks>
    /// <param name="view">これから包み直す View</param>
    private void DisposeRetiredViewsOf(View view)
    {
        for (int i = _retiredViews.Count - 1; i >= 0; i--)
        {
            RetiredView entry = _retiredViews[i];
            if (!ReferenceEquals(entry.View, view))
            {
                continue;
            }

            _retiredViews.RemoveAt(i);
            entry.Lease.Dispose();
        }
    }

    // ---- CustomCell の内容の View の所有 ----

    /// <summary>
    /// この Cell の内容に置く View を差し替える。
    /// </summary>
    /// <remarks>
    /// 進め方は accessory と同じ — 「多重配置の検査 → 論理上の所有の確定 → 新しい状態の用意 →
    /// native への配信 → 旧実体の破棄」の順で進める。配信は Cell 1 件の内容更新として単発で送る。
    /// 送る写しには新しい世代が載るため、Native 側は内容の View を入れ替えたときにだけ作り直す。
    /// </remarks>
    /// <param name="cell">対象の Cell</param>
    /// <param name="view">置く View。null で解除</param>
    private void SetCellContent(CustomCell cell, View? view)
    {
        EnsureContentViewIsNotPlaced(cell, view);

        IKsViewLease? retired = null;
        _cellContents.Remove(cell, out ViewPlacement? previous);
        if (previous is not null)
        {
            _placedContentViews.Remove(previous.View);
            retired = TakeLease(previous);
        }

        KsAccessoryViewOwnership.Reassign(cell, previous?.View, view);

        if (view is not null)
        {
            ViewPlacement placement = new(view);
            _cellContents[cell] = placement;
            _placedContentViews[view] = cell;
            MaterializeContent(placement);
        }

        IssueContentToken(cell);

        if (CanDeliverCellContent(cell))
        {
            DeliverCellContent(cell);
        }

        retired?.Dispose();
    }

    /// <summary>この Cell に指定済みの内容の View を置き場所へ載せ、実体化して配信する。</summary>
    /// <param name="cell">対象の Cell</param>
    private void PlaceCellContent(CustomCell cell)
    {
        if (cell.Content is not null)
        {
            SetCellContent(cell, cell.Content);
        }
    }

    /// <summary>
    /// この Cell の内容を、Native Host の世代に合わせて実体化し直して配信する。
    /// </summary>
    /// <remarks>
    /// 実体は Host と同じ寿命であり、Host を作り直すと退役している。作り直した実体は前の実体とは
    /// 別物のため、世代を振り直して送り直す。内容が置かれていない Cell も、Host をまたいで世代が
    /// 続いていると見なされないよう振り直す。
    /// </remarks>
    /// <param name="cell">対象の Cell</param>
    /// <returns>配信すべき状態になったかどうか</returns>
    private bool ApplyCellContent(CustomCell cell)
    {
        if (_cellContents.TryGetValue(cell, out ViewPlacement? placement))
        {
            MaterializeContent(placement);
        }

        IssueContentToken(cell);

        return CanDeliverCellContent(cell);
    }

    /// <summary>
    /// 内容に置かれている View を platform view として実体化する。
    /// </summary>
    /// <remarks>
    /// 論理上の所有と BindingContext の継承は、この呼び出しより前に
    /// <see cref="KsAccessoryViewOwnership"/> で確定させてある。
    /// 実体は自分の必要サイズが変わったときに platform の計測をやり直させ、行の高さはそれだけで
    /// 内容のサイズに追従する。そのため変換経路から Native へ送り直す再計測の要求は持たない
    /// (領域の高さを Native Host が抱える accessory との違い)。
    /// </remarks>
    /// <param name="placement">置かれている View の状態</param>
    private void MaterializeContent(ViewPlacement placement)
    {
        if (_views is not { } materializer || placement.Lease is not null)
        {
            return;
        }

        DisposeRetiredViewsOf(placement.View);
        placement.Lease = materializer.Materialize(placement.View, static () => { });
    }

    /// <summary>この Cell の内容に新しい世代を振る。</summary>
    /// <param name="cell">対象の Cell</param>
    private void IssueContentToken(CustomCell cell)
        => cell.ContentToken = $"content-{++_contentSequence}";

    /// <summary>
    /// この Cell の内容を今の時点で配信してよいかどうか。
    /// </summary>
    /// <remarks>
    /// View が置かれているのに実体が無いのは Native Host を持たない間だけであり、その分は Host が
    /// 取り付けられた後の適用でまとめて送る。
    /// </remarks>
    /// <param name="cell">対象の Cell</param>
    private bool CanDeliverCellContent(CustomCell cell)
        => !_cellContents.TryGetValue(cell, out ViewPlacement? placement) || placement.Lease is not null;

    /// <summary>この Cell の現在の内容を単発の内容更新として配信する。</summary>
    /// <param name="cell">対象の Cell</param>
    private void DeliverCellContent(CustomCell cell)
    {
        if (_gateway is { } gateway && _cellEntries.TryGetValue(cell, out CellEntry? entry))
        {
            gateway.ReplaceCell(entry.Id, cell);
        }
    }

    /// <summary>複数の Cell の現在の内容を 1 回の内容更新としてまとめて配信する。</summary>
    /// <remarks>
    /// 複数 Cell の内容更新は 1 回にまとめて送る。1 件ずつ送ると、Android では先行する更新の
    /// 反映通知が後続の更新に追い越されて破棄され、行が古い内容のまま残る (内容の View を
    /// 持たない世代で表示が止まる)。1 件だけのときは単発の経路をそのまま使う。
    /// </remarks>
    /// <param name="cells">配信する Cell</param>
    private void DeliverCellContents(List<CustomCell> cells)
    {
        if (_gateway is not { } gateway)
        {
            return;
        }

        List<KsCellUpdate> updates = new(cells.Count);
        foreach (CustomCell cell in cells)
        {
            if (_cellEntries.TryGetValue(cell, out CellEntry? entry))
            {
                updates.Add(new KsCellUpdate(entry.Id, cell));
            }
        }

        if (updates.Count == 1)
        {
            gateway.ReplaceCell(updates[0].CellId, updates[0].Cell);
        }
        else if (updates.Count > 1)
        {
            gateway.ReplaceCells(updates);
        }
    }

    /// <summary>
    /// Native Host と同じ寿命の内容の実体を退役させる。
    /// </summary>
    /// <remarks>
    /// Cell は表示の状態として保たれるため、退役した実体を指したままにならないよう、内容なしの
    /// 世代を振って送り直してから破棄する。置き場所と論理上の所有は Host の有無に依らず保たれる。
    /// </remarks>
    private void ReleaseCellContentViews()
    {
        List<IKsViewLease> retired = [];
        List<CustomCell> delivered = [];
        foreach ((CustomCell cell, ViewPlacement placement) in _cellContents)
        {
            if (TakeLease(placement) is not { } lease)
            {
                continue;
            }

            retired.Add(lease);
            IssueContentToken(cell);
            delivered.Add(cell);
        }

        DeliverCellContents(delivered);

        foreach (IKsViewLease lease in retired)
        {
            lease.Dispose();
        }
    }

    /// <summary>
    /// この Cell の内容を置き場所ごと外し、実体を後片付け待ちへ回す。
    /// </summary>
    /// <remarks>
    /// Cell が表示から外れると内容の View も表示先を失うため、論理上の所有も解く。外れた View は
    /// そのまま別の Cell や accessory へ置き直せる。Cell を表示へ戻したときは、置き場所への
    /// 載せ直しで所有も実体も作り直される。
    /// </remarks>
    /// <param name="cell">対象の Cell</param>
    private void RetireCellContent(CustomCell cell)
    {
        if (!_cellContents.Remove(cell, out ViewPlacement? placement))
        {
            return;
        }

        _placedContentViews.Remove(placement.View);
        KsAccessoryViewOwnership.Reassign(cell, placement.View, null);

        if (TakeLease(placement) is { } lease)
        {
            _retiredViews.Add(new RetiredView(placement.View, lease));
        }
    }

    // ---- 設定ツリー全体の再構築 ----

    private void RebuildRoot()
    {
        if (_gateway is null)
        {
            return;
        }

        List<Section> sections = Snapshot(_root);
        EnsureTreeHasNoDuplicates(sections);

        ClearRegistrations();

        IReadOnlyList<KsSectionIdentity> identities = _gateway.SetRoot(sections);
        for (int i = 0; i < sections.Count && i < identities.Count; i++)
        {
            RegisterSection(sections[i], identities[i]);
        }

        SubscribeSections();

        // 作り直しが native へ届いた後なら、前の表示が使っていた画像と実体を後片付けしてよい。
        DisposeRetired();
    }

    private void SubscribeSections()
    {
        _rootSubscription?.Unsubscribe();
        _rootSubscription = null;

        if (_root is not INotifyCollectionChanged observable)
        {
            return;
        }

        KsWeakCollectionSubscription subscription = new(this, null);
        subscription.Subscribe(observable);
        _rootSubscription = subscription;
    }

    private void ClearRegistrations()
    {
        _rootSubscription?.Unsubscribe();
        _rootSubscription = null;

        foreach (Section section in Sections())
        {
            section.AccessoryGuard = null;
            RetireSectionAccessoryViews(section);
        }

        foreach (CellBase cell in Registered())
        {
            if (cell is CustomCell custom)
            {
                custom.ContentGuard = null;
                RetireCellContent(custom);
            }
        }

        foreach (SectionEntry entry in _sectionEntries.Values)
        {
            entry.PropertySubscription.Unsubscribe();
            entry.CellsSubscription?.Unsubscribe();
        }

        foreach (CellEntry entry in _cellEntries.Values)
        {
            entry.PropertySubscription.Unsubscribe();
        }

        _sectionEntries.Clear();
        _sectionsById.Clear();
        _cellEntries.Clear();
        _cellsById.Clear();
        _dirtyCells.Clear();
        _dirtyLookup.Clear();
        _visibilityDirtyCells.Clear();
        _replacePendingSections.Clear();

        // 表示は gateway を呼び直すまで残るため、この時点では破棄せず後片付け待ちへ回す。
        _retiredIcons.AddRange(_icons.Values);

        _icons.Clear();
        _iconGenerations.Clear();
    }

    // ---- Section コレクションの構造変更 ----

    private void HandleSectionsChanged(NotifyCollectionChangedEventArgs args)
    {
        switch (args.Action)
        {
            case NotifyCollectionChangedAction.Add:
                AddSections(Items<Section>(args.NewItems), args.NewStartingIndex);
                break;
            case NotifyCollectionChangedAction.Remove:
                RemoveSections(Items<Section>(args.OldItems));
                break;
            case NotifyCollectionChangedAction.Replace:
                ReplaceSections(Items<Section>(args.OldItems), Items<Section>(args.NewItems));
                break;
            case NotifyCollectionChangedAction.Move:
                MoveSections(args.OldStartingIndex, args.NewStartingIndex, args.OldItems?.Count ?? 1);
                break;
            default:
                RebuildRoot();
                break;
        }
    }

    /// <summary>連続した <paramref name="count"/> 件の Section をまとめて移動する。</summary>
    /// <param name="from">移動元の先頭位置</param>
    /// <param name="to">移動先の先頭位置</param>
    /// <param name="count">移動する件数</param>
    private void MoveSections(int from, int to, int count)
    {
        foreach ((int source, int destination) in KsRangeMove.Steps(from, to, count))
        {
            _gateway!.MoveSection(source, destination);
        }
    }

    private void AddSections(List<Section> sections, int startIndex)
    {
        EnsureTreeHasNoDuplicates(sections);
        EnsureSectionsAreNotPlaced(sections);

        int index = startIndex >= 0 ? startIndex : _sectionEntries.Count;
        foreach (Section section in sections)
        {
            KsSectionIdentity? identity = _gateway!.InsertSection(section, index);
            if (identity is not null)
            {
                RegisterSection(section, identity);
            }

            index++;
        }
    }

    private void RemoveSections(List<Section> sections)
    {
        foreach (Section section in sections)
        {
            if (!_sectionEntries.TryGetValue(section, out SectionEntry? entry))
            {
                continue;
            }

            string sectionId = entry.Id;
            UnregisterSection(section, entry);
            _gateway!.RemoveSection(sectionId);
        }
    }

    private void ReplaceSections(List<Section> oldSections, List<Section> newSections)
    {
        List<Section> added = [];
        for (int i = 0; i < newSections.Count; i++)
        {
            if (i < oldSections.Count && ReferenceEquals(oldSections[i], newSections[i]))
            {
                continue;
            }

            added.Add(newSections[i]);
        }

        EnsureTreeHasNoDuplicates(added);
        EnsureSectionsAreNotPlaced(added);

        for (int i = 0; i < newSections.Count && i < oldSections.Count; i++)
        {
            Section oldSection = oldSections[i];
            Section newSection = newSections[i];
            if (ReferenceEquals(oldSection, newSection))
            {
                continue;
            }

            if (!_sectionEntries.TryGetValue(oldSection, out SectionEntry? entry))
            {
                continue;
            }

            string sectionId = entry.Id;
            UnregisterSection(oldSection, entry);

            KsSectionIdentity? identity = _gateway!.ReplaceSection(sectionId, newSection, []);
            if (identity is not null)
            {
                RegisterSection(newSection, identity);
            }
        }
    }

    // ---- Cell コレクションの構造変更 ----

    private void HandleCellsChanged(Section section, NotifyCollectionChangedEventArgs args)
    {
        if (!_sectionEntries.TryGetValue(section, out SectionEntry? entry))
        {
            return;
        }

        switch (args.Action)
        {
            case NotifyCollectionChangedAction.Add:
                AddCells(entry, Items<CellBase>(args.NewItems), args.NewStartingIndex);
                break;
            case NotifyCollectionChangedAction.Remove:
                RemoveCells(entry, Items<CellBase>(args.OldItems));
                break;
            case NotifyCollectionChangedAction.Replace:
                ReplaceCells(entry, Items<CellBase>(args.OldItems), Items<CellBase>(args.NewItems));
                break;
            case NotifyCollectionChangedAction.Move:
                MoveCells(entry, Items<CellBase>(args.OldItems), args.OldStartingIndex, args.NewStartingIndex);
                break;
            default:
                RebuildRoot();
                break;
        }
    }

    private void AddCells(SectionEntry entry, List<CellBase> cells, int startIndex)
    {
        EnsureCellsHaveNoDuplicates(cells);
        EnsureCellsAreNotPlaced(cells);

        int index = startIndex >= 0 ? startIndex : entry.Cells.Count;
        foreach (CellBase cell in cells)
        {
            string? cellId = _gateway!.InsertCell(cell, entry.Id, index);
            if (cellId is not null)
            {
                RegisterCell(cell, cellId);
                entry.Cells.Insert(Math.Clamp(index, 0, entry.Cells.Count), cell);
            }

            index++;
        }
    }

    private void RemoveCells(SectionEntry entry, List<CellBase> cells)
    {
        foreach (CellBase cell in cells)
        {
            if (!_cellEntries.TryGetValue(cell, out CellEntry? cellEntry))
            {
                continue;
            }

            string cellId = cellEntry.Id;
            UnregisterCell(cell, cellEntry);
            entry.Cells.Remove(cell);
            _gateway!.RemoveCell(cellId);
        }
    }

    private void ReplaceCells(SectionEntry entry, List<CellBase> oldCells, List<CellBase> newCells)
    {
        List<CellBase> added = [];
        for (int i = 0; i < newCells.Count; i++)
        {
            if (i < oldCells.Count && ReferenceEquals(oldCells[i], newCells[i]))
            {
                continue;
            }

            added.Add(newCells[i]);
        }

        EnsureCellsHaveNoDuplicates(added);
        EnsureCellsAreNotPlaced(added);

        for (int i = 0; i < newCells.Count && i < oldCells.Count; i++)
        {
            CellBase oldCell = oldCells[i];
            CellBase newCell = newCells[i];
            if (ReferenceEquals(oldCell, newCell))
            {
                continue;
            }

            if (!_cellEntries.TryGetValue(oldCell, out CellEntry? cellEntry))
            {
                continue;
            }

            string cellId = cellEntry.Id;
            int position = entry.Cells.IndexOf(oldCell);
            UnregisterCell(oldCell, cellEntry);
            entry.Cells.Remove(oldCell);

            string? retainedId = _gateway!.ReplaceCell(cellId, newCell);
            if (retainedId is null)
            {
                continue;
            }

            RegisterCell(newCell, retainedId);
            entry.Cells.Insert(Math.Clamp(position, 0, entry.Cells.Count), newCell);
        }
    }

    /// <summary>連続した Cell をまとめて移動する。</summary>
    /// <param name="entry">移動元の Section の登録内容</param>
    /// <param name="cells">移動する Cell (移動前の並び)</param>
    /// <param name="from">移動元の先頭位置</param>
    /// <param name="to">移動先の先頭位置</param>
    private void MoveCells(SectionEntry entry, List<CellBase> cells, int from, int to)
    {
        int step = 0;
        foreach ((int _, int destination) in KsRangeMove.Steps(from, to, cells.Count))
        {
            CellBase cell = cells[step];
            step++;

            if (!_cellEntries.TryGetValue(cell, out CellEntry? cellEntry))
            {
                continue;
            }

            _gateway!.MoveCell(cellEntry.Id, destination);
            entry.Cells.Remove(cell);
            entry.Cells.Insert(Math.Clamp(destination, 0, entry.Cells.Count), cell);
        }
    }

    // ---- プロパティ変更 ----

    private void HandleSectionPropertyChanged(Section section, string? propertyName)
    {
        // 対応表に生きている ID がある Section だけを対象にする。未知 ID は Store 側でも
        // no-op になる (core/ADR-0020) が、除去済み Section の通知は MAUI 層で先に遮断する。
        if (!_sectionEntries.TryGetValue(section, out SectionEntry? entry))
        {
            return;
        }

        switch (propertyName)
        {
            case nameof(Section.HeaderText):
                DeliverSectionAccessoryText(section, entry, KsAccessoryTarget.SectionHeader);
                break;
            case nameof(Section.FooterText):
                DeliverSectionAccessoryText(section, entry, KsAccessoryTarget.SectionFooter);
                break;
            case nameof(Section.HeaderView):
                SetAccessoryView(
                    new KsAccessorySlot(KsAccessoryTarget.SectionHeader, section),
                    section.HeaderView);
                break;
            case nameof(Section.FooterView):
                SetAccessoryView(
                    new KsAccessorySlot(KsAccessoryTarget.SectionFooter, section),
                    section.FooterView);
                break;
            case nameof(Section.IsVisible):
            case nameof(Section.IsHeaderVisible):
            case nameof(Section.IsFooterVisible):
            case nameof(Section.HeaderHeight):
                _replacePendingSections.Add(section);
                ScheduleFlush();
                break;
            case nameof(Section.Cells):
                RebuildSectionCells(section, entry);

                // 差し替えが native へ届いた後なので、外れた Cell の画像を後片付けしてよい。
                // 他の分岐は配信が flush まで遅れるため、ここでしか後片付けを走らせない。
                DisposeRetired();
                break;
        }
    }

    /// <summary>Section の header / footer のテキストを配信する。</summary>
    /// <remarks>View が置かれている間は表示に使われないため配信しない。</remarks>
    /// <param name="section">対象の Section</param>
    /// <param name="entry">対象 Section の登録内容</param>
    /// <param name="target">配信する位置</param>
    private void DeliverSectionAccessoryText(
        Section section,
        SectionEntry entry,
        KsAccessoryTarget target)
    {
        if (_accessories.ContainsKey(new KsAccessorySlot(target, section)))
        {
            return;
        }

        string? text = target == KsAccessoryTarget.SectionHeader ? section.HeaderText : section.FooterText;
        _gateway!.UpdateAccessory(target, entry.Id, text);
    }

    /// <summary>
    /// Section 自身のプロパティが変わった Section を、配下 Cell の ID を温存したまま差し替える。
    /// </summary>
    /// <remarks>
    /// 可視性とヘッダ高さは Section 単位の差し替えでしか送れないため、内容更新のバッチには載せず
    /// 単発で配信する。配下 Cell の ID を引き継ぐことで、差し替えの後もユーザー操作の通知が
    /// 従前と同じ ID で届き、双方向バインドが継続する。
    /// </remarks>
    /// <param name="section">対象の Section</param>
    /// <param name="entry">対象 Section の登録内容</param>
    private void ReplaceSectionKeepingCellIds(Section section, SectionEntry entry)
    {
        List<CellBase> cells = Snapshot(section.Cells);
        List<string> retained = new(cells.Count);
        foreach (CellBase cell in cells)
        {
            if (!_cellEntries.TryGetValue(cell, out CellEntry? cellEntry))
            {
                break;
            }

            retained.Add(cellEntry.Id);
        }

        KsSectionIdentity? identity = _gateway!.ReplaceSection(entry.Id, section, retained);
        if (identity is null)
        {
            return;
        }

        // 引き継げなかった Cell だけ、新しく採番された ID で登録し直す。
        for (int i = 0; i < cells.Count && i < identity.CellIds.Count; i++)
        {
            CellBase cell = cells[i];
            string cellId = identity.CellIds[i];
            _cellEntries.TryGetValue(cell, out CellEntry? cellEntry);
            if (cellEntry is not null && cellEntry.Id == cellId)
            {
                continue;
            }

            if (cellEntry is not null)
            {
                UnregisterCell(cell, cellEntry);
            }

            RegisterCell(cell, cellId);
        }
    }

    private void RebuildSectionCells(Section section, SectionEntry entry)
    {
        List<CellBase> cells = Snapshot(section.Cells);
        EnsureCellsHaveNoDuplicates(cells);

        // 差し替えで登録を解く Cell は、同じ実体が新しいコレクションに残っていても重複ではない。
        HashSet<CellBase> releasing = new(entry.Cells, KsReferenceComparer<CellBase>.Instance);
        foreach (CellBase cell in cells)
        {
            if (!releasing.Contains(cell) && _cellEntries.ContainsKey(cell))
            {
                throw DuplicatePlacement("Cell");
            }
        }

        EnsureCellContentsAreFree(cells, releasing);

        foreach (CellBase cell in entry.Cells)
        {
            if (_cellEntries.TryGetValue(cell, out CellEntry? cellEntry))
            {
                UnregisterCell(cell, cellEntry);
            }
        }

        entry.Cells.Clear();
        SubscribeCells(section, entry);

        KsSectionIdentity? identity = _gateway!.ReplaceSection(entry.Id, section, []);
        if (identity is null)
        {
            return;
        }

        for (int i = 0; i < cells.Count && i < identity.CellIds.Count; i++)
        {
            RegisterCell(cells[i], identity.CellIds[i]);
            entry.Cells.Add(cells[i]);
        }
    }

    private void HandleCellPropertyChanged(CellBase cell, string? propertyName)
    {
        if (!_cellEntries.ContainsKey(cell))
        {
            return;
        }

        // icon は写しに載らず解決を挟むため、内容更新とは別の経路で追う。
        if (propertyName == nameof(CellBase.IconSource))
        {
            ResolveIcon(cell);
            return;
        }

        // 内容の View も写しに載らず実体化を挟むため、同じく別の経路で追う。
        if (cell is CustomCell custom && propertyName == nameof(CustomCell.Content))
        {
            SetCellContent(custom, custom.Content);
            return;
        }

        if (!cell.AffectsSnapshot(propertyName))
        {
            return;
        }

        if (propertyName == nameof(CellBase.IsVisible))
        {
            _visibilityDirtyCells.Add(cell);
        }

        MarkContentDirty(cell);
    }

    /// <summary>この Cell の内容を次の配信対象に加える。</summary>
    /// <param name="cell">対象の Cell</param>
    private void MarkContentDirty(CellBase cell)
    {
        if (_dirtyLookup.Add(cell))
        {
            _dirtyCells.Add(cell);
        }

        ScheduleFlush();
    }

    // ---- icon の解決 ----

    /// <summary>
    /// この Cell の画像指定を解決し直す。
    /// </summary>
    /// <remarks>
    /// 呼ぶたびに新しい通番を割り当てるため、先に始まっていた解決の結果は完了しても採用されない
    /// (最後の指定が勝つ)。画像なしはその場で確定し、解決口が無い間 (Handler 未接続) は保留して
    /// 接続時にやり直す。
    /// </remarks>
    /// <param name="cell">対象の Cell</param>
    private void ResolveIcon(CellBase cell)
    {
        int generation = ++_iconRequestSequence;
        _iconGenerations[cell] = generation;

        if (cell.IconSource is not { } source)
        {
            StoreIcon(cell, null);
            return;
        }

        if (_images is not { } images)
        {
            return;
        }

        int imageGeneration = _imageGeneration;
        images.Resolve(source, lease => CompleteIcon(cell, generation, imageGeneration, lease));
    }

    /// <summary>解決の完了を受け取り、最新の指定に対する結果だけを採用する。</summary>
    /// <remarks>採用しなかった結果はその場で破棄し、後片付けを取りこぼさない。</remarks>
    /// <param name="cell">対象の Cell</param>
    /// <param name="generation">解決を始めた時点の要求の通番</param>
    /// <param name="imageGeneration">解決を始めた時点の解決口の世代</param>
    /// <param name="lease">解決できた画像のリース。失敗は null</param>
    private void CompleteIcon(CellBase cell, int generation, int imageGeneration, KsImageLease? lease)
    {
        if (imageGeneration != _imageGeneration)
        {
            lease?.Dispose();
            return;
        }

        if (!_iconGenerations.TryGetValue(cell, out int current) || current != generation)
        {
            lease?.Dispose();
            return;
        }

        StoreIcon(cell, lease);
    }

    /// <summary>解決結果を控え、表示が変わる場合だけ内容更新として流す。</summary>
    /// <remarks>
    /// 控えから外れたリースは、差し替えが native へ配信されるまで表示に使われ続けるため、その場では
    /// 破棄せず後片付け待ちへ回す。控えられなかった結果は native へ渡らないためその場で破棄する。
    /// 再解決が表示中と同一の画像インスタンスを返した場合は表示内容が変わらず配信も起きないため、
    /// 旧リースは後片付け待ちに滞留させずその場で解放する。この即時解放は、同一インスタンスが返る
    /// 画像は platform が所有していて解決口が後片付けの口を付けない、という分類
    /// (<see cref="KsFileImageOwnership"/>) を前提にしている — facade が所有する画像は解決のたびに
    /// 別実体になり、ここで同一と判定されることがない。分類を誤ればこの解放が表示を壊す側に働く。
    /// </remarks>
    /// <param name="cell">対象の Cell</param>
    /// <param name="lease">控える画像のリース。icon なしは null</param>
    private void StoreIcon(CellBase cell, KsImageLease? lease)
    {
        if (!_cellEntries.ContainsKey(cell))
        {
            lease?.Dispose();
            return;
        }

        _icons.TryGetValue(cell, out KsImageLease? previous);
        if (ReferenceEquals(previous, lease))
        {
            return;
        }

        if (lease is null)
        {
            _icons.Remove(cell);
        }
        else
        {
            _icons[cell] = lease;
        }

        if (ReferenceEquals(previous?.Image, lease?.Image))
        {
            previous?.Dispose();
            return;
        }

        if (previous is not null)
        {
            _retiredIcons.Add(previous);
        }

        MarkContentDirty(cell);
    }

    /// <summary>後片付け待ちのリースをまとめて破棄する。</summary>
    /// <remarks>
    /// 外した内容の配信を終えた時点で呼ぶ。破棄の最中に新しい後片付けが積まれても取り違えないよう、
    /// 待ち行列を先に空にしてから破棄する。
    /// 待ち行列を空にした後は取り出したリースが唯一の持ち主になるため、1 件の後片付けが失敗しても
    /// 残りを取りこぼさないよう、全件の破棄を試みてから失敗をまとめて投げる。
    /// 実体側の失敗は既にまとめられた形で届くため、平坦化して失敗 1 件ごとに取り出せる形で渡す。
    /// </remarks>
    private void DisposeRetired()
    {
        List<Exception>? failures = null;

        try
        {
            DisposeRetiredViews();
        }
        catch (Exception exception)
        {
            failures ??= [];
            failures.Add(exception);
        }

        if (_retiredIcons.Count > 0)
        {
            List<KsImageLease> retired = [.. _retiredIcons];
            _retiredIcons.Clear();

            foreach (KsImageLease lease in retired)
            {
                try
                {
                    lease.Dispose();
                }
                catch (Exception exception)
                {
                    failures ??= [];
                    failures.Add(exception);
                }
            }
        }

        if (failures is not null)
        {
            throw new AggregateException(failures).Flatten();
        }
    }

    // ---- 内容更新のバッチ配信 ----

    private void ScheduleFlush()
    {
        if (_flushScheduled || _dispatcher is null)
        {
            return;
        }

        _flushScheduled = _dispatcher.Dispatch(Flush);
    }

    /// <summary>
    /// 予約された時点までに溜まった内容更新をまとめて配信する。
    /// </summary>
    /// <remarks>
    /// Section 単位の差し替えを要する Section は先に送る。可視性が変わった Cell は
    /// 表示対象の並びを作り直させるため単発で送り、残りの内容変更は 1 件なら単発・複数なら
    /// 1 バッチで送る。配信直前に対応表を引き直し、保留中に除去された Section / Cell は捨てる。
    /// accessory の測り直しは内容の配信を終えてから、位置ごとに 1 回だけ送る。
    /// </remarks>
    private void Flush()
    {
        _flushScheduled = false;

        List<CellBase> pending = [.. _dirtyCells];
        HashSet<CellBase> visibilityChanged = new(_visibilityDirtyCells, KsReferenceComparer<CellBase>.Instance);
        List<Section> pendingSections = [.. _replacePendingSections];
        List<KsAccessorySlot> measureSlots = [.. _measureDirtySlots];
        _dirtyCells.Clear();
        _dirtyLookup.Clear();
        _visibilityDirtyCells.Clear();
        _replacePendingSections.Clear();
        _measureDirtySlots.Clear();

        if (_gateway is null)
        {
            // 表示先が無い間は後片付けを待たせる理由がない。
            DisposeRetired();
            return;
        }

        foreach (Section section in pendingSections)
        {
            if (_sectionEntries.TryGetValue(section, out SectionEntry? sectionEntry))
            {
                ReplaceSectionKeepingCellIds(section, sectionEntry);
            }
        }

        List<KsCellUpdate> updates = [];
        foreach (CellBase cell in pending)
        {
            if (!_cellEntries.TryGetValue(cell, out CellEntry? entry))
            {
                continue;
            }

            if (visibilityChanged.Contains(cell))
            {
                _gateway.ReplaceCell(entry.Id, cell);
            }
            else
            {
                updates.Add(new KsCellUpdate(entry.Id, cell));
            }
        }

        if (updates.Count == 1)
        {
            _gateway.ReplaceCell(updates[0].CellId, updates[0].Cell);
        }
        else if (updates.Count > 1)
        {
            _gateway.ReplaceCells(updates);
        }

        foreach (KsAccessorySlot slot in measureSlots)
        {
            if (_accessories.ContainsKey(slot) && TryResolveSectionId(slot, out string? sectionId))
            {
                _gateway.InvalidateAccessoryMeasurement(slot.Target, sectionId);
            }
        }

        // 差し替えが native へ届いた後なら、置き換えられた画像を後片付けしてよい。
        DisposeRetired();
    }

    // ---- ユーザー操作の書き戻し ----

    /// <inheritdoc/>
    public void CommandCellTapped(string cellId)
    {
        if (FindCell(cellId) is CommandCell cell)
        {
            cell.NotifyTapped();
        }
    }

    /// <inheritdoc/>
    public void ButtonCellTapped(string cellId)
    {
        if (FindCell(cellId) is ButtonCell cell)
        {
            cell.NotifyTapped();
        }
    }

    /// <inheritdoc/>
    public void CustomCellTapped(string cellId)
    {
        if (FindCell(cellId) is CustomCell cell)
        {
            cell.NotifyTapped();
        }
    }

    /// <inheritdoc/>
    public void SwitchCellChanged(string cellId, bool isOn)
        => ApplyNativeValue<SwitchCell, bool>(cellId, isOn, cell => cell.On, (cell, value) => cell.On = value);

    /// <inheritdoc/>
    public void CheckboxCellChanged(string cellId, bool isChecked)
        => ApplyNativeValue<CheckboxCell, bool>(
            cellId,
            isChecked,
            cell => cell.Checked,
            (cell, value) => cell.Checked = value);

    /// <inheritdoc/>
    public void SimpleCheckCellChanged(string cellId, bool isChecked)
        => ApplyNativeValue<SimpleCheckCell, bool>(
            cellId,
            isChecked,
            cell => cell.Checked,
            (cell, value) => cell.Checked = value);

    /// <summary>
    /// RadioCell の選択を、同じグループの全ての行へ書き戻す。
    /// </summary>
    /// <remarks>
    /// 選択値はグループで 1 つであり、通知元の行だけを更新すると同グループの他の行の選択表示が
    /// 古い値のまま残る。既に新しい値を持つ行は同値として飛ばされる。
    /// </remarks>
    /// <param name="cellId">通知元の Cell の ID</param>
    /// <param name="value">選択された値</param>
    public void RadioCellSelected(string cellId, string value)
    {
        if (FindCell(cellId) is not RadioCell origin)
        {
            return;
        }

        string groupId = origin.GroupId ?? string.Empty;
        foreach (CellBase cell in Registered())
        {
            if (cell is RadioCell radio && (radio.GroupId ?? string.Empty) == groupId)
            {
                Write(radio, value, target => target.SelectedValue, (target, v) => target.SelectedValue = v);
            }
        }
    }

    /// <inheritdoc/>
    public void EntryCellTextChanged(string cellId, string text)
        => ApplyNativeValue<EntryCell, string>(
            cellId,
            text,
            cell => cell.ValueText,
            (cell, value) => cell.ValueText = value);

    /// <inheritdoc/>
    public void PickerCellSelectionChanged(string cellId, int index)
    {
        if (FindCell(cellId) is not PickerCell cell)
        {
            return;
        }

        Write(cell, (int?)index, target => target.SelectedIndex, (target, value) => target.SelectedIndex = value);
        cell.NotifySelectionCompleted(PickerSelectionMode.Single);
    }

    /// <summary>
    /// PickerCell (複数選択) の選択を書き戻す。
    /// </summary>
    /// <remarks>
    /// 選択位置は集合として比べる。順序や重複だけが違う通知は同値として扱い、再配信の折り返しを
    /// 起こさない。
    /// </remarks>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="indices">選択された位置の並び</param>
    public void PickerCellMultiSelectionChanged(string cellId, IReadOnlyList<int> indices)
    {
        if (FindCell(cellId) is not PickerCell cell)
        {
            return;
        }

        if (!KsWireValues.IndicesEqual(cell.SelectedIndices, indices))
        {
            cell.SelectedIndices = [.. KsWireValues.Indices(indices)];
        }

        cell.NotifySelectionCompleted(PickerSelectionMode.Multiple);
    }

    /// <inheritdoc/>
    public void NumberPickerCellChanged(string cellId, int value)
        => ApplyNativeValue<NumberPickerCell, int>(
            cellId,
            value,
            cell => cell.Number,
            (cell, number) => cell.Number = number);

    /// <summary>TimePickerCell の時刻を書き戻す。解釈できない時刻文字列は捨てる。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="time">新しい時刻 ("HH:mm")</param>
    public void TimePickerCellChanged(string cellId, string time)
    {
        if (!KsWireValues.TryParseTime(time, out TimeSpan parsed))
        {
            return;
        }

        ApplyNativeValue<TimePickerCell, TimeSpan>(
            cellId,
            parsed,
            cell => cell.Time,
            (cell, value) => cell.Time = value);
    }

    /// <summary>DatePickerCell の日付を書き戻す。解釈できない日付文字列は捨てる。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="date">新しい日付 ("yyyy-MM-dd")</param>
    public void DatePickerCellChanged(string cellId, string date)
    {
        if (!KsWireValues.TryParseDate(date, out DateTime parsed))
        {
            return;
        }

        ApplyNativeValue<DatePickerCell, DateTime>(
            cellId,
            parsed,
            cell => cell.Date,
            (cell, value) => cell.Date = value);
    }

    /// <summary>
    /// Native から届いた値を対応する Cell へ書き戻す入口。
    /// </summary>
    /// <remarks>
    /// 対応表から Cell を引き当て、現値と同値なら何もしない。値が違うときだけ書き込み、以後は
    /// 通常の内容更新として Store へコミットされる (maui/ADR-0012)。未知の ID・Cell 種別の
    /// 食い違いでは何も起こらない。
    /// </remarks>
    /// <typeparam name="TCell">書き戻し先の Cell の種別</typeparam>
    /// <typeparam name="TValue">書き戻す値の型</typeparam>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="value">Native から届いた値</param>
    /// <param name="read">現値を読む処理</param>
    /// <param name="write">新しい値を書く処理</param>
    private void ApplyNativeValue<TCell, TValue>(
        string cellId,
        TValue value,
        Func<TCell, TValue> read,
        Action<TCell, TValue> write)
        where TCell : CellBase
    {
        if (FindCell(cellId) is TCell cell)
        {
            Write(cell, value, read, write);
        }
    }

    /// <summary>現値と違うときだけ書き込む。</summary>
    /// <typeparam name="TCell">書き戻し先の Cell の種別</typeparam>
    /// <typeparam name="TValue">書き戻す値の型</typeparam>
    /// <param name="cell">書き戻し先の Cell</param>
    /// <param name="value">Native から届いた値</param>
    /// <param name="read">現値を読む処理</param>
    /// <param name="write">新しい値を書く処理</param>
    private static void Write<TCell, TValue>(
        TCell cell,
        TValue value,
        Func<TCell, TValue> read,
        Action<TCell, TValue> write)
        where TCell : CellBase
    {
        if (EqualityComparer<TValue>.Default.Equals(read(cell), value))
        {
            return;
        }

        write(cell, value);
    }

    /// <summary>対応表に載っている Cell を列挙する。</summary>
    private List<CellBase> Registered() => [.. _cellEntries.Keys];

    /// <summary>対応表に載っている Section を列挙する。</summary>
    private List<Section> Sections() => [.. _sectionEntries.Keys];

    /// <summary>root の指定位置に置かれている View。無ければ null。</summary>
    /// <param name="target">対象の位置</param>
    private View? FindRootAccessoryView(KsAccessoryTarget target)
        => _accessories.TryGetValue(new KsAccessorySlot(target, null), out ViewPlacement? placement)
            ? placement.View
            : null;

    // ---- 対応表の出し入れ ----

    private void RegisterSection(Section section, KsSectionIdentity identity)
    {
        KsWeakPropertySubscription propertySubscription = new(this);
        propertySubscription.Subscribe(section);

        SectionEntry entry = new(identity.SectionId, propertySubscription);
        _sectionEntries[section] = entry;
        _sectionsById[identity.SectionId] = section;

        SubscribeCells(section, entry);

        List<CellBase> cells = Snapshot(section.Cells);
        for (int i = 0; i < cells.Count && i < identity.CellIds.Count; i++)
        {
            RegisterCell(cells[i], identity.CellIds[i]);
            entry.Cells.Add(cells[i]);
        }

        // 輸送 DTO は実体化前に組み立てられるため、accessory の View はここで実体化して送り直す。
        section.AccessoryGuard = this;
        PlaceSectionAccessoryViews(section);
    }

    private void UnregisterSection(Section section, SectionEntry entry)
    {
        entry.PropertySubscription.Unsubscribe();
        entry.CellsSubscription?.Unsubscribe();
        section.AccessoryGuard = null;
        RetireSectionAccessoryViews(section);

        foreach (CellBase cell in entry.Cells)
        {
            if (_cellEntries.TryGetValue(cell, out CellEntry? cellEntry))
            {
                UnregisterCell(cell, cellEntry);
            }
        }

        entry.Cells.Clear();
        _sectionEntries.Remove(section);
        _sectionsById.Remove(entry.Id);
        _replacePendingSections.Remove(section);
    }

    private void RegisterCell(CellBase cell, string cellId)
    {
        KsWeakPropertySubscription propertySubscription = new(this);
        propertySubscription.Subscribe(cell);

        _cellEntries[cell] = new CellEntry(cellId, propertySubscription);
        _cellsById[cellId] = cell;

        ResolveIcon(cell);

        // 輸送 DTO は実体化前に組み立てられるため、内容の View はここで実体化して送り直す。
        if (cell is CustomCell custom)
        {
            custom.ContentGuard = this;
            PlaceCellContent(custom);
        }
    }

    private void UnregisterCell(CellBase cell, CellEntry entry)
    {
        entry.PropertySubscription.Unsubscribe();
        _cellEntries.Remove(cell);
        _cellsById.Remove(entry.Id);

        if (cell is CustomCell custom)
        {
            custom.ContentGuard = null;
            RetireCellContent(custom);
        }

        // native からこの Cell が消えるのは gateway を呼んだ後なので、破棄は後片付け待ちへ回す。
        if (_icons.Remove(cell, out KsImageLease? lease))
        {
            _retiredIcons.Add(lease);
        }

        _iconGenerations.Remove(cell);

        if (_dirtyLookup.Remove(cell))
        {
            _dirtyCells.Remove(cell);
        }

        _visibilityDirtyCells.Remove(cell);
    }

    private void SubscribeCells(Section section, SectionEntry entry)
    {
        entry.CellsSubscription?.Unsubscribe();
        entry.CellsSubscription = null;

        if (section.Cells is not INotifyCollectionChanged observable)
        {
            return;
        }

        KsWeakCollectionSubscription subscription = new(this, section);
        subscription.Subscribe(observable);
        entry.CellsSubscription = subscription;
    }

    // ---- 重複配置の検出 ----

    private void EnsureTreeHasNoDuplicates(IReadOnlyList<Section> sections)
    {
        HashSet<Section> seenSections = new(KsReferenceComparer<Section>.Instance);
        HashSet<CellBase> seenCells = new(KsReferenceComparer<CellBase>.Instance);

        // root の accessory はツリーの外に置かれているため、衝突の相手として先に数えておく。
        HashSet<View> seenViews = new(KsReferenceComparer<View>.Instance);
        AddSeenView(seenViews, FindRootAccessoryView(KsAccessoryTarget.RootHeader));
        AddSeenView(seenViews, FindRootAccessoryView(KsAccessoryTarget.RootFooter));

        foreach (Section section in sections)
        {
            if (section is null)
            {
                throw new InvalidOperationException("A null Section cannot be placed in SettingsView.Root.");
            }

            if (!seenSections.Add(section))
            {
                throw DuplicatePlacement("Section");
            }

            AddSeenView(seenViews, section.HeaderView);
            AddSeenView(seenViews, section.FooterView);

            foreach (CellBase cell in Snapshot(section.Cells))
            {
                if (cell is null)
                {
                    throw new InvalidOperationException("A null cell cannot be placed in Section.Cells.");
                }

                if (!seenCells.Add(cell))
                {
                    throw DuplicatePlacement("Cell");
                }

                if (cell is CustomCell custom)
                {
                    AddSeenView(seenViews, custom.Content);
                }
            }
        }
    }

    private static void EnsureCellsHaveNoDuplicates(IReadOnlyList<CellBase> cells)
    {
        HashSet<CellBase> seen = new(KsReferenceComparer<CellBase>.Instance);
        foreach (CellBase cell in cells)
        {
            if (cell is null)
            {
                throw new InvalidOperationException("A null cell cannot be placed in Section.Cells.");
            }

            if (!seen.Add(cell))
            {
                throw DuplicatePlacement("Cell");
            }
        }
    }

    private void EnsureSectionsAreNotPlaced(IReadOnlyList<Section> sections)
    {
        // 同じまとまりの中で accessory の View が重なっていないことも、native へ触れる前にここで
        // 見る。1 件目を native と対応表へ入れた後に 2 件目で例外にすると、途中まで進んだ配置が
        // 残る。配下 Cell の内容との交差も同じ数えあげで見るため、まとまり全体で 1 つを使い回す。
        // 今の呼び出し元はいずれも設定ツリー全体の重複検査を先に通すため数えあげは重なるが、
        // この検査だけで呼ばれる口ができても成り立つよう、ここでも自分で数える。
        HashSet<View> seen = new(KsReferenceComparer<View>.Instance);
        foreach (Section section in sections)
        {
            if (_sectionEntries.ContainsKey(section))
            {
                throw DuplicatePlacement("Section");
            }

            EnsureViewIsFree(section.HeaderView);
            AddSeenView(seen, section.HeaderView);
            EnsureViewIsFree(section.FooterView);
            AddSeenView(seen, section.FooterView);
            EnsureCellsAreNotPlaced(Snapshot(section.Cells), seen);
        }
    }

    private void EnsureCellsAreNotPlaced(IReadOnlyList<CellBase> cells)
        => EnsureCellsAreNotPlaced(cells, new HashSet<View>(KsReferenceComparer<View>.Instance));

    /// <summary>
    /// これから載せる Cell が、既存の配置ともまとまりの中でも衝突しないことを確かめる。
    /// </summary>
    /// <remarks>
    /// 内容の View の数えあげを呼び出し元から受け取るのは、Section ごと載せるときに accessory の
    /// View との交差もひとまとまりとして見るため。
    /// </remarks>
    /// <param name="cells">これから載せる Cell</param>
    /// <param name="seen">このまとまりで既に数えた View</param>
    private void EnsureCellsAreNotPlaced(IReadOnlyList<CellBase> cells, HashSet<View> seen)
    {
        foreach (CellBase cell in cells)
        {
            if (_cellEntries.ContainsKey(cell))
            {
                throw DuplicatePlacement("Cell");
            }

            if (cell is CustomCell custom)
            {
                EnsureViewIsFree(custom.Content);
                AddSeenView(seen, custom.Content);
            }
        }
    }

    /// <summary>
    /// 差し替え後の Cell 群の内容の View が、互いにも残る配置とも衝突しないことを確かめる。
    /// </summary>
    /// <remarks>
    /// 登録を解く前・gateway を呼ぶ前に全件を通す。途中の 1 件で例外にすると、native の表示と
    /// 対応表と実体の寿命が食い違ったまま残るため。この差し替えで登録を解く Cell が抱えている
    /// View は、解いた後には空くので衝突の相手として数えない。
    /// </remarks>
    /// <param name="cells">差し替え後の Cell</param>
    /// <param name="releasing">この差し替えで登録を解く Cell</param>
    private void EnsureCellContentsAreFree(IReadOnlyList<CellBase> cells, HashSet<CellBase> releasing)
    {
        HashSet<View> seen = new(KsReferenceComparer<View>.Instance);
        foreach (CellBase cell in cells)
        {
            if (cell is not CustomCell custom || custom.Content is not { } content)
            {
                continue;
            }

            AddSeenView(seen, content);

            if (_placedViews.ContainsKey(content))
            {
                throw DuplicatePlacement("View");
            }

            if (_placedContentViews.TryGetValue(content, out CustomCell? holder)
                && !releasing.Contains(holder))
            {
                throw DuplicatePlacement("View");
            }
        }
    }

    /// <summary>この View がどこにも置かれていないことを確かめる。</summary>
    /// <remarks>accessory の位置と Cell の内容は同じ置き場所の集合として扱う。</remarks>
    /// <param name="view">確かめる View。null なら何もしない</param>
    private void EnsureViewIsFree(View? view)
    {
        if (view is not null && (_placedViews.ContainsKey(view) || _placedContentViews.ContainsKey(view)))
        {
            throw DuplicatePlacement("View");
        }
    }

    /// <summary>この View が、これから置く位置とは別の場所に置かれていないことを確かめる。</summary>
    /// <param name="slot">これから置く位置</param>
    /// <param name="view">置く View。null なら何もしない</param>
    private void EnsureAccessoryViewIsNotPlaced(KsAccessorySlot slot, View? view)
    {
        if (view is null)
        {
            return;
        }

        if (_placedViews.TryGetValue(view, out KsAccessorySlot placed) && placed != slot)
        {
            throw DuplicatePlacement("View");
        }

        if (_placedContentViews.ContainsKey(view))
        {
            throw DuplicatePlacement("View");
        }
    }

    /// <summary>この View が、これから置く Cell とは別の場所に置かれていないことを確かめる。</summary>
    /// <param name="cell">これから置く Cell</param>
    /// <param name="view">置く View。null なら何もしない</param>
    private void EnsureContentViewIsNotPlaced(CustomCell cell, View? view)
    {
        if (IsContentViewPlacedElsewhere(cell, view))
        {
            throw DuplicatePlacement("View");
        }
    }

    /// <summary>この View が、これから置く Cell とは別の場所に置かれているかどうか。</summary>
    /// <param name="cell">これから置く Cell</param>
    /// <param name="view">置く View。null なら置かれていない扱い</param>
    private bool IsContentViewPlacedElsewhere(CustomCell cell, View? view)
    {
        if (view is null)
        {
            return false;
        }

        if (_placedViews.ContainsKey(view))
        {
            return true;
        }

        return _placedContentViews.TryGetValue(view, out CustomCell? holder)
            && !ReferenceEquals(holder, cell);
    }

    /// <summary>重複判定の数えあげに View を加える。既に数えられていれば重複として弾く。</summary>
    /// <param name="seen">数えあげ</param>
    /// <param name="view">加える View。null なら何もしない</param>
    private static void AddSeenView(HashSet<View> seen, View? view)
    {
        if (view is not null && !seen.Add(view))
        {
            throw DuplicatePlacement("View");
        }
    }

    private static InvalidOperationException DuplicatePlacement(string kind)
        => new($"The same {kind} instance cannot be placed more than once.");

    // ---- 小道具 ----

    private static List<T> Snapshot<T>(IList<T>? source) => source is null ? [] : [.. source];

    private static List<T> Items<T>(System.Collections.IList? items)
        where T : class
    {
        if (items is null)
        {
            return [];
        }

        List<T> result = new(items.Count);
        foreach (object? item in items)
        {
            if (item is T typed)
            {
                result.Add(typed);
            }
        }

        return result;
    }

    /// <summary>1 箇所に置かれた View 1 件分の状態。</summary>
    /// <param name="view">置かれている View</param>
    private sealed class ViewPlacement(View view)
    {
        /// <summary>置かれている View。</summary>
        public View View { get; } = view;

        /// <summary>実体化済みの platform view。未実体化なら null。</summary>
        public IKsViewLease? Lease { get; set; }
    }

    /// <summary>後片付けを待っている実体と、それが包んでいた View。</summary>
    /// <param name="View">包んでいた View</param>
    /// <param name="Lease">後片付けを待っている実体</param>
    private readonly record struct RetiredView(View View, IKsViewLease Lease);

    /// <summary>対応表に載せた Section 1 件分の状態。</summary>
    /// <param name="id">gateway が採番した Section の ID</param>
    /// <param name="propertySubscription">この Section のプロパティ変更購読</param>
    private sealed class SectionEntry(string id, KsWeakPropertySubscription propertySubscription)
    {
        public string Id { get; } = id;

        public KsWeakPropertySubscription PropertySubscription { get; } = propertySubscription;

        /// <summary>この Section の Cell コレクションの購読。静的なコレクションでは null。</summary>
        public KsWeakCollectionSubscription? CellsSubscription { get; set; }

        /// <summary>この Section 配下として登録済みの Cell (配置順)。</summary>
        public List<CellBase> Cells { get; } = [];
    }

    /// <summary>対応表に載せた Cell 1 件分の状態。</summary>
    /// <param name="id">gateway が採番した Cell の ID</param>
    /// <param name="propertySubscription">この Cell のプロパティ変更購読</param>
    private sealed class CellEntry(string id, KsWeakPropertySubscription propertySubscription)
    {
        public string Id { get; } = id;

        public KsWeakPropertySubscription PropertySubscription { get; } = propertySubscription;
    }
}
