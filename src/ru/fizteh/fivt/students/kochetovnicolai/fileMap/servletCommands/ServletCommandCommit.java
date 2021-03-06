package ru.fizteh.fivt.students.kochetovnicolai.fileMap.servletCommands;


import ru.fizteh.fivt.students.kochetovnicolai.fileMap.DistributedTable;
import ru.fizteh.fivt.students.kochetovnicolai.fileMap.TableManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServletCommandCommit extends ServletCommand {

    public ServletCommandCommit(TableManager manager) {
        super(manager);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        DistributedTable table = getTable(req, resp);
        if (table == null) {
            return;
        }

        int diff;
        try {
            table.useTransaction(sessionID);
            diff = table.commit();
            table.removeTransaction(sessionID);
        } catch (IOException|IllegalStateException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        } finally {
            manager.deleteTableByID(sessionID);
        }

        resp.setStatus(HttpServletResponse.SC_OK);

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF8");
        resp.getWriter().println("diff=" + diff);
    }
}
