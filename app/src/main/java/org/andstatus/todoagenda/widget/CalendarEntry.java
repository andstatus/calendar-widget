package org.andstatus.todoagenda.widget;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;

import org.andstatus.todoagenda.R;
import org.andstatus.todoagenda.calendar.CalendarEvent;
import org.andstatus.todoagenda.prefs.InstanceSettings;
import org.andstatus.todoagenda.prefs.OrderedEventSource;
import org.andstatus.todoagenda.util.DateUtil;
import org.joda.time.DateTime;

import static org.andstatus.todoagenda.util.MyClock.isDateDefined;

public class CalendarEntry extends WidgetEntry<CalendarEntry> {

    private static final String TWELVE = "12";
    private static final String AUTO = "auto";
    private static final String ARROW = "→";
    private static final String SPACE = " ";
    private static final String EMPTY_STRING = "";
    static final String SPACE_DASH_SPACE = " - ";

    private boolean allDay;
    private CalendarEvent event;

    public static CalendarEntry fromEvent(InstanceSettings settings, CalendarEvent event, DateTime entryDate) {
        CalendarEntry entry = new CalendarEntry(settings, entryDate);
        entry.allDay = event.isAllDay();
        entry.event = event;
        return entry;
    }

    private CalendarEntry(InstanceSettings settings, DateTime entryDate) {
        super(settings, WidgetEntryPosition.ENTRY_DATE, entryDate);
    }

    @Nullable
    @Override
    public DateTime getEndDate() {
        return event.getEndDate();
    }

    @Override
    public String getTitle() {
        String title = event.getTitle();
        if (TextUtils.isEmpty(title)) {
            title = getContext().getResources().getString(R.string.no_title);
        }
        return title;
    }

    public int getColor() {
        return event.getColor();
    }

    public boolean isAllDay() {
        return allDay;
    }

    @Override
    public String getLocation() {
        return event.getLocation();
    }

    public boolean isAlarmActive() {
        return event.isAlarmActive();
    }

    public boolean isRecurring() {
        return event.isRecurring();
    }

    public boolean isPartOfMultiDayEvent() {
        return getEvent().isPartOfMultiDayEvent();
    }

    public boolean isStartOfMultiDayEvent() {
        return isPartOfMultiDayEvent() && !getEvent().getStartDate().isBefore(entryDate);
    }

    public boolean isEndOfMultiDayEvent() {
        return isPartOfMultiDayEvent() && isLastEntryOfEvent();
    }

    public boolean spansOneFullDay() {
        return entryDate.plusDays(1).isEqual(event.getEndDate());
    }

    public CalendarEvent getEvent() {
        return event;
    }

    public String getEventTimeString() {
        return hideEventTime() ? "" : createTimeSpanString(getContext());
    }

    private boolean hideEventTime() {
        return spansOneFullDay() && !(isStartOfMultiDayEvent() || isEndOfMultiDayEvent()) ||
                isAllDay();
    }

    String getLocationString() {
        return hideLocation() ? "" : getLocation();
    }

    private boolean hideLocation() {
        return getLocation().isEmpty() || !getSettings().getShowLocation();
    }

    private String createTimeSpanString(Context context) {
        if (isAllDay() && !getSettings().getFillAllDayEvents()) {
            DateTime dateTime = getEvent().getEndDate().minusDays(1);
            return ARROW + SPACE + DateUtil.createDateString(getSettings(), dateTime);
        } else {
            return createTimeStringForCalendarEntry(context);
        }
    }

    private String createTimeStringForCalendarEntry(Context context) {
        String startStr;
        String endStr;
        String separator = SPACE_DASH_SPACE;
        if (!isDateDefined(entryDate) || (isPartOfMultiDayEvent() && DateUtil.isMidnight(entryDate)
                && !isStartOfMultiDayEvent())) {
            startStr = ARROW;
            separator = SPACE;
        } else {
            startStr = createTimeString(context, entryDate);
        }
        if (getSettings().getShowEndTime()) {
            if (!isDateDefined(event.getEndDate()) || (isPartOfMultiDayEvent() && !isLastEntryOfEvent())) {
                endStr = ARROW;
                separator = SPACE;
            } else {
                endStr = createTimeString(context, event.getEndDate());
            }
        } else {
            separator = EMPTY_STRING;
            endStr = EMPTY_STRING;
        }

        if (startStr.equals(endStr)) {
            return startStr;
        }

        return startStr + separator + endStr;
    }

    private String createTimeString(Context context, @Nullable DateTime time) {
        if (!isDateDefined(time)) return EMPTY_STRING;

        String dateFormat = getSettings().getDateFormat();
        if (!DateFormat.is24HourFormat(context) && dateFormat.equals(AUTO)
                || dateFormat.equals(TWELVE)) {
            return DateUtil.formatDateTime(getSettings(), time,
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_12HOUR);
        }
        return DateUtil.formatDateTime(getSettings(), time,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR);
    }

    public Context getContext() {
        return event.getContext();
    }

    public InstanceSettings getSettings() {
        return event.getSettings();
    }

    @Override
    public OrderedEventSource getSource() {
        return event.getEventSource();
    }

    @Override
    public String toString() {
        return super.toString() + " CalendarEntry ["
                + "endDate=" + getEndDate()
                + ", allDay=" + allDay
                + ", time=" + getEventTimeString()
                + ", location=" + getLocationString()
                + ", event=" + event
                + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CalendarEntry that = (CalendarEntry) o;
        return event.equals(that.event) && entryDate.equals(that.entryDate);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result += 31 * event.hashCode();
        result += 31 * entryDate.hashCode();
        return result;
    }
}
