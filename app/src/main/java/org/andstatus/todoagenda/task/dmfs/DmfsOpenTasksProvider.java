package org.andstatus.todoagenda.task.dmfs;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.andstatus.todoagenda.prefs.EventSource;
import org.andstatus.todoagenda.prefs.FilterMode;
import org.andstatus.todoagenda.prefs.OrderedEventSource;
import org.andstatus.todoagenda.provider.EventProviderType;
import org.andstatus.todoagenda.task.AbstractTaskProvider;
import org.andstatus.todoagenda.task.TaskEvent;
import org.andstatus.todoagenda.task.TaskStatus;
import org.andstatus.todoagenda.util.CalendarIntentUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DmfsOpenTasksProvider extends AbstractTaskProvider {

    public DmfsOpenTasksProvider(EventProviderType type, Context context, int widgetId) {
        super(type, context, widgetId);
    }

    @Override
    public List<TaskEvent> queryTasks() {
        myContentResolver.onQueryEvents();

        Uri uri = DmfsOpenTasksContract.Tasks.PROVIDER_URI;
        String[] projection = {
                DmfsOpenTasksContract.Tasks.COLUMN_LIST_ID,
                DmfsOpenTasksContract.Tasks.COLUMN_ID,
                DmfsOpenTasksContract.Tasks.COLUMN_TITLE,
                DmfsOpenTasksContract.Tasks.COLUMN_DUE_DATE,
                DmfsOpenTasksContract.Tasks.COLUMN_START_DATE,
                DmfsOpenTasksContract.Tasks.COLUMN_COLOR,
                DmfsOpenTasksContract.Tasks.COLUMN_STATUS,
        };
        String where = getWhereClause();

        return myContentResolver.foldEvents(uri, projection, where, null, null,
                new ArrayList<>(), tasks -> cursor -> {
                    TaskEvent task = createTask(cursor);
                    if (matchedFilter(task)) {
                        tasks.add(task);
                    }
                    return tasks;
                });
    }

    private String getWhereClause() {
        StringBuilder whereBuilder = new StringBuilder();

        if (getFilterMode() == FilterMode.NORMAL_FILTER) {
            whereBuilder.append(DmfsOpenTasksContract.Tasks.COLUMN_STATUS).append(NOT_EQUALS)
                .append(DmfsOpenTasksContract.Tasks.STATUS_COMPLETED);

            whereBuilder.append(AND_BRACKET)
                .append(DmfsOpenTasksContract.Tasks.COLUMN_DUE_DATE).append(LTE).append(mEndOfTimeRange.getMillis())
                .append(OR)
                .append(OPEN_BRACKET)
                .append(DmfsOpenTasksContract.Tasks.COLUMN_DUE_DATE).append(IS_NULL)
                .append(AND_BRACKET)
                .append(DmfsOpenTasksContract.Tasks.COLUMN_START_DATE).append(LTE).append(mEndOfTimeRange.getMillis())
                .append(OR)
                .append(DmfsOpenTasksContract.Tasks.COLUMN_START_DATE).append(IS_NULL)
                .append(CLOSING_BRACKET)
                .append(CLOSING_BRACKET)
                .append(CLOSING_BRACKET);
        }

        Set<String> taskLists = new HashSet<>();
        for (OrderedEventSource orderedSource: getSettings().getActiveEventSources(type)) {
            taskLists.add(Integer.toString(orderedSource.source.getId()));
        }
        if (!taskLists.isEmpty()) {
            if (whereBuilder.length() > 0) {
                whereBuilder.append(AND);
            }
            whereBuilder.append(DmfsOpenTasksContract.Tasks.COLUMN_LIST_ID);
            whereBuilder.append(" IN ( ");
            whereBuilder.append(TextUtils.join(",", taskLists));
            whereBuilder.append(CLOSING_BRACKET);
        }

        return whereBuilder.toString();
    }

    private TaskEvent createTask(Cursor cursor) {
        OrderedEventSource source = getSettings()
                .getActiveEventSource(type,
                        cursor.getInt(cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_LIST_ID)));
        TaskEvent task = new TaskEvent(getSettings(), zone);
        task.setEventSource(source);
        task.setId(cursor.getLong(cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_ID)));
        task.setTitle(cursor.getString(cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_TITLE)));

        int dueDateIdx = cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_DUE_DATE);
        Long dueMillis = null;
        if (!cursor.isNull(dueDateIdx)) {
            dueMillis = cursor.getLong(dueDateIdx);
        }
        int startDateIdx = cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_START_DATE);
        Long startMillis = null;
        if (!cursor.isNull(startDateIdx)) {
            startMillis = cursor.getLong(startDateIdx);
        }
        task.setDates(startMillis, dueMillis);

        task.setColor(getAsOpaque(cursor.getInt(cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_COLOR))));
        task.setStatus(loadStatus(cursor));

        return task;
    }

    private TaskStatus loadStatus(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex(DmfsOpenTasksContract.Tasks.COLUMN_STATUS);
        if (columnIndex < 0) return TaskStatus.UNKNOWN;

        switch (cursor.getInt(columnIndex)) {
            case 0:
                return TaskStatus.NEEDS_ACTION;
            case 1:
                return TaskStatus.IN_PROGRESS;
            case 2:
                return TaskStatus.COMPLETED;
            case 3:
                return TaskStatus.CANCELLED;
            default:
                return TaskStatus.UNKNOWN;
        }
    }

    @Override
    public List<EventSource> fetchAvailableSources() {
        String[] projection = {
                DmfsOpenTasksContract.TaskLists.COLUMN_ID,
                DmfsOpenTasksContract.TaskLists.COLUMN_NAME,
                DmfsOpenTasksContract.TaskLists.COLUMN_COLOR,
                DmfsOpenTasksContract.TaskLists.COLUMN_ACCOUNT_NAME,
        };

        return myContentResolver.foldAvailableSources(
                DmfsOpenTasksContract.TaskLists.PROVIDER_URI,
                projection,
                new ArrayList<>(),
                eventSources -> cursor -> {
                    int indId = cursor.getColumnIndex(DmfsOpenTasksContract.TaskLists.COLUMN_ID);
                    int indTitle = cursor.getColumnIndex(DmfsOpenTasksContract.TaskLists.COLUMN_NAME);
                    int indColor = cursor.getColumnIndex(DmfsOpenTasksContract.TaskLists.COLUMN_COLOR);
                    int indSummary = cursor.getColumnIndex(DmfsOpenTasksContract.TaskLists.COLUMN_ACCOUNT_NAME);
                    EventSource source = new EventSource(type, cursor.getInt(indId), cursor.getString(indTitle),
                            cursor.getString(indSummary), cursor.getInt(indColor), true);
                    eventSources.add(source);
                    return eventSources;
                });
    }

    @Override
    public Intent createViewEventIntent(TaskEvent event) {
        Intent intent = CalendarIntentUtil.createViewIntent();
        intent.setData(ContentUris.withAppendedId(DmfsOpenTasksContract.Tasks.PROVIDER_URI, event.getId()));
        return intent;
    }
}
