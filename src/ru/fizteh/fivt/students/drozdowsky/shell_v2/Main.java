package ru.fizteh.fivt.students.drozdowsky.shell_v2;

import ru.fizteh.fivt.students.drozdowsky.modes.ModeController;
import ru.fizteh.fivt.students.drozdowsky.modes.Utils;

import java.lang.reflect.Method;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        String[] commandNames = {"cd", "cp", "dir", "mkdir", "mv", "pwd", "rm", "exit"};
        HashMap<String, Method> map = Utils.getMethods(commandNames, ShellCommands.class, PathController.class);
        PathController path = new PathController();
        ModeController<PathController> start = new ModeController<PathController>(path);
        start.execute(map, args);
    }
}
