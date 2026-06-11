package com.dexer.aquanaut.client;

import com.dexer.aquanaut.common.notebook.NotebookProgress;

public final class ClientNotebookData {

    private static NotebookProgress notebook = NotebookProgress.EMPTY;

    private ClientNotebookData() {
    }

    public static void setFromData(NotebookProgress data) {
        notebook = data;
    }

    public static void setFromSerialized(String serializedData) {
        notebook = NotebookProgress.deserialize(serializedData);
    }

    public static NotebookProgress getNotebook() {
        return notebook;
    }
}
