# Chatterrr
Chatterrr is a modern Android chat application built with Jetpack Compose, Firebase, Hilt, and the MVVM architectural pattern.

## Features
- Current Features:

- Real-time Messaging: Send and receive text messages in real-time within channels.

- Image Sharing: Share images directly within chat conversations.

- Video Sharing: Share videos directly within chat conversations.

- Channel Management: Create and view chat channels.

- Channel Search: Search for existing channels by name.

## Future Features:

- Voice Calls

- Video Calls

## Technologies Used
- Jetpack Compose: Modern Android UI toolkit for building native interfaces.

- Firebase: Backend services including:

- Firebase Authentication

- Firebase Realtime Database

- Firebase Storage

- Hilt: Dependency injection library for Android, built on top of Dagger.

- MVVM (Model-View-ViewModel): Architectural pattern for building robust and maintainable applications.

## Architecture
- The application follows the MVVM architectural pattern, promoting separation of concerns and testability.

- Model: Represents the data and business logic (e.g., Message, Channel data classes).

- View: The UI layer built with Jetpack Compose (e.g., ChatScreen, HomeScreen). Observes changes in the ViewModel.

- ViewModel: Holds UI state, interacts with the data layer (Firebase), and exposes data streams to the View.

## Setup and Installation
To set up and run Chatterrr locally:

- ### Clone the repository:

  - git clone <https://github.com/gitInfinity/ChatApp.git>

  - ### Set up Firebase:

    - Create a new Firebase project in the Firebase console.

    - Add an Android app to your Firebase project.

    - Download the google-services.json file and place it in the app/ directory of your Android project.
    
    - Enable Firebase Authentication, Realtime Database, and Cloud Storage in your Firebase project.
    
    - Configure Firebase Rules: Set up appropriate security rules for your Realtime Database and Cloud Storage.
    
    - Run the application: Open the project in Android Studio and run it on an emulator or physical device.
