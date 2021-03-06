package ru.fizteh.fivt.students.chernigovsky.storeable;

import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ExtendedStoreableTable extends Table, AutoCloseable {
    Set<Map.Entry<String, Storeable>> getEntrySet();
    int getDiffCount();
    void setColumnTypeList(List<Class<?>> newColumnTypeList);
    boolean isClosed();
}
