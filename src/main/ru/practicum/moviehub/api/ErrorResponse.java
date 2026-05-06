package ru.practicum.moviehub.api;

public class ErrorResponse {
    String shortDescription;
    String[] details;

    public ErrorResponse(String shortDescription, String[] details) {
        this.shortDescription = shortDescription;
        this.details = details;
    }
}