package com.example.matchinghandapp;

import java.util.List;

public class OpportunityModel {

    private String id;
    private String title;
    private String description;
    private String date;
    private String duration;
    private String time;
    private String address;
    private String imageUrl;
    private String createdBy;
    private String location;
    private long timestamp;
    private List<String> tags;

    // 🔹 Store skills as a comma-separated string (e.g. "Teaching, Communication, Leadership")
    private String skills;

    // 🔹 Used to display Gemini-calculated match percentage
    private String matchPercentage = "Calculating...";

    // Required empty constructor for Firestore
    public OpportunityModel() {}

    // ✅ Getters and Setters
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

    // 🔹 Skills field
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    // 🔹 Match percentage for Gemini result
    public String getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(String matchPercentage) { this.matchPercentage = matchPercentage; }
}
