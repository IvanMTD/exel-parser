package ru.morgan.exelparser.models;

public enum Role {
    USER("Пользователь"),
    MANAGER("Менеджер"),
    ADMIN("Администратор");

    private final String name;
    Role(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
