package ru.fizteh.fivt.students.kislenko.shell;

import java.io.IOException;

public interface Command {
    public String getName();

    public void run(State state, String[] args) throws IOException;
}