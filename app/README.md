# Laera Leika (Kotlin Android)
A native Android app (Kotlin + Jetpack + Navigation) for teaching children language skills through various mini-games. This project demonstrates:

- Login & Dashboard with session-based or token-based authentication
- Child Selection for teachers
- Game Selection (letters, numbers, locate)
- Game Screen with dynamic images & audio

## Table of Contents
- [Project Overview](#project-overview)
- [Features](#features)
- [Requirements](#requirements)
- [Setup & Installation](#setup--installation)
- [Architecture](#architecture)
- [Endpoints / API Usage](#endpoints--api-usage)
- [Libraries & Dependencies](#libraries--dependencies)
- [License](#license)

## Project Overview
Laera Leika is an Android app designed for teachers and children, allowing them to learn letters, numbers, and more through interactive games. The app communicates with a Spring Boot backend (on Render) for data and media files (images, audio).

Main modules:

- Login: Authenticates an admin (teacher) and stores session/cookie if needed.
- Dashboard: Displays children for a given admin’s school, plus a link to child selection and game selection.
- Child Selection: Lets the teacher pick which children they want to manage.
- Game Selection: Chooses which game a specific child will play.
- Game Screen: Dynamically fetches question data, images, and audio from the backend.

## Features
- Login with a warm-up request to handle “cold starts” on the backend.
- Dashboard that loads school name and children (either all or filtered).
- Child Selection that fetches all children in a teacher’s school and allows multiple selection.
- Game Selection that lists available games (letters, numbers, locate).
- Game Screen that loads a random question from the backend, displays images, plays audio, and handles correct/wrong feedback.

## Requirements
- Android Studio (version Arctic Fox or newer recommended)
- Gradle (the project uses Gradle 8.x or higher)
- Kotlin 2.x (or your specified version)
- Internet Access (for connecting to the backend on Render)

## Setup & Installation
1. Clone the Repo
```bash
Copy
git clone https://github.com/yourusername/laera-leika-kotlin.git
cd laera-leika-kotlin
```
2. Open in Android Studio
  - Open Android Studio.
  - Select File > Open and navigate to the project folder.
3. Sync Gradle
  - Android Studio should automatically prompt you to sync.
  - Wait until Gradle sync completes successfully.
4. Run on Emulator or Device
  - Configure an Android Virtual Device (AVD) or plug in a physical device.
  - Click the Run button in Android Studio.
  - The app should launch, presenting a login screen.
5. Configure the Backend
  - By default, the project points to https://hugbotvo.onrender.com.
  - If your backend is at a different URL, update RetrofitClient.kt.

## Architecture
- Kotlin + Android: The entire codebase uses Kotlin with Jetpack libraries.
- MVVM / Clean-ish: The project is fairly simple, but you can adapt MVVM or other patterns.
- Navigation Component: Each screen is a Fragment. We have a nav_graph.xml that defines routes:
  - LoginFragment → DashboardFragment → ChildSelectionFragment → GameSelectionFragment → GameFragment.
- Retrofit for networking: Manages calls to endpoints like /api/auth/login, /api/admins/{adminId}, etc.
- Glide for image loading: Dynamically fetches images from /getImage.
- MediaPlayer for audio: Streams from /playAudio with the correct ID.

## Endpoints / API Usage
Backend is a Spring Boot server. The relevant endpoints:
- POST /api/auth/login: Authenticates admin, returns adminId.
- GET /api/admins/{adminId}/children/all: Returns all children in that admin’s school.
- GET /api/admins/{adminId}/children/{childId}/games?gameType=letters|numbers|locate: Returns random question data.
- GET /getImage?id=xx: Returns image bytes (PNG).
- GET /playAudio?id=xx: Returns audio bytes (MP3).
Check ApiService.kt for specifics.

## Libraries & Dependencies
- Jetpack
  - Navigation Component (androidx.navigation.fragment.ktx, androidx.navigation.ui.ktx)
  - ViewModel / LiveData (optional, can be used if you prefer MVVM)
- Retrofit + Gson + OkHttp (for networking)
- Glide (for image loading)
- MediaPlayer (for audio streaming)
- KSP (for annotation processing, e.g. if you’re using Room)

build.gradle snippet:
```kotlin
Copy
dependencies {
implementation("com.github.bumptech.glide:glide:4.15.1")
ksp("com.github.bumptech.glide:compiler:4.15.1")
// ...
}
```

## License
```sql
MIT License

Copyright (c) 2025 ...

Permission is hereby granted, free of charge, to any person obtaining a copy
...
```