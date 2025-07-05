# To-do List REST API

This project is a RESTful API for a simple to-do list application, built with Spring Boot. It provides core
functionalities including user registration, JWT-based authentication, and protected CRUD operations for tasks.

---

# Features

- **User Authentication**: Secure user registration and login.
- **JWT-Based Security**: API endpoints protected using JSON Web Tokens.
- **Task Management**: Full CRUD (Create, Read, Update, Delete) functionality for user-specific tasks.
- **Validation**: Input data validation to ensure data integrity.
- **Centralized Error Handling**: Consistent, clear error responses.

---

# Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring Security** for authentication and authorization
- **Spring Data JPA (Hibernate)** for data persistence
- **PostgreSQL** as the relational database
- **Gradle** as the build tool
- **Lombok** to reduce boilerplate code

---

# Prerequisites

Before you begin, ensure you have:

1. **JDK 21**
2. **A running PostgreSQL instance**
3. **An IDE** (e.g., IntelliJ IDEA or VS Code)

---

# Getting Started

## 1. Clone the Repository

```bash
git clone <your-repository-url>
cd <your-project-directory>
```

## 2. Configure the Database

1. Ensure your PostgreSQL server is running.

2. Create a new database:

```bash
CREATE DATABASE api_task_tracker;
```

3. The **init_db.sql** script can set up initial tables if needed, but **spring.jpa.hibernate.ddl-auto=update** will
   auto-manage the schema.

## 3. Set Environment Variables

This project requires two environment variables:

- DB_PASSWORD – Password for your PostgreSQL user (e.g., postgres).

- JWT_SECRET – A long, random string used to sign JWTs.

#### Example (IntelliJ IDEA):

Go to Run → Edit Configurations… → Application → [your-app-name] → Environment variables.

## 4. Run the Application

You can run the application using the Gradle wrapper:

```bash
./gradlew bootRun
```

The API will be available at:
http://localhost:8080

## API Documentation

All endpoints consume and return JSON.

## Authentication Endpoints

Base Path: /api/auth

### 1. Register a New User

- **Endpoint:** POST /api/auth/register
- **Description:** Creates a new user account.

- **Request Body**:

```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "password123"
}
```

- **Success Response (201 Created):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

- Error Responses:

    - 400 Bad Request – Validation fails (e.g., blank fields, invalid email).
    - 409 Conflict – Email already in use.

### 2. Log In

- **Endpoint**: POST /api/auth/login
- **Description**: Authenticates a user and returns a JWT.
- **Request Body:**

```json
{
  "email": "john.doe@example.com",
  "password": "password123"
}
```

- **Success Response (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

- **Error Response:**

    - 401 Unauthorized – Incorrect credentials.

## Task Endpoints

**Note:** All task endpoints require a valid JWT in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
Base Path: /api/tasks
```

### 1. Create a Task

- **Endpoint:** POST /api/tasks
- **Description:** Creates a new task for the authenticated user.
- **Request Body:**

```json
{
  "title": "Implement README",
  "description": "Write a comprehensive README for the project.",
  "status": "IN_PROGRESS"
}
```

- **Success Response (201 Created):**

```json
{
  "id": 1,
  "title": "Implement README",
  "description": "Write a comprehensive README for the project.",
  "status": "IN_PROGRESS",
  "createdAt": "2023-10-27T10:00:00.000000",
  "updatedAt": "2023-10-27T10:00:00.000000",
  "userId": 123,
  "userEmail": "john.doe@example.com"
}
```

### 2. Get All Tasks (Paginated)

* **Endpoint**: `GET /api/tasks`
* **Description**: Retrieves a paginated list of tasks belonging to the authenticated user.
* **Query Parameters**:
    * `status`: Filters tasks by their status (e.g., `TO_DO`, `IN_PROGRESS`, `DONE`).
    * `page`: The page number to retrieve (0-indexed).
    * `size`: The number of items per page.
    * `sort`: A property to sort by, followed by direction (e.g., `createdAt,desc`).
* **Defaults**: If no parameters are provided, the API defaults to `page=0`, `size=10`, and sorts by `createdAt`
  descending.
* **Success Response (200 OK)**:
  ```json
  {
      "content": [
          {
              "id": 1,
              "title": "Implement README",
              "description": "Write a comprehensive README for the project.",
              "status": "IN_PROGRESS",
              "createdAt": "2023-10-27T10:00:00.000000",
              "updatedAt": "2023-10-27T10:00:00.000000",
              "userId": 123,
              "userEmail": "john.doe@example.com"
          }
      ],
      "pageable": {
          "pageNumber": 0,
          "pageSize": 10,
          "sort": {
              "sorted": true,
              "unsorted": false,
              "empty": false
          },
          "offset": 0,
          "paged": true,
          "unpaged": false
      },
      "last": true,
      "totalPages": 1,
      "totalElements": 1,
      "size": 10,
      "number": 0,
      "sort": {
          "sorted": true,
          "unsorted": false,
          "empty": false
      },
      "first": true,
      "numberOfElements": 1,
      "empty": false
  }
  ```

### 3. Update a Task

- **Endpoint:** PUT /api/tasks/{id}
- **Description:** Updates an existing task (only your own).
- **Request Body:**

```json
{
  "title": "Implement a Great README",
  "description": "Write a comprehensive and professional README for the project.",
  "status": "DONE"
}
```

- **Success Response (200 OK):** Returns the updated Task object.
- **Error Response:**
    - 404 Not Found – Task doesn’t exist or isn’t yours.

### 4. Delete a Task

- **Endpoint:** DELETE /api/tasks/{id}
- **Description:** Deletes a task (only your own).
- **Success Response:** 204 No Content
- **Error Response:**
    - 404 Not Found – Task doesn’t exist or isn’t yours.
