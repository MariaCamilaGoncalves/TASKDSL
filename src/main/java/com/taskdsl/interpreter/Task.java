package com.taskdsl.interpreter;

/** Estado em tempo de execução de uma tarefa criada via comando 'task'. */
public class Task {
    public final String name;
    public int priority;
    public String assignee; // null se ninguém foi designado ainda
    public boolean completed;

    public Task(String name, int priority) {
        this.name = name;
        this.priority = priority;
        this.assignee = null;
        this.completed = false;
    }

    @Override
    public String toString() {
        String status = completed ? "concluída" : "pendente";
        String quem = assignee != null ? assignee : "(sem responsável)";
        return "- \"" + name + "\" | prioridade: " + priority +
                " | responsável: " + quem + " | status: " + status;
    }
}
