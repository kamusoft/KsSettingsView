using System.Collections.Generic;
using System.Diagnostics;

namespace KsSettingsView.Internals;

/// <summary>
/// スクロール命令の対象を解決し、gateway へ渡す経路。
/// </summary>
/// <remarks>
/// 命令は Store を経由しない更新として gateway の専用メソッドへ渡す。対象は明示 ID を先に、
/// 次に ItemsSource の項目で表示順に探し、見つかった要素の gateway 採番 ID で命令する
/// (maui/ADR-0029)。見つからないとき・gateway 未接続のときは gateway を呼ばない。Native Host が
/// 解放されている間の命令は gateway へ渡し、何もしない扱いは Bridge に委ねる (maui/ADR-0030)。
/// 非表示の要素も解決の対象に含め、表示しないことによる何もしない扱いは Native に委ねる。
/// </remarks>
internal sealed partial class KsSettingsController
{
    /// <summary>Cell への命令を解決して gateway へ渡す。</summary>
    /// <param name="target">Cell の明示 ID、または Cell を生成した項目</param>
    /// <param name="position">行を合わせる位置</param>
    /// <param name="animated">アニメーションするか</param>
    public void ScrollToCell(object target, ScrollPosition position, bool animated)
    {
        if (_gateway is not { } gateway)
        {
            WarnNotConnected();
            return;
        }

        if (ResolveCellId(target) is not { } cellId)
        {
            WarnUnresolved("cell", target);
            return;
        }

        gateway.ScrollToCell(cellId, position, animated);
    }

    /// <summary>Section への命令を解決して gateway へ渡す。</summary>
    /// <param name="target">Section の明示 ID、または Section を生成した項目</param>
    /// <param name="position">Section の範囲を合わせる位置</param>
    /// <param name="animated">アニメーションするか</param>
    public void ScrollToSection(object target, ScrollPosition position, bool animated)
    {
        if (_gateway is not { } gateway)
        {
            WarnNotConnected();
            return;
        }

        if (ResolveSectionId(target) is not { } sectionId)
        {
            WarnUnresolved("section", target);
            return;
        }

        gateway.ScrollToSection(sectionId, position, animated);
    }

    /// <summary>内容の最上端への命令を gateway へ渡す。</summary>
    /// <param name="animated">アニメーションするか</param>
    public void ScrollToStart(bool animated)
    {
        if (_gateway is not { } gateway)
        {
            WarnNotConnected();
            return;
        }

        gateway.ScrollToStart(animated);
    }

    /// <summary>内容の最下端への命令を gateway へ渡す。</summary>
    /// <param name="animated">アニメーションするか</param>
    public void ScrollToEnd(bool animated)
    {
        if (_gateway is not { } gateway)
        {
            WarnNotConnected();
            return;
        }

        gateway.ScrollToEnd(animated);
    }

    /// <summary>命令の対象の Cell を、gateway 採番の ID へ解決する。</summary>
    /// <remarks>
    /// 明示 ID が等しい Cell を表示順に探し、無ければ Section ごとに、その Section の ItemsSource の
    /// 項目から生成した Cell を探す。いずれも対応表に登録済み (表示へ送った) の要素だけを採る。
    /// </remarks>
    /// <param name="target">Cell の明示 ID、または Cell を生成した項目</param>
    private string? ResolveCellId(object? target)
    {
        if (target is null)
        {
            return null;
        }

        List<Section> sections = SectionsInDisplayOrder();
        foreach (Section section in sections)
        {
            foreach (CellBase? cell in section.Cells ?? [])
            {
                if (cell is not null && Equals(cell.CellId, target) && FindCellId(cell) is { } id)
                {
                    return id;
                }
            }
        }

        foreach (Section section in sections)
        {
            foreach (CellBase cell in section.FindGeneratedCells(target))
            {
                if (FindCellId(cell) is { } id)
                {
                    return id;
                }
            }
        }

        return null;
    }

    /// <summary>命令の対象の Section を、gateway 採番の ID へ解決する。</summary>
    /// <remarks>
    /// 明示 ID が等しい Section を表示順に探し、無ければ SettingsView の ItemsSource の項目から
    /// 生成した Section を探す。いずれも対応表に登録済みの要素だけを採る。
    /// </remarks>
    /// <param name="target">Section の明示 ID、または Section を生成した項目</param>
    private string? ResolveSectionId(object? target)
    {
        if (target is null)
        {
            return null;
        }

        foreach (Section section in SectionsInDisplayOrder())
        {
            if (Equals(section.SectionId, target) && FindSectionId(section) is { } id)
            {
                return id;
            }
        }

        foreach (Section section in owner.FindGeneratedSections(target))
        {
            if (FindSectionId(section) is { } id)
            {
                return id;
            }
        }

        return null;
    }

    /// <summary>設定ツリーの Section を表示順に並べる。</summary>
    private List<Section> SectionsInDisplayOrder()
    {
        List<Section> sections = [];
        if (_root is null)
        {
            return sections;
        }

        foreach (Section? section in _root)
        {
            if (section is not null)
            {
                sections.Add(section);
            }
        }

        return sections;
    }

    private static void WarnNotConnected()
        => Debug.WriteLine(
            "KsSettingsView: ignored a scroll command because the settings view has not created its native host yet.");

    private static void WarnUnresolved(string kind, object? target)
        => Debug.WriteLine(
            $"KsSettingsView: ignored a scroll command because no {kind} matches the target '{target}'.");
}
