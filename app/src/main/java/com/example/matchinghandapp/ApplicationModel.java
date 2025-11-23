package com.example.matchinghandapp;

// Data model representing a volunteer's application for an opportunity.
public class ApplicationModel {
    // Fields for application data.
    private String docId, applicantId, organizerId, opportunityId;
    private String title, location, date, imageUrl, status;
    private String phone, motivation, experience;
    private String opportunityTitle;

    // Default constructor required for Firestore data mapping.
    public ApplicationModel() {}

    // Getter for the opportunity title.
    public String getOpportunityTitle() { return opportunityTitle; }
    // Setter for the opportunity title.
    public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }

    // Getter for the document ID.
    public String getDocId() { return docId; }
    // Setter for the document ID.
    public void setDocId(String docId) { this.docId = docId; }

    // Getter for the applicant's ID.
    public String getApplicantId() { return applicantId; }
    // Setter for the applicant's ID.
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }

    // Getter for the organizer's ID.
    public String getOrganizerId() { return organizerId; }
    // Setter for the organizer's ID.
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    // Getter for the opportunity ID.
    public String getOpportunityId() { return opportunityId; }
    // Setter for the opportunity ID.
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }

    // Getter for the application title (often the opportunity title).
    public String getTitle() { return title; }
    // Setter for the application title.
    public void setTitle(String title) { this.title = title; }

    // Getter for the location.
    public String getLocation() { return location; }
    // Setter for the location.
    public void setLocation(String location) { this.location = location; }

    // Getter for the date.
    public String getDate() { return date; }
    // Setter for the date.
    public void setDate(String date) { this.date = date; }

    // Getter for the image URL.
    public String getImageUrl() { return imageUrl; }
    // Setter for the image URL.
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // Getter for the application status (e.g., "Pending", "Accepted").
    public String getStatus() { return status; }
    // Setter for the application status.
    public void setStatus(String status) { this.status = status; }

    // Getter for the applicant's phone number.
    public String getPhone() { return phone; }
    // Setter for the applicant's phone number.
    public void setPhone(String phone) { this.phone = phone; }

    // Getter for the applicant's motivation statement.
    public String getMotivation() { return motivation; }
    // Setter for the applicant's motivation statement.
    public void setMotivation(String motivation) { this.motivation = motivation; }

    // Getter for the applicant's experience.
    public String getExperience() { return experience; }
    // Setter for the applicant's experience.
    public void setExperience(String experience) { this.experience = experience; }
}
