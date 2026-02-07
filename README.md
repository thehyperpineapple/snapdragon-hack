<p align="center">
  <img src="logo.png" alt="FuelForm Logo" width="180"/>
</p>

<h1 align="center">FuelForm</h1>

FuelForm is an adaptive fitness and nutrition planning system that automates workout and dietary decisions using AI and machine learning. The goal of the application is to reduce the cognitive burden of planning workouts and meals by continuously adapting recommendations based on user behavior, recovery, and progress.

FuelForm models fitness as a dynamic system where a user’s physical state evolves over time. The application uses three AI/ML models working together across devices to deliver personalized, low-effort fitness optimization.

---

## Application Description

FuelForm consists of an Android application and a laptop-based AI system connected through Firebase.

The Android app collects user health data through Android Health Connect and runs a lightweight general AI model locally to handle fast, on-device decisions and coordination. The phone syncs user state data to Firebase, which serves as the backend and communication layer.

A connected laptop acts as the primary compute hub and runs two dedicated AI/ML models:
- A **Nutrition Model** that adapts calorie targets, macro emphasis, and hydration goals
- A **Fitness Model** that adapts workout intensity, duration, exercise category, and rest days

Together, these models continuously adjust recommendations based on user activity, recovery, and adherence, minimizing manual planning while promoting sustainable long-term progress.

---

## Team Members

- **Shruti Bhamidipati**  
  Email: sb5197@columbia.edu

- **Raghav Sampath**  
  Email: rs4760@columbia.edu
  
- **Kartik Kumar Gounder**  
  Email: kkg2125@columbia.edu
  
- **Anushk Pandey**  
  Email: ap7151@nyu.edu
  
- **Aditya Pendyala**  
  Email: ap4839@columbia.edu

---

## System Architecture

- **Android App (`fuelform.apk`)**
  - Runs a general AI model locally on the device
  - Reads health and activity data via Android Health Connect
  - Displays workout and nutrition recommendations
  - Syncs user state with Firebase

- **Laptop (AI Compute Node)**
  - Runs the Nutrition AI/ML model
  - Runs the Fitness AI/ML model
  - Reads user state from Firebase
  - Writes updated recommendations back to Firebase

- **Firebase Backend**
  - Stores user state and history
  - Synchronizes data between the phone and laptop
  - Enables real-time updates across devices

---

## Setup Instructions

### Prerequisites

**Android Phone**
- Android 9 (API level 28) or higher
- Health Connect installed and enabled
- USB debugging enabled (for APK installation)

**Laptop**
- Python 3.9 or higher
- Git
- Internet connection

---

### Android App Setup

1. Install the FuelForm APK on the Android device:
   ```bash
   adb install fuelform.apk
    ```

2. Open the app and grant permissions for:

   * Activity and exercise data
   * Sleep data
   * Nutrition and hydration data
   * Body metrics (such as weight)

3. Ensure Android Health Connect is enabled and has granted FuelForm read access.

---

### Laptop Setup (AI/ML Models)

1. Clone the project repository:

   ```bash
   git clone https://github.com/<your-org>/fuelform.git
   cd fuelform
   ```

2. Create and activate a Python virtual environment:

   ```bash
   python3 -m venv venv
   source venv/bin/activate
   ```

3. Install required dependencies:

   ```bash
   pip install -r requirements.txt
   ```

4. Set up Firebase:

   * Create a Firebase project
   * Generate a Firebase service account key
   * Save the key as `firebase_key.json` in the project root

5. Export Firebase credentials:

   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=firebase_key.json
   ```

---

## Running the System

### Start Laptop-Based Models

1. Start the Fitness AI model:

   ```bash
   python models/fitness_model/run.py
   ```

2. Start the Nutrition AI model:

   ```bash
   python models/nutrition_model/run.py
   ```

Both models continuously listen for user state updates from Firebase and publish updated recommendations.

---

### Running the Android App

1. Launch the FuelForm app on the Android phone.

2. The app performs the following steps:

   * Reads health data from Android Health Connect
   * Runs the local general AI model for lightweight inference
   * Syncs user state to Firebase
   * Fetches updated workout and nutrition recommendations

3. Recommendations are displayed directly in the app UI.

---

## Usage Instructions

1. Open the FuelForm app daily.
2. Allow the app to sync health data automatically.
3. Review recommended workouts and nutrition targets.
4. Follow the plan or log deviations as needed.
5. The system adapts automatically over time based on adherence, recovery, and progress.

No manual planning is required from the user.

---

## Models Overview

* **General AI Model (Android Device)**

  * Runs locally on the phone
  * Handles lightweight inference and coordination
  * Enables low-latency and offline-capable behavior

* **Nutrition AI Model (Laptop)**

  * Adjusts calorie targets, macro emphasis, and hydration
  * Responds to training load and body composition trends

* **Fitness AI Model (Laptop)**

  * Selects training intensity, duration, exercise type, and rest days
  * Adapts based on recovery, fatigue, and adherence patterns

---

## Notes

FuelForm is a research and development prototype designed to demonstrate adaptive, AI-driven fitness planning using real-world health data and a multi-device architecture.

---

## License

MIT License
