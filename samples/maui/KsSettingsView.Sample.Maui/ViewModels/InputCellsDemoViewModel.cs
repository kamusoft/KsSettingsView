using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Windows.Input;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>
/// 入力 Cell 5 種デモの状態と、直近のイベント表示を供給する ViewModel。
/// </summary>
/// <remarks>
/// 各プロパティは入力 Cell と双方向にバインドされる。「通知先メンバー」は選択完了ごと、
/// それ以外は値が実際に変わったときに「&lt;Cell の title&gt; → &lt;変更後の値&gt;」を
/// <see cref="LastEvent"/> へ記録する。
/// </remarks>
public sealed class InputCellsDemoViewModel : SampleViewModel
{
    private static readonly CultureInfo s_culture = CultureInfo.InvariantCulture;

    private string _lastEvent = "(none)";
    private string _userName = "Tanaka Taro";
    private string _email = "tanaka.taro@example.com";
    private string _phone = "090-0000-0000";
    private string _password = "secret123";
    private string _nickname = string.Empty;
    private string _displayName = string.Empty;
    private int? _themeIndex = 0;
    private IList<int> _notificationSelection = [0, 2];
    private SampleMember? _assignee = SampleMember.NotificationTargets[0];
    private IList<int> _memberSelection = [0, 2];
    private int _size = 30;
    private TimeSpan _alarmTime = new(7, 30, 0);
    private TimeSpan _bedTime = new(22, 15, 0);
    private DateTime _birthday = new(1990, 1, 1);
    private DateTime _reservation = new(2026, 6, 1);
    private string _memo = string.Empty;
    private string _signature = string.Empty;

    /// <summary>入力 Cell デモの状態を作る。</summary>
    public InputCellsDemoViewModel()
    {
        MemberSelectionCompletedCommand = new SampleCommand(parameter =>
        {
            IList members = parameter as IList ?? Array.Empty<object>();
            LastEvent = $"通知先メンバー → {FormatMembers(members)}";
        });
    }

    /// <summary>画面上部に出す直近のイベント。</summary>
    public string LastEvent
    {
        get => _lastEvent;
        private set => Set(ref _lastEvent, value);
    }

    /// <summary>EntryCell「名前」の入力値。</summary>
    public string UserName
    {
        get => _userName;
        set
        {
            if (Set(ref _userName, value))
            {
                LastEvent = $"名前 → {value}";
            }
        }
    }

    /// <summary>EntryCell「メール」の入力値。</summary>
    public string Email
    {
        get => _email;
        set
        {
            if (Set(ref _email, value))
            {
                LastEvent = $"メール → {value}";
            }
        }
    }

    /// <summary>EntryCell「電話」の入力値。</summary>
    public string Phone
    {
        get => _phone;
        set
        {
            if (Set(ref _phone, value))
            {
                LastEvent = $"電話 → {value}";
            }
        }
    }

    /// <summary>EntryCell「パスワード」の入力値。</summary>
    public string Password
    {
        get => _password;
        set
        {
            if (Set(ref _password, value))
            {
                LastEvent = $"パスワード → {Mask(value)}";
            }
        }
    }

    /// <summary>EntryCell「ニックネーム (callback)」の入力値。</summary>
    public string Nickname
    {
        get => _nickname;
        set
        {
            if (Set(ref _nickname, value))
            {
                LastEvent = $"ニックネーム (callback) → {value}";
            }
        }
    }

    /// <summary>EntryCell「表示名」の入力値。</summary>
    public string DisplayName
    {
        get => _displayName;
        set
        {
            if (Set(ref _displayName, value))
            {
                LastEvent = $"表示名 → {value}";
            }
        }
    }

    /// <summary>PickerCell「テーマ」の選択候補。</summary>
    public IList<string> Themes { get; } = ["ライト", "ダーク", "自動"];

    /// <summary>PickerCell「テーマ」の選択位置。</summary>
    public int? ThemeIndex
    {
        get => _themeIndex;
        set
        {
            if (Set(ref _themeIndex, value))
            {
                LastEvent = $"テーマ → {FormatTheme(value)}";
            }
        }
    }

    /// <summary>PickerCell「通知種別」の選択候補。</summary>
    public IList<string> NotificationTypes { get; } = ["メール", "プッシュ", "SMS", "アプリ内", "電話"];

    /// <summary>
    /// PickerCell「通知種別」の選択位置の並び。
    /// </summary>
    /// <remarks>
    /// 選択位置は集合として比べる。同じ顔ぶれを別の並びで書き戻されたときにイベントを
    /// 記録しないようにするため、既定の等値比較は使わない。
    /// </remarks>
    public IList<int> NotificationSelection
    {
        get => _notificationSelection;
        set
        {
            IList<int> next = value ?? [];
            if (SameSelection(_notificationSelection, next))
            {
                return;
            }

            _notificationSelection = next;
            OnPropertyChanged();
            LastEvent = $"通知種別 → {FormatSelection(next)}";
        }
    }

    /// <summary>PickerCell「担当者」「通知先メンバー」の選択候補 (object 候補)。</summary>
    public List<SampleMember> Members { get; } = [.. SampleMember.NotificationTargets];

    /// <summary>PickerCell「担当者」で選ばれた要素そのもの。</summary>
    public SampleMember? Assignee
    {
        get => _assignee;
        set
        {
            if (Set(ref _assignee, value))
            {
                LastEvent = $"担当者 → {value?.Name ?? "(未選択)"}";
            }
        }
    }

    /// <summary>PickerCell「通知先メンバー」の選択位置の並び。</summary>
    public IList<int> MemberSelection
    {
        get => _memberSelection;
        set
        {
            IList<int> next = value ?? [];
            if (SameSelection(_memberSelection, next))
            {
                return;
            }

            _memberSelection = next;
            OnPropertyChanged();
        }
    }

    /// <summary>PickerCell「通知先メンバー」の選択完了を受け取る Command。</summary>
    public ICommand MemberSelectionCompletedCommand { get; }

    /// <summary>NumberPickerCell「サイズ」の値。</summary>
    public int Size
    {
        get => _size;
        set
        {
            if (Set(ref _size, value))
            {
                LastEvent = $"サイズ → {value} px";
            }
        }
    }

    /// <summary>TimePickerCell「アラーム」の時刻。</summary>
    public TimeSpan AlarmTime
    {
        get => _alarmTime;
        set
        {
            if (Set(ref _alarmTime, value))
            {
                LastEvent = $"アラーム → {value:hh\\:mm}";
            }
        }
    }

    /// <summary>TimePickerCell「就寝」の時刻。</summary>
    public TimeSpan BedTime
    {
        get => _bedTime;
        set
        {
            if (Set(ref _bedTime, value))
            {
                LastEvent = $"就寝 → {FormatTime12Hour(value)}";
            }
        }
    }

    /// <summary>DatePickerCell「誕生日」の日付。</summary>
    public DateTime Birthday
    {
        get => _birthday;
        set
        {
            if (Set(ref _birthday, value))
            {
                LastEvent = $"誕生日 → {FormatDate(value)}";
            }
        }
    }

    /// <summary>DatePickerCell「誕生日」で選べる最小の日付。</summary>
    public DateTime BirthdayMinimum { get; } = new(1900, 1, 1);

    /// <summary>DatePickerCell「誕生日」で選べる最大の日付 (今日)。</summary>
    public DateTime BirthdayMaximum { get; } = DateTime.Today;

    /// <summary>DatePickerCell「予約日」の日付。</summary>
    public DateTime Reservation
    {
        get => _reservation;
        set
        {
            if (Set(ref _reservation, value))
            {
                LastEvent = $"予約日 → {FormatDate(value)}";
            }
        }
    }

    /// <summary>EntryCell「メモ」(下部配置・キーボード回避検証用) の入力値。</summary>
    public string Memo
    {
        get => _memo;
        set
        {
            if (Set(ref _memo, value))
            {
                LastEvent = $"メモ → {value}";
            }
        }
    }

    /// <summary>EntryCell「署名」(最下部・キーボード回避検証用) の入力値。</summary>
    public string Signature
    {
        get => _signature;
        set
        {
            if (Set(ref _signature, value))
            {
                LastEvent = $"署名 → {value}";
            }
        }
    }

    /// <summary>パスワードの表示形式 (入力欄と同じマスク表現)。</summary>
    private static string Mask(string value) => new('•', value?.Length ?? 0);

    /// <summary>12時間制の時刻の表示形式 (Cell に指定した "h:mm a" と同じ並び。AM/PM 表記は Cell の valueText と同じく端末 Locale に揃える)。</summary>
    private static string FormatTime12Hour(TimeSpan value)
        => DateTime.MinValue.Add(value).ToString("h:mm tt", CultureInfo.CurrentCulture);

    /// <summary>日付の表示形式。</summary>
    private static string FormatDate(DateTime value) => value.ToString("yyyy/MM/dd", s_culture);

    /// <summary>2 つの選択位置の並びが集合として等しいかどうか。</summary>
    private static bool SameSelection(IList<int> left, IList<int> right)
        => left.Count == right.Count && !left.Except(right).Any() && !right.Except(left).Any();

    /// <summary>選択要素の並びに対応する名前。未選択では "(未選択)"。</summary>
    private static string FormatMembers(IList members)
    {
        string[] names = [.. members.Cast<object?>().OfType<SampleMember>().Select(member => member.Name)];

        return names.Length == 0 ? "(未選択)" : string.Join(", ", names);
    }

    /// <summary>選択位置に対応するテーマ名。未選択・範囲外では "(未選択)"。</summary>
    private string FormatTheme(int? index)
        => index is int position && position >= 0 && position < Themes.Count
            ? Themes[position]
            : "(未選択)";

    /// <summary>選択位置の並びに対応する通知種別名。未選択では "(未選択)"。</summary>
    private string FormatSelection(IList<int> indices)
    {
        string[] labels =
        [
            .. indices
                .Order()
                .Where(index => index >= 0 && index < NotificationTypes.Count)
                .Select(index => NotificationTypes[index]),
        ];

        return labels.Length == 0 ? "(未選択)" : string.Join(", ", labels);
    }
}
