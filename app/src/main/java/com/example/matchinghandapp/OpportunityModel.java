package com.example.matchinghandapp;

import java.util.List;

// Data model representing a single volunteering opportunity.
public class OpportunityModel {

    // Fields for opportunity data.
    private String id; // The unique ID of the opportunity document.
    private String title;
    private String description;
    private String date;
    private String duration;
    private String time;
    private String address; // Can be used for organizer name or address.
    private String imageUrl;
    private String createdBy; // The user ID of the NGO that created the opportunity.
    private String location;
    private long timestamp;
    private List<String> tags; // A list of tags associated with the opportunity.

    // Field for storing required skills as a comma-separated string.
    private String skills;

    // Field to hold the match percentage calculated by the Gemini AI.
    private String matchPercentage = "Calculating...";

    // Required empty constructor for Firestore data mapping.
    public OpportunityModel() {}

    // Getters and setters for all fields.
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(String matchPercentage) { this.matchPercentage = matchPercentage; }
}
