# Matching Hands - Volunteer Matching Android App

Matching Hands is an Android application designed to connect volunteers with volunteering opportunities provided by NGOs. The app features distinct user roles (Volunteer, NGO, Admin), AI-powered opportunity matching, and real-time chat functionality.

## Features

-   **User Roles:** Separate dashboards and functionalities for Volunteers, NGOs, and Admins.
-   **Opportunity Management:** NGOs can create, edit, and delete volunteering opportunities.
-   **Volunteer Applications:** Volunteers can apply for opportunities.
-   **AI-Powered Matching:** Utilizes the Gemini API to calculate a match percentage between a volunteer's profile and an opportunity.
-   **Real-time Chat:** In-app messaging between users.
-   **Admin Dashboard:** Admins can manage users and their roles/status.
-   **Bookmarking:** Volunteers can save opportunities for later.

## Prerequisites

-   [Android Studio](https://developer.android.com/studio) (latest stable version recommended)
-   [Git](https://git-scm.com/)

## Setup Instructions

Follow these steps to get the project running on your local machine for development and testing purposes.

### 1. Clone the Repository

Clone this repository to your local machine using your preferred method.

```bash
git clone <YOUR_REPOSITORY_URL>
cd MatchingHands
```

### 2. Open in Android Studio

1.  Open Android Studio.
2.  Click on **File -> Open**.
3.  Navigate to the cloned `MatchingHands` directory and select it.
4.  Allow Android Studio to build the project and sync Gradle dependencies.

### 3. Firebase Setup (Crucial)

This project is heavily integrated with Google Firebase. You must create your own Firebase project to run the app.

**Step 1: Create a Firebase Project**
1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Click **Add project** and follow the on-screen instructions to create a new project.

**Step 2: Add an Android App to your Project**
1.  In your Firebase project dashboard, click the Android icon to add a new Android app.
2.  **Package Name:** Use `com.example.matchinghandapp`. You can find this in your `app/build.gradle.kts` file (`applicationId`).
3.  **App Nickname:** (Optional) Enter a name like "Matching Hands".
4.  **Debug Signing Certificate (SHA-1):** This is required for Google Sign-In and other Firebase services.
    -   In Android Studio, open the **Gradle** tab on the right side.
    -   Navigate to **MatchingHands -> app -> Tasks -> android**.
    -   Double-click on `signingReport`.
    -   Find the **SHA-1** key for the `debug` variant and copy-paste it into the Firebase setup form.
5.  Click **Register app**.

**Step 3: Add `google-services.json`**
1.  After registering the app, Firebase will prompt you to download a `google-services.json` file.
2.  Download this file and place it in the **`app`** directory of your Android Studio project (e.g., `MatchingHands/app/google-services.json`).

**Step 4: Enable Firebase Services**
In the Firebase console, navigate to the **Build** section on the left-hand menu:

1.  **Authentication:**
    -   Go to the **Sign-in method** tab.
    -   Click on **Email/Password** and enable it.

2.  **Firestore Database:**
    -   Click **Create database**.
    -   Start in **Test mode**. This allows open access during development. *Remember to secure your rules before production!* 
    -   Choose a location for your database.
    -   Firestore requires a specific composite index for the chat notification query. Click the link below while logged into your Google account, and it will pre-fill the index details for this project. Simply click **Create Index**.
        -   [Create Firestore Index](https://console.firebase.google.com/v1/r/project/matchinghandsapp/firestore/indexes?create_composite=Clxwcm9qZWN0cy9tYXRjaGluZ2hhbmRzYXBwL2RhdGFiYXNlcy8oZGVmYXVsdCkvY29sbGVjdGlvbkdyb3Vwcy9NZXNzYWdlcy9pbmRleGVzL0NJQ0FnT2pYaDRFSxACGggKBHJlYWQQARoOCgpyZWNlaXZlcklkEAEaDAoIX19uYW1lX18QAQ)
        -   **Note:** The index may take a few minutes to build. The app's chat feature may not work correctly until the index is enabled.

3.  **Storage:**
    -   Click **Get started**.
    -   Follow the prompts to create a Cloud Storage bucket. Start with the default security rules for development.

### 4. Add Gemini API Key

The AI matching feature requires an API key from Google AI Studio.

1.  Visit [Google AI Studio](https://aistudio.google.com/).
2.  Click **Get API key** and create a new key.
3.  Open the file `app/src/main/java/com/example/matchinghandapp/utils/GeminiMatchCalculator.java`.
4.  Find the following line:
    ```java
    private static final String API_KEY = "YOUR_API_KEY";
    ```
5.  Replace `"YOUR_API_KEY"` with the key you just generated.
    **Important:** Do not commit your API key to version control. It's recommended to use a secrets management tool or local properties file for production apps.

### 5. Build and Run

1.  Clean and Rebuild your project in Android Studio (**Build -> Clean Project** then **Build -> Rebuild Project**).
2.  Run the app on an emulator or a physical device.

### 6. Manual Data Setup

For the app to function correctly after a fresh setup, you need to create some initial data.

1.  **Create an Admin Account:**
    -   Register a new user in the app.
    -   Go to your Firebase Console -> Firestore Database.
    -   Navigate to the `Users` collection and find the document for the user you just registered.
    -   Change the `role` field from `"volunteer"` to `"admin"`.
    -   Ensure the user has an `active` field set to `true`.
2.  **Update Existing Users (If any):**
    -   As discussed, ensure all user documents in your `Users` collection have a boolean field named `active` and it is set to `true`. Otherwise, they will not be visible in the app.
