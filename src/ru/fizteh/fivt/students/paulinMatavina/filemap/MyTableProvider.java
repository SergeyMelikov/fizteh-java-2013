package ru.fizteh.fivt.students.paulinMatavina.filemap;

import ru.fizteh.fivt.storage.strings.*;
import ru.fizteh.fivt.students.paulinMatavina.utils.*;

public class MyTableProvider implements TableProvider {
    private MultiDbState table;
    
    public MyTableProvider(String dir) throws IllegalArgumentException {
        table = new MultiDbState(dir);
    }
    
    public Table getTable(String name) {
        if (!table.fileExist(name)) {
            return null;
        }
        Command use = new MultiDbUse();
        use.execute(new String[] {name}, table);
        return table;
    }

    public Table createTable(String name) {
        if (table.fileExist(name)) {
            return null;
        }
        Command create = new MultiDbCreate();
        create.execute(new String[] {name}, table);
        return table;
    }

    public void removeTable(String name) {
        if (!table.fileExist(name)) {
            throw new IllegalStateException();
        }
        Command remove = new DbRemove();
        remove.execute(new String[] {name}, table);
        return;
    }
}
