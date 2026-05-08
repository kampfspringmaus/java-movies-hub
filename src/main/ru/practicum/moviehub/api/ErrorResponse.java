package ru.practicum.moviehub.api;

import java.util.ArrayList;

public class ErrorResponse {
    private String shortDescription;
    private ArrayList<String> details;

    public ErrorResponse(String shortDescription, ArrayList<String> details) {
        this.shortDescription = shortDescription;
        this.details = details;
    }
}