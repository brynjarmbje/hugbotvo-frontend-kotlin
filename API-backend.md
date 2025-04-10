# Born Laera Leikur API Documentation

This document provides a comprehensive guide to all available endpoints in the Born Laera Leikur API. It is intended for frontend developers who need to integrate with our backend services.

## Table of Contents

- [Authentication](#authentication)
- [Supervisor Functions](#supervisor-functions)
- [Admin Management](#admin-management)
- [Question Management](#question-management)
- [Game Management](#game-management)
- [Game Sessions](#game-sessions)
- [Points Management](#points-management)

## Authentication

#### Login

Authenticates a user and creates a session.

- URL: ⁠/api/auth/login
- Method: ⁠POST
  Request Body:

```JSON
{
"username": "string",
"password": "string"
}
```

Success Response:

```JSON
{
"message": "Login successful",
"username": "string",
"isSupervisor": boolean,
"adminId": number
}
```

Error Response: ⁠401 Unauthorized if credentials are incorrect

#### Ping

Checks if the server is running.

- URL: ⁠/api/ping
- Method: ⁠GET
  Success Response: ⁠"pong"

#### Logout

Ends the current session.

- URL: ⁠/api/logout
- Method: ⁠POST
  Success Response:

```JSON
{
"message": "User logged out successfully"
}
```

### Supervisor Functions

#### Get Supervisor Dashboard

Retrieves dashboard data for a supervisor.

- URL: ⁠/api/supervisor/{adminId}/dashboard
- Method: ⁠GET
  Success Response: Dashboard data including school information, children, and admins

#### Create Child

Creates a new child.

- URL: ⁠/api/supervisor/child/create
- Method: ⁠POST
- Query Parameters:
- ⁠adminId: ID of the admin
- Request Body: Child object
  Success Response: Created child object

#### Delete Child

Deletes a child.

- URL: ⁠/api/supervisor/child/{id}
- Method: ⁠DELETE
- Query Parameters:
- ⁠adminId: ID of the admin
  Success Response: ⁠204 No Content

#### Get All Children in School

Retrieves all children in the supervisor's school.

- URL: ⁠/api/supervisor/{adminId}/children
- Method: ⁠GET
  Success Response: List of all children in the school

#### Create Admin

Creates a new admin.

- URL: ⁠/api/supervisor/admin/create
- Method: ⁠POST
- Query Parameters:
- ⁠adminId: ID of the supervisor
- Request Body: Admin object
  Success Response: Created admin object

#### Delete Admin

Deletes an admin.

- URL: ⁠/api/supervisor/admin/{id}
- Method: ⁠DELETE
- Query Parameters:
- ⁠adminId: ID of the supervisor
  Success Response: ⁠204 No Content

#### Get All Admins in School

Retrieves all admins in the supervisor's school.

- URL: ⁠/api/supervisor/{adminId}/admins
- Method: ⁠GET
  Success Response: List of all admins in the school

#### Change Admin Password

Changes an admin's password.

- URL: ⁠/api/supervisor/admin/change-password
- Method: ⁠POST
- Query Parameters:
- ⁠adminId: ID of the supervisor
- ⁠id: ID of the admin to change
- ⁠newPassword: New password
  Success Response: ⁠"Password changed successfully"

### Update Session Attributes

#### Updates session attributes.

- URL: ⁠/api/supervisor/update-session
- Method: ⁠POST
- Query Parameters:
- ⁠username: Username
- ⁠adminId: (Optional) ID of the admin
  Success Response: ⁠"Session updated successfully"

#### Clear Session

Clears the current session.

- URL: ⁠/api/supervisor/clear-session
- Method: ⁠POST
  Success Response: ⁠"Session cleared"

#### Check Session Status

Checks the status of the current session.

- URL: ⁠/api/supervisor/session-status
- Method: ⁠GET
  Success Response: List of session attribute names

### Admin Management

#### Get Admin Dashboard

Retrieves dashboard data for an admin.

- URL: ⁠/api/admins/{adminId}
- Method: ⁠GET
  Success Response: Dashboard data including managed children and school information

#### Add Child to Admin Group

Adds a child to an admin's managed group.

- URL: ⁠/api/admins/{adminId}/children
- Method: ⁠POST
- Query Parameters:
- ⁠childId: ID of the child to add
  Success Response:

```JSON
{
"message": "Child added to the group successfully!"
}
```

#### Remove Child from Admin Group

Removes a child from an admin's managed group.

- URL: ⁠/api/admins/{adminId}/children/{childId}
- Method: ⁠DELETE
  Success Response:

```JSON
{
"message": "Child removed from the group successfully!"
}
```

#### Clear All Children from Admin Group

Removes all children from an admin's managed group.

- URL: ⁠/api/admins/{adminId}/children
- Method: ⁠DELETE
  Success Response:

```JSON
{
"message": "Group cleared successfully!"
}
```

#### Select Child to Play

Simulates selecting a child for gameplay.

- URL: ⁠/api/admins/{adminId}/select-child
- Method: ⁠POST
- Query Parameters:
- ⁠childId: ID of the child to select
  Success Response:

```JSON
{
"message": "Child selected for game session",
"childId": number
}
```

#### Get All Children for Admin's School

Retrieves all children in the admin's school.

- URL: ⁠/api/admins/{adminId}/children/all
- Method: ⁠GET
  Success Response: List of all children in the school

#### Get Unmanaged Children

Retrieves children not currently managed by the admin.

- URL: ⁠/api/admins/{adminId}/children/unmanaged
- Method: ⁠GET
  Success Response: List of unmanaged children

#### Get School Name

Retrieves the name of the admin's school.

- URL: ⁠/api/admins/{adminId}/school
- Method: ⁠GET
  Success Response:

```JSON
{
"schoolName": "string"
}
```

#### Get Managed Children

Retrieves children currently managed by the admin.

- URL: ⁠/api/admins/{adminId}/children/managed
- Method: ⁠GET
  Success Response: List of managed children

### Child Management

#### Get Child Info

Retrieves information about a specific child.

- URL: ⁠/api/admins/{adminId}/children/{childId}
- Method: ⁠GET
  Success Response:

```JSON
{
"adminId": number,
"childId": number,
"childName": "string"
}
```

#### Start Letters Game

Starts a letters game for a child.

- URL: ⁠/api/admins/{adminId}/children/{childId}/games/letters
- Method: ⁠GET
  Success Response:

```JSON
{
"adminId": number,
"childId": number,
"gameType": 1,
"gameName": "letters",
"message": "Game started successfully"
}
```

#### Start Numbers Game

Starts a numbers game for a child.

- URL: ⁠/api/admins/{adminId}/children/{childId}/games/numbers
- Method: ⁠GET
  Success Response:

```JSON
{
"adminId": number,
"childId": number,
"gameType": 2,
"gameName": "numbers",
"message": "Game started successfully"
}
```

#### Start Locate Game

Starts a locate game for a child.

- URL: ⁠/api/admins/{adminId}/children/{childId}/games/locate
- Method: ⁠GET
  Success Response:

```JSON
{
"adminId": number,
"childId": number,
"gameType": 5,
"gameName": "locate",
"message": "Game started successfully"
}
```

### Question Management

#### Get Question Image

Retrieves the image for a question.

- URL: ⁠/getImage
- Method: ⁠GET
- Query Parameters:
- ⁠id: ID of the question
  Success Response: Image data with content type ⁠image/png

#### Play Question Audio

Plays the audio for a question.

- URL: ⁠/playAudio
- Method: ⁠GET
- Query Parameters:
- ⁠id: ID of the question
  Success Response: Audio data with content type ⁠audio/mpeg

### Game Management

#### Start Game

Starts a game with options filtered by child's points.

- URL: ⁠/api/admins/{adminId}/children/{childId}/games
- Method: ⁠GET
- Query Parameters:
- ⁠gameType: Type of game to start (letters, numbers, locate)
  Success Response:

```JSON
{
"adminId": number,
"childId": number,
"gameType": number,
"correctId": number,
"optionIds": [number, number, number],
"message": "Game loaded successfully"
}
```

```NOTE
Note: This endpoint only returns questions where the child's points are greater than or equal to the question's points.
```

### Game Sessions

#### Start Game Session

Starts a new game session.

- URL: ⁠/api/admins/{adminId}/children/{childId}/games/{gameId}/sessions/start
- Method: ⁠POST
  Success Response:

```JSON
{
"message": "Game session started successfully",
"sessionId": number,
"currentLevel": number,
"totalGamePoints": number
}
```

#### Record Answer

Records an answer for a question in the current session.

- URL: ⁠/api/admins/{adminId}/children/{childId}/games/{gameId}/sessions/{sessionId}/answer
- Method: ⁠POST
- Query Parameters:
- ⁠questionId: ID of the question
- ⁠optionChosen: ID of the chosen option
- ⁠correctOption: ID of the correct option
- ⁠isCorrect: Whether the answer was correct
  Success Response:

```JSON
{
"message": "Answer recorded",
"currentSessionPoints": number,
"pointsEarned": number,
"totalGamePoints": number,
"isCorrect": boolean
}
```

#### End Game Session

Ends a game session and finalizes points.

- URL: ⁠/api/admins/{adminId}/children/{childId}/games/{gameId}/sessions/{sessionId}/end
- Method: ⁠POST
  Success Response:

```JSON
{
"message": "Game session ended successfully",
"sessionId": number,
"sessionPoints": number,
"correctAnswers": number,
"totalQuestions": number,
"totalGamePoints": number
}
```

#### Get Session Details

Retrieves details about a game session.

- URL: ⁠/api/admins/{adminId}/children/{childId}/games/{gameId}/sessions/{sessionId}
- Method: ⁠GET
  Success Response:

```JSON
{
"session": {
// Session details
},
"attempts": [
// List of question attempts
]
}
```

#### Get Latest Sessions

Retrieves the latest game sessions for a child and game type.

- URL: ⁠/api/admins/{adminId}/children/{childId}/games/{gameId}/sessions/latest
- Method: ⁠GET
  Success Response: Information about the latest sessions

### Points Management

#### Get Child Points by Game Type

Retrieves points for a child for a specific game type.

- URL: ⁠/api/points/children/{childId}/games/{gameType}
- Method: ⁠GET
  Success Response:

```JSON
{
"childId": number,
"gameType": number,
"points": number
}
```

#### Get All Child Points

Retrieves all points for a child across all categories.

- URL: ⁠/api/points/children/{childId}
- Method: ⁠GET
  Success Response: Map of category names to point values

#### Add Points to Child

Adds points to a child for a specific game type.

- URL: ⁠/api/points/children/{childId}/games/{gameType}/add
- Method: ⁠POST
- Query Parameters:
- ⁠points: Points to add
  Success Response:

```JSON
{
"message": "Points added successfully",
"childId": number,
"gameType": number,
"addedPoints": number,
"totalPoints": number
}
```

#### Set Child Points

Sets points for a child for a specific game type (overwrites previous value).

- URL: ⁠/api/points/children/{childId}/games/{gameType}
- Method: ⁠PUT
- Query Parameters:
- ⁠points: New points value
  Success Response:

```JSON
{
"message": "Points set successfully",
"childId": number,
"gameType": number,
"points": number
}
```

#### Get Total Child Points

Retrieves the total points for a child across all categories.

- URL: ⁠/api/points/children/{childId}/total
- Method: ⁠GET
  Success Response:

```JSON
{
"childId": number,
"totalPoints": number
}
```
