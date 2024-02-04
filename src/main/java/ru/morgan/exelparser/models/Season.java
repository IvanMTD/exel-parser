package ru.morgan.exelparser.models;

public enum Season {
    WINTER("Зимний"),
    SUMMER("Летний"),
    ALL("Всесезонный");

    private final String title;

    Season(String title){
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}